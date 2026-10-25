/**
 * WealthOS 7-7-7 Health Rules & Vital Signs Engine
 * Conforms to WEALTHOS_FINAL_SPEC.md v3 §5.6.
 * Pure integer math.
 */

import { calculatePercentage } from './money';

export type HealthStatus = 'GREEN' | 'AMBER' | 'RED';

export interface EmergencyTiers {
  instantCashPaise: number;
  autoSweepFdRdPaise: number;
  liquidFundGoldPaise: number;
}

export interface VitalSigns {
  netWorthPaise: number;
  cashFlowPaise: number;
  runwayMonths: number;
  freedomYears: number;
}

/**
 * Evaluates EMI health conforming to §5.6.
 * All comparisons use pure integer math.
 * green if emi * 7 <= salary; amber if emi * 100 <= salary * 30; else red.
 */
export function evaluateEmi(emiPaise: number, salaryPaise: number): HealthStatus {
  if (salaryPaise <= 0) return emiPaise === 0 ? 'GREEN' : 'RED';
  if (emiPaise * 7 <= salaryPaise) return 'GREEN';
  if (emiPaise * 100 <= salaryPaise * 30) return 'AMBER';
  return 'RED';
}

/**
 * Evaluates Savings Rate health.
 * green >= 20%, amber >= 7%, else red.
 */
export function evaluateSavingsRate(savingsPaise: number, salaryPaise: number): HealthStatus {
  if (salaryPaise <= 0) return 'RED';
  const pct = calculatePercentage(savingsPaise, salaryPaise);
  if (pct >= 20) return 'GREEN';
  if (pct >= 7) return 'AMBER';
  return 'RED';
}

/**
 * Evaluates Emergency Runway health.
 * green >= 7 months, amber >= 3 months, else red.
 */
export function evaluateRunway(liquidEmergencyPaise: number, monthlyBaselinePaise: number): HealthStatus {
  if (monthlyBaselinePaise <= 0) return 'GREEN';
  const months = Math.floor(liquidEmergencyPaise / monthlyBaselinePaise);
  if (months >= 7) return 'GREEN';
  if (months >= 3) return 'AMBER';
  return 'RED';
}

/**
 * Calculates the three-tier emergency distribution on the target amount.
 * 20% instant-access cash (floor), 50% auto-sweep FD/RD (floor), 30% liquid fund or gold (remainder).
 */
export function calculateEmergencyTiers(targetPaise: number): EmergencyTiers {
  if (targetPaise < 0) throw new Error('Emergency target must be non-negative');
  const instant = Math.floor((targetPaise * 20) / 100);
  const autoSweep = Math.floor((targetPaise * 50) / 100);
  const liquidGold = targetPaise - instant - autoSweep;
  return {
    instantCashPaise: instant,
    autoSweepFdRdPaise: autoSweep,
    liquidFundGoldPaise: liquidGold,
  };
}

/**
 * Calculates vital signs:
 * - netWorth = assets - liabilities
 * - cashFlow = inflow - outflow
 * - runwayMonths = liquid / baseline
 * - freedomYears = investedAssets / (baseline * 12)
 */
export function calculateVitalSigns(
  totalAssetsPaise: number,
  totalLiabilitiesPaise: number,
  cycleInflowsPaise: number,
  cycleOutflowsPaise: number,
  liquidPaise: number,
  investedAssetsPaise: number,
  monthlyBaselinePaise: number
): VitalSigns {
  const netWorth = totalAssetsPaise - totalLiabilitiesPaise;
  const cashFlow = cycleInflowsPaise - cycleOutflowsPaise;
  const runway = monthlyBaselinePaise > 0 ? Math.floor(liquidPaise / monthlyBaselinePaise) : 0;
  const annualBaseline = monthlyBaselinePaise * 12;
  const freedomYears = annualBaseline > 0 ? Math.floor(investedAssetsPaise / annualBaseline) : 0;

  return {
    netWorthPaise: netWorth,
    cashFlowPaise: cashFlow,
    runwayMonths: runway,
    freedomYears,
  };
}
