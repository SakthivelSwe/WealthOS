import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  createEncryptedBackupEnvelope,
  decryptBackupEnvelope,
  readEnvelopeMetadata,
  normalizePayloadForExport,
} from '../src/lib/backupCrypto.ts';

describe('WealthOS Web Backup Crypto & Interoperability Engine', () => {
  const samplePayload = {
    accounts: [
      {
        id: 'acc_main',
        name: 'Axis Bank Salary',
        type: 'BANK',
        institution: 'Axis Bank',
        last_four: '4821',
        opening_balance_minor: 15000000,
        currency: 'INR',
        include_in_net_worth: true,
        include_in_stats: true,
        archived: false,
        created_at: 1700000000000,
        updated_at: 1700000000000,
      },
    ],
    categories: [
      {
        id: 'cat_food',
        parent_id: null,
        name: '🍔 Food',
        kind: 'EXPENSE',
        need_want: 'NEED',
        icon: 'food',
        color: 0xffc9a66b,
        archived: false,
        sort_order: 0,
      },
    ],
    txns: [
      {
        id: 'txn_101',
        type: 'EXPENSE',
        amount_minor: 45000,
        currency: 'INR',
        local_date: '2026-10-18',
        local_time: '12:30',
        account_id: 'acc_main',
        to_account_id: null,
        category_id: 'cat_food',
        merchant: 'Swiggy',
        description: 'Lunch Order',
        notes: null,
        payment_method: 'UPI',
        need_want: 'NEED',
        imported: false,
        source: 'WEB_MANUAL',
        created_at: 1700000000000,
        updated_at: 1700000000000,
        deleted_at: null,
      },
    ],
    journal_entries: [
      {
        txn_id: 'txn_101',
        account_id: 'acc_main',
        category_id: null,
        amount_minor: -45000,
      },
      {
        txn_id: 'txn_101',
        account_id: null,
        category_id: 'cat_food',
        amount_minor: 45000,
      },
    ],
    goals: [
      {
        id: 'goal_emergency',
        name: 'Emergency Fund',
        target_amount_minor: 50000000,
        current_amount_minor: 15000000,
        planned_monthly_contribution_minor: 5000000,
        currency: 'INR',
        target_date: '2027-12-31',
        archived: false,
        created_at: 1700000000000,
        updated_at: 1700000000000,
      },
    ],
    debts: [],
    chits: [],
    recurring: [],
    budgets: [],
  };

  test('Envelope encrypts and round-trips decrypts cleanly with valid passphrase', async () => {
    const pin = 'SecretVault123!';
    const envelope = await createEncryptedBackupEnvelope(samplePayload, pin);

    assert.ok(envelope instanceof Uint8Array, 'Envelope must be a Uint8Array');
    assert.ok(envelope.length >= 58, 'Envelope must satisfy MIN_ENVELOPE_SIZE');

    // Header check
    assert.equal(envelope[0], 0x57); // 'W'
    assert.equal(envelope[1], 0x4f); // 'O'
    assert.equal(envelope[2], 0x53); // 'S'
    assert.equal(envelope[3], 0x42); // 'B'

    const metadata = readEnvelopeMetadata(envelope);
    assert.equal(metadata.version, 1);
    assert.equal(metadata.accountCount, 1);
    assert.equal(metadata.txnCount, 1);
    assert.equal(metadata.goalCount, 1);
    assert.ok(metadata.checksumSha256.length === 64, 'Checksum must be 64-char SHA256');

    // Decrypt
    const { metadata: decMeta, payload } = await decryptBackupEnvelope(envelope, pin);
    assert.equal(decMeta.checksumSha256, metadata.checksumSha256);
    assert.equal(payload.accounts.length, 1);
    assert.equal(payload.txns.length, 1);
    assert.equal(payload.journal_entries.length, 2);
    assert.equal(payload.accounts[0].name, 'Axis Bank Salary');
  });

  test('Empty or blank passphrase strictly throws error (Finding B)', async () => {
    await assert.rejects(
      async () => {
        await createEncryptedBackupEnvelope(samplePayload, '');
      },
      /secure passphrase is required/i
    );

    await assert.rejects(
      async () => {
        await createEncryptedBackupEnvelope(samplePayload, '   ');
      },
      /secure passphrase is required/i
    );
  });

  test('Decryption fails when incorrect passphrase is provided', async () => {
    const pin = 'CorrectPin123';
    const wrongPin = 'WrongPin999';
    const envelope = await createEncryptedBackupEnvelope(samplePayload, pin);

    await assert.rejects(
      async () => {
        await decryptBackupEnvelope(envelope, wrongPin);
      },
      /incorrect passphrase/i
    );
  });

  test('Tampered ciphertext is rejected by AEAD GCM authentication tag', async () => {
    const pin = 'IntegrityGuard';
    const envelope = await createEncryptedBackupEnvelope(samplePayload, pin);
    const corrupted = new Uint8Array(envelope);

    // Tamper with the last byte (part of AES-GCM tag or ciphertext)
    corrupted[corrupted.length - 1] ^= 0xff;

    await assert.rejects(
      async () => {
        await decryptBackupEnvelope(corrupted, pin);
      },
      /incorrect passphrase or tampered/i
    );
  });

  test('Tampered unencrypted metadata header is rejected by AEAD AAD', async () => {
    const pin = 'IntegrityGuard';
    const envelope = await createEncryptedBackupEnvelope(samplePayload, pin);
    const corrupted = new Uint8Array(envelope);

    // Modify a digit in the metadata JSON while preserving valid JSON syntax
    // (e.g. changing accountCount from 1 to 2 in unencrypted header)
    const dec = new TextDecoder();
    const enc = new TextEncoder();
    const view = new DataView(corrupted.buffer, corrupted.byteOffset, corrupted.byteLength);
    const metaLen = view.getInt32(8, false);
    const metaStr = dec.decode(corrupted.subarray(12, 12 + metaLen));
    const tamperedMetaStr = metaStr.replace('"accountCount":1', '"accountCount":9');

    if (tamperedMetaStr.length === metaStr.length) {
      corrupted.set(enc.encode(tamperedMetaStr), 12);
      await assert.rejects(
        async () => {
          await decryptBackupEnvelope(corrupted, pin);
        },
        /tampered backup data|incorrect passphrase/i
      );
    } else {
      // If length differed, any alteration triggers rejection
      corrupted[12 + Math.floor(metaLen / 2)] ^= 0x01;
      await assert.rejects(
        async () => {
          await decryptBackupEnvelope(corrupted, pin);
        }
      );
    }
  });

  test('Truncated envelope (< 58 bytes) is rejected', async () => {
    const tiny = new Uint8Array(30);
    assert.throws(() => {
      readEnvelopeMetadata(tiny);
    }, /too small/i);
  });

  test('Bilingual normalization produces both camelCase and snake_case properties for cross-platform compatibility', () => {
    const normalized = normalizePayloadForExport(samplePayload);

    // Account entity has both camelCase and snake_case
    const acc = normalized.accounts[0];
    assert.equal(acc.opening_balance_minor, 15000000);
    assert.equal(acc.openingBalanceMinor, 15000000);
    assert.equal(acc.last_four, '4821');
    assert.equal(acc.lastFour, '4821');

    // Txn entity has both camelCase and snake_case
    const txn = normalized.txns[0];
    assert.equal(txn.amount_minor, 45000);
    assert.equal(txn.amountMinor, 45000);
    assert.equal(txn.local_date, '2026-10-18');
    assert.equal(txn.localDate, '2026-10-18');
    assert.equal(txn.account_id, 'acc_main');
    assert.equal(txn.accountId, 'acc_main');

    // Journal entry has both camelCase and snake_case
    const entry = normalized.journal_entries[0];
    assert.equal(entry.txn_id, 'txn_101');
    assert.equal(entry.txnId, 'txn_101');
    assert.equal(entry.amount_minor, -45000);
    assert.equal(entry.amountMinor, -45000);
  });
});
