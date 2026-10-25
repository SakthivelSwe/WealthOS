/**
 * WealthOS Baseline Estimation Engine
 * Conforms to WEALTHOS_FINAL_SPEC.md v3 §4.3.
 */

import { roundHalfUp } from './money';

export type BaselineMethod = 'USER_ENTERED' | 'HISTORY_MEDIAN' | 'ESTIMATE_55_PERCENT';

export interface BaselineLineItems {
  rentOrHomePaise?: number;
  groceriesAndMealsPaise?: number;
  transportAndCommutePaise?: number;
  utilitiesAndMobilePaise?: number;
  medicalAndToiletriesPaise?: number;
  otherEssentialsPaise?: number;
}

export interface BaselineResult {
  baselinePaise: number;
  method: BaselineMethod;
  label: string;
}

export function determineBaseline(
  userLineItems?: BaselineLineItems,
  recentCycleNeedTotalsPaise: number[] = [],
  lowestRecentSalaryPaise?: number
): BaselineResult {
  if (userLineItems) {
    const total = (userLineItems.rentOrHomePaise || 0) +
      (userLineItems.groceriesAndMealsPaise || 0) +
      (userLineItems.transportAndCommutePaise || 0) +
      (userLineItems.utilitiesAndMobilePaise || 0) +
      (userLineItems.medicalAndToiletriesPaise || 0) +
      (userLineItems.otherEssentialsPaise || 0);

    if (total > 0) {
      return {
        baselinePaise: total,
        method: 'USER_ENTERED',
        label: 'User entered',
      };
    }
  }

  if (recentCycleNeedTotalsPaise.length > 0) {
    const sorted = [...recentCycleNeedTotalsPaise].sort((a, b) => a - b);
    const mid = Math.floor(sorted.length / 2);
    const median = sorted.length % 2 === 1
      ? sorted[mid]
      : Math.floor((sorted[mid - 1] + sorted[mid]) / 2);

    if (median > 0) {
      return {
        baselinePaise: median,
        method: 'HISTORY_MEDIAN',
        label: `Historical median (${recentCycleNeedTotalsPaise.length} cycles)`,
      };
    }
  }

  if (lowestRecentSalaryPaise && lowestRecentSalaryPaise > 0) {
    const estimate = roundHalfUp(lowestRecentSalaryPaise * 55, 100);
    return {
      baselinePaise: estimate,
      method: 'ESTIMATE_55_PERCENT',
      label: `Estimate (55% of ₹${Math.floor(lowestRecentSalaryPaise / 100)}), replace with your real number`,
    };
  }

  return {
    baselinePaise: 0,
    method: 'USER_ENTERED',
    label: 'Not configured',
  };
}
