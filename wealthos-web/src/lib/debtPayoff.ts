/**
 * WealthOS Multi-Loan Debt Payoff Engine
 * Conforms to WEALTHOS_FINAL_SPEC.md v3 §5.9 and Appendix B.
 * Implements constant monthly budget roll-forward for Snowball and Avalanche.
 * Pure integer paise arithmetic.
 */

import { roundHalfUp } from './money';

export type DebtStrategy = 'snowball' | 'avalanche';

export interface DebtLoan {
  name: string;
  balancePaise: number;
  rateBps: number; // annual percent * 100 (e.g. 1800 for 18%)
  minimumPaymentPaise: number;
}

export interface DebtPayoffResult {
  strategy: DebtStrategy;
  months: number;
  totalInterestPaise: number;
  isTerminating: boolean;
}

export interface DebtComparisonResult {
  snowball: DebtPayoffResult;
  avalanche: DebtPayoffResult;
  interestSavedPaise: number;
  monthsSaved: number;
}

const MAX_MONTHS = 1200;

/**
 * Simulates debt payoff across multiple loans with a constant monthly budget.
 */
export function payoff(
  loans: DebtLoan[],
  extraMonthlyPaymentPaise: number,
  strategy: DebtStrategy
): DebtPayoffResult {
  if (loans.length === 0 || loans.every(l => l.balancePaise === 0)) {
    return { strategy, months: 0, totalInterestPaise: 0, isTerminating: true };
  }

  const ls = loans.map(l => ({
    name: l.name,
    bal: l.balancePaise,
    rate: l.rateBps,
    min: l.minimumPaymentPaise,
  }));

  const budget = ls.reduce((acc, l) => acc + l.min, 0) + extraMonthlyPaymentPaise;

  // First month interest guard: budget must exceed first month interest
  const initialMonthInterest = ls.reduce(
    (acc, l) => acc + roundHalfUp(l.bal * l.rate, 120000),
    0
  );

  if (budget <= initialMonthInterest) {
    return {
      strategy,
      months: MAX_MONTHS,
      totalInterestPaise: initialMonthInterest * MAX_MONTHS,
      isTerminating: false,
    };
  }

  let months = 0;
  let totalInterest = 0;

  while (ls.some(l => l.bal > 0)) {
    if (months >= MAX_MONTHS) {
      return { strategy, months: MAX_MONTHS, totalInterestPaise: totalInterest, isTerminating: false };
    }
    months++;

    // 1. Accrue interest on open loans
    for (const l of ls) {
      if (l.bal > 0) {
        const i = roundHalfUp(l.bal * l.rate, 120000);
        l.bal += i;
        totalInterest += i;
      }
    }

    let avail = budget;

    // 2. Pay minimums
    for (const l of ls) {
      if (l.bal > 0) {
        const p = Math.min(l.min, l.bal);
        l.bal -= p;
        avail -= p;
      }
    }

    // 3. Apply available budget to target active loans
    const act = ls.filter(l => l.bal > 0);
    act.sort((a, b) => {
      if (strategy === 'snowball') {
        const diff = a.bal - b.bal;
        if (diff !== 0) return diff;
        return a.name.localeCompare(b.name);
      } else {
        const rateDiff = b.rate - a.rate;
        if (rateDiff !== 0) return rateDiff;
        const balDiff = a.bal - b.bal;
        if (balDiff !== 0) return balDiff;
        return a.name.localeCompare(b.name);
      }
    });

    for (const l of act) {
      if (avail <= 0) break;
      const p = Math.min(avail, l.bal);
      l.bal -= p;
      avail -= p;
    }
  }

  return { strategy, months, totalInterestPaise: totalInterest, isTerminating: true };
}

export function compareStrategies(
  loans: DebtLoan[],
  extraMonthlyPaymentPaise: number
): DebtComparisonResult {
  const snowball = payoff(loans, extraMonthlyPaymentPaise, 'snowball');
  const avalanche = payoff(loans, extraMonthlyPaymentPaise, 'avalanche');
  return {
    snowball,
    avalanche,
    interestSavedPaise: snowball.totalInterestPaise - avalanche.totalInterestPaise,
    monthsSaved: snowball.months - avalanche.months,
  };
}
