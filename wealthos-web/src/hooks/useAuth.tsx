import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from 'react';
import { supabase } from '../lib/supabase';
import type { Session, User } from '@supabase/supabase-js';

interface AuthContextType {
  session: Session | null;
  user: User | null;
  loading: boolean;
  signInWithOtp: (phone: string) => Promise<{ error: Error | null; isDemoFallback?: boolean }>;
  verifyOtp: (phone: string, token: string) => Promise<{ error: Error | null }>;
  loginAsDemo: () => void;
  signOut: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

const DEMO_SESSION_KEY = 'wealthos_demo_session';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const [demoPendingPhone, setDemoPendingPhone] = useState<string | null>(null);

  useEffect(() => {
    // 1. Check local demo session first
    const savedDemo = localStorage.getItem(DEMO_SESSION_KEY);
    if (savedDemo) {
      try {
        const parsed = JSON.parse(savedDemo);
        setSession({
          access_token: 'demo-access-token',
          token_type: 'bearer',
          expires_in: 31536000,
          refresh_token: 'demo-refresh-token',
          user: parsed,
        } as unknown as Session);
        setLoading(false);
        return;
      } catch (_) {}
    }

    // 2. Check Supabase session
    try {
      supabase.auth.getSession().then(({ data: { session } }) => {
        if (session) {
          setSession(session);
        }
        setLoading(false);
      }).catch(() => {
        setLoading(false);
      });

      const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
        if (session) {
          setSession(session);
        }
      });

      return () => subscription.unsubscribe();
    } catch {
      setLoading(false);
    }
  }, []);

  const signInWithOtp = useCallback(async (phone: string) => {
    try {
      const { error } = await supabase.auth.signInWithOtp({ phone });
      if (error) {
        // If Supabase SMS provider is not active or credentials not set, fallback to dev OTP
        console.warn('Supabase OTP notice:', error.message, 'Enabling local dev OTP fallback.');
        setDemoPendingPhone(phone);
        return { error: null, isDemoFallback: true };
      }
      return { error: null };
    } catch (err: any) {
      setDemoPendingPhone(phone);
      return { error: null, isDemoFallback: true };
    }
  }, []);

  const verifyOtp = useCallback(async (phone: string, token: string) => {
    // 1. Try authenticating directly with Supabase first
    try {
      const { data, error } = await supabase.auth.verifyOtp({ phone, token, type: 'sms' });
      if (!error && data?.session) {
        localStorage.removeItem(DEMO_SESSION_KEY);
        setSession(data.session);
        setDemoPendingPhone(null);
        return { error: null };
      }
      if (error) {
        console.warn('Supabase verifyOtp notice:', error.message);
      }
    } catch (err: any) {
      console.warn('Supabase auth network notice:', err.message);
    }

    // 2. Fallback to local dev test OTP if token is 123456 or demo mode was triggered
    if (demoPendingPhone === phone || token === '123456') {
      const rawDigits = phone.replace(/\D/g, '').slice(-12).padStart(12, '0');
      const demoUuid = `00000000-0000-4000-8000-${rawDigits}`;

      const demoUser: User = {
        id: demoUuid,
        app_metadata: { provider: 'phone' },
        user_metadata: { phone },
        aud: 'authenticated',
        created_at: new Date().toISOString(),
        phone,
      } as unknown as User;

      const mockSession = {
        access_token: 'demo-access-token',
        token_type: 'bearer',
        expires_in: 31536000,
        refresh_token: 'demo-refresh-token',
        user: demoUser,
      } as unknown as Session;

      localStorage.setItem(DEMO_SESSION_KEY, JSON.stringify(demoUser));
      setSession(mockSession);
      setDemoPendingPhone(null);
      return { error: null };
    }

    return { error: new Error('Invalid verification code. Please check code or use dev OTP 123456.') };
  }, [demoPendingPhone]);

  const loginAsDemo = useCallback(() => {
    const demoUser: User = {
      id: '00000000-0000-4000-8000-000000000001',
      app_metadata: { provider: 'phone' },
      user_metadata: { phone: '+919876543210', name: 'Demo User' },
      aud: 'authenticated',
      created_at: new Date().toISOString(),
      phone: '+919876543210',
    } as unknown as User;

    localStorage.setItem(DEMO_SESSION_KEY, JSON.stringify(demoUser));
    setSession({
      access_token: 'demo-token',
      token_type: 'bearer',
      expires_in: 31536000,
      refresh_token: 'demo-refresh',
      user: demoUser,
    } as unknown as Session);
  }, []);

  const signOut = useCallback(async () => {
    try {
      await supabase.auth.signOut();
    } catch (_) {}
    localStorage.removeItem(DEMO_SESSION_KEY);
    setSession(null);
  }, []);

  return (
    <AuthContext.Provider value={{
      session,
      user: session?.user ?? null,
      loading,
      signInWithOtp,
      verifyOtp,
      loginAsDemo,
      signOut,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
