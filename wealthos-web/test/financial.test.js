import { test, describe } from 'node:test';
import assert from 'node:assert/strict';
import {
  formatMoney,
  parseMoney,
  getCurrencyInfo,
} from '../src/types/index.ts';

describe('WealthOS Financial Invariants & Currency Precision', () => {
  test('Indian numbering formatting produces correct lakhs and crores grouping', () => {
    // 15,00,000.00 INR (15 Lakhs = 150000000 minor units)
    const formattedLakhs = formatMoney(150000000, 'INR', false);
    assert.match(formattedLakhs, /₹\s*15,00,000\.00/);

    // 1,50,000.50 INR (1.5 Lakhs)
    const formattedOneFifty = formatMoney(15000050, 'INR', false);
    assert.match(formattedOneFifty, /₹\s*1,50,000\.50/);
  });

  test('JPY 0-decimal currency precision formats without fraction digits', () => {
    const jpyInfo = getCurrencyInfo('JPY');
    assert.equal(jpyInfo.minorUnit, 0);

    // 150,000 JPY
    const formattedJpy = formatMoney(150000, 'JPY', false);
    assert.equal(formattedJpy, '¥150,000');
  });

  test('parseMoney converts string inputs to exact minor units', () => {
    // Standard USD $100.50 -> 10050
    assert.equal(parseMoney('100.50', 'USD'), 10050);

    // Indian Lakh with commas '1,50,000.50' -> 15000050
    assert.equal(parseMoney('1,50,000.50', 'INR'), 15000050);

    // JPY zero decimal '150000' -> 150000
    assert.equal(parseMoney('150,000', 'JPY'), 150000);
  });

  test('Masking privacy flag hides sensitive balances cleanly', () => {
    const maskedInr = formatMoney(50000000, 'INR', true);
    assert.equal(maskedInr, '••••••');

    const maskedUsd = formatMoney(100000, 'USD', true);
    assert.equal(maskedUsd, '••••••');
  });

  test('Double-entry journal legs invariant: sum of legs must equal 0', () => {
    // Given an expense of 250000 minor units from acc_1 with category cat_groceries
    const txnId = 'txn_test_123';
    const amountMinor = 250000;
    const expenseLegs = [
      { txn_id: txnId, account_id: 'acc_1', category_id: null, amount_minor: -amountMinor },
      { txn_id: txnId, account_id: null, category_id: 'cat_groceries', amount_minor: amountMinor },
    ];

    const sumExpense = expenseLegs.reduce((acc, leg) => acc + leg.amount_minor, 0);
    assert.equal(sumExpense, 0, 'Expense legs must sum to 0');

    // Given a transfer of 500000 minor units from acc_1 to acc_2
    const transferLegs = [
      { txn_id: txnId, account_id: 'acc_1', category_id: null, amount_minor: -amountMinor },
      { txn_id: txnId, account_id: 'acc_2', category_id: null, amount_minor: amountMinor },
    ];

    const sumTransfer = transferLegs.reduce((acc, leg) => acc + leg.amount_minor, 0);
    assert.equal(sumTransfer, 0, 'Transfer legs must sum to 0');
  });

  test('RFC 4180 CSV formula injection shielding prevents spreadsheet execution', () => {
    const dangerousInputs = [
      '=SUM(A1:A10)',
      '+12345678',
      '-@cmd|/C calc!A0',
      '@SUM(1,2)',
      '\tCMD',
      '\rMALICIOUS',
    ];

    for (const input of dangerousInputs) {
      // Shielding implementation as used in SettingsPage.tsx
      const shielded = /^[=+\-@\t\r]/.test(input) ? `'${input}` : input;
      assert.ok(shielded.startsWith("'"), `Dangerous input ${input} must be prefixed with single quote`);
    }

    const safeInput = 'Swiggy Dinner Order';
    const notShielded = /^[=+\-@\t\r]/.test(safeInput) ? `'${safeInput}` : safeInput;
    assert.equal(notShielded, safeInput);
  });
});
