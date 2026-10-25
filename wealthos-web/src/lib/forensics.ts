/**
 * WealthOS Forensics Engine
 * Directly answers "Where did my money go?" conforming to WEALTHOS_FINAL_SPEC.md v3 §7 and §A.6.
 * Zero float.
 */

import { calculatePercentage } from './money';

export interface ForensicsReport {
  openingBalancePaise: number;
  totalCreditsPaise: number;
  totalDebitsPaise: number;
  expectedClosingPaise: number;
  actualClosingPaise: number;
  discrepancyPaise: number;
  isReconciled: boolean;
  daysElapsed: number;
  averageBurnPerDayPaise: number;
  first7DaysSpendPaise: number;
  sevenDayVelocityPct: number;
  isVelocityAlertTriggered: boolean;
}

export type CrisisBucketKey =
  | 'FIXED_BILLS_RENT_EMI'
  | 'FOOD_TEA_SNACKS'
  | 'SHOPPING_SUBSCRIPTIONS'
  | 'TRANSFERS_FRIENDS_FAMILY';

export interface CrisisBucketSummary {
  bucket: CrisisBucketKey;
  label: string;
  totalPaise: number;
  percentage: number;
  count: number;
}

export interface MerchantSpendSummary {
  merchant: string;
  totalPaise: number;
  count: number;
}

export interface MandateSummary {
  name: string;
  amountPaise: number;
  count: number;
}

export interface ForensicTxnItem {
  id: string;
  amountMinor: number;
  type: string;             // 'EXPENSE' | 'INCOME' | 'TRANSFER'
  local_date: string;        // 'YYYY-MM-DD'
  merchant?: string | null;
  description?: string | null;
  category_id?: string | null;
  categoryName?: string | null;
  payment_method?: string | null;
  paymentRail?: string | null;
  need_want?: string | null;
  spendEmotionTag?: string | null;
  jarBucket?: string | null;
  isRecurringMandate?: boolean;
}

export interface CycleForensicsResult {
  openingBalancePaise: number;
  actualClosingPaise: number;
  expectedClosingPaise: number;
  discrepancyPaise: number;
  isReconciled: boolean;
  reconciliationDiagnosis: 'BALANCED' | 'MISSING_ROWS' | 'DUPLICATES_LIKELY' | 'UNKNOWN_DISCREPANCY';
  totalCreditsPaise: number;
  totalDebitsPaise: number;
  daysElapsed: number;
  averageBurnPerDayPaise: number;
  first7DaysSpendPaise: number;
  sevenDaySalarySpendPct: number;
  sevenDayDiscretionaryBurnPct: number;
  isVelocityAlertTriggered: boolean;
  smallScanCount: number;
  smallScanTotalPaise: number;
  topMerchants: MerchantSpendSummary[];
  mandates: MandateSummary[];
  crisisBuckets: Record<CrisisBucketKey, CrisisBucketSummary>;
}

/**
 * Classifies a transaction into one of the 4 Crisis Buckets (§7):
 * (1) Fixed bills, rent, EMI
 * (2) Food, tea, snacks
 * (3) Shopping and subscriptions
 * (4) Transfers to friends and family
 */
export function classifyCrisisBucket(item: {
  description?: string | null;
  merchant?: string | null;
  categoryName?: string | null;
  paymentRail?: string | null;
  type?: string;
  jarBucket?: string | null;
  spendEmotionTag?: string | null;
}): CrisisBucketKey {
  const text = `${item.merchant || ''} ${item.description || ''} ${item.categoryName || ''}`.toLowerCase();

  // (1) Fixed bills, rent, EMI
  if (
    /\b(rent|maintenance|pg|flat|deposit|emi|loan|bajaj|hdfc loan|sbi loan|cred|slice|electricity|bescom|tneb|gas|cylinder|wifi|broadband|airtel|jio|vi|act fibernet|insurance|lic)\b/i.test(text) ||
    (item.jarBucket === 'NEEDS' && item.spendEmotionTag === 'NEED')
  ) {
    return 'FIXED_BILLS_RENT_EMI';
  }

  // (2) Food, tea, snacks
  if (
    /\b(swiggy|zomato|starbucks|tea|chai|coffee|snacks|bakery|cafe|restaurant|burger|pizza|mcdonald|kfc|blinkit|zepto|instamart|dmart|bigbasket|supermarket|groceries|vegetables|milk|dairy)\b/i.test(text)
  ) {
    return 'FOOD_TEA_SNACKS';
  }

  // (4) Transfers to friends and family
  if (
    item.type === 'TRANSFER' ||
    item.paymentRail === 'UPI_P2A' ||
    item.spendEmotionTag === 'UNPLANNED_TRANSFER' ||
    /\b(transfer|p2a|sent to|friend|family|mom|dad|brother|sister|wife|husband|self transfer)\b/i.test(text)
  ) {
    return 'TRANSFERS_FRIENDS_FAMILY';
  }

  // (3) Shopping & subscriptions
  if (
    /\b(amazon|flipkart|myntra|meesho|ajio|nykaa|zara|h&m|netflix|spotify|youtube|prime|hotstar|apple|google play|gym|cult\.fit|shopping)\b/i.test(text) ||
    item.jarBucket === 'PLAY' ||
    item.spendEmotionTag === 'PLANNED_WANT' ||
    item.spendEmotionTag === 'REGRET_LEAK'
  ) {
    return 'SHOPPING_SUBSCRIPTIONS';
  }

  // Fallback based on emotional tag
  if (item.spendEmotionTag === 'NEED') {
    return 'FIXED_BILLS_RENT_EMI';
  }
  return 'SHOPPING_SUBSCRIPTIONS';
}

