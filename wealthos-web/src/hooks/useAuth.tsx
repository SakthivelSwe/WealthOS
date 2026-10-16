import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from 'react';
import { supabase } from '../lib/supabase';
import type { Session, User } from '@supabase/supabase-js';

interface AuthContextType {
  session: Session | null;
  user: User | null;
  loading: boolean;
  isDemoFallback: boolean;
  signInWithOtp: (target: string, isEmail?: boolean) => Promise<{ error: Error | null; isDemoFallback?: boolean }>;
  verifyOtp: (target: string, token: string, isEmail?: boolean) => Promise<{ error: Error | null }>;
  loginAsDemo: () => void;
  signOut: () => Promise<void>;
  deleteAllUserData: () => Promise<{ error: Error | null }>;
}

const AuthContext = createContext<AuthContextType | null>(null);

const DEMO_SESSION_KEY = 'wealthos_demo_session';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);
  const [demoPendingPhone, setDemoPendingPhone] = useState<string | null>(null);
  const [isDemoFallback, setIsDemoFallback] = useState(false);

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

  const signInWithOtp = useCallback(async (target: string, isEmail: boolean = false) => {
    try {
      const payload: any = isEmail
        ? {
            email: target,
            options: {
              emailRedirectTo: typeof window !== 'undefined' ? `${window.location.origin}/login` : 'https://wealthos-dw9.pages.dev/login',
            },
          }
        : { phone: target };
      const { error } = await supabase.auth.signInWithOtp(payload);
      if (error) {
        if (!isEmail) {
          // Supabase SMS provider not configured – use dev/demo OTP fallback
          const isProviderError = error.message?.toLowerCase().includes('sms') ||
            error.message?.toLowerCase().includes('provider') ||
            error.message?.toLowerCase().includes('not enabled') ||
            error.message?.toLowerCase().includes('phone') ||
            error.status === 422;
          if (isProviderError) {
            console.warn('[WealthOS] Supabase SMS provider not configured. Using dev OTP fallback.');
            setDemoPendingPhone(target);
            setIsDemoFallback(true);
            return { error: null, isDemoFallback: true };
          }
        }
        return { error: new Error(error.message) };
      }
      setIsDemoFallback(false);
      return { error: null, isDemoFallback: false };
    } catch (err: any) {
      if (!isEmail) {
        console.warn('[WealthOS] Network error sending OTP. Using dev fallback.');
        setDemoPendingPhone(target);
        setIsDemoFallback(true);
        return { error: null, isDemoFallback: true };
      }
      return { error: new Error(err.message || 'Failed to send OTP') };
    }
  }, []);

  const verifyOtp = useCallback(async (target: string, token: string, isEmail: boolean = false) => {
    // 1. Try Supabase real OTP first (production path for both phone and email)
    if (!isDemoFallback || isEmail) {
      try {
        const payload: any = isEmail
          ? { email: target, token, type: 'email' }
          : { phone: target, token, type: 'sms' };
        const { data, error } = await supabase.auth.verifyOtp(payload);
        if (!error && data?.session) {
          localStorage.removeItem(DEMO_SESSION_KEY);
          setSession(data.session);
          setDemoPendingPhone(null);
          return { error: null };
        }
        if (error) {
          return { error: new Error(error.message || 'Invalid verification code. Please check and try again.') };
        }
      } catch (err: any) {
        return { error: new Error('Network error verifying OTP. Please try again.') };
      }
    }

    // 2. Dev/demo fallback – only for phone when SMS provider isn't configured in Supabase
    if (isDemoFallback && !isEmail && demoPendingPhone === target) {
      if (token !== '123456') {
        return { error: new Error('Dev OTP is 123456 (SMS provider not configured in Supabase)') };
      }
      const rawDigits = target.replace(/\D/g, '').slice(-12).padStart(12, '0');
      const demoUuid = `00000000-0000-4000-8000-${rawDigits}`;

      const demoUser: User = {
        id: demoUuid,
        app_metadata: { provider: 'phone' },
        user_metadata: { phone: target },
        aud: 'authenticated',
        created_at: new Date().toISOString(),
        phone: target,
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
      setIsDemoFallback(false);
      return { error: null };
    }

    return { error: new Error('Unable to verify OTP. Please request a new code.') };
  }, [isDemoFallback, demoPendingPhone]);

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
    setIsDemoFallback(false);
  }, []);

  const deleteAllUserData = useCallback(async () => {
    if (!session?.user?.id) return { error: new Error('Not authenticated') };
    const userId = session.user.id;
    try {
      // Delete all user data from Supabase in dependency order
      const tables = [
        'user_notifications',
        'user_journal_entries',
        'user_transactions',
        'user_budgets',
        'user_recurring_transactions',
        'user_chit_funds',
        'user_debts',
        'user_goals',
        'user_accounts',
        'user_categories',
        'user_settings',
      ];
      for (const table of tables) {
        try {
          await supabase.from(table).delete().eq('user_id', userId);
        } catch (e) {
          console.warn(`Could not delete from ${table}:`, e);
        }
      }
      // Delete auth user if using real Supabase auth
      try {
        await supabase.auth.admin?.deleteUser(userId);
      } catch (_) {}
    } catch (e: any) {
      // Even if cloud deletion fails, clear local data
      console.warn('Cloud deletion error:', e);
    }
    // Always clear all local storage
    const keysToRemove = Object.keys(localStorage).filter(k => k.startsWith('wealthos'));
    keysToRemove.forEach(k => localStorage.removeItem(k));
    setSession(null);
    setIsDemoFallback(false);
    return { error: null };
  }, [session]);

  return (
    <AuthContext.Provider value={{
      session,
      user: session?.user ?? null,
      loading,
      isDemoFallback,
      signInWithOtp,
      verifyOtp,
      loginAsDemo,
      signOut,
      deleteAllUserData,
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
