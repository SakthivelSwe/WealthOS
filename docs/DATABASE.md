# Database Design (Room + SQLCipher)

All money columns are `INTEGER` minor units. All timestamps are epoch-millis UTC `INTEGER`.
Financial day columns are ISO `TEXT` (`yyyy-MM-dd`) in the user's zone. IDs are UUID `TEXT`.
Soft delete via `deleted_at`. Migrations are exported as schemas and tested.

## Tables (v1)

- `currency(code PK, minor_unit)`
- `account(id, name, type, institution, last_four, opening_balance_minor, currency, include_networth, include_stats, archived, created_at, updated_at)`
- `category(id, parent_id, name, kind[EXPENSE|INCOME|SAVING], need_want, icon, color, archived, sort)`
- `txn(id, type, amount_minor, currency, local_date, local_time, account_id, to_account_id, category_id, subcategory_id, merchant, description, notes, payment_method, goal_id, recurring_id, attachment_id, need_want, imported, source, reconciliation, created_at, updated_at, deleted_at)`
- `tag`, `txn_tag(txn_id, tag_id)`
- `journal_entry(id, txn_id, account_id NULL, category_id NULL, amount_minor SIGNED)` — per `txn_id` the sum is 0.
- `budget`, `budget_line` (period, scope, planned_minor, rollover)
- `goal`, `goal_contribution(txn_id)`
- `recurring_rule(...)`, `debt`, `chit_fund`, `chit_installment`
- `asset_holding(quantity, purchase_value_minor, current_value_minor, date)`
- `import_batch`, `import_row_hash` (duplicate detection)
- `audit_event(id, entity, entity_id, action, at)` — no amounts or names stored.
- `settings` (non-secret preferences)

## Indexes
`txn(local_date)`, `txn(account_id, local_date)`, `txn(category_id)`, `txn(type)`,
`txn(merchant)`, `txn(created_at)`, `txn(updated_at)`, `journal_entry(txn_id)`,
`journal_entry(account_id)`. Ledger uses Paging 3 keyset pagination.

## Invariants (checked by Integrity Validator)
1. Each live txn has balanced journal entries.
2. Transfers have distinct source/destination and equal amounts.
3. FK targets exist; currency known; amount > 0; plausible dates.
4. No duplicate IDs. Cached balances equal derived balances.

## Encryption
SQLCipher with a 256-bit random key, wrapped by an Android Keystore AES-GCM key; only the wrapped blob lives in private storage.
Room + SQLCipher versions are pinned as a verified pair; upgrade only after migration and device tests.

---

# Cloud Database Design (Supabase PostgreSQL + RLS)

All amounts in Supabase are stored as `bigint` minor units (paise for INR, cents for USD).
Row Level Security (RLS) is enabled on all 12 user tables, ensuring `auth.uid() = user_id`.

## Supabase Tables (v2)

- `user_accounts(id, user_id, name, type, institution, last_four, opening_balance_minor, currency, include_in_net_worth, include_in_stats, archived, created_at, updated_at)`
- `user_categories(id, user_id, parent_id, name, kind, need_want, icon, color, archived, sort_order, budget_limit_minor)`
- `user_transactions(id, user_id, type, amount_minor, currency, local_date, local_time, account_id, to_account_id, category_id, merchant, description, notes, payment_method, need_want, tags, imported, created_at, updated_at, deleted_at)`
- `user_journal_entries(row_id bigserial, user_id, txn_id, account_id NULL, category_id NULL, amount_minor SIGNED)` — per `txn_id` sum of legs is strictly 0.
- `user_goals(id, user_id, name, target_amount_minor, current_amount_minor, planned_monthly_contribution_minor, currency, target_date, archived, created_at, updated_at)`
- `user_debts(id, user_id, name, principal_minor, current_balance_minor, annual_percent, expected_monthly_payment_minor, currency, archived, created_at, updated_at)`
- `user_chit_funds(id, user_id, name, total_months, start_month, monthly_contribution_minor, currency, active, created_at, updated_at)` — synced bidirectionally between Android and Web.
- `user_recurring_transactions(id, user_id, type, amount_minor, currency, account_id, category_id, description, frequency, next_run_date, active, created_at, updated_at)` — synced bidirectionally between Android and Web.
- `user_budgets(id, user_id, name, period, basis_income_minor, needs_pct, wants_pct, savings_pct, currency, active, created_at, updated_at)`
- `user_monthly_plans(id, user_id, month[YYYY-MM], currency, status, confirmed_income_minor, estimated_income_minor, income_source, income_received_date, needs_pct, wants_pct, savings_pct, committed_bills_minor, committed_debt_payments_minor, committed_savings_minor, buffer_pct, notes, created_at, updated_at)` — tracks variable monthly salary and allocations.
- `user_settings(user_id PK, currency, theme_mode, mask_amounts, user_name, phone_number, basis_income_minor, needs_pct, wants_pct, savings_pct, daily_reminder_enabled, daily_reminder_hour, updated_at)`
- `user_notifications(id, user_id, title, message, category, action_type, payload, read, created_at)`

## Supabase Database Functions (RPCs)

1. `replace_journal_entries(p_user_id uuid, p_txn_id text, p_entries jsonb)`:
   Atomically replaces double-entry journal legs for a transaction within a single database transaction, preventing orphaned or missing legs if an edit operation is interrupted.
2. `get_safe_to_spend(p_user_id uuid, p_month text)`:
   Computes transparent safe-to-spend balance: `confirmed_income - actual_expenses - committed_bills - committed_debts - committed_savings - cash_buffer`.
3. `get_account_totals(p_user_id uuid)`:
   Computes server-side balance aggregates from `user_journal_entries` across full transaction history, bypassing client-side 500-row query limits.
4. `consume_ai_quota(p_user uuid, p_limit integer)`:
   Atomically enforces daily quota limits for AI financial coach queries per user.

