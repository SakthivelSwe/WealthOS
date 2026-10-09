-- Private Money OS: minimal server data model. The server only ever stores ciphertext backups
-- and usage counters. Row Level Security is enabled on every table; clients can only touch their own rows.

create table if not exists public.profiles (
  user_id uuid primary key references auth.users (id) on delete cascade,
  created_at timestamptz not null default now()
);

create table if not exists public.backup_objects (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  device_id text not null check (char_length(device_id) between 1 and 128),
  object_path text not null check (char_length(object_path) between 1 and 512),
  ciphertext_hash text not null check (ciphertext_hash ~ '^[0-9a-f]{64}$'),
  size_bytes bigint not null check (size_bytes >= 0 and size_bytes <= 52428800),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, object_path)
);
create index if not exists backup_objects_user_idx on public.backup_objects (user_id, created_at desc);

create table if not exists public.sync_metadata (
  user_id uuid not null references auth.users (id) on delete cascade,
  device_id text not null check (char_length(device_id) between 1 and 128),
  version bigint not null default 0 check (version >= 0),
  last_sync_at timestamptz,
  primary key (user_id, device_id)
);

create table if not exists public.ai_usage (
  user_id uuid not null references auth.users (id) on delete cascade,
  day date not null,
  request_count integer not null default 0 check (request_count >= 0),
  primary key (user_id, day)
);

alter table public.profiles enable row level security;
alter table public.backup_objects enable row level security;
alter table public.sync_metadata enable row level security;
alter table public.ai_usage enable row level security;

-- profiles
create policy "profiles_select_own" on public.profiles for select to authenticated
  using ((select auth.uid()) = user_id);
create policy "profiles_insert_own" on public.profiles for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy "profiles_delete_own" on public.profiles for delete to authenticated
  using ((select auth.uid()) = user_id);

-- backup_objects
create policy "backup_select_own" on public.backup_objects for select to authenticated
  using ((select auth.uid()) = user_id);
create policy "backup_insert_own" on public.backup_objects for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy "backup_update_own" on public.backup_objects for update to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "backup_delete_own" on public.backup_objects for delete to authenticated
  using ((select auth.uid()) = user_id);

-- sync_metadata
create policy "sync_select_own" on public.sync_metadata for select to authenticated
  using ((select auth.uid()) = user_id);
create policy "sync_insert_own" on public.sync_metadata for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy "sync_update_own" on public.sync_metadata for update to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "sync_delete_own" on public.sync_metadata for delete to authenticated
  using ((select auth.uid()) = user_id);

-- ai_usage: users may read their own counters; only server-side code (service role) may write.
create policy "ai_usage_select_own" on public.ai_usage for select to authenticated
  using ((select auth.uid()) = user_id);

-- Atomic daily quota. Returns true when the request is allowed and counted.
create or replace function public.consume_ai_quota(p_user uuid, p_limit integer)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  new_count integer;
begin
  insert into public.ai_usage (user_id, day, request_count)
  values (p_user, (now() at time zone 'utc')::date, 1)
  on conflict (user_id, day)
  do update set request_count = public.ai_usage.request_count + 1
  returning request_count into new_count;

  if new_count > p_limit then
    update public.ai_usage set request_count = p_limit
      where user_id = p_user and day = (now() at time zone 'utc')::date;
    return false;
  end if;
  return true;
end;
$$;

revoke all on function public.consume_ai_quota(uuid, integer) from public, anon, authenticated;
grant execute on function public.consume_ai_quota(uuid, integer) to service_role;

-- Private bucket for client-side-encrypted backups. Path convention: <user_id>/<file>.
insert into storage.buckets (id, name, public, file_size_limit)
values ('backups', 'backups', false, 52428800)
on conflict (id) do update set public = false, file_size_limit = 52428800;

create policy "backups_objects_select_own" on storage.objects for select to authenticated
  using (bucket_id = 'backups' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "backups_objects_insert_own" on storage.objects for insert to authenticated
  with check (bucket_id = 'backups' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "backups_objects_update_own" on storage.objects for update to authenticated
  using (bucket_id = 'backups' and (storage.foldername(name))[1] = (select auth.uid())::text)
  with check (bucket_id = 'backups' and (storage.foldername(name))[1] = (select auth.uid())::text);
create policy "backups_objects_delete_own" on storage.objects for delete to authenticated
  using (bucket_id = 'backups' and (storage.foldername(name))[1] = (select auth.uid())::text);
