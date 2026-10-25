-- ==============================================================================
-- WealthOS v2 Schema Migration: Multi-Bank Universal Ledger, Salary Cycles, & Financial Profiles
-- Conforms to WEALTHOS_FINAL_SPEC.md v3 §4.1 - §4.7
-- Idempotent, RLS-secured, zero hardcoded money defaults.
-- Apply after: 20261023000001_monthly_salary_plans.sql
-- ==============================================================================

-- 1. TRANSACTION ADDITIONS (§4.4, §4.6)
ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS jar_bucket text;
ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS spend_emotion_tag text
    CHECK (spend_emotion_tag IN ('NEED', 'PLANNED_WANT', 'REGRET_LEAK', 'UNPLANNED_TRANSFER', 'INCOME', 'UNCATEGORISED') OR spend_emotion_tag IS NULL);
ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS payment_rail text
    CHECK (payment_rail IN ('UPI_P2M', 'UPI_P2A', 'UPI_AUTOPAY', 'IMPS_NEFT_RTGS', 'NON_UPI') OR payment_rail IS NULL);
ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS rrn text;
ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS counterparty_name text;
ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS vpa text;
ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS import_batch_id text;
ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS user_overridden boolean NOT NULL DEFAULT false;
ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS is_recurring_mandate boolean NOT NULL DEFAULT false;

-- Unique (user_id, rrn) index for transactions with non-null RRN (§4.6)
CREATE UNIQUE INDEX IF NOT EXISTS user_txn_unique_rrn
    ON public.user_transactions (user_id, rrn)
    WHERE rrn IS NOT NULL;

-- 2. ACCOUNT ROLE & LIABILITY EXTENSIONS (§4.2)
ALTER TABLE public.user_accounts ADD COLUMN IF NOT EXISTS account_role text
    CHECK (account_role IN ('INCOME_VAULT', 'DAILY_UPI_SPEND', 'EMERGENCY_RESERVE', 'WEALTH_PORTFOLIO', 'LIABILITY_LOAN') OR account_role IS NULL);
ALTER TABLE public.user_accounts ADD COLUMN IF NOT EXISTS bank_name text;
ALTER TABLE public.user_accounts ADD COLUMN IF NOT EXISTS interest_rate_percent_bps integer DEFAULT 0;
ALTER TABLE public.user_accounts ADD COLUMN IF NOT EXISTS monthly_emi_paise bigint DEFAULT 0;
ALTER TABLE public.user_accounts ADD COLUMN IF NOT EXISTS outstanding_paise bigint DEFAULT 0;
ALTER TABLE public.user_accounts ADD COLUMN IF NOT EXISTS is_bnpl boolean NOT NULL DEFAULT false;

-- 3. SALARY CYCLES (§4.5)
CREATE TABLE IF NOT EXISTS public.salary_cycles (
    id                          text        NOT NULL,
    user_id                     uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    cycle_start_date            text        NOT NULL,
    salary_paise                bigint      NOT NULL CHECK (salary_paise >= 0),
    baseline_paise              bigint      NOT NULL CHECK (baseline_paise >= 0),
    emi_paise                   bigint      NOT NULL DEFAULT 0 CHECK (emi_paise >= 0),
    framework                   text        NOT NULL CHECK (framework IN ('SIX_JAR', 'THREE_BOX', 'FIFTY_THIRTY_TWENTY', 'CUSTOM')),
    jar_amounts                 jsonb       NOT NULL DEFAULT '{}'::jsonb,
    days_in_cycle               integer     NOT NULL CHECK (days_in_cycle > 0),
    daily_safe_upi_paise        bigint      NOT NULL DEFAULT 0,
    net_worth_snapshot_paise    bigint,
    checklist_state             jsonb       NOT NULL DEFAULT '{}'::jsonb,
    created_at                  bigint      NOT NULL,
    PRIMARY KEY (id, user_id),
    UNIQUE (user_id, cycle_start_date)
);

