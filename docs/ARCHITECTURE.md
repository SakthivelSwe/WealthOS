# Architecture — Private Money OS

Native Android (Kotlin, Jetpack Compose). Local-first, encrypted, offline-capable.
Cloud (Supabase) and AI (Gemini via Edge Function) are optional and consent-gated.

## Layering

```
feature/*  (Compose UI + ViewModels, sealed UiState)
    │ depends on
domain/    (pure Kotlin: models, repository interfaces, use cases, calculators)
    ▲ implemented by
data/      (Room+SQLCipher repositories, Supabase client, importers)
core/      (security, crypto, design system, common)
```

Rules:
- `domain` has **no Android imports**, so every financial calculation is plain JVM unit/property-testable.
- Composables never calculate money. ViewModels call use cases and expose immutable UiState.
- Dependencies point inward only. No god repositories: one repository per aggregate
  (Accounts, Ledger, Budgets, Goals, Debts, Chit, Recurring, Statements).

## Modules (deliberately few)

| Module | Purpose |
|---|---|
| `:app` | Application, MainActivity, nav graph, DI wiring, manifest |
| `:core:design` | Theme ("Quiet Financial Instrument"), reusable components |
| `:core:security` | Keystore key wrapping, app lock, FLAG_SECURE, redacted logger |
| `:domain` | Pure-Kotlin models and deterministic engines |
| `:data` | Room/SQLCipher, repositories, importers, backup, Supabase client |
| `:feature:*` | Added per vertical slice only when a screen exists |

Features start as packages inside `:app`/`:feature` and are split only when justified.

## Money model
- `Money(amountMinor: Long, currency: CurrencyCode)`; never Float/Double.
- Currency table holds `minorUnit` (INR=2, JPY=0...). Display uses `BigDecimal`.
- Percentages/projections use integer or `BigDecimal` math with explicit `RoundingMode`.

## Ledger model
Friendly transaction UI over a **journal**: every transaction produces ≥2 balanced
`journal_entry` rows (sum of signed amountMinor = 0 per transaction).
- Expense: debit expense-category node, credit asset/liability account.
- Income: debit asset account, credit income node.
- Transfer: debit destination, credit source. Excluded from income/expense analytics and net worth change.
Balances are derived from opening balance + journal sums; a cached balance column is
verified by the integrity checker. All writes occur in a single Room `@Transaction`.

## Time
`Instant` for stored timestamps (epoch ms UTC), `LocalDate` for the financial day derived
with the user's configured `ZoneId`. Month start day is configurable.

## Safe-to-Spend
`SafeToSpend = Liquid − UpcomingMandatoryBills − ExpectedDebtPayments − PlannedGoalContributions − CreditCardDue − Buffer`.
Returns a `SafeToSpendBreakdown` listing every term so UI can show the explanation. No AI involvement.

## AI path
App → Supabase Edge Function (JWT-validated) → Gemini. The app sends locally-computed
aggregates only. AI is read-only; output is schema-validated then re-validated against local records.

## Offline
All core features use local DB only. Network state is informational; WorkManager handles
recurring generation, reminders, and deferred uploads.

## Background work
WorkManager only (no exact alarms): recurring generation, reminders, imports, backups.
