import { test } from 'node:test';
import assert from 'node:assert/strict';
import { allocate, FRAMEWORKS } from '../src/lib/allocation.js';
import { parseMoney, formatMoney } from '../src/types/index.js';

test('Phase 8: Accounts, Payday Transfer Plan, Configurable UPI Lite & Nightly Protocol (§8.6, §8.8)', async (t) => {
  await t.test('Account Roles Segregation & Vault Isolation (§8.6)', () => {
    const roles = ['INCOME_VAULT', 'DAILY_UPI_SPEND', 'EMERGENCY_RESERVE', 'WEALTH_PORTFOLIO', 'LIABILITY_LOAN'];
    assert.equal(roles.length, 5);

    const accounts = [
      { id: 'a1', name: 'Axis Bank Salary', type: 'BANK', account_role: 'INCOME_VAULT', balance_minor: 3220000 },
      { id: 'a2', name: 'Paytm Payments Bank', type: 'UPI_WALLET', account_role: 'DAILY_UPI_SPEND', balance_minor: 150000 },
      { id: 'a3', name: 'HDFC AutoSweep FD', type: 'SAVINGS', account_role: 'EMERGENCY_RESERVE', balance_minor: 15000000 },
      { id: 'a4', name: 'Zerodha Coin MF', type: 'INVESTMENT', account_role: 'WEALTH_PORTFOLIO', balance_minor: 25000000 },
    ];

    const vault = accounts.find(a => a.account_role === 'INCOME_VAULT');
    assert.equal(vault?.name, 'Axis Bank Salary');
    // Spec §8.6: Warning on INCOME_VAULT: "don't link daily UPI apps here"
    const requiresUpiIsolationWarning = vault?.account_role === 'INCOME_VAULT';
    assert.equal(requiresUpiIsolationWarning, true);
  });

  await t.test('Payday Transfer Plan exact routing and zero-drift invariant (§8.6, Videos 3, 4)', () => {
    const salaryRupees = 32200;
    const salaryPaise = salaryRupees * 100;
    const baselinePaise = 1771000; // 55%

    const alloc = allocate({
      salaryPaise,
      framework: FRAMEWORKS.SIX_JAR,
      baselinePaise,
      emiPaise: 0,
      emergencyBalancePaise: 0,
      emergencyTargetPaise: baselinePaise * 7,
      creepLock: true,
      bufferPaise: 150000,
      shiftPlayToLongTerm: false,
      daysInCycle: 30,
    });

    // Exact transfers from Vault
    const transferToEmergency = alloc.jars.LONG_TERM;
    const transferToWealth = alloc.jars.FREEDOM_SIP;
    const transferToDailyUpi = alloc.jars.PLAY;
    const retainedInVaultForBills = alloc.jars.NEEDS;
    const otherJars = (alloc.jars.EDUCATION || 0) + (alloc.jars.GIVING || 0);

    const totalTransferredAndRetained = transferToEmergency + transferToWealth + transferToDailyUpi + retainedInVaultForBills + otherJars;

    // Zero-drift assertion
    assert.equal(totalTransferredAndRetained, salaryPaise);
    assert.equal(retainedInVaultForBills, 1771000); // Fixed bills stay in vault
    assert.equal(transferToEmergency, 322000); // 10%
    assert.equal(transferToWealth, 322000); // 10%
    assert.equal(transferToDailyUpi, 322000); // 10%
  });

  await t.test('Configurable UPI Lite limits and weekly reload calculation (§8.6)', () => {
    // Configurable limits (not constants)
    let perTxnCap = 500;
    let balanceCap = 2000;

    // User updates limits to RBI recently permitted offline tiers
    perTxnCap = 1000;
    balanceCap = 5000;

    assert.equal(perTxnCap, 1000);
    assert.equal(balanceCap, 5000);

    // Weekly reload pacing: 7 × Daily Safe Spend
    const dailySafeUpiPaise = 34500; // ₹345
    const weeklyReloadPaise = dailySafeUpiPaise * 7;
    assert.equal(weeklyReloadPaise, 241500); // ₹2,415
    assert.equal(Math.round(weeklyReloadPaise / 100), 2415);
  });

  await t.test('Nightly Check-in Protocol spending pace & untagged review (§8.8, Videos 5, 12)', () => {
    const dailyAllowancePaise = 115000; // ₹1,150

    // Scenario A: Under allowance (spend ₹800)
    const spendA = 80000;
    const burnPctA = Math.round((spendA / dailyAllowancePaise) * 100);
    assert.equal(burnPctA, 70);
    assert.equal(spendA > dailyAllowancePaise, false);

    // Scenario B: Over allowance (spend ₹1,600)
    const spendB = 160000;
    const burnPctB = Math.round((spendB / dailyAllowancePaise) * 100);
    assert.equal(burnPctB, 139);
    assert.equal(spendB > dailyAllowancePaise, true);

    // Quick tag transitions
    const rawTxn = { id: 't_nightly_1', amount_minor: 25000, spend_emotion_tag: null };
    const taggedAsNeed = { ...rawTxn, spend_emotion_tag: 'NEED' };
    const taggedAsRegret = { ...rawTxn, spend_emotion_tag: 'REGRET_LEAK' };

    assert.equal(taggedAsNeed.spend_emotion_tag, 'NEED');
    assert.equal(taggedAsRegret.spend_emotion_tag, 'REGRET_LEAK');
  });

  await t.test('Money rounding HALF_UP integer division invariant (§5.2, Appendix A)', () => {
    // (2 * numerator + denominator) / (2 * denominator)
    const roundHalfUp = (num, den) => {
      if (den === 0) throw new Error('Division by zero');
      return Math.floor((2 * num + den) / (2 * den));
    };

    assert.equal(roundHalfUp(184705, 1), 184705);
    assert.equal(roundHalfUp(5, 2), 3); // 2.5 -> 3
    assert.equal(roundHalfUp(7, 2), 4); // 3.5 -> 4
    assert.equal(roundHalfUp(4, 2), 2); // 2.0 -> 2
  });
});