/**
 * Standard Forensics analyze function directly adhering to Appendix A.6 Golden Values.
 */
export function analyzeForensics(
  openingBalancePaise: number,
  totalCreditsPaise: number,
  totalDebitsPaise: number,
  actualClosingPaise: number,
  daysElapsed: number,
  discretionaryBudgetPaise: number = 0,
  first7DaysSpendPaise: number = 0
): ForensicsReport {
  if (daysElapsed < 1) throw new Error('Days elapsed must be at least 1');

  const expectedClosing = openingBalancePaise + totalCreditsPaise - totalDebitsPaise;
  const discrepancy = expectedClosing - actualClosingPaise;
  const isReconciled = discrepancy === 0;

  const avgBurn = Math.floor(totalDebitsPaise / daysElapsed);

  const velocityPct = discretionaryBudgetPaise > 0
    ? calculatePercentage(first7DaysSpendPaise, discretionaryBudgetPaise)
    : 0;

  const velocityAlert = velocityPct > 50;

  return {
    openingBalancePaise,
    totalCreditsPaise,
    totalDebitsPaise,
    expectedClosingPaise: expectedClosing,
    actualClosingPaise,
    discrepancyPaise: discrepancy,
    isReconciled,
    daysElapsed,
    averageBurnPerDayPaise: avgBurn,
    first7DaysSpendPaise,
    sevenDayVelocityPct: velocityPct,
    isVelocityAlertTriggered: velocityAlert,
  };
}

/**
 * Full Cycle Forensics analysis from transaction list, opening balance, and cycle parameters.
 * Answers the October crisis: ₹31,400 over 17 days -> 184,705 paise per day.
 */
