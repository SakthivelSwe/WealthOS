import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateEmergencyTiers,
  evaluateEmi,
  evaluateSavingsRate,
  evaluateRunway,
  calculateVitalSigns,
} from '../src/lib/healthRules.js';
import { analyzeForensics } from '../src/lib/forensics.js';

test('Appendix A.6: Emergency Tiers Golden Values', () => {
  // Target: 7 * ₹17,710 = ₹1,23,970 (12,397,000 paise)
  const target = 12397000;
  const tiers = calculateEmergencyTiers(target);

  assert.equal(tiers.instantCashPaise, 2479400);    // ₹24,794
  assert.equal(tiers.autoSweepFdRdPaise, 6198500);  // ₹61,985
  assert.equal(tiers.liquidFundGoldPaise, 3719100); // ₹37,191
  assert.equal(tiers.instantCashPaise + tiers.autoSweepFdRdPaise + tiers.liquidFundGoldPaise, target);
});

test('Appendix A.6: EMI Health Boundaries', () => {
  // ₹4,600 on ₹32,200 passes 1/7 rule
  assert.equal(evaluateEmi(460000, 3220000), 'GREEN');
  // ₹4,601 fails 1/7 rule, but 4601 * 100 <= 32200 * 30 -> AMBER
  assert.equal(evaluateEmi(460100, 3220000), 'AMBER');
  // > 30% is RED
  assert.equal(evaluateEmi(1000000, 3220000), 'RED');
});

test('Savings Rate and Runway Boundaries', () => {
  // Savings rate
  assert.equal(evaluateSavingsRate(200000, 1000000), 'GREEN');
  assert.equal(evaluateSavingsRate(150000, 1000000), 'AMBER');
  assert.equal(evaluateSavingsRate(70000, 1000000), 'AMBER');
  assert.equal(evaluateSavingsRate(60000, 1000000), 'RED');

  // Runway
  assert.equal(evaluateRunway(700000, 100000), 'GREEN');
  assert.equal(evaluateRunway(600000, 100000), 'AMBER');
  assert.equal(evaluateRunway(300000, 100000), 'AMBER');
  assert.equal(evaluateRunway(200000, 100000), 'RED');
});

test('Appendix A.6: Forensics Golden Check', () => {
  // ₹31,400 over 17 days -> 184,705 paise per day
  const debits = 3140000;
  const days = 17;
  const report = analyzeForensics(3220000, 0, debits, 80000, days);

  assert.equal(report.averageBurnPerDayPaise, 184705);
  assert.equal(report.expectedClosingPaise, 80000);
  assert.equal(report.isReconciled, true);
  assert.equal(report.discrepancyPaise, 0);
});
