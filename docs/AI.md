# Money Lens (AI)

**Default: OFF.** Nothing is sent anywhere until you opt in. The client for these functions is not built yet; the server side exists and is untested against real Gemini (see PROJECT_STATUS.md).

## Path
App -> Supabase Edge Function (validates your Supabase JWT) -> Gemini API. The Gemini key exists only as a Supabase secret named `GEMINI_API_KEY`. The model comes from the server variable `GEMINI_MODEL`; the app has no model name.

## Functions
| Function | Purpose | Output |
|---|---|---|
| `ai-analyze` | Explain locally computed aggregates, answer questions | insights, recommendations, warnings, confidence |
| `ai-categorize` | Suggest categories for merchants | suggestions with ids from the request only |
| `ai-statement-extract` | Planned, not implemented | n/a |

## What may leave the device
Only pre-computed aggregates the app selects, for example "Food: Jan 2,100, Feb 3,740, top merchants X/Y/Z". Before any request the app removes account numbers, identifiers, notes, attachment metadata and unneeded history. Raw rows are sent only when a feature truly needs them, with a confirmation.

## What is never sent
Passwords, API keys, tokens, account numbers, the database, attachments, banking credentials, full note text.

## Safety rules
- AI is read-only. It has no tools and cannot change records, budgets or accounts.
- Prompts keep trusted instructions and untrusted data in separate delimited sections; delimiter-like text inside data is stripped. Merchant names, notes and imported text are treated purely as data.
- Output must be strict JSON and is validated: types, lengths, ranges, and for categorisation every id must exist in the request. Anything else is rejected, never repaired.
- The app re-validates amounts, dates and ids against local records before showing anything, and shows AI output as suggestions.
- All arithmetic (balances, savings rate, EMI, projections, safe-to-spend) is done in app code.

## Limits and costs
32 KB request cap, 1024 output tokens, 20 s timeout, one retry with backoff, circuit breaker after repeated upstream failures, 10 requests per minute per user per instance, and a daily quota (`AI_DAILY_LIMIT`, default 50) enforced atomically in Postgres.

## Failure behaviour
Errors return a short code (`rate_limited`, `ai_bad_response`, ...) and a request id; no stack traces or upstream text. When AI fails the app falls back to deterministic insights.

## Change the model
`supabase secrets set GEMINI_MODEL=<name>` then redeploy is not required; secrets apply to new invocations.

## Disable
Keep the AI switch off in the app, or delete the functions / unset `GEMINI_API_KEY`.

This is an automated analysis of your recorded transactions, not professional financial advice.
