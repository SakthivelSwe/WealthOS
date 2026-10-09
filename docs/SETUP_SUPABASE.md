# Supabase setup (optional)

The app works fully without Supabase. Supabase is used only for optional encrypted backup storage and the authenticated AI proxy. Project ref: `zahzagnindkkttdpmqii`.

> Never commit or paste secret keys. The Android app only ever receives the project URL and the **publishable** key. The `service_role`/secret key and the Gemini key stay server-side.

## 1. Tools and login
```
scoop install supabase        # or: npx supabase ...
supabase login
supabase link --project-ref zahzagnindkkttdpmqii
```

## 2. Tables, RLS and storage
```
supabase db push
```
This applies [20261007000000_init.sql](../supabase/migrations/20261007000000_init.sql): `profiles`, `backup_objects`, `sync_metadata`, `ai_usage` with RLS (`auth.uid() = user_id`), the atomic `consume_ai_quota` function (service role only), and a private `backups` bucket whose policies require the first path folder to equal the user id.

Check in the dashboard: Table Editor shows RLS **enabled** on all four tables; Storage shows `backups` as private.

## 3. Authentication
Authentication > Providers > Email: enable. Create your user under Authentication > Users. Consider disabling public sign-ups (Authentication > Sign In / Providers) because this is a personal project.

## 4. Gemini secret and model (server-side only)
```
supabase secrets set GEMINI_API_KEY=<your rotated key> GEMINI_MODEL=<model name you choose>
supabase secrets set AI_DAILY_LIMIT=50
```
The model name is read on the server; the app never contains one. Use `supabase secrets list` to confirm names (values are hidden).

## 5. Deploy functions
```
supabase functions deploy ai-analyze ai-categorize health
```

## 6. Test
```
curl https://zahzagnindkkttdpmqii.supabase.co/functions/v1/health
```
Expected `{"status":"ok"}`. Authenticated call (use a real user JWT, not the service key):
```
curl -X POST https://zahzagnindkkttdpmqii.supabase.co/functions/v1/ai-analyze \
  -H "Authorization: Bearer <user access token>" -H "Content-Type: application/json" \
  -d '{"question":"What changed?","periodLabel":"Oct 2026","currency":"INR","aggregates":{"food":{"prev":210000,"now":374000}}}'
```
Expected: 200 with `insights`, `recommendations`, `warnings`, `confidence`. Without the header: 401.

## 7. Verify RLS from an unauthorized user
1. Create a second test user.
2. Using user B's token, `select` from `backup_objects` through the REST API: it must return no rows that belong to user A.
3. Try to download `backups/<user A id>/x` as user B: must be denied.
4. Try `insert into ai_usage` as an authenticated user: must be denied (no insert policy).
5. Call `rpc/consume_ai_quota` as an authenticated user: must be denied (execute revoked).

## 8. App configuration
Put `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` in `local.properties` (git-ignored). Cloud client code is not implemented yet, see PROJECT_STATUS.md.

## Notes
- The circuit breaker and per-minute limiter are per function instance, so they are best-effort. The daily quota in Postgres is the authoritative limit.
- If a key was ever pasted anywhere public, rotate it first.
