/**
 * WealthOS EMI & Loan Affordability Simulator
 * Conforms to WEALTHOS_FINAL_SPEC.md v3 §5.10 and Appendix B.
 * Pure integer / BigInt scaled arithmetic. Zero float.
 */

import { roundHalfUp } from './money';

export interface EmiSimulationResult {
  principalPaise: number;
  annualPercent: number;
  tenureMonths: number;
  emiPaise: number;
  totalInterestPaise: number;
  totalRepaymentPaise: number;
  finalPaymentPaise: number;
}

export interface EmiAffordabilityCheck {
  emiPaise: number;
  salaryPaise: number;
  baselinePaise: number;
  liquidEmergencyPaise: number;
  passesOneSeventhRule: boolean; // emi * 7 <= salary
  passesThirtyPercentCap: boolean; // emi * 100 <= salary * 30
  emiToIncomePctOneDecimal: string;
  jobLossRunwayMonths: number;
}

const SCALE = 100000000000000n; // 10^14 precision for rate calculation

/**
 * Calculates reducing-balance EMI and amortisation using exact BigInt scaled arithmetic.
 * The last payment absorbs rounding.
 */
export function calculateEmi(
  principalPaise: number,
  annualPercent: number,
  tenureMonths: number
): EmiSimulationResult {
  if (principalPaise <= 0) throw new Error('Principal must be positive');
  if (tenureMonths <= 0) throw new Error('Tenure must be positive');
  if (annualPercent < 0) throw new Error('Annual interest rate must be non-negative');

  if (annualPercent === 0) {
    const emi = roundHalfUp(principalPaise, tenureMonths);
    const totalRepayment = principalPaise;
    const finalPayment = principalPaise - emi * (tenureMonths - 1);
    return {
      principalPaise,
      annualPercent,
      tenureMonths,
      emiPaise: emi,
      totalInterestPaise: 0,
      totalRepaymentPaise: totalRepayment,
      finalPaymentPaise: finalPayment,
    };
  }

  // r = annualPercent / 1200
  // Scaled r = (annualPercent * SCALE) / 1200
  // Support fractional annual rates (e.g. 10.5% -> rateBps 1050)
  const rateBps = BigInt(Math.round(annualPercent * 100));
  const rScale = (rateBps * SCALE) / 120000n;

  // (1 + r)^n
  let growth = SCALE;
  for (let i = 0; i < tenureMonths; i++) {
    growth = (growth * (SCALE + rScale)) / SCALE;
  }

  // EMI = P * r * (1+r)^n / ((1+r)^n - 1)
  const P = BigInt(principalPaise);
  const num = P * rScale * growth;
  const den = (growth - SCALE) * SCALE;
  const emi = Number((2n * num + den) / (2n * den));

  // Amortisation schedule to determine exact interest and final payment
  let balance = principalPaise;
  let totalInterest = 0;
  let finalPayment = emi;

  for (let m = 1; m <= tenureMonths; m++) {
    const interest = Number((2n * BigInt(balance) * rScale + SCALE) / (2n * SCALE));
    totalInterest += interest;

    if (m === tenureMonths) {
      finalPayment = balance + interest;
      balance = 0;
    } else {
      const principalPaid = emi - interest;
      balance -= principalPaid;
    }
  }

  const totalRepayment = principalPaise + totalInterest;

  return {
    principalPaise,
    annualPercent,
    tenureMonths,
    emiPaise: emi,
    totalInterestPaise: totalInterest,
    totalRepaymentPaise: totalRepayment,
    finalPaymentPaise: finalPayment,
  };
}

/**
 * Evaluates EMI affordability and job-loss survival check.
 */
export function evaluateEmiAffordability(
  emiPaise: number,
  salaryPaise: number,
  baselinePaise: number,
  liquidEmergencyPaise: number
): EmiAffordabilityCheck {
  const passesOneSeventh = emiPaise * 7 <= salaryPaise;
  const passesThirtyPercent = emiPaise * 100 <= salaryPaise * 30;

  const ratioBps = salaryPaise > 0 ? Math.floor((emiPaise * 1000) / salaryPaise) : 0;
  const whole = Math.floor(ratioBps / 10);
  const dec = ratioBps % 10;
  const emiToIncomeStr = `${whole}.${dec}%`;

  const monthlyBurn = baselinePaise + emiPaise;
  const runwayMonths = monthlyBurn > 0 ? Math.floor(liquidEmergencyPaise / monthlyBurn) : 0;

  return {
    emiPaise,
    salaryPaise,
    baselinePaise,
    liquidEmergencyPaise,
    passesOneSeventhRule: passesOneSeventh,
    passesThirtyPercentCap: passesThirtyPercent,
    emiToIncomePctOneDecimal: emiToIncomeStr,
    jobLossRunwayMonths: runwayMonths,
  };
}