export function analyzeCycleForensics(params: {
  transactions: ForensicTxnItem[];
  cycleStartDate: string;           // 'YYYY-MM-DD'
  asOfDate?: string;                // 'YYYY-MM-DD' (defaults to latest txn or today)
  salaryPaise: number;
  openingBalancePaise: number;
  actualClosingPaise?: number;
  discretionaryBudgetPaise?: number;
  smallScanThresholdPaise?: number; // defaults to ₹200 (20,000 paise)
}): CycleForensicsResult {
  const {
    transactions,
    cycleStartDate,
    salaryPaise,
    openingBalancePaise,
    discretionaryBudgetPaise = 0,
    smallScanThresholdPaise = 20000,
  } = params;

  // Filter txns from cycle start onwards
  const cycleTxns = transactions.filter(t => t.local_date >= cycleStartDate);

  // Compute days elapsed
  let latestDate = params.asOfDate || '';
  if (!latestDate) {
    if (cycleTxns.length > 0) {
      latestDate = cycleTxns.reduce((latest, t) => t.local_date > latest ? t.local_date : latest, cycleStartDate);
    } else {
      latestDate = cycleStartDate;
    }
  }

  const startMs = new Date(cycleStartDate + 'T00:00:00Z').getTime();
  const endMs = new Date(latestDate + 'T00:00:00Z').getTime();
  const diffDays = Math.max(1, Math.round((endMs - startMs) / (1000 * 60 * 60 * 24)));
  const daysElapsed = diffDays;

  // First 7 days boundary
  const sevenDaysEndMs = startMs + 6 * 24 * 60 * 60 * 1000;
  const sevenDaysEndDate = new Date(sevenDaysEndMs).toISOString().split('T')[0];

  let totalCreditsPaise = 0;
  let totalDebitsPaise = 0;
  let first7DaysSpendPaise = 0;
  let smallScanCount = 0;
  let smallScanTotalPaise = 0;

  // Merchant aggregations
  const merchantTotals: Record<string, { totalPaise: number; count: number }> = {};
  const mandateTotals: Record<string, { totalPaise: number; count: number }> = {};

  // 4 Crisis Buckets
  const crisisBuckets: Record<CrisisBucketKey, { totalPaise: number; count: number }> = {
    FIXED_BILLS_RENT_EMI: { totalPaise: 0, count: 0 },
    FOOD_TEA_SNACKS: { totalPaise: 0, count: 0 },
    SHOPPING_SUBSCRIPTIONS: { totalPaise: 0, count: 0 },
    TRANSFERS_FRIENDS_FAMILY: { totalPaise: 0, count: 0 },
  };

  for (const t of cycleTxns) {
    if (t.type === 'INCOME') {
      totalCreditsPaise += t.amountMinor;
      continue;
    }

    // Debit (EXPENSE or TRANSFER)
    totalDebitsPaise += t.amountMinor;

    if (t.local_date <= sevenDaysEndDate) {
      first7DaysSpendPaise += t.amountMinor;
    }

    // Small scans
    if (t.type === 'EXPENSE' && t.amountMinor <= smallScanThresholdPaise) {
      smallScanCount++;
      smallScanTotalPaise += t.amountMinor;
    }

    // Merchants
    const merchantName = (t.merchant || t.description || 'Other Spend').trim();
    if (!merchantTotals[merchantName]) {
      merchantTotals[merchantName] = { totalPaise: 0, count: 0 };
    }
    merchantTotals[merchantName].totalPaise += t.amountMinor;
    merchantTotals[merchantName].count++;

    // Mandates
    if (t.isRecurringMandate) {
      if (!mandateTotals[merchantName]) {
        mandateTotals[merchantName] = { totalPaise: 0, count: 0 };
      }
      mandateTotals[merchantName].totalPaise += t.amountMinor;
      mandateTotals[merchantName].count++;
    }

    // Crisis Bucket
    const bucket = classifyCrisisBucket(t);
    crisisBuckets[bucket].totalPaise += t.amountMinor;
    crisisBuckets[bucket].count++;
  }

  // Reconciliation check
  const expectedClosingPaise = openingBalancePaise + totalCreditsPaise - totalDebitsPaise;
  const actualClosingPaise = params.actualClosingPaise !== undefined
    ? params.actualClosingPaise
    : expectedClosingPaise;
  const discrepancyPaise = expectedClosingPaise - actualClosingPaise;
  const isReconciled = discrepancyPaise === 0;

  let reconciliationDiagnosis: 'BALANCED' | 'MISSING_ROWS' | 'DUPLICATES_LIKELY' | 'UNKNOWN_DISCREPANCY' = 'BALANCED';
  if (!isReconciled) {
    if (discrepancyPaise > 0) {
      reconciliationDiagnosis = 'MISSING_ROWS'; // expected closing is higher than actual -> debits were missed
    } else {
      reconciliationDiagnosis = 'DUPLICATES_LIKELY'; // expected closing is lower than actual -> excess debits or duplicate rows
    }
  }

  // Burn rate
  const averageBurnPerDayPaise = Math.floor(totalDebitsPaise / daysElapsed);

  // Velocity checks
  const sevenDaySalarySpendPct = salaryPaise > 0
    ? calculatePercentage(first7DaysSpendPaise, salaryPaise)
    : 0;
  const sevenDayDiscretionaryBurnPct = discretionaryBudgetPaise > 0
    ? calculatePercentage(first7DaysSpendPaise, discretionaryBudgetPaise)
    : 0;
  const isVelocityAlertTriggered = sevenDayDiscretionaryBurnPct > 50 || sevenDaySalarySpendPct > 50;

  // Top 10 merchants
  const topMerchants: MerchantSpendSummary[] = Object.entries(merchantTotals)
    .map(([merchant, val]) => ({ merchant, totalPaise: val.totalPaise, count: val.count }))
    .sort((a, b) => b.totalPaise - a.totalPaise)
    .slice(0, 10);

  // Mandates
  const mandates: MandateSummary[] = Object.entries(mandateTotals)
    .map(([name, val]) => ({ name, amountPaise: val.totalPaise, count: val.count }))
    .sort((a, b) => b.amountPaise - a.amountPaise);

  // Crisis bucket summaries with percentages
  const bucketLabels: Record<CrisisBucketKey, string> = {
    FIXED_BILLS_RENT_EMI: 'Fixed Bills, Rent & EMI',
    FOOD_TEA_SNACKS: 'Food, Tea & Snacks',
    SHOPPING_SUBSCRIPTIONS: 'Shopping & Subscriptions',
    TRANSFERS_FRIENDS_FAMILY: 'Transfers to Friends & Family',
  };

  const crisisBucketsResult = {} as Record<CrisisBucketKey, CrisisBucketSummary>;
  for (const [key, val] of Object.entries(crisisBuckets) as [CrisisBucketKey, { totalPaise: number; count: number }][]) {
    crisisBucketsResult[key] = {
      bucket: key,
      label: bucketLabels[key],
      totalPaise: val.totalPaise,
      percentage: totalDebitsPaise > 0 ? calculatePercentage(val.totalPaise, totalDebitsPaise) : 0,
      count: val.count,
    };
  }

  return {
    openingBalancePaise,
    actualClosingPaise,
    expectedClosingPaise,
    discrepancyPaise,
    isReconciled,
    reconciliationDiagnosis,
    totalCreditsPaise,
    totalDebitsPaise,
    daysElapsed,
    averageBurnPerDayPaise,
    first7DaysSpendPaise,
    sevenDaySalarySpendPct,
    sevenDayDiscretionaryBurnPct,
    isVelocityAlertTriggered,
    smallScanCount,
    smallScanTotalPaise,
    topMerchants,
    mandates,
    crisisBuckets: crisisBucketsResult,
  };
}
