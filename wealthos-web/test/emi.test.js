import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { calculateEmi, evaluateEmiAffordability } from '../src/lib/emiSimulator.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

test('Appendix A.5: EMI Simulator Golden Values', () => {
  const vectorPath = path.resolve(__dirname, '../../shared-test-vectors/emi.json');
  const raw = fs.readFileSync(vectorPath, 'utf8');
  const vectors = JSON.parse(raw);

  const res = calculateEmi(vectors.principalPaise, vectors.annualPercent, vectors.tenureMonths);

  assert.equal(res.emiPaise, vectors.expectedEmiPaise);
  assert.equal(res.totalInterestPaise, vectors.expectedTotalInterestPaise);
  assert.equal(res.totalRepaymentPaise, vectors.expectedTotalRepaymentPaise);
});

test('Appendix A.6: EMI Affordability 1/7 Boundary', () => {
  // ₹4,600 on ₹32,200 passes 1/7 rule (4600*7 = 32200 <= 32200); ₹4,601 does not
  const salary = 3220000;
  const baseline = 1771000;
  const emg = 12397000;

  const passCheck = evaluateEmiAffordability(460000, salary, baseline, emg);
  assert.equal(passCheck.passesOneSeventhRule, true);

  const failCheck = evaluateEmiAffordability(460100, salary, baseline, emg);
  assert.equal(failCheck.passesOneSeventhRule, false);
});
