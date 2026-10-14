-- WealthOS Web Sync: full financial data in Supabase for web ↔ mobile sync.
-- All tables have RLS: users only see their own data.
-- Realtime is enabled for instant sync.

-- ============ ACCOUNTS ============
create table if not exists public.user_accounts (
  id text not null,
  user_id uuid not null references auth.users (id) on delete cascade,
  name text not null check (char_length(name) between 1 and 60),
  type text not null check (type in ('BANK','CASH','UPI_WALLET','SAVINGS','CREDIT_CARD','INVESTMENT','LOAN','OTHER_ASSET','OTHER_LIABILITY')),
  institution text,
  last_four text check (last_four is null or char_length(last_four) = 4),
  opening_balance_minor bigint not null default 0,
  currency text not null default 'INR' check (char_length(currency) = 3),
  include_in_net_worth boolean not null default true,
  include_in_stats boolean not null default true,
  archived boolean not null default false,
  created_at bigint not null,
  updated_at bigint not null,
  primary key (id, user_id)
);
create index if not exists user_accounts_user_idx on public.user_accounts (user_id);

-- ============ CATEGORIES ============
create table if not exists public.user_categories (
  id text not null,
  user_id uuid not null references auth.users (id) on delete cascade,
  parent_id text,
  name text not null,
  kind text not null check (kind in ('EXPENSE','INCOME')),
  need_want text check (need_want in ('NEED','WANT','SAVING') or need_want is null),
  icon text not null default 'default',
  color bigint not null default 0,
  archived boolean not null default false,
  sort_order integer not null default 0,
  primary key (id, user_id)
);
create index if not exists user_categories_user_idx on public.user_categories (user_id);

-- ============ TRANSACTIONS ============
create table if not exists public.user_transactions (
  id text not null,
  user_id uuid not null references auth.users (id) on delete cascade,
  type text not null check (type in ('EXPENSE','INCOME','TRANSFER')),
  amount_minor bigint not null check (amount_minor >= 0),
  currency text not null default 'INR',
  local_date text not null,
  local_time text not null default '00:00',
  account_id text not null,
  to_account_id text,
  category_id text,
  merchant text,
  description text,
  notes text,
  payment_method text,
  need_want text check (need_want in ('NEED','WANT','SAVING') or need_want is null),
  tags text[] default '{}',
  imported boolean not null default false,
  source text not null default 'MANUAL',
  created_at bigint not null,
  updated_at bigint not null,
  deleted_at bigint,
  primary key (id, user_id)
);
create index if not exists user_txn_user_date_idx on public.user_transactions (user_id, local_date desc);
create index if not exists user_txn_user_type_idx on public.user_transactions (user_id, type);
create index if not exists user_txn_user_category_idx on public.user_transactions (user_id, category_id);
create index if not exists user_txn_user_account_idx on public.user_transactions (user_id, account_id, local_date);

-- ============ JOURNAL ENTRIES ============
create table if not exists public.user_journal_entries (
  row_id bigserial,
  user_id uuid not null references auth.users (id) on delete cascade,
  txn_id text not null,
  account_id text,
  category_id text,
  amount_minor bigint not null,
  primary key (row_id)
);
create index if not exists user_journal_user_txn_idx on public.user_journal_entries (user_id, txn_id);
create index if not exists user_journal_user_account_idx on public.user_journal_entries (user_id, account_id);

-- ============ GOALS ============
create table if not exists public.user_goals (
  id text not null,
  user_id uuid not null references auth.users (id) on delete cascade,
  name text not null,
  target_amount_minor bigint not null,
  current_amount_minor bigint not null default 0,
  planned_monthly_contribution_minor bigint not null default 0,
  currency text not null default 'INR',
  target_date text not null,
  archived boolean not null default false,
  created_at bigint not null,
  updated_at bigint not null,
  primary key (id, user_id)
);
create index if not exists user_goals_user_idx on public.user_goals (user_id);

-- ============ DEBTS ============
create table if not exists public.user_debts (
  id text not null,
  user_id uuid not null references auth.users (id) on delete cascade,
  name text not null,
  principal_minor bigint not null,
  current_balance_minor bigint not null,
  annual_percent text not null default '0',
  expected_monthly_payment_minor bigint not null default 0,
  currency text not null default 'INR',
  archived boolean not null default false,
  created_at bigint not null,
  updated_at bigint not null,
  primary key (id, user_id)
);
create index if not exists user_debts_user_idx on public.user_debts (user_id);

-- ============ CHIT FUNDS ============
create table if not exists public.user_chit_funds (
  id text not null,
  user_id uuid not null references auth.users (id) on delete cascade,
  name text not null,
  total_months integer not null,
  start_month text not null,
  monthly_contribution_minor bigint not null,
  currency text not null default 'INR',
  active boolean not null default true,
  created_at bigint not null,
  updated_at bigint not null,
  primary key (id, user_id)
);
create index if not exists user_chit_funds_user_idx on public.user_chit_funds (user_id);

