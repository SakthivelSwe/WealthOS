/**
 * Bank Statement Parser & Reconciliation Unit Tests
 * Tests Axis Bank format, Indian number formatting, duplicate candidate flagging,
 * and mathematical closing balance reconciliation arithmetic.
 */

import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  parseStatementAmount,
  parseStatementDate,
  cleanNarration,
  suggestCategory,
  parseBankStatement,
} from '../src/lib/statementParser.ts';

describe('Phase 5: Bank Statement Parser & Reconciliation Engine', () => {

  it('Parses Indian monetary string with lakhs/crores formatting into minor units', () => {
    assert.equal(parseStatementAmount('32,200.00'), 3220000, '₹32,200.00 should be 3220000 paise');
    assert.equal(parseStatementAmount('1,50,000.50'), 15000050, '₹1,50,000.50 should be 15000050 paise');
    assert.equal(parseStatementAmount('800.00'), 80000, '₹800.00 should be 80000 paise');
    assert.equal(parseStatementAmount('-'), 0, 'Dash should be 0');
    assert.equal(parseStatementAmount(''), 0, 'Empty string should be 0');
  });

  it('Handles negative amounts with Dr suffix or parentheses', () => {
    assert.equal(parseStatementAmount('(500.00)'), -50000, '(500.00) should be -50000 paise');
    assert.equal(parseStatementAmount('1,200.00 Dr'), -120000, '1,200.00 Dr should be -120000 paise');
  });

  it('Parses various standard Indian date formats', () => {
    assert.equal(parseStatementDate('05/10/2026'), '2026-10-05', 'DD/MM/YYYY');
    assert.equal(parseStatementDate('05-10-2026'), '2026-10-05', 'DD-MM-YYYY');
    assert.equal(parseStatementDate('2026-10-05'), '2026-10-05', 'YYYY-MM-DD');
    assert.equal(parseStatementDate('05-Oct-2026'), '2026-10-05', 'DD-MMM-YYYY');
    assert.equal(parseStatementDate('05 Oct 2026'), '2026-10-05', 'DD MMM YYYY');
  });

  it('Extracts UPI merchant and reference number cleanly', () => {
    const { merchant, reference, isSelfTransfer } = cleanNarration('UPI/P2A/627991823/Swiggy/FoodOrder');
    assert.equal(reference, '627991823', 'Reference should be UPI ref');
    assert.equal(merchant, 'Swiggy', 'Merchant should be Swiggy');
    assert.equal(isSelfTransfer, false, 'Not self transfer');
  });

  it('Detects self transfer narrations', () => {
    const { isSelfTransfer } = cleanNarration('UPI/Self Transfer to SBI Account');
    assert.equal(isSelfTransfer, true, 'Should detect self transfer');
  });

  it('Suggests appropriate categories based on Indian banking keywords', () => {
    assert.equal(suggestCategory('SALARY CREDIT CMS3948', 'INCOME').categoryId, 'cat_income_0');
    assert.equal(suggestCategory('UPI/House Rent October', 'EXPENSE').categoryId, 'cat_housing');
    assert.equal(suggestCategory('UPI/Zomato Daily', 'EXPENSE').categoryId, 'cat_dining');
    assert.equal(suggestCategory('ACH/HDFC HOME LOAN EMI', 'EXPENSE').categoryId, 'cat_debt');
    assert.equal(suggestCategory('BESCOM Electricity Bill', 'EXPENSE').categoryId, 'cat_utilities');
  });

  it('Parses full Axis Bank CSV statement with mathematical reconciliation', () => {
    // Simulating user's October Axis statement:
    // Opening balance on Oct 1: ₹5,000.00 (500,000)
    // Oct 5: Salary credit ₹32,200.00 (3,220,000) -> balance 37,200.00 (3,720,000)
    // Oct 6: Rent transfer ₹15,000.00 (1,500,000) -> balance 22,200.00 (2,220,000)
    // Oct 7: Loan EMI ₹10,000.00 (1,000,000) -> balance 12,200.00 (1,220,000)
    // Oct 10: Swiggy/food ₹1,400.00 (140,000) -> balance 10,800.00 (1,080,000)
    // Oct 20: Transferred to second account ₹10,000.00 (1,000,000) -> balance 800.00 (80,000)
    // Closing balance: ₹800.00 (80,000)
    const csvContent = `Tran Date,Particulars,Chq No,Withdrawal (Dr),Deposit (Cr),Balance
05-10-2026,SALARY CREDIT / CMS394821,CMS394821,-,32200.00,37200.00
06-10-2026,UPI/P2A/627811/House Rent,627811,15000.00,-,22200.00
07-10-2026,ACH/LOAN EMI HDFC,ACH991,10000.00,-,12200.00
10-10-2026,UPI/P2A/627991/Swiggy,627991,1400.00,-,10800.00
20-10-2026,UPI/Self Transfer to Secondary Bank,628211,10000.00,-,800.00`;

    const result = parseBankStatement(csvContent, []);

    assert.equal(result.rows.length, 5, 'Should parse 5 transaction rows');
    assert.equal(result.totalCreditsMinor, 3220000, 'Total credits must be ₹32,200.00');
    assert.equal(result.totalDebitsMinor, 3640000, 'Total debits must be ₹36,400.00');

    // Mathematical reconciliation check
    assert.equal(result.hasReconciliation, true, 'Reconciliation check should be enabled');
    assert.equal(result.openingBalanceMinor, 500000, 'Opening balance should be ₹5,000.00 (500000)');
    assert.equal(result.closingBalanceMinor, 80000, 'Closing balance should be ₹800.00 (80000)');

    // Expected Closing = 500000 + 3220000 - 3640000 = 80000 (₹800.00)
    assert.equal(result.expectedClosingMinor, 80000, 'Expected closing must be ₹800.00');
    assert.equal(result.discrepancyMinor, 0, 'Discrepancy must be 0 (100% reconciled)');
  });

  it('Flags duplicate transactions against existing ledger entries', () => {
    const csvContent = `Tran Date,Particulars,Chq No,Withdrawal (Dr),Deposit (Cr),Balance
05-10-2026,SALARY CREDIT,CMS394821,-,32200.00,37200.00
10-10-2026,UPI/Swiggy,627991,1400.00,-,35800.00`;

    const existingTxns = [
      {
        id: 'txn_existing_1',
        local_date: '2026-10-05',
        amount_minor: 3220000,
        type: 'INCOME',
        account_id: 'acc_axis',
      },
    ];

    const result = parseBankStatement(csvContent, existingTxns, 'acc_axis');

    assert.equal(result.rows.length, 2);
    assert.equal(result.rows[0].isDuplicate, true, 'Oct 5 Salary is already in ledger — must be flagged as duplicate');
    assert.equal(result.rows[0].selected, false, 'Duplicate rows should be unselected by default');
    assert.equal(result.rows[1].isDuplicate, false, 'New transaction should not be duplicate');
    assert.equal(result.rows[1].selected, true, 'New transaction should be selected by default');
    assert.equal(result.duplicateCount, 1, 'Duplicate count should be 1');
  });

  it('Discrepancy is detected when arithmetic does not balance', () => {
    // Statement where an unrecorded debit or missing row occurs
    const csvContent = `Tran Date,Particulars,Withdrawal (Dr),Deposit (Cr),Balance
05-10-2026,SALARY CREDIT,-,32200.00,32200.00
20-10-2026,UPI/ATM Cash Withdrawal,5000.00,-,26000.00`;
    // Opening = 0
    // Total Credits = 32,200
    // Total Debits = 5,000
    // Expected = 27,200
    // Actual Closing = 26,000 (Bank took ₹1,200 that is not in rows)
    // Discrepancy = 27,200 - 26,000 = 1,200 (120000 paise)

    const result = parseBankStatement(csvContent, []);
    assert.equal(result.hasReconciliation, true);
    assert.equal(result.expectedClosingMinor, 2720000);
    assert.equal(result.closingBalanceMinor, 2600000);
    assert.equal(result.discrepancyMinor, 120000, 'Discrepancy should accurately be ₹1,200.00');
  });
});
