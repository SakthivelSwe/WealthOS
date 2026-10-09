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