-- ============ RECURRING TRANSACTIONS ============
create table if not exists public.user_recurring_transactions (
  id text not null,
  user_id uuid not null references auth.users (id) on delete cascade,
  type text not null check (type in ('EXPENSE','INCOME','TRANSFER')),
  amount_minor bigint not null,
  currency text not null default 'INR',
  account_id text not null,
  category_id text,
  description text,
  frequency text not null check (frequency in ('DAILY','WEEKLY','BIWEEKLY','MONTHLY','QUARTERLY','YEARLY')),
  next_run_date text not null,
  active boolean not null default true,
  created_at bigint not null,
  updated_at bigint not null,
  primary key (id, user_id)
);
create index if not exists user_recurring_user_idx on public.user_recurring_transactions (user_id);

-- ============ BUDGETS ============
create table if not exists public.user_budgets (
  id text not null,
  user_id uuid not null references auth.users (id) on delete cascade,
  name text not null,
  period text not null check (period in ('MONTHLY','WEEKLY','YEARLY')),
  basis_income_minor bigint not null default 0,
  needs_pct integer not null default 50,
  wants_pct integer not null default 30,
  savings_pct integer not null default 20,
  currency text not null default 'INR',
  active boolean not null default true,
  created_at bigint not null,
  updated_at bigint not null,
  primary key (id, user_id)
);
create index if not exists user_budgets_user_idx on public.user_budgets (user_id);

-- ============ USER SETTINGS ============
create table if not exists public.user_settings (
  user_id uuid primary key references auth.users (id) on delete cascade,
  currency text not null default 'INR',
  theme_mode text not null default 'DARK',
  mask_amounts boolean not null default false,
  user_name text not null default 'WealthOS User',
  phone_number text,
  basis_income_minor bigint not null default 0,
  needs_pct integer not null default 50,
  wants_pct integer not null default 30,
  savings_pct integer not null default 20,
  daily_reminder_enabled boolean not null default true,
  daily_reminder_hour integer not null default 21,
  updated_at bigint not null default 0
);

-- ============ NOTIFICATIONS ============
create table if not exists public.user_notifications (
  id text not null,
  user_id uuid not null references auth.users (id) on delete cascade,
  title text not null,
  message text not null,
  category text not null,
  action_type text,
  payload text,
  read boolean not null default false,
  created_at bigint not null,
  primary key (id, user_id)
);
create index if not exists user_notifications_user_idx on public.user_notifications (user_id, created_at desc);

-- ============ ROW LEVEL SECURITY ============
alter table public.user_accounts enable row level security;
alter table public.user_categories enable row level security;
alter table public.user_transactions enable row level security;
alter table public.user_journal_entries enable row level security;
alter table public.user_goals enable row level security;
alter table public.user_debts enable row level security;
alter table public.user_chit_funds enable row level security;
alter table public.user_recurring_transactions enable row level security;
alter table public.user_budgets enable row level security;
alter table public.user_settings enable row level security;
alter table public.user_notifications enable row level security;

-- Macro for RLS policies: each user accesses only their own rows
do $$
declare
  t text;
begin
  foreach t in array array[
    'user_accounts','user_categories','user_transactions','user_journal_entries',
    'user_goals','user_debts','user_chit_funds','user_recurring_transactions',
    'user_budgets','user_notifications'
  ]
  loop
    execute format('create policy "%s_select_own" on public.%I for select to authenticated using ((select auth.uid()) = user_id)', t, t);
    execute format('create policy "%s_insert_own" on public.%I for insert to authenticated with check ((select auth.uid()) = user_id)', t, t);
    execute format('create policy "%s_update_own" on public.%I for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id)', t, t);
    execute format('create policy "%s_delete_own" on public.%I for delete to authenticated using ((select auth.uid()) = user_id)', t, t);
  end loop;
end
$$;

-- Settings: single row per user
create policy "user_settings_select_own" on public.user_settings for select to authenticated using ((select auth.uid()) = user_id);
create policy "user_settings_insert_own" on public.user_settings for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "user_settings_update_own" on public.user_settings for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "user_settings_delete_own" on public.user_settings for delete to authenticated using ((select auth.uid()) = user_id);

-- ============ ENABLE REALTIME ============
alter publication supabase_realtime add table public.user_accounts;
alter publication supabase_realtime add table public.user_transactions;
alter publication supabase_realtime add table public.user_journal_entries;
alter publication supabase_realtime add table public.user_goals;
alter publication supabase_realtime add table public.user_debts;
alter publication supabase_realtime add table public.user_chit_funds;
alter publication supabase_realtime add table public.user_recurring_transactions;
alter publication supabase_realtime add table public.user_budgets;
alter publication supabase_realtime add table public.user_settings;
alter publication supabase_realtime add table public.user_notifications;
