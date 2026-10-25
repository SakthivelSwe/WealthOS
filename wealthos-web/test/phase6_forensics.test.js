import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  analyzeForensics,
  analyzeCycleForensics,
  classifyCrisisBucket,
} from '../src/lib/forensics.js';
import { parseBankStatement } from '../src/lib/statementParser.js';

test('Phase 6: Forensics & Crisis Diagnostic (§7 & §A.6)', async (t) => {
  await t.test('Appendix A.6: Golden Forensics Check (₹31,400 over 17 days -> 184,705 paise/day)', () => {
    // Salary 32,200 + 5,000 opening buffer = 37,200
    // 31,400 debited over 17 days leaving 5,800
    const opening = 3720000;
    const debits = 3140000;
    const days = 17;
    const closing = 580000;

    const report = analyzeForensics(opening, 0, debits, closing, days);
    assert.equal(report.averageBurnPerDayPaise, 184705);
    assert.equal(report.expectedClosingPaise, 580000);
    assert.equal(report.actualClosingPaise, 580000);
    assert.equal(report.isReconciled, true);
    assert.equal(report.discrepancyPaise, 0);
  });

  await t.test('Four Crisis Buckets (§7) cleanly categorises and sums to total debits', () => {
    const transactions = [
      {
        id: 't1',
        amountMinor: 1500000, // ₹15,000 House Rent
        type: 'EXPENSE',
        local_date: '2026-10-06',
        merchant: 'House Rent',
        description: 'UPI/P2A/627811/House Rent',
        categoryName: 'Rent',
        jarBucket: 'NEEDS',
        spendEmotionTag: 'NEED',
      },
      {
        id: 't2',
        amountMinor: 1000000, // ₹10,000 HDFC Loan EMI
        type: 'EXPENSE',
        local_date: '2026-10-07',
        merchant: 'HDFC Loan',
        description: 'ACH/LOAN EMI HDFC',
        categoryName: 'EMI',
        jarBucket: 'NEEDS',
        spendEmotionTag: 'NEED',
        isRecurringMandate: true,
      },
      {
        id: 't3',
        amountMinor: 140000, // ₹1,400 Swiggy Dining
        type: 'EXPENSE',
        local_date: '2026-10-10',
        merchant: 'Swiggy',
        description: 'UPI/P2M/627991/Swiggy',
        categoryName: 'Food & Dining',
        jarBucket: 'PLAY',
        spendEmotionTag: 'PLANNED_WANT',
      },
      {
        id: 't4',
        amountMinor: 20000, // ₹200 Tea Stall
        type: 'EXPENSE',
        local_date: '2026-10-12',
        merchant: 'Chai Point',
        description: 'UPI/P2M/Chai Point',
        categoryName: 'Food & Dining',
        jarBucket: 'PLAY',
        spendEmotionTag: 'PLANNED_WANT',
      },
      {
        id: 't5',
        amountMinor: 64900, // ₹649 Netflix
        type: 'EXPENSE',
        local_date: '2026-10-14',
        merchant: 'Netflix',
        description: 'UPI/AUTOPAY/Netflix',
        categoryName: 'Entertainment',
        jarBucket: 'PLAY',
        spendEmotionTag: 'PLANNED_WANT',
        isRecurringMandate: true,
      },
      {
        id: 't6',
        amountMinor: 415100, // ₹4,151 Transfer to Mom
        type: 'TRANSFER',
        local_date: '2026-10-18',
        merchant: 'Mom Family Support',
        description: 'UPI/P2A/Mom',
        categoryName: 'Transfers',
        paymentRail: 'UPI_P2A',
        spendEmotionTag: 'UNPLANNED_TRANSFER',
      },
    ];

    // Total debits = 15000 + 10000 + 1400 + 200 + 649 + 4151 = ₹31,400 (3,140,000 paise)
    const result = analyzeCycleForensics({
      transactions,
      cycleStartDate: '2026-10-05',
      asOfDate: '2026-10-22',
      salaryPaise: 3220000,
      openingBalancePaise: 3720000,
      actualClosingPaise: 580000,
      discretionaryBudgetPaise: 322000, // 10% = ₹3,220
    });

    assert.equal(result.totalDebitsPaise, 3140000);
    assert.equal(result.daysElapsed, 17);
    assert.equal(result.averageBurnPerDayPaise, 184705);
    assert.equal(result.isReconciled, true);

    // Crisis buckets
    const b = result.crisisBuckets;
    assert.equal(b.FIXED_BILLS_RENT_EMI.totalPaise, 2500000); // 15,000 + 10,000
    assert.equal(b.FOOD_TEA_SNACKS.totalPaise, 160000);        // 1,400 + 200
    assert.equal(b.SHOPPING_SUBSCRIPTIONS.totalPaise, 64900); // 649
    assert.equal(b.TRANSFERS_FRIENDS_FAMILY.totalPaise, 415100); // 4,151

    // Sum of all 4 buckets must exactly match total debits
    const sumBuckets =
      b.FIXED_BILLS_RENT_EMI.totalPaise +
      b.FOOD_TEA_SNACKS.totalPaise +
      b.SHOPPING_SUBSCRIPTIONS.totalPaise +
      b.TRANSFERS_FRIENDS_FAMILY.totalPaise;
    assert.equal(sumBuckets, 3140000);
  });

  await t.test('7-Day Velocity alert triggers when >50% spent in first 7 days', () => {
    const transactions = [
      {
        id: 't1',
        amountMinor: 2000000, // ₹20,000 spent on day 2
        type: 'EXPENSE',
        local_date: '2026-10-07',
        merchant: 'Shopping',
        description: 'UPI/Shopping',
      },
    ];

    const result = analyzeCycleForensics({
      transactions,
      cycleStartDate: '2026-10-05',
      salaryPaise: 3220000,
      openingBalancePaise: 3220000,
      discretionaryBudgetPaise: 322000,
    });

    assert.equal(result.first7DaysSpendPaise, 2000000);
    assert.equal(result.sevenDaySalarySpendPct > 50, true);
    assert.equal(result.isVelocityAlertTriggered, true);
  });

  await t.test('Small UPI scans aggregation detects micro-leaks <= ₹200', () => {
    const transactions = [
      { id: '1', amountMinor: 15000, type: 'EXPENSE', local_date: '2026-10-06', merchant: 'Chai' },
      { id: '2', amountMinor: 5000, type: 'EXPENSE', local_date: '2026-10-07', merchant: 'Bakery' },
      { id: '3', amountMinor: 250000, type: 'EXPENSE', local_date: '2026-10-08', merchant: 'Dinner' },
    ];

    const result = analyzeCycleForensics({
      transactions,
      cycleStartDate: '2026-10-05',
      salaryPaise: 3220000,
      openingBalancePaise: 3220000,
      smallScanThresholdPaise: 20000, // ₹200
    });

    assert.equal(result.smallScanCount, 2);
    assert.equal(result.smallScanTotalPaise, 20000); // ₹150 + ₹50 = ₹200
  });

  await t.test('Mandate Radar flags recurring subscriptions and AutoPay', () => {
    const transactions = [
      { id: '1', amountMinor: 64900, type: 'EXPENSE', local_date: '2026-10-06', merchant: 'Netflix', isRecurringMandate: true },
      { id: '2', amountMinor: 1000000, type: 'EXPENSE', local_date: '2026-10-07', merchant: 'Loan EMI', isRecurringMandate: true },
      { id: '3', amountMinor: 50000, type: 'EXPENSE', local_date: '2026-10-08', merchant: 'Swiggy', isRecurringMandate: false },
    ];

    const result = analyzeCycleForensics({
      transactions,
      cycleStartDate: '2026-10-05',
      salaryPaise: 3220000,
      openingBalancePaise: 3220000,
    });

    assert.equal(result.mandates.length, 2);
    assert.equal(result.mandates[0].name, 'Loan EMI');
    assert.equal(result.mandates[0].amountPaise, 1000000);
  });

  await t.test('Universal Statement Importer supports custom column mapping override', () => {
    // Non-standard CSV: Col 0 = Memo, Col 1 = When, Col 2 = Out, Col 3 = In, Col 4 = Bal
    const nonStandardCsv = `Memo,When,Out,In,Bal
Salary Credit,05-10-2026,,32200.00,32200.00
Swiggy Dinner,06-10-2026,1200.00,,31000.00`;

    const customMapping = {
      headerRowIndex: 0,
      dateCol: 1,
      descriptionCol: 0,
      debitCol: 2,
      creditCol: 3,
      balanceCol: 4,
    };

    const res = parseBankStatement(nonStandardCsv, [], undefined, 20000, customMapping);
    assert.equal(res.rows.length, 2);
    assert.equal(res.totalCreditsMinor, 3220000);
    assert.equal(res.totalDebitsMinor, 120000);
    assert.equal(res.rows[1].merchant, 'Swiggy Dinner');
    assert.equal(res.rows[1].amountMinor, 120000);
    assert.equal(res.hasReconciliation, true);
    assert.equal(res.discrepancyMinor, 0);
  });

  await t.test('Universal Statement Importer generates atomic importBatchId for undo capability', () => {
    const csv = `Date,Particulars,Debit,Credit,Balance
05-10-2026,SALARY CREDIT,,32200.00,32200.00
06-10-2026,UPI/P2M/Swiggy,500.00,,31700.00`;

    const res = parseBankStatement(csv);
    assert.ok(res.importBatchId?.startsWith('batch_'));
    assert.equal(res.rows.length, 2);
    assert.equal(res.rows[0].importBatchId, res.importBatchId);
    assert.equal(res.rows[1].importBatchId, res.importBatchId);
  });
});
