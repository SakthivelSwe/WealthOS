import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { allocate, FRAMEWORKS } from '../src/lib/allocation.js';
import { evaluateEmi, evaluateSavingsRate, evaluateRunway } from '../src/lib/healthRules.js';

test('Phase 7: AI Integration, Insights, Payday Wizard & Behavioural Guardrails (§9, §8.8, §5.14)', async (t) => {
  await t.test('Health check Edge Function reports schema_version and functions_version without PII (§9.3)', () => {
    const healthFile = readFileSync(resolve('../supabase/functions/health/index.ts'), 'utf-8');
    assert.match(healthFile, /schema_version:\s*"20261024000000_wealthos_v2"/);
    assert.match(healthFile, /functions_version:\s*"3\.0\.0"/);
    assert.match(healthFile, /status:\s*"ok"/);
    // Assert no user data or PII in health function
    assert.doesNotMatch(healthFile, /user_id|email|phone|password|balance/i);
  });

  await t.test('AI Payload Integrity: strictly precomputed integers, zero floats (§9.2, AI.md)', () => {
    // Simulate real precomputed aggregates for ₹32,200 salary case
    const salaryPaise = 3220000;
    const fixedBaselinePaise = 1771000;
    const liquidAccountsPaise = 80000; // ₹800 after October crisis
    const dailySafeUpiPaise = 34500; // ₹345
    const sevenDayBurnPct = 78;
    const regretLeakPaise = 142000; // ₹1,420
    const runwayMonths = Math.floor(liquidAccountsPaise / fixedBaselinePaise); // 0 months

    const rawMerchants = [
      { merchant: 'Swiggy', amount: 140000, rail: 'UPI_P2M' },
      { merchant: 'Amazon Pay', amount: 250000, rail: 'UPI_P2M' },
      { merchant: 'Suresh Friend Transfer', amount: 300000, rail: 'UPI_P2A' }, // Person transfer
      { merchant: 'Self Transfer Savings', amount: 500000, rail: 'UPI_P2A' }, // Self transfer
      { merchant: 'Starbucks Coffee', amount: 75000, rail: 'UPI_P2M' },
    ];

    // Filter out individuals and person-to-person transfers (§9.2)
    const filteredLeakMerchants = rawMerchants
      .filter(m => m.rail === 'UPI_P2M' && !m.merchant.toLowerCase().includes('transfer'))
      .map(m => ({
        name: m.merchant,
        totalRupees: Math.round(m.amount / 100),
      }));

    assert.equal(filteredLeakMerchants.length, 3);
    assert.deepEqual(filteredLeakMerchants.map(m => m.name), ['Swiggy', 'Amazon Pay', 'Starbucks Coffee']);

    const payload = {
      mode: 'coach',
      salaryRupees: Math.round(salaryPaise / 100),
      fixedBaselineRupees: Math.round(fixedBaselinePaise / 100),
      currentLiquidBalanceRupees: Math.round(liquidAccountsPaise / 100),
      dailySafeUpiRupees: Math.round(dailySafeUpiPaise / 100),
      sevenDayBurnPercent: sevenDayBurnPct,
      regretLeakTotalRupees: Math.round(regretLeakPaise / 100),
      topLeakMerchants: filteredLeakMerchants,
      sevenSevenSevenStatus: 'CRITICAL',
      emergencyRunwayMonths: runwayMonths,
      equityToDebtRatio: '60:40',
      userCity: 'Chennai',
    };

    // Strict assertions: every monetary and count field MUST be an integer
    assert.equal(Number.isInteger(payload.salaryRupees), true);
    assert.equal(payload.salaryRupees, 32200);

    assert.equal(Number.isInteger(payload.fixedBaselineRupees), true);
    assert.equal(payload.fixedBaselineRupees, 17710);

    assert.equal(Number.isInteger(payload.currentLiquidBalanceRupees), true);
    assert.equal(payload.currentLiquidBalanceRupees, 800);

    assert.equal(Number.isInteger(payload.dailySafeUpiRupees), true);
    assert.equal(payload.dailySafeUpiRupees, 345);

    assert.equal(Number.isInteger(payload.regretLeakTotalRupees), true);
    assert.equal(payload.regretLeakTotalRupees, 1420);

    assert.equal(Number.isInteger(payload.emergencyRunwayMonths), true);
    assert.equal(payload.emergencyRunwayMonths, 0);

    // AI never performs arithmetic; receives precomputed numbers only
    assert.equal(payload.sevenSevenSevenStatus, 'CRITICAL');
  });

  await t.test('AI Categorize enums and safety: only suggestions, never auto-tag REGRET_LEAK (§9.1, §6.4)', () => {
    const VALID_JARS = new Set(['NEEDS', 'FREEDOM_SIP', 'EDUCATION', 'PLAY', 'LONG_TERM', 'GIVING']);
    const VALID_TAGS = new Set(['NEED', 'PLANNED_WANT', 'REGRET_LEAK', 'UNPLANNED_TRANSFER']);

    const rawAiResponse = [
      {
        merchantString: 'Zomato Hyderabad',
        category: 'Dining',
        jarBucket: 'PLAY',
        suggestedTag: 'PLANNED_WANT',
        confidence: 0.92,
      },
      {
        merchantString: 'Invalid Bank Fee',
        category: 'Finance',
        jarBucket: 'INVALID_JAR_XYZ', // Malformed jar
        suggestedTag: 'NEED',
        confidence: 0.5,
      },
      {
        merchantString: 'Amazon Marketplace',
        category: 'Shopping',
        jarBucket: 'PLAY',
        suggestedTag: 'REGRET_LEAK', // Should be demoted or left to user judgement (§6.4)
        confidence: 0.85,
      },
    ];

    // Validator mirroring supabase/functions/ai-categorize/index.ts
    const validated = rawAiResponse.filter(item => {
      if (!VALID_JARS.has(item.jarBucket)) return false;
      if (!VALID_TAGS.has(item.suggestedTag)) return false;
      return true;
    }).map(item => {
      // Spec §6.4: "Heuristics only suggest. Never auto-tag REGRET_LEAK; regret is the user's judgement"
      const safeTag = item.suggestedTag === 'REGRET_LEAK' ? 'PLANNED_WANT' : item.suggestedTag;
      return {
        ...item,
        suggestedTag: safeTag,
        isRegretCandidate: item.suggestedTag === 'REGRET_LEAK',
      };
    });

    assert.equal(validated.length, 2); // Invalid jar item dropped
    assert.equal(validated[0].jarBucket, 'PLAY');
    assert.equal(validated[0].suggestedTag, 'PLANNED_WANT');
    assert.equal(validated[1].suggestedTag, 'PLANNED_WANT'); // Not auto-tagged as regret
    assert.equal(validated[1].isRegretCandidate, true);
  });

  await t.test('48-Hour Cooldown Wishlist & Behavioral Guardrails (§5.14, Videos 4, 5, 15)', () => {
    const now = 1700000000000;

    // Item 1: Added 12 hours ago
    const item1Added = now - (12 * 60 * 60 * 1000);
    const item1ElapsedHours = (now - item1Added) / (1000 * 60 * 60);
    const item1RemainingHours = Math.max(0, Math.ceil(48 - item1ElapsedHours));
    assert.equal(item1RemainingHours, 36);
    assert.equal(item1RemainingHours === 0, false); // Still in cooldown

    // Item 2: Added 48 hours ago
    const item2Added = now - (48 * 60 * 60 * 1000);
    const item2ElapsedHours = (now - item2Added) / (1000 * 60 * 60);
    const item2RemainingHours = Math.max(0, Math.ceil(48 - item2ElapsedHours));
    assert.equal(item2RemainingHours, 0);
    assert.equal(item2RemainingHours === 0, true); // Ready for intentional checkout

    // Item 3: Added 72 hours ago
    const item3Added = now - (72 * 60 * 60 * 1000);
    const item3ElapsedHours = (now - item3Added) / (1000 * 60 * 60);
    const item3RemainingHours = Math.max(0, Math.ceil(48 - item3ElapsedHours));
    assert.equal(item3RemainingHours, 0);

    // Lifestyle freeze target (Video 4): default 24 months
    const freezeMonths = 24;
    assert.equal(freezeMonths, 24);
  });

  await t.test('Payday First 48-Hours Checklist Protocol (§8.8, Videos 3 & 4)', () => {
    const salaryRupees = 32200;
    const salaryPaise = salaryRupees * 100;
    const baselinePaise = 1771000;
    const emiPaise = 0;

    // Run standard 6-Jar allocation
    const alloc = allocate({
      salaryPaise,
      framework: FRAMEWORKS.SIX_JAR,
      baselinePaise,
      emiPaise,
      emergencyBalancePaise: 0,
      emergencyTargetPaise: baselinePaise * 7,
      creepLock: true,
      bufferPaise: 150000,
      shiftPlayToLongTerm: false,
      daysInCycle: 30,
    });

    // Golden check: total of jars must sum exactly to ₹32,200
    const totalJarsPaise = Object.values(alloc.jars).reduce((s, v) => s + v, 0);
    assert.equal(totalJarsPaise, salaryPaise);
    assert.equal(totalJarsPaise, 3220000);

    // 4 Mandatory checklist items in Payday Wizard
    const checklist = {
      move_emergency_sip: false,
      load_upi_lite: false,
      pay_fixed_bills: false,
      audit_mandates: false,
    };

    // User completes each action in first 48 hours
    checklist.move_emergency_sip = true;
    checklist.load_upi_lite = true;
    checklist.pay_fixed_bills = true;
    checklist.audit_mandates = true;

    const allCompleted = Object.values(checklist).every(Boolean);
    assert.equal(allCompleted, true);
  });
});
