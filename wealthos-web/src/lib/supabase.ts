/// <reference types="vite/client" />
import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL || 'https://zahzagnindkkttdpmqii.supabase.co';
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY || '';

if (!SUPABASE_ANON_KEY && import.meta.env.PROD) {
  console.error('[WealthOS Security] VITE_SUPABASE_ANON_KEY is not configured in production build!');
}

export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY || 'public-anon-key', {
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
