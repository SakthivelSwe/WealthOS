/// <reference types="vite/client" />
import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = 'https://zahzagnindkkttdpmqii.supabase.co';
// If VITE_SUPABASE_ANON_KEY is not defined in .env, use a valid JWT structure fallback so createClient initializes safely
const FALLBACK_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InphaHphZ25pbmRra3R0ZHBtcWlpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MDk4Mjk5MDAsImV4cCI6MjAyNTQwNTkwMH0.placeholder';
const SUPABASE_ANON_KEY = import.meta.env.VITE_SUPABASE_ANON_KEY || FALLBACK_ANON_KEY;

export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
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
