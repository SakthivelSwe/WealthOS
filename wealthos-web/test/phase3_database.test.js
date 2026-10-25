import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

describe('Phase 3: Database & Storage Migration (Room v5 & Supabase v2)', () => {
  it('Validates Supabase migration 20261024000000_wealthos_v2.sql conforms to Spec §4.1-§4.7', () => {
    const migrationPath = path.join(__dirname, '../../supabase/migrations/20261024000000_wealthos_v2.sql');
    assert.ok(fs.existsSync(migrationPath), 'Migration file 20261024000000_wealthos_v2.sql must exist');

    const sql = fs.readFileSync(migrationPath, 'utf8');

    // 1. Transaction additions & RRN unique index
    assert.ok(sql.includes('ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS jar_bucket text'), 'Adds jar_bucket');
    assert.ok(sql.includes('ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS spend_emotion_tag text'), 'Adds spend_emotion_tag');
    assert.ok(sql.includes('ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS payment_rail text'), 'Adds payment_rail');
    assert.ok(sql.includes('ALTER TABLE public.user_transactions ADD COLUMN IF NOT EXISTS rrn text'), 'Adds rrn');
    assert.ok(sql.includes('user_txn_unique_rrn'), 'Creates unique (user_id, rrn) index');

    // 2. Account role extensions
    assert.ok(sql.includes('ALTER TABLE public.user_accounts ADD COLUMN IF NOT EXISTS account_role text'), 'Adds account_role');
    assert.ok(sql.includes('monthly_emi_paise'), 'Adds monthly_emi_paise');

    // 3. New Tables
    assert.ok(sql.includes('CREATE TABLE IF NOT EXISTS public.salary_cycles'), 'Creates salary_cycles table');
    assert.ok(sql.includes('CREATE TABLE IF NOT EXISTS public.user_financial_profiles'), 'Creates user_financial_profiles table');
    assert.ok(sql.includes('CREATE TABLE IF NOT EXISTS public.sinking_funds'), 'Creates sinking_funds table');
    assert.ok(sql.includes('CREATE TABLE IF NOT EXISTS public.merchant_rules'), 'Creates merchant_rules table');
    assert.ok(sql.includes('CREATE TABLE IF NOT EXISTS public.mandates'), 'Creates mandates table');

    // 4. Invariant: NO hardcoded default baseline paise
    assert.ok(!sql.includes('1771000'), 'Must not hardcode 1771000 as baseline default in migration');

    // 5. RLS verification
    assert.ok(sql.includes('ALTER TABLE public.salary_cycles ENABLE ROW LEVEL SECURITY'), 'RLS enabled on salary_cycles');
    assert.ok(sql.includes('ALTER TABLE public.user_financial_profiles ENABLE ROW LEVEL SECURITY'), 'RLS enabled on user_financial_profiles');
  });

  it('Validates Android Room v5 exported schema', () => {
    const schema5Path = path.join(__dirname, '../../app/schemas/app.privatemoney.data.local.AppDatabase/5.json');
    assert.ok(fs.existsSync(schema5Path), 'Room schema 5.json must be generated and exported');

    const schema5 = JSON.parse(fs.readFileSync(schema5Path, 'utf8'));
    assert.equal(schema5.database.version, 5, 'Database version must be 5');

    const entityNames = schema5.database.entities.map(e => e.tableName);
    assert.ok(entityNames.includes('salary_cycles'), 'Contains salary_cycles entity');
    assert.ok(entityNames.includes('account'), 'Contains account entity');
    assert.ok(entityNames.includes('txn'), 'Contains txn entity');

    // Account fields
    const accountEntity = schema5.database.entities.find(e => e.tableName === 'account');
    const accountFields = accountEntity.fields.map(f => f.columnName);
    assert.ok(accountFields.includes('accountRole'), 'account contains accountRole');
    assert.ok(accountFields.includes('bankName'), 'account contains bankName');
    assert.ok(accountFields.includes('interestRatePercentBps'), 'account contains interestRatePercentBps');
    assert.ok(accountFields.includes('monthlyEmiPaise'), 'account contains monthlyEmiPaise');

    // Txn fields & indices
    const txnEntity = schema5.database.entities.find(e => e.tableName === 'txn');
    const txnFields = txnEntity.fields.map(f => f.columnName);
    assert.ok(txnFields.includes('jarBucket'), 'txn contains jarBucket');
    assert.ok(txnFields.includes('spendEmotionTag'), 'txn contains spendEmotionTag');
    assert.ok(txnFields.includes('paymentRail'), 'txn contains paymentRail');
    assert.ok(txnFields.includes('rrn'), 'txn contains rrn');
    assert.ok(txnFields.includes('counterpartyName'), 'txn contains counterpartyName');

    const indexNames = txnEntity.indices.map(i => i.name);
    assert.ok(indexNames.includes('index_txn_rrn'), 'txn contains index on rrn');
  });
});
