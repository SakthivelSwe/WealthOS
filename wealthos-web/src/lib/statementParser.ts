/**
 * WealthOS Bank Statement Parser & Reconciliation Engine
 * Conforms to WEALTHOS_FINAL_SPEC.md v3 §6.
 * Zero float. Supports Axis Bank, HDFC, SBI, ICICI, and generic Indian bank CSV/text statements.
 */

import { parseMoney as parseMoneyInteger } from './money';
import { readCsv } from './csvReader';
import { discoverMapping, parseStatementDate, type ColumnMapping } from './columnMapper';
import { decodeNarration, type PaymentRail } from './narrationDecoder';
import { classifyTransaction, type SpendEmotionTag } from './classifier';
import { computeDedupKey } from './duplicateDetector';
import type { Transaction, TxnType, NeedWant } from '../types';

export { parseStatementDate, type ColumnMapping };

export interface ParsedStatementRow {
  id: string;
  date: string;              // 'YYYY-MM-DD'
  rawDate: string;
  description: string;
  merchant: string | null;
  reference: string | null;
  debitMinor: number;
  creditMinor: number;
  amountMinor: number;       // absolute amount
  type: TxnType;             // 'EXPENSE' | 'INCOME' | 'TRANSFER'
  balanceMinor: number | null;
  suggestedCategoryId: string | null;
  needWant: NeedWant | null;
  jarBucket?: string;
  emotionTag?: SpendEmotionTag;
  paymentRail?: PaymentRail;
  isRecurringMandate?: boolean;
  isDuplicate: boolean;
  duplicateReason?: string;
  isSelfTransfer: boolean;
  selected: boolean;
  formatName?: string;
  importBatchId?: string;
}

export interface StatementParseResult {
  rows: ParsedStatementRow[];
  totalCreditsMinor: number;
  totalDebitsMinor: number;
  netCashflowMinor: number;
  openingBalanceMinor: number | null;
  closingBalanceMinor: number | null;
  expectedClosingMinor: number | null;
  discrepancyMinor: number | null;
  hasReconciliation: boolean;
  duplicateCount: number;
  headers: string[];
  smallScansTotalMinor?: number;
  smallScansCount?: number;
  detectedMandatesCount?: number;
  mapping?: ColumnMapping | null;
  detectedFormatName?: string;
  mandates?: Array<{ name: string; amountMinor: number; count: number }>;
  smallScansByMerchant?: Array<{ name: string; amountMinor: number; count: number }>;
  importBatchId?: string;
}

/** Parse an Indian or international monetary string into integer minor units (paise/cents). */
export function parseStatementAmount(raw: string | number | null | undefined): number {
  return parseMoneyInteger(raw, 2);
}

/** Legacy splitCsvLine helper for tests, backed by RFC 4180 parser */
export function splitCsvLine(line: string, delimiter = ','): string[] {
  const rows = readCsv(line, delimiter);
  return rows[0] || [];
}

/** Extract merchant name, UPI ID, or reference number from common Indian bank narrations */
export function cleanNarration(rawNarration: string): {
  merchant: string | null;
  reference: string | null;
  isSelfTransfer: boolean;
} {
  const decoded = decodeNarration(rawNarration);
  return {
    merchant: decoded.cleanMerchantOrPayee,
    reference: decoded.referenceOrRrn,
    isSelfTransfer: decoded.isSelfTransfer,
  };
}

/** Auto-suggest a WealthOS category and need/want classification based on narration */
export function suggestCategory(
  narration: string,
  type: TxnType
): { categoryId: string | null; needWant: NeedWant | null } {
  const isCredit = type === 'INCOME';
  const res = classifyTransaction(narration, 0, 'NON_UPI', isCredit);
  const needWant: NeedWant | null = isCredit
    ? null
    : (res.emotionTag === 'NEED' ? 'NEED' : 'WANT');
  return {
    categoryId: res.categoryId,
    needWant,
  };
}

