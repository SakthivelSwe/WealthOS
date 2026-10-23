-- WealthOS Financial Integrity Migrations
-- Phase 1 & 2 (F-02, F-06): Journal entry deduplication and atomic replacement
-- Apply after: 20261014000000_web_sync.sql

-- ============================================================
-- F-02: Partial unique index to prevent duplicate journal entries.
-- A "duplicate" is defined as: same user, same txn_id, same account (or null),
-- same category (or null). This prevents repeated imports from inflating balances.
-- NOTE: Because account_id and category_id are nullable, standard UNIQUE
-- constraints don't cover NULL values. We use a partial expression index instead.
-- ============================================================

-- For entries with account_id (asset/liability legs)
CREATE UNIQUE INDEX IF NOT EXISTS user_journal_entries_dedup_acct_idx
    ON public.user_journal_entries (user_id, txn_id, account_id)
    WHERE account_id IS NOT NULL;

-- For entries with category_id (income/expense legs)
CREATE UNIQUE INDEX IF NOT EXISTS user_journal_entries_dedup_cat_idx
    ON public.user_journal_entries (user_id, txn_id, category_id)
    WHERE category_id IS NOT NULL AND account_id IS NULL;

-- ============================================================
-- F-06: Atomic journal entry replacement.
-- Replaces the non-atomic delete-then-insert pattern in useData.tsx.
-- If either operation fails, the transaction is rolled back and the
-- old journal entries are preserved.
-- ============================================================

CREATE OR REPLACE FUNCTION public.replace_journal_entries(
    p_user_id uuid,
    p_txn_id  text,
    p_entries jsonb
)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    DELETE FROM public.user_journal_entries
    WHERE user_id = p_user_id
      AND txn_id  = p_txn_id;

    IF jsonb_array_length(p_entries) > 0 THEN
        INSERT INTO public.user_journal_entries (user_id, txn_id, account_id, category_id, amount_minor)
        SELECT
            p_user_id,
            p_txn_id,
            NULLIF(entry->>'account_id',  '')::text,
            NULLIF(entry->>'category_id', '')::text,
            (entry->>'amount_minor')::bigint
        FROM jsonb_array_elements(p_entries) AS entry;
    END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.replace_journal_entries(uuid, text, jsonb) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.replace_journal_entries(uuid, text, jsonb) TO authenticated;

-- ============================================================
-- F-10: Server-side account balance aggregates.
-- Avoids the 500-row truncation bug in the client-side fetch.
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_account_balance_totals(p_user_id uuid)
RETURNS TABLE(
    account_id        text,
    opening_balance   bigint,
    journal_delta     bigint,
    current_balance   bigint
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        a.id                                          AS account_id,
        a.opening_balance_minor                       AS opening_balance,
        COALESCE(SUM(je.amount_minor), 0)::bigint     AS journal_delta,
        (a.opening_balance_minor + COALESCE(SUM(je.amount_minor), 0))::bigint AS current_balance
    FROM public.user_accounts a
    LEFT JOIN public.user_journal_entries je
           ON je.user_id    = a.user_id
          AND je.account_id = a.id
    WHERE a.user_id = p_user_id
    GROUP BY a.id, a.opening_balance_minor;
$$;

REVOKE ALL ON FUNCTION public.get_account_balance_totals(uuid) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_account_balance_totals(uuid) TO authenticated;

-- ============================================================
-- F-10: Period income/expense totals without row limit.
-- ============================================================

CREATE OR REPLACE FUNCTION public.get_period_totals(
    p_user_id   uuid,
    p_from_date text,
    p_to_date   text
)
RETURNS TABLE(
    txn_type    text,
    total_minor bigint
)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT
        t.type::text                AS txn_type,
        SUM(t.amount_minor)::bigint AS total_minor
    FROM public.user_transactions t
    WHERE t.user_id     = p_user_id
      AND t.deleted_at  IS NULL
      AND t.local_date >= p_from_date
      AND t.local_date <= p_to_date
    GROUP BY t.type;
$$;

REVOKE ALL ON FUNCTION public.get_period_totals(uuid, text, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_period_totals(uuid, text, text) TO authenticated;
