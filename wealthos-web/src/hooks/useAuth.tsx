import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from 'react';
import { supabase } from '../lib/supabase';
import type { Session, User } from '@supabase/supabase-js';

interface AuthContextType {
  session: Session | null;
  user: User | null;
  loading: boolean;
  isDemo: boolean;
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

  const isDemo = Boolean(
    session?.user?.id === '00000000-0000-4000-8000-000000000001' ||
    (typeof window !== 'undefined' && localStorage.getItem(DEMO_SESSION_KEY) && !session?.user?.email)
  );

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
          if (session.user?.email) {
            localStorage.removeItem(DEMO_SESSION_KEY);
          }
          setSession(session);
        }
        setLoading(false);
      }).catch(() => {
        setLoading(false);
      });

      const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
        if (session) {
          if (session.user?.email) {
            localStorage.removeItem(DEMO_SESSION_KEY);
          }
          setSession(session);
        } else if (_event === 'SIGNED_OUT') {
          setSession(null);
        }
      });

      return () => subscription.unsubscribe();
    } catch {
      setLoading(false);
    }
  }, []);

  const signInWithOtp = useCallback(async (target: string, isEmail: boolean = false) => {
    // Dev/demo fallback is STRICTLY restricted to localhost — never activates in production.
    const isLocalDev = typeof window !== 'undefined' &&
      (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1');

    try {
      const payload: any = isEmail
        ? {
            email: target,
            options: {
              emailRedirectTo: typeof window !== 'undefined' ? `${window.location.origin}/` : 'https://wealthos-dw9.pages.dev/',
            },
          }
        : { phone: target };
      const { error } = await supabase.auth.signInWithOtp(payload);
      if (error) {
        const isApiKeyError = error.message?.toLowerCase().includes('api key') ||
          error.message?.toLowerCase().includes('invalid');

        // Email dev fallback: localhost only, never in production
        if (isEmail && isApiKeyError && isLocalDev) {
          console.warn('[WealthOS] Localhost dev: Supabase key not configured — enabling localhost email simulation.');
          setDemoPendingPhone(target);
          setIsDemoFallback(true);
          return { error: null, isDemoFallback: true };
        }

        if (isEmail && isApiKeyError && !isLocalDev) {
          // Production: surface the real error — do not silently fall back
          return { error: new Error('Authentication service configuration error. Please contact support.') };
        }

        if (!isEmail && isLocalDev) {
          // SMS dev fallback: localhost only
          const isProviderError = error.message?.toLowerCase().includes('sms') ||
            error.message?.toLowerCase().includes('provider') ||
            error.message?.toLowerCase().includes('not enabled') ||
            error.message?.toLowerCase().includes('phone') ||
            error.status === 422;
          if (isProviderError) {
            console.warn('[WealthOS] Localhost dev: SMS provider not configured — using dev OTP fallback (123456).');
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
      // Network error SMS fallback: localhost only
      if (!isEmail && isLocalDev) {
        console.warn('[WealthOS] Localhost dev: Network error sending OTP — using dev fallback.');
        setDemoPendingPhone(target);
        setIsDemoFallback(true);
        return { error: null, isDemoFallback: true };
      }
      return { error: new Error(err.message || 'Failed to send OTP. Please check your connection and try again.') };
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
        if (error && !isDemoFallback) {
          return { error: new Error(error.message || 'Invalid verification code. Please check and try again.') };
        }
      } catch (err: any) {
        if (!isDemoFallback) {
          return { error: new Error('Network error verifying OTP. Please try again.') };
        }
      }
    }

    // 2. Dev/demo fallback – for phone or localhost dev without Supabase key
    if (isDemoFallback && demoPendingPhone === target) {
      if (isEmail) {
        const rawId = 'usr_' + btoa(target).replace(/[^a-zA-Z0-9]/g, '').slice(0, 16);
        const emailUser: User = {
          id: rawId,
          email: target,
          app_metadata: { provider: 'email' },
          user_metadata: { email: target, name: target.split('@')[0] },
          aud: 'authenticated',
          created_at: new Date().toISOString(),
        } as unknown as User;

        const emailSession: Session = {
          access_token: 'local-dev-token',
          token_type: 'bearer',
          expires_in: 31536000,
          refresh_token: 'local-dev-refresh',
          user: emailUser,
        } as unknown as Session;

        localStorage.setItem(DEMO_SESSION_KEY, JSON.stringify(emailUser));
        setSession(emailSession);
        setDemoPendingPhone(null);
        setIsDemoFallback(false);
        return { error: null };
      }

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
    const isDemoUser = session.user.id.startsWith('00000000-0000-') || session.user.id.startsWith('usr_');

    const failedTables: string[] = [];

    if (!isDemoUser) {
      // Delete all user data from Supabase in reverse dependency order
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
          const { error } = await supabase.from(table).delete().eq('user_id', userId);
          if (error) {
            console.error(`Failed to delete from ${table}:`, error.message);
            failedTables.push(`${table} (${error.message})`);
          }
        } catch (e: any) {
          console.error(`Exception deleting from ${table}:`, e);
          failedTables.push(`${table} (${e?.message || 'network error'})`);
        }
      }

      // Purge any encrypted backup objects stored in Supabase Storage for this user
      // Storage cleanup failures are tracked and reported — not silently swallowed.
      try {
        const { data: files, error: listErr } = await supabase.storage.from('backups').list(userId);
        if (listErr) {
          failedTables.push(`storage/backups/list (${listErr.message})`);
        } else if (files && files.length > 0) {
          const paths = files.map(f => `${userId}/${f.name}`);
          const { error: storageErr } = await supabase.storage.from('backups').remove(paths);
          if (storageErr) {
            // This is a real failure — user's backup files remain in cloud storage.
            failedTables.push(`storage/backups/remove (${storageErr.message})`);
          }
        }
      } catch (storageErr: any) {
        failedTables.push(`storage/backups (${storageErr?.message || 'unexpected error'})`);
      }

      // If cloud deletion failed, preserve local data so user does not suffer unrecoverable data loss
      if (failedTables.length > 0) {
        return {
          error: new Error(`Cloud deletion could not complete for: ${failedTables.join(', ')}. Local data preserved for recovery. Please retry.`),
        };
      }
    }

    // Cloud records safely deleted (or demo user): now clear user-scoped local storage and sign out
    try {
      await supabase.auth.signOut();
    } catch (_) {}

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
      isDemo,
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
