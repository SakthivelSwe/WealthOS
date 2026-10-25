import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { payoff, compareStrategies } from '../src/lib/debtPayoff.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

test('Appendix A.4: Debt Payoff Golden Values', () => {
  const vectorPath = path.resolve(__dirname, '../../shared-test-vectors/debt.json');
  const raw = fs.readFileSync(vectorPath, 'utf8');
  const vectors = JSON.parse(raw);

  const loans = vectors.loans.map(l => ({
    name: l.name,
    balancePaise: l.balancePaise,
    rateBps: l.annualRateBps,
    minimumPaymentPaise: l.minimumPaymentPaise,
  }));
  const extra = vectors.extraPaymentPaise;

  // Snowball
  const snowball = payoff(loans, extra, 'snowball');
  assert.equal(snowball.months, vectors.snowball.months);
  assert.equal(snowball.totalInterestPaise, vectors.snowball.totalInterestPaise);

  // Avalanche
  const avalanche = payoff(loans, extra, 'avalanche');
  assert.equal(avalanche.months, vectors.avalanche.months);
  assert.equal(avalanche.totalInterestPaise, vectors.avalanche.totalInterestPaise);

  // Comparison
  const comparison = compareStrategies(loans, extra);
  assert.equal(comparison.interestSavedPaise, vectors.avalancheSavings.interestSavedPaise);
  assert.equal(comparison.monthsSaved, vectors.avalancheSavings.monthsSaved);
});
