import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { allocate, FRAMEWORKS } from '../src/lib/allocation.ts';
import { sipMonthsToTarget, projectSipTrajectory } from '../src/lib/sipProjection.ts';
import { compareStrategies, payoff } from '../src/lib/debtPayoff.ts';
import { calculateEmi, evaluateEmiAffordability } from '../src/lib/emiSimulator.ts';
import { evaluateEmi, evaluateSavingsRate, evaluateRunway, calculateEmergencyTiers } from '../src/lib/healthRules.ts';

describe('Phase 5: Plans & Allocation UI / Simulators (§8.3, §5.8 - §5.12)', () => {
  it('Allocation Simulator reproduces golden values across all frameworks (§5.3)', () => {
    // 6-Jar at ₹32,200 salary
    const allocSixJar = allocate({
      salaryPaise: 3220000,
      framework: FRAMEWORKS.SIX_JAR,
      baselinePaise: 1771000,
      emiPaise: 0,
      daysInCycle: 30,
    });

    assert.equal(allocSixJar.jars['NEEDS'], 1771000); // ₹17,710
    assert.equal(allocSixJar.jars['FREEDOM_SIP'], 322000); // ₹3,220
    assert.equal(allocSixJar.jars['EDUCATION'], 322000); // ₹3,220
    assert.equal(allocSixJar.jars['PLAY'], 322000); // ₹3,220
    assert.equal(allocSixJar.jars['LONG_TERM'], 322000); // ₹3,220
    assert.equal(allocSixJar.jars['GIVING'], 161000); // ₹1,610
    assert.equal(allocSixJar.dailySafePaise, 10733); // ₹107.33/day
    assert.equal(allocSixJar.weeklySafePaise, 75133); // ₹751.33/week

    // 3-Box at ₹40,000 salary
    const allocThreeBox = allocate({
      salaryPaise: 4000000,
      framework: FRAMEWORKS.THREE_BOX,
      baselinePaise: 1771000,
      emiPaise: 0,
      bufferPaise: 150000,
      daysInCycle: 30,
    });

    assert.equal(allocThreeBox.jars['NEEDS'], 1921000); // ₹19,210 (baseline + buffer)
    assert.equal(allocThreeBox.jars['EMERGENCY'], 479000); // ₹4,790
    assert.equal(allocThreeBox.jars['BUFFER'], 400000); // ₹4,000
    assert.equal(allocThreeBox.jars['MEDIUM_TERM'], 400000); // ₹4,000
    assert.equal(allocThreeBox.jars['LONG_TERM_SIP'], 800000); // ₹8,000

    // 50/30/20 at ₹32,200 salary
    const alloc503020 = allocate({
      salaryPaise: 3220000,
      framework: FRAMEWORKS.FIFTY_THIRTY_TWENTY,
      baselinePaise: 1771000,
      emiPaise: 0,
      daysInCycle: 30,
    });

    assert.equal(alloc503020.jars['NEEDS'], 1771000); // ₹17,710
    assert.equal(alloc503020.jars['WANTS'], 805000); // ₹8,050
    assert.equal(alloc503020.jars['SAVINGS'], 644000); // ₹6,440
    assert.equal(alloc503020.dailySafePaise, 26833); // ₹268.33/day
  });

  it('SIP Simulator accurately calculates milestone speedometers (§5.8 / Appendix A.3)', () => {
    const sipPaise = 1000000; // ₹10,000/month
    const stepUpPct = 10; // 10% annual step-up

    // Expected 12% returns
    assert.equal(sipMonthsToTarget(sipPaise, stepUpPct, 12, 100000000), 61); // ₹10L -> Month 61
    assert.equal(sipMonthsToTarget(sipPaise, stepUpPct, 12, 500000000), 144); // ₹50L -> Month 144
    assert.equal(sipMonthsToTarget(sipPaise, stepUpPct, 12, 1000000000), 190); // ₹1Cr -> Month 190
    assert.equal(sipMonthsToTarget(sipPaise, stepUpPct, 12, 1500000000), 219); // ₹1.5Cr -> Month 219

    // Margin of safety 8% returns
    assert.equal(sipMonthsToTarget(sipPaise, stepUpPct, 8, 100000000), 65); // ₹10L -> Month 65
    assert.equal(sipMonthsToTarget(sipPaise, stepUpPct, 8, 500000000), 162); // ₹50L -> Month 162
    assert.equal(sipMonthsToTarget(sipPaise, stepUpPct, 8, 1000000000), 216); // ₹1Cr -> Month 216
    assert.equal(sipMonthsToTarget(sipPaise, stepUpPct, 8, 1500000000), 251); // ₹1.5Cr -> Month 251
  });

  it('Debt Payoff Simulator computes Snowball vs Avalanche advantage (§5.9 / Appendix A.4)', () => {
    const loans = [
      { name: 'Card A', balancePaise: 6000000, rateBps: 3600, minimumPaymentPaise: 300000 },
      { name: 'Personal B', balancePaise: 2000000, rateBps: 1800, minimumPaymentPaise: 150000 },
      { name: 'Vehicle C', balancePaise: 12000000, rateBps: 1400, minimumPaymentPaise: 400000 },
    ];
    const extraPaise = 200000; // ₹2,000 extra

    const comparison = compareStrategies(loans, extraPaise);

    // Snowball
    assert.equal(comparison.snowball.months, 24);
    assert.equal(comparison.snowball.totalInterestPaise, 4165614); // ₹41,656.14

    // Avalanche
    assert.equal(comparison.avalanche.months, 23);
    assert.equal(comparison.avalanche.totalInterestPaise, 3970646); // ₹39,706.46

    // Savings
    assert.equal(comparison.monthsSaved, 1);
    assert.equal(comparison.interestSavedPaise, 194968); // ₹1,949.68 saved
  });

  it('EMI Affordability Simulator tests reducing-balance and job-loss runway (§5.10 / Appendix A.5)', () => {
    // ₹5,00,000 at 12% for 36 months
    const emi = calculateEmi(50000000, 12, 36);
    assert.equal(emi.emiPaise, 1660715); // ₹16,607.15/month
    assert.equal(emi.totalInterestPaise, 9785763); // ₹97,857.63 total interest
    assert.equal(emi.totalRepaymentPaise, 59785763); // ₹5,97,857.63 total repayment

    // Affordability check on ₹60,000 salary
    const check = evaluateEmiAffordability(
      emi.emiPaise, // ₹16,607.15
      6000000, // ₹60,000 salary
      2000000, // ₹20,000 baseline
      15000000 // ₹1,50,000 liquid emergency reserve
    );

    // 16607.15 * 7 = 116250.05 > 60000 -> fails 1/7 rule
    assert.equal(check.passesOneSeventhRule, false);
    // 16607.15 * 100 <= 60000 * 30 (27.6% <= 30%) -> passes 30% cap
    assert.equal(check.passesThirtyPercentCap, true);
    assert.equal(check.emiToIncomePctOneDecimal, '27.6%');

    // Monthly burn: ₹20,000 baseline + ₹16,607.15 EMI = ₹36,607.15
    // Runway with ₹1,50,000 reserve = 150000 / 36607.15 = 4 months
    assert.equal(check.jobLossRunwayMonths, 4);
  });

  it('7-7-7 Health Rules enforce boundaries (§5.6 / Appendix A.6)', () => {
    // EMI 1/7 Boundary: ₹4,600 on ₹32,200 passes; ₹4,601 does not
    assert.equal(evaluateEmi(460000, 3220000), 'GREEN');
    assert.equal(evaluateEmi(460100, 3220000), 'AMBER');

    // 30% ceiling on ₹32,200: 32200 * 0.30 = 9660. ₹9,660 is AMBER; ₹9,661 is RED
    assert.equal(evaluateEmi(966000, 3220000), 'AMBER');
    assert.equal(evaluateEmi(966100, 3220000), 'RED');

    // Emergency tiers on 7 * ₹17,710 = ₹1,23,970 (12,397,000 paise)
    const tiers = calculateEmergencyTiers(12397000);
    assert.equal(tiers.instantCashPaise, 2479400); // 20% = ₹24,794
    assert.equal(tiers.autoSweepFdRdPaise, 6198500); // 50% = ₹61,985
    assert.equal(tiers.liquidFundGoldPaise, 3719100); // 30% = ₹37,191
    assert.equal(tiers.instantCashPaise + tiers.autoSweepFdRdPaise + tiers.liquidFundGoldPaise, 12397000);
  });
});
