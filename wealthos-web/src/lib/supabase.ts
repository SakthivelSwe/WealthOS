/// <reference types="vite/client" />
import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL || 'https://zahzagnindkkttdpmqii.supabase.co';
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY;

if (!SUPABASE_ANON_KEY) {
  if (import.meta.env.PROD) {
    // In production we cannot silently proceed with a fake key — all writes would
    // appear to succeed locally but never reach Supabase, causing invisible data loss.
    throw new Error(
      '[WealthOS] FATAL: VITE_SUPABASE_ANON_KEY environment variable is required in production. ' +
      'Set it in your Cloudflare Pages / deployment environment variables.'
    );
  } else {
    console.warn(
      '[WealthOS] VITE_SUPABASE_ANON_KEY is not configured. ' +
      'Cloud sync and authentication will not work. Set it in wealthos-web/.env.local.'
    );
  }
}

export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY ?? '', {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
    storage: localStorage,
  },
  realtime: {
    params: {
      eventsPerSecond: 10,
    },
  },
});

export const SUPABASE_URL_VALUE = SUPABASE_URL;

/** True only when we have a real non-empty anon key configured. */
export const isSupabaseConfigured = Boolean(SUPABASE_ANON_KEY);
