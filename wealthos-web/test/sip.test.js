import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { sipMonthsToTarget } from '../src/lib/sipProjection.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

test('Appendix A.3: SIP Milestone Golden Values', () => {
  const vectorPath = path.resolve(__dirname, '../../shared-test-vectors/sip.json');
  const raw = fs.readFileSync(vectorPath, 'utf8');
  const vectors = JSON.parse(raw);

  const startSip = vectors.startSipPaise; // 1,000,000 paise (₹10,000)
  const stepUp = vectors.stepUpPct;       // 10%

  for (const m of vectors.milestones) {
    // 12% expected return
    const actual12 = sipMonthsToTarget(startSip, stepUp, 12, m.targetPaise);
    assert.equal(
      actual12,
      m.expectedMonths12Pct,
      `Milestone ${m.name} @ 12%: expected month ${m.expectedMonths12Pct}, got ${actual12}`
    );

    // 8% margin of safety return
    const actual8 = sipMonthsToTarget(startSip, stepUp, 8, m.targetPaise);
    assert.equal(
      actual8,
      m.marginOfSafetyMonths8Pct,
      `Milestone ${m.name} @ 8%: expected month ${m.marginOfSafetyMonths8Pct}, got ${actual8}`
    );
  }
});