ALTER TABLE public.salary_cycles ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'salary_cycles' AND policyname = 'salary_cycles_access_own'
    ) THEN
        CREATE POLICY "salary_cycles_access_own"
            ON public.salary_cycles FOR ALL
            TO authenticated
            USING (auth.uid() = user_id)
            WITH CHECK (auth.uid() = user_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS salary_cycles_user_date_idx
    ON public.salary_cycles (user_id, cycle_start_date DESC);

-- 4. USER FINANCIAL PROFILES (§4.1)
CREATE TABLE IF NOT EXISTS public.user_financial_profiles (
    user_id                     uuid        PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    income_schedule_type        text        NOT NULL DEFAULT 'VARIABLE_WINDOW' CHECK (income_schedule_type IN ('VARIABLE_WINDOW', 'FIXED_DATE', 'IRREGULAR')),
    payday_window_start_day     integer     NOT NULL DEFAULT 2 CHECK (payday_window_start_day BETWEEN 1 AND 31),
    payday_window_end_day       integer     NOT NULL DEFAULT 5 CHECK (payday_window_end_day BETWEEN 1 AND 31),
    expected_salary_min_paise   bigint,
    expected_salary_max_paise   bigint,
    fixed_baseline_paise        bigint,     -- Spec requirement: NO default constant
    baseline_method             text        NOT NULL DEFAULT 'ESTIMATE_55_PERCENT' CHECK (baseline_method IN ('USER_ENTERED', 'HISTORY_MEDIAN', 'ESTIMATE_55_PERCENT')),
    lifestyle_creep_lock        boolean     NOT NULL DEFAULT true,
    needs_buffer_paise          bigint      NOT NULL DEFAULT 150000, -- ₹1,500
    allocation_framework        text        NOT NULL DEFAULT 'SIX_JAR' CHECK (allocation_framework IN ('SIX_JAR', 'THREE_BOX', 'FIFTY_THIRTY_TWENTY', 'CUSTOM')),
    custom_bps                  jsonb,
    play_to_long_term_shift     boolean     NOT NULL DEFAULT false,
    upi_needs_allowance_paise   bigint      NOT NULL DEFAULT 0,
    emergency_target_months     integer     NOT NULL DEFAULT 7 CHECK (emergency_target_months BETWEEN 1 AND 36),
    survival_threshold_paise    bigint,
    small_scan_threshold_paise  bigint      NOT NULL DEFAULT 20000, -- ₹200
    ego_check_threshold_paise   bigint      NOT NULL DEFAULT 200000, -- ₹2,000
    cooldown_hours              integer     NOT NULL DEFAULT 48,
    currency                    text        NOT NULL DEFAULT 'INR',
    locale                      text        NOT NULL DEFAULT 'en-IN',
    timezone                    text        NOT NULL DEFAULT 'Asia/Kolkata',
    language                    text        NOT NULL DEFAULT 'en' CHECK (language IN ('en', 'ta')),
    updated_at                  bigint      NOT NULL DEFAULT 0
);

ALTER TABLE public.user_financial_profiles ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'user_financial_profiles' AND policyname = 'user_financial_profiles_access_own'
    ) THEN
        CREATE POLICY "user_financial_profiles_access_own"
            ON public.user_financial_profiles FOR ALL
            TO authenticated
            USING (auth.uid() = user_id)
            WITH CHECK (auth.uid() = user_id);
    END IF;
END $$;

-- 5. SINKING FUNDS (§4.7)
CREATE TABLE IF NOT EXISTS public.sinking_funds (
    id                          text        NOT NULL,
    user_id                     uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name                        text        NOT NULL,
    target_paise                bigint      NOT NULL CHECK (target_paise > 0),
    current_paise               bigint      NOT NULL DEFAULT 0,
    target_date                 text        NOT NULL,
    monthly_allocation_paise    bigint      NOT NULL DEFAULT 0,
    archived                    boolean     NOT NULL DEFAULT false,
    created_at                  bigint      NOT NULL,
    updated_at                  bigint      NOT NULL,
    PRIMARY KEY (id, user_id)
);

ALTER TABLE public.sinking_funds ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'sinking_funds' AND policyname = 'sinking_funds_access_own'
    ) THEN
        CREATE POLICY "sinking_funds_access_own"
            ON public.sinking_funds FOR ALL
            TO authenticated
            USING (auth.uid() = user_id)
            WITH CHECK (auth.uid() = user_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS sinking_funds_user_idx
    ON public.sinking_funds (user_id);

-- 6. MERCHANT RULES (§4.7)
CREATE TABLE IF NOT EXISTS public.merchant_rules (
    id                          text        NOT NULL,
    user_id                     uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    merchant_pattern            text        NOT NULL,
    category_id                 text,
    jar_bucket                  text,
    spend_emotion_tag           text,
    is_recurring_mandate        boolean     NOT NULL DEFAULT false,
    created_at                  bigint      NOT NULL,
    PRIMARY KEY (id, user_id)
);

ALTER TABLE public.merchant_rules ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'merchant_rules' AND policyname = 'merchant_rules_access_own'
    ) THEN
        CREATE POLICY "merchant_rules_access_own"
            ON public.merchant_rules FOR ALL
            TO authenticated
            USING (auth.uid() = user_id)
            WITH CHECK (auth.uid() = user_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS merchant_rules_user_idx
    ON public.merchant_rules (user_id);

-- 7. MANDATES (§4.7)
CREATE TABLE IF NOT EXISTS public.mandates (
    id                          text        NOT NULL,
    user_id                     uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    merchant_name               text        NOT NULL,
    amount_paise                bigint      NOT NULL CHECK (amount_paise >= 0),
    frequency                   text        NOT NULL DEFAULT 'MONTHLY' CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    next_deduction_date         text        NOT NULL,
    account_id                  text,
    payment_rail                text        NOT NULL DEFAULT 'UPI_AUTOPAY',
    active                      boolean     NOT NULL DEFAULT true,
    created_at                  bigint      NOT NULL,
    updated_at                  bigint      NOT NULL,
    PRIMARY KEY (id, user_id)
);

ALTER TABLE public.mandates ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'mandates' AND policyname = 'mandates_access_own'
    ) THEN
        CREATE POLICY "mandates_access_own"
            ON public.mandates FOR ALL
            TO authenticated
            USING (auth.uid() = user_id)
            WITH CHECK (auth.uid() = user_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS mandates_user_idx
    ON public.mandates (user_id);

-- 8. REALTIME REPLICATION PUBLICATION
DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.salary_cycles;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.user_financial_profiles;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.sinking_funds;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.merchant_rules;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.mandates;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;
