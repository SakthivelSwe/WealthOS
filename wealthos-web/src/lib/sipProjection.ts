/**
 * WealthOS SIP Milestone & Trajectory Engine
 * Conforms to WEALTHOS_FINAL_SPEC.md v3 §5.8 and Appendix B.
 * Zero float. Integer paise. HALF_UP rounding.
 */

import { roundHalfUp } from './money';

export interface SipPoint {
  month: number;
  investedPaise: number;
  expectedValuePaise: number;
  marginOfSafetyValuePaise: number;
}

/**
 * Determines the month in which the SIP portfolio reaches the target amount.
 * Convention:
 * contribution at start of month
 * balance = roundHalfUp((balance + sip) * (1200 + annualPct) / 1200)
 * milestone check after growth
 * step-up applied after every 12th month: sip = roundHalfUp(sip * (100 + stepUpPct) / 100)
 */
export function sipMonthsToTarget(
  startSipPaise: number,
  annualStepUpPct: number,
  annualReturnPct: number,
  targetPaise: number,
  capMonths: number = 600
): number | null {
  if (startSipPaise <= 0) throw new Error('Start SIP must be positive');
  if (annualReturnPct < 0) throw new Error('Annual return must be non-negative');
  if (annualStepUpPct < 0) throw new Error('Step-up percent must be non-negative');

  let currentSip = startSipPaise;
  let balance = 0;

  for (let m = 1; m <= capMonths; m++) {
    balance = roundHalfUp((balance + currentSip) * (1200 + annualReturnPct), 1200);
    if (balance >= targetPaise) return m;
    if (m % 12 === 0) {
      currentSip = roundHalfUp(currentSip * (100 + annualStepUpPct), 100);
    }
  }

  return null;
}

/**
 * Projects the dual-curve SIP trajectory (expected vs margin of safety).
 */
export function projectSipTrajectory(
  startSipPaise: number,
  annualStepUpPct: number,
  months: number = 360,
  expectedReturnPct: number = 12,
  marginOfSafetyReturnPct: number = 8
): SipPoint[] {
  const points: SipPoint[] = [];
  let sipExpected = startSipPaise;
  let sipSafety = startSipPaise;
  let balExpected = 0;
  let balSafety = 0;
  let cumulativeInvested = 0;

  for (let m = 1; m <= months; m++) {
    cumulativeInvested += sipExpected;
    balExpected = roundHalfUp((balExpected + sipExpected) * (1200 + expectedReturnPct), 1200);
    balSafety = roundHalfUp((balSafety + sipSafety) * (1200 + marginOfSafetyReturnPct), 1200);

    points.push({
      month: m,
      investedPaise: cumulativeInvested,
      expectedValuePaise: balExpected,
      marginOfSafetyValuePaise: balSafety,
    });

    if (m % 12 === 0) {
      sipExpected = roundHalfUp(sipExpected * (100 + annualStepUpPct), 100);
      sipSafety = roundHalfUp(sipSafety * (100 + annualStepUpPct), 100);
    }
  }

  return points;
}
