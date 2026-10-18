# WealthOS Sovereign Encrypted Vault & Portable Backup Specification (.wosenc)

## 1. Specification Overview
The `.wosenc` format is a zero-knowledge, tamper-evident cryptographic container designed for seamless cross-platform backup and recovery between WealthOS Android (Kotlin/Room) and WealthOS Web (React/TypeScript).

---

## 2. Binary Framing & Envelope Architecture

Each `.wosenc` envelope is formatted as a continuous big-endian binary byte stream:

```
+-----------------------------------------------------------------------------------+
| MAGIC: 4 bytes | VERSION: 4 bytes | META_LEN: 4 bytes | METADATA_BYTES: N bytes   |
| 0x57 0x4F 0x53 0x42 (ASCII "WOSB") | (int32 = 1)       | (int32 > 0)       | (UTF-8 JSON)            |
+-----------------------------------------------------------------------------------+
| SALT: 16 bytes  | IV: 12 bytes   | CIPHERTEXT + AES-GCM TAG: Remaining bytes      |
| Cryptographic   | Random Nonce   | Encrypted payload bytes with 16-byte GCM tag   |
+-----------------------------------------------------------------------------------+
```

### Framing Constants
| Constant | Value | Description |
|---|---|---|
| `MAGIC` | `0x57, 0x4F, 0x53, 0x42` | "WOSB" (WealthOS Binary Vault) |
| `CURRENT_VERSION` | `1` | Canonical schema version |
| `MIN_ENVELOPE_SIZE` | `58 bytes` | Header (12B) + Min Meta (2B) + Salt (16B) + IV (12B) + GCM Tag (16B) |
| `MAX_ENVELOPE_SIZE` | `52,428,800 bytes` | 50 MB hard safety upper bound against buffer exhaustion |
| `MAX_METADATA_SIZE` | `65,536 bytes` | 64 KB safety upper bound for JSON metadata header |

---

## 3. Cryptographic Parameters

1. **Key Derivation Function (KDF):**
   - **Algorithm:** PBKDF2 with HMAC-SHA256 (`PBKDF2WithHmacSHA256`)
   - **Iterations:** 100,000 rounds
   - **Key Length:** 256 bits (32 bytes)
   - **Salt:** 16 cryptographically secure pseudorandom bytes (`SecureRandom` / `crypto.getRandomValues`)
2. **Authenticated Encryption with Associated Data (AEAD):**
   - **Algorithm:** AES-256 in Galois/Counter Mode (`AES/GCM/NoPadding`)
   - **Nonce / IV:** 12 random bytes
   - **Authentication Tag:** 128 bits (16 bytes) appended to ciphertext
   - **Associated Authenticated Data (AAD):** `METADATA_BYTES` is fed into the cipher via `cipher.updateAAD()` / Web Crypto `additionalData`. Any tampering with the plaintext metadata header invalidates the GCM tag upon decryption.
3. **Payload Checksum:**
   - **Algorithm:** SHA-256 over raw UTF-8 JSON bytes prior to encryption
   - Stored in the authenticated metadata as `checksumSha256` hex string for secondary integrity verification.

---

## 4. Metadata Schema (Header)

Unencrypted header metadata allows instant preview of backup size and entity counts before asking the user for their passphrase:

```json
{
  "version": 1,
  "appVersion": "0.2.0",
  "timestamp": 1771313427000,
  "accountCount": 3,
  "txnCount": 142,
  "goalCount": 2,
  "debtCount": 1,
  "chitFundCount": 0,
  "recurringCount": 4,
  "checksumSha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
}
```

---

## 5. Canonical Bilingual Payload Schema

To guarantee interoperability between Kotlin Room entities (`camelCase`) and PostgreSQL Supabase tables (`snake_case`), the payload exporter normalizes all entities into a **bilingual schema** containing both conventions:

### Supported Entities
1. **`accounts`**: `id`, `name`, `type`, `institution`, `lastFour`/`last_four`, `openingBalanceMinor`/`opening_balance_minor`, `currency`, `includeInNetWorth`/`include_in_net_worth`, `includeInStats`/`include_in_stats`, `archived`, `createdAt`/`created_at`, `updatedAt`/`updated_at`.
2. **`categories`**: `id`, `parentId`/`parent_id`, `name`, `kind`, `needWant`/`need_want`, `icon`, `color`, `archived`, `sortOrder`/`sort_order`.
3. **`txns`**: `id`, `type`, `amountMinor`/`amount_minor`, `currency`, `localDate`/`local_date`, `localTime`/`local_time`, `accountId`/`account_id`, `toAccountId`/`to_account_id`, `categoryId`/`category_id`, `merchant`, `description`, `notes`, `paymentMethod`/`payment_method`, `needWant`/`need_want`, `imported`, `source`, `createdAt`/`created_at`, `updatedAt`/`updated_at`.
4. **`journal_entries`**: `txnId`/`txn_id`, `accountId`/`account_id`, `categoryId`/`category_id`, `amountMinor`/`amount_minor`.
5. **`goals`**: `id`, `name`, `targetAmountMinor`/`target_amount_minor`, `currentAmountMinor`/`current_amount_minor`, `plannedMonthlyContributionMinor`/`planned_monthly_contribution_minor`, `currency`, `targetDate`/`target_date`, `archived`, `createdAt`/`created_at`, `updatedAt`/`updated_at`.
6. **`debts`**: `id`, `name`, `principalMinor`/`principal_minor`, `currentBalanceMinor`/`current_balance_minor`, `annualPercent`/`annual_percent`, `expectedMonthlyPaymentMinor`/`expected_monthly_payment_minor`, `currency`, `archived`, `createdAt`/`created_at`, `updatedAt`/`updated_at`.
7. **`chits`**: `id`, `name`, `totalMonths`/`total_months`, `startMonth`/`start_month`, `monthlyContributionMinor`/`monthly_contribution_minor`, `currency`, `active`, `createdAt`/`created_at`, `updatedAt`/`updated_at`.
8. **`recurring`**: `id`, `type`, `amountMinor`/`amount_minor`, `currency`, `accountId`/`account_id`, `categoryId`/`category_id`, `description`, `frequency`, `nextRunDate`/`next_run_date`, `active`, `createdAt`/`created_at`, `updatedAt`/`updated_at`, `autoPost`/`auto_post`.
9. **`budgets`**: `id`, `name`, `period`, `limitMinor`/`limit_minor`, `active`.

---

## 6. Passphrase & Security Policy

- **No Predictable Fallbacks:** Blank passphrases are strictly rejected during export. The deterministic key fallback (`wealthos_default_vault_key`) has been completely eradicated.
- **Minimum Complexity:** Passphrases must be at least 4 characters long and confirmed via accessible modal inputs.
- **Passphrase Lifecycle:** Kept in ephemeral memory only. Passphrases are explicitly overwritten in character arrays (`Arrays.fill(chars, '\0')`) and never persisted to local storage, cookies, session storage, or telemetry.

---

## 7. Automated Test Coverage
- **Android (`BackupEnvelopeTest.kt`):**
  - Framing and header validation
  - Correct PIN decryption
  - AEAD AAD tampering rejection (`AEADBadTagException`)
  - Ciphertext tampering rejection
  - Minimum and maximum envelope size bounds
- **Web (`wealthos-web/test/backupCrypto.test.js`):**
  - Round-trip envelope encryption and decryption
  - Blank passphrase error rejection
  - Wrong passphrase rejection
  - Ciphertext tampering rejection
  - Header AAD tampering rejection
  - Envelope truncation checks
  - Bilingual serialization validation
