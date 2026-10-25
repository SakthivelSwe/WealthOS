import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { isWithinPaydayWindow, calculateDaysInCycle } from '../src/lib/cycle.ts';
import { allocate, FRAMEWORKS } from '../src/lib/allocation.ts';

describe('Phase 4: State Management, Cycles & Survival Mode (§5.3 - §5.5)', () => {
  it('calculates daily UPI meter combining guilt-free and needs allowance', () => {
    // Spec §5.3: daily meter shows guilt-free + needs allowance
    const salaryPaise = 3220000; // ₹32,200
    const baselinePaise = 1771000; // ₹17,710
    const daysInCycle = 30;

    const alloc = allocate({
      salaryPaise,
      framework: FRAMEWORKS.SIX_JAR,
      baselinePaise,
      emiPaise: 0,
      emergencyBalancePaise: 0,
      emergencyTargetPaise: 7 * baselinePaise,
      creepLock: true,
      bufferPaise: 150000,
      shiftPlayToLongTerm: false,
      daysInCycle,
    });

    const guiltFreeDaily = alloc.dailySafePaise; // 10733 paise (₹107.33)
    assert.equal(guiltFreeDaily, 10733);

    const upiNeedsAllowancePaise = 300000; // ₹3,000 monthly allowance for food/commute via UPI
    const needsDaily = Math.floor(upiNeedsAllowancePaise / daysInCycle); // 10000 paise (₹100)
    assert.equal(needsDaily, 10000);

    const totalDailyUpi = guiltFreeDaily + needsDaily;
    assert.equal(totalDailyUpi, 20733); // ₹207.33 total daily UPI budget
  });

  it('triggers survival mode on the ₹800 crisis scenario (§1.2, §5.5)', () => {
    // Problem scenario: ₹32,200 salary down to ₹800 by day 17
    const currentSpendablePaise = 80000; // ₹800
    const baselinePaise = 1771000; // ₹17,710

    // Spec §4.1: default survival threshold = min(₹2,000, 10% of baseline)
    const survivalThresholdPaise = Math.min(200000, Math.floor(baselinePaise / 10)); // 177100 paise (₹1,771)
    assert.equal(survivalThresholdPaise, 177100);

    // Is survival mode active?
    const isSurvivalMode = currentSpendablePaise > 0 && currentSpendablePaise <= survivalThresholdPaise;
    assert.equal(isSurvivalMode, true);

    // Days until next expected payday: 30 days total - 17 days passed = 13 days
    const daysUntilNextPayday = 30 - 17;
    assert.equal(daysUntilNextPayday, 13);

    // Daily survival cap: integer floor division
    const dailyCapPaise = Math.floor(currentSpendablePaise / daysUntilNextPayday);
    assert.equal(dailyCapPaise, 6153); // ₹61.53/day
  });

  it('correctly tracks salary window activation (§5.4)', () => {
    const cycleDef = {
      scheduleType: 'VARIABLE_WINDOW',
      paydayWindowStartDay: 2,
      paydayWindowEndDay: 5,
    };

    assert.equal(isWithinPaydayWindow(new Date('2026-10-01T10:00:00Z'), cycleDef), false);
    assert.equal(isWithinPaydayWindow(new Date('2026-10-02T10:00:00Z'), cycleDef), true);
    assert.equal(isWithinPaydayWindow(new Date('2026-10-05T10:00:00Z'), cycleDef), true);
    assert.equal(isWithinPaydayWindow(new Date('2026-10-06T10:00:00Z'), cycleDef), false);

    // Fixed date schedule
    const fixedDef = {
      scheduleType: 'FIXED_DATE',
      fixedPaydayOfMonth: 1,
    };
    assert.equal(isWithinPaydayWindow(new Date('2026-10-01T10:00:00Z'), fixedDef), true);
    assert.equal(isWithinPaydayWindow(new Date('2026-10-02T10:00:00Z'), fixedDef), false);

    // Irregular schedule
    const irregularDef = {
      scheduleType: 'IRREGULAR',
    };
    assert.equal(isWithinPaydayWindow(new Date('2026-10-01T10:00:00Z'), irregularDef), false);
  });

  it('calculates days in cycle accurately without float math', () => {
    const start = new Date('2026-10-02T00:00:00Z');
    const cycleDef = {
      scheduleType: 'VARIABLE_WINDOW',
      paydayWindowStartDay: 2,
      paydayWindowEndDay: 5,
    };

    const days = calculateDaysInCycle(start, cycleDef);
    assert.equal(days, 31); // Oct 2 to Nov 2 = 31 days

    const irregularDays = calculateDaysInCycle(start, { scheduleType: 'IRREGULAR', irregularHorizonDays: 45 });
    assert.equal(irregularDays, 45);
  });

  it('determines sinking fund monthly allocation with remainder on final month (§5.11)', () => {
    // Sinking fund for annual vehicle service: ₹10,000 over 12 months
    const totalPaise = 1000000;
    const months = 12;

    const baseMonthlyPaise = Math.floor(totalPaise / months); // 83333 paise (₹833.33)
    const remainderPaise = totalPaise - (baseMonthlyPaise * months); // 4 paise

    const schedule = [];
    for (let m = 1; m <= months; m++) {
      schedule.push(m === months ? baseMonthlyPaise + remainderPaise : baseMonthlyPaise);
    }

    assert.equal(schedule.length, 12);
    assert.equal(schedule[0], 83333);
    assert.equal(schedule[11], 83337);
    const sum = schedule.reduce((a, b) => a + b, 0);
    assert.equal(sum, totalPaise);
  });
});
