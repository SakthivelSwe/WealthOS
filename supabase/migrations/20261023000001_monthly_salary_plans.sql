-- WealthOS: Monthly Salary / Income Planning
-- Phase 4 (F-13, F-14): Variable monthly income tracking
-- Apply after: 20261023000000_financial_integrity.sql

-- ============================================================
-- Monthly plan: one row per user per calendar month (YYYY-MM).
-- Records the ACTUAL salary or other income received that month,
-- plus planned allocation percentages and committed obligations.
-- ============================================================

CREATE TABLE IF NOT EXISTS public.user_monthly_plans (
    id                              text        NOT NULL,
    user_id                         uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    month                           text        NOT NULL,   -- 'YYYY-MM' e.g. '2026-10'
    currency                        text        NOT NULL DEFAULT 'INR',
    status                          text        NOT NULL DEFAULT 'DRAFT'
                                        CHECK (status IN ('DRAFT','ACTIVE','CLOSED')),
    -- Income (in minor units, e.g. paise)
    confirmed_income_minor          bigint      NOT NULL DEFAULT 0,
    estimated_income_minor          bigint      NOT NULL DEFAULT 0,
    income_source                   text,       -- e.g. 'Salary', 'Freelance', 'Bonus', 'Other'
    income_received_date            text,       -- 'YYYY-MM-DD' when salary actually credited
    -- Allocation percentages (must sum <= 100)
    needs_pct                       integer     NOT NULL DEFAULT 50,
    wants_pct                       integer     NOT NULL DEFAULT 30,
    savings_pct                     integer     NOT NULL DEFAULT 20,
    -- Committed outflows (auto-computed or user-set, minor units)
    committed_bills_minor           bigint      NOT NULL DEFAULT 0,
    committed_debt_payments_minor   bigint      NOT NULL DEFAULT 0,
    committed_savings_minor         bigint      NOT NULL DEFAULT 0,
    -- Safe-to-spend buffer (percentage of income, default 5%)
    buffer_pct                      integer     NOT NULL DEFAULT 5,
    notes                           text,
    -- Timestamps (epoch millis)
    created_at                      bigint      NOT NULL,
    updated_at                      bigint      NOT NULL,
    PRIMARY KEY (id, user_id),
    UNIQUE (user_id, month)
);

-- Row Level Security
ALTER TABLE public.user_monthly_plans ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users see own monthly plans"
    ON public.user_monthly_plans FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Index for fast month lookups
CREATE INDEX IF NOT EXISTS user_monthly_plans_month_idx
    ON public.user_monthly_plans (user_id, month DESC);

-- ============================================================
-- RPC: Get safe-to-spend for a given month.
-- Looks up actual income from the monthly plan,
-- subtracts committed bills + debt payments + savings + buffer,
-- and verifies against actual journal balance.
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_safe_to_spend(
    p_user_id   uuid,
    p_month     text   -- 'YYYY-MM'
)
RETURNS TABLE(
    confirmed_income_minor      bigint,
    actual_expense_minor        bigint,
    committed_obligations_minor bigint,
    buffer_minor                bigint,
    safe_to_spend_minor         bigint
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    WITH plan AS (
        SELECT
            confirmed_income_minor,
            committed_bills_minor + committed_debt_payments_minor + committed_savings_minor
                AS total_committed,
            buffer_pct
        FROM public.user_monthly_plans
        WHERE user_id = p_user_id
          AND month   = p_month
        LIMIT 1
    ),
    expenses AS (
        SELECT COALESCE(SUM(t.amount_minor), 0)::bigint AS total_expense
        FROM public.user_transactions t
        WHERE t.user_id     = p_user_id
          AND t.type        = 'EXPENSE'
          AND t.deleted_at  IS NULL
          AND t.local_date  >= p_month || '-01'
          AND t.local_date  <= p_month || '-31'
    )
    SELECT
        COALESCE(plan.confirmed_income_minor, 0)::bigint            AS confirmed_income_minor,
        expenses.total_expense::bigint                              AS actual_expense_minor,
        COALESCE(plan.total_committed, 0)::bigint                   AS committed_obligations_minor,
        COALESCE(
            (plan.confirmed_income_minor * plan.buffer_pct / 100), 0
        )::bigint                                                   AS buffer_minor,
        GREATEST(
            0,
            COALESCE(plan.confirmed_income_minor, 0)
            - expenses.total_expense
            - COALESCE(plan.total_committed, 0)
            - COALESCE(plan.confirmed_income_minor * plan.buffer_pct / 100, 0)
        )::bigint                                                   AS safe_to_spend_minor
    FROM expenses
    LEFT JOIN plan ON true;
$$;

REVOKE ALL ON FUNCTION public.get_safe_to_spend(uuid, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_safe_to_spend(uuid, text) TO authenticated;
