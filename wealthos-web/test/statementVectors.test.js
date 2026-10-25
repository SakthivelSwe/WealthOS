import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseBankStatement } from '../src/lib/statementParser.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function getVectorPath(subpath) {
  const candidates = [
    path.join(__dirname, '../../shared-test-vectors', subpath),
    path.join(__dirname, '../shared-test-vectors', subpath),
  ];
  for (const c of candidates) {
    if (fs.existsSync(c)) return c;
  }
  throw new Error(`Vector not found: ${subpath}`);
}

describe('Phase 2: Universal Statement Import Shared Vectors', () => {
  it('Parses Axis Bank CSV statement vector accurately', () => {
    const csvPath = getVectorPath('parser/axis_statement.csv');
    const content = fs.readFileSync(csvPath, 'utf8');

    const result = parseBankStatement(content, []);

    assert.equal(result.rows.length, 5, 'Should parse 5 transaction rows');
    assert.equal(result.totalCreditsMinor, 3220000, 'Total credits must be ₹32,200.00');
    assert.equal(result.totalDebitsMinor, 618400, 'Total debits must be ₹6,184.00');
    assert.equal(result.openingBalanceMinor, 0, 'Opening balance should be ₹0.00');
    assert.equal(result.closingBalanceMinor, 2601600, 'Closing balance should be ₹26,016.00');
    assert.equal(result.hasReconciliation, true, 'Reconciliation should pass');
    assert.equal(result.discrepancyMinor, 0, 'Discrepancy must be 0');

    assert.equal(result.smallScansCount, 1, 'Small scans count');
    assert.equal(result.smallScansTotalMinor, 8500, 'Small scans total ₹85.00');
    assert.equal(result.detectedMandatesCount, 2, 'Detected mandates: Salary and Netflix');

    // Row 0: Salary credit
    const r0 = result.rows[0];
    assert.equal(r0.date, '2026-10-05');
    assert.equal(r0.amountMinor, 3220000);
    assert.equal(r0.type, 'INCOME');
    assert.equal(r0.merchant, 'TECHCORP SALARY');
    assert.equal(r0.reference, 'N0981726451');

    // Row 1: Swiggy
    const r1 = result.rows[1];
    assert.equal(r1.date, '2026-10-06');
    assert.equal(r1.amountMinor, 45000);
    assert.equal(r1.type, 'EXPENSE');
    assert.equal(r1.merchant, 'SWIGGY');
    assert.equal(r1.reference, '327918291048');
    assert.equal(r1.jarBucket, 'PLAY');
  });

  it('Parses HDFC Single Column statement vector accurately', () => {
    const csvPath = getVectorPath('parser/hdfc_single_col.csv');
    const content = fs.readFileSync(csvPath, 'utf8');

    const result = parseBankStatement(content, []);

    assert.equal(result.rows.length, 2, 'Should parse 2 transaction rows');
    assert.equal(result.totalCreditsMinor, 4500000, 'Total credits ₹45,000.00');
    assert.equal(result.totalDebitsMinor, 35000, 'Total debits ₹350.00');

    const r0 = result.rows[0];
    assert.equal(r0.date, '2026-10-01');
    assert.equal(r0.amountMinor, 35000);
    assert.equal(r0.type, 'EXPENSE');
    assert.equal(r0.merchant, 'SWIGGY');
    assert.equal(r0.reference, '482910394857');

    const r1 = result.rows[1];
    assert.equal(r1.date, '2026-10-02');
    assert.equal(r1.amountMinor, 4500000);
    assert.equal(r1.type, 'INCOME');
    assert.equal(r1.reference, 'CMS987654321');
  });
});