/**
 * Main parser function: parses raw CSV or TSV text and returns structured rows
 * with duplicate detection and mathematical closing balance reconciliation.
 */
export function parseBankStatement(
  text: string,
  existingTxns: Transaction[] = [],
  accountId?: string,
  smallScanThresholdPaise: number = 20000, // ₹200
  customMapping?: Partial<ColumnMapping>
): StatementParseResult {
  const rawRows = readCsv(text);
  const importBatchId = 'batch_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6);

  if (rawRows.length === 0) {
    return {
      rows: [],
      totalCreditsMinor: 0,
      totalDebitsMinor: 0,
      netCashflowMinor: 0,
      openingBalanceMinor: null,
      closingBalanceMinor: null,
      expectedClosingMinor: null,
      discrepancyMinor: null,
      hasReconciliation: false,
      duplicateCount: 0,
      headers: [],
      smallScansTotalMinor: 0,
      smallScansCount: 0,
      detectedMandatesCount: 0,
      mapping: null,
      detectedFormatName: 'Unknown',
      mandates: [],
      smallScansByMerchant: [],
      importBatchId,
    };
  }

  const discovered = discoverMapping(rawRows);
  const mapping: ColumnMapping | null = customMapping
    ? {
        headerRowIndex: customMapping.headerRowIndex ?? (discovered?.headerRowIndex ?? 0),
        dateCol: customMapping.dateCol ?? (discovered?.dateCol ?? 0),
        descriptionCol: customMapping.descriptionCol ?? (discovered?.descriptionCol ?? 1),
        debitCol: customMapping.debitCol ?? (discovered?.debitCol ?? -1),
        creditCol: customMapping.creditCol ?? (discovered?.creditCol ?? -1),
        amountCol: customMapping.amountCol ?? (discovered?.amountCol ?? -1),
        indicatorCol: customMapping.indicatorCol ?? (discovered?.indicatorCol ?? -1),
        balanceCol: customMapping.balanceCol ?? (discovered?.balanceCol ?? -1),
        referenceCol: customMapping.referenceCol ?? (discovered?.referenceCol ?? -1),
        confidence: customMapping.confidence ?? (discovered?.confidence ?? 'HIGH'),
      }
    : discovered;

  const headerIndex = mapping?.headerRowIndex ?? 0;
  const headers = rawRows[headerIndex] || [];

  const dateCol = mapping?.dateCol ?? 0;
  const descCol = mapping?.descriptionCol ?? 1;
  const debitCol = mapping?.debitCol ?? -1;
  const creditCol = mapping?.creditCol ?? -1;
  const amountCol = mapping?.amountCol ?? -1;
  const indicatorCol = mapping?.indicatorCol ?? -1;
  const balanceCol = mapping?.balanceCol ?? -1;
  const refCol = mapping?.referenceCol ?? -1;

  const parsedRows: ParsedStatementRow[] = [];
  const runningBalances: number[] = [];

  // Existing lookup sets
  const existingRrnSet = new Set<string>();
  const existingFallbackSet = new Set<string>();

  existingTxns.forEach(t => {
    if (accountId && t.account_id !== accountId) return;
    const rrn = (t as any).rrn || null;
    if (rrn) {
      existingRrnSet.add(rrn);
    }
    const key = `${t.local_date}|${t.amount_minor}|${t.type}`;
    existingFallbackSet.add(key);
  });

  for (let i = headerIndex + 1; i < rawRows.length; i++) {
    const cols = rawRows[i];
    if (cols.length < 2 || cols.every(c => !c)) continue;

    const rawDate = cols[dateCol] || '';
    const parsedDate = parseStatementDate(rawDate);
    if (!parsedDate) continue;

    const narration = cols[descCol] || '';
    if (!narration.trim()) continue;

    let debitMinor = debitCol >= 0 ? parseStatementAmount(cols[debitCol]) : 0;
    let creditMinor = creditCol >= 0 ? parseStatementAmount(cols[creditCol]) : 0;

    // Single amount column logic
    if (debitMinor === 0 && creditMinor === 0 && amountCol >= 0) {
      const amtStr = cols[amountCol] || '';
      const indicatorStr = indicatorCol >= 0 ? (cols[indicatorCol] || '').toUpperCase().trim() : '';
      const isNegativeByFormat = amtStr.startsWith('-') || (amtStr.startsWith('(') && amtStr.endsWith(')')) || amtStr.endsWith('Dr');
      const isDebitIndicator = indicatorStr === 'DR' || indicatorStr === 'D' || indicatorStr === 'DEBIT' || indicatorStr === 'W';
      const isCreditIndicator = indicatorStr === 'CR' || indicatorStr === 'C' || indicatorStr === 'CREDIT' || indicatorStr === 'DEP';

      const amt = parseStatementAmount(amtStr);
      if (isCreditIndicator) {
        creditMinor = amt;
      } else if (isDebitIndicator || isNegativeByFormat || amt < 0) {
        debitMinor = Math.abs(amt);
      } else {
        creditMinor = amt; // default positive to credit
      }
    }

    if (debitMinor === 0 && creditMinor === 0) continue;

    const balanceMinor = balanceCol >= 0 && cols[balanceCol]
      ? parseStatementAmount(cols[balanceCol])
      : null;
    if (balanceMinor !== null) {
      runningBalances.push(balanceMinor);
    }

    // Decode narration
    const decoded = decodeNarration(narration);
    const rawRef = refCol >= 0 && cols[refCol] ? cols[refCol].trim() : null;
    const reference = rawRef || decoded.referenceOrRrn;

    let type: TxnType = 'EXPENSE';
    let amountMinor = debitMinor;

    if (creditMinor > 0) {
      type = 'INCOME';
      amountMinor = creditMinor;
    } else if (decoded.isSelfTransfer) {
      type = 'TRANSFER';
      amountMinor = debitMinor;
    }

    // Classify
    const isCredit = type === 'INCOME';
    const classification = classifyTransaction(
      decoded.cleanMerchantOrPayee,
      amountMinor,
      decoded.paymentRail,
      isCredit
    );

    const needWant: NeedWant | null = isCredit
      ? null
      : (classification.emotionTag === 'NEED' ? 'NEED' : 'WANT');

    // Duplicate Detection
    let isDuplicate = false;
    let duplicateReason: string | undefined = undefined;

    if (reference && existingRrnSet.has(reference)) {
      isDuplicate = true;
      duplicateReason = `Exact RRN/UTR match (${reference}) already recorded in ledger`;
    } else {
      const fallbackKey = `${parsedDate}|${amountMinor}|${type}`;
      if (existingFallbackSet.has(fallbackKey)) {
        isDuplicate = true;
        duplicateReason = 'Exact match found in ledger on this date';
      }
    }

    parsedRows.push({
      id: `stmt_${i}_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
      date: parsedDate,
      rawDate,
      description: narration,
      merchant: decoded.cleanMerchantOrPayee,
      reference,
      debitMinor,
      creditMinor,
      amountMinor,
      type,
      balanceMinor,
      suggestedCategoryId: classification.categoryId,
      needWant,
      jarBucket: classification.jarBucket,
      emotionTag: classification.emotionTag,
      paymentRail: decoded.paymentRail,
      isRecurringMandate: decoded.isRecurringMandate || classification.isRecurringMandate,
      isDuplicate,
      duplicateReason,
      isSelfTransfer: decoded.isSelfTransfer,
      selected: !isDuplicate,
      formatName: decoded.formatName,
      importBatchId,
    });
  }

  // Summary calculations
  const totalCreditsMinor = parsedRows.filter(r => r.type === 'INCOME').reduce((sum, r) => sum + r.amountMinor, 0);
  const totalDebitsMinor = parsedRows.filter(r => r.type === 'EXPENSE' || r.type === 'TRANSFER').reduce((sum, r) => sum + r.amountMinor, 0);
  const netCashflowMinor = totalCreditsMinor - totalDebitsMinor;
  const duplicateCount = parsedRows.filter(r => r.isDuplicate).length;

  const smallScans = parsedRows.filter(r => r.type === 'EXPENSE' && r.amountMinor <= smallScanThresholdPaise);
  const smallScansTotalMinor = smallScans.reduce((sum, r) => sum + r.amountMinor, 0);
  const detectedMandatesCount = parsedRows.filter(r => r.isRecurringMandate).length;

  // Group mandates
  const mandateMap: Record<string, { amountMinor: number; count: number }> = {};
  parsedRows.filter(r => r.isRecurringMandate).forEach(r => {
    const name = (r.merchant || r.description || 'Recurring Mandate').trim();
    if (!mandateMap[name]) mandateMap[name] = { amountMinor: 0, count: 0 };
    mandateMap[name].amountMinor += r.amountMinor;
    mandateMap[name].count++;
  });
  const mandates = Object.entries(mandateMap).map(([name, val]) => ({
    name,
    amountMinor: val.amountMinor,
    count: val.count,
  })).sort((a, b) => b.amountMinor - a.amountMinor);

  // Group small scans by merchant
  const smallScanMerchantMap: Record<string, { amountMinor: number; count: number }> = {};
  smallScans.forEach(r => {
    const name = (r.merchant || r.description || 'Small Scan').trim();
    if (!smallScanMerchantMap[name]) smallScanMerchantMap[name] = { amountMinor: 0, count: 0 };
    smallScanMerchantMap[name].amountMinor += r.amountMinor;
    smallScanMerchantMap[name].count++;
  });
  const smallScansByMerchant = Object.entries(smallScanMerchantMap)
    .map(([name, val]) => ({ name, amountMinor: val.amountMinor, count: val.count }))
    .sort((a, b) => b.amountMinor - a.amountMinor);

  // Mathematical Reconciliation Check
  let openingBalanceMinor: number | null = null;
  let closingBalanceMinor: number | null = null;
  let expectedClosingMinor: number | null = null;
  let discrepancyMinor: number | null = null;
  let hasReconciliation = false;

  if (runningBalances.length >= 2 && parsedRows.length > 0) {
    const firstRow = parsedRows[0];
    const lastRow = parsedRows[parsedRows.length - 1];

    if (firstRow.balanceMinor !== null && lastRow.balanceMinor !== null) {
      const isAscending = firstRow.date <= lastRow.date;

      if (isAscending) {
        const firstEffect = firstRow.type === 'INCOME' ? firstRow.amountMinor : -firstRow.amountMinor;
        openingBalanceMinor = firstRow.balanceMinor - firstEffect;
        closingBalanceMinor = lastRow.balanceMinor;
      } else {
        const lastEffect = lastRow.type === 'INCOME' ? lastRow.amountMinor : -lastRow.amountMinor;
        openingBalanceMinor = lastRow.balanceMinor - lastEffect;
        closingBalanceMinor = firstRow.balanceMinor;
      }

      expectedClosingMinor = openingBalanceMinor + totalCreditsMinor - totalDebitsMinor;
      discrepancyMinor = expectedClosingMinor - closingBalanceMinor;
      hasReconciliation = true;
    }
  }

  const detectedFormatName = parsedRows[0]?.formatName || (discovered ? 'Generic Bank CSV' : 'Custom Mapped CSV');

  return {
    rows: parsedRows,
    totalCreditsMinor,
    totalDebitsMinor,
    netCashflowMinor,
    openingBalanceMinor,
    closingBalanceMinor,
    expectedClosingMinor,
    discrepancyMinor,
    hasReconciliation,
    duplicateCount,
    headers,
    smallScansTotalMinor,
    smallScansCount: smallScans.length,
    detectedMandatesCount,
    mapping,
    detectedFormatName,
    mandates,
    smallScansByMerchant,
    importBatchId,
  };
}
