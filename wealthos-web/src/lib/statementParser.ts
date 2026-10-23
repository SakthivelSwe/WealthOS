/**
 * WealthOS Bank Statement Parser & Reconciliation Engine
 * Supports Axis Bank, HDFC, SBI, ICICI, and generic Indian bank CSV/text statements.
 */

import type { Transaction, TxnType, NeedWant } from '../types';

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
  isDuplicate: boolean;
  duplicateReason?: string;
  isSelfTransfer: boolean;
  selected: boolean;
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
}

/** Parse an Indian or international monetary string into integer minor units (paise/cents). */
export function parseStatementAmount(raw: string | number | null | undefined): number {
  if (raw === null || raw === undefined) return 0;
  let str = String(raw).trim();
  if (!str || str === '-' || str === '—') return 0;

  // Handle trailing Dr/Cr or (parentheses)
  const isNegative = str.startsWith('(') && str.endsWith(')') || str.endsWith('Dr') || str.endsWith('DR');
  str = str.replace(/[^\d.-]/g, '');
  if (!str) return 0;

  const num = parseFloat(str);
  if (isNaN(num)) return 0;
  const minor = Math.round(Math.abs(num) * 100);
  return isNegative ? -minor : minor;
}

/** Parse various date formats: DD/MM/YYYY, DD-MM-YYYY, YYYY-MM-DD, DD-MMM-YYYY */
export function parseStatementDate(raw: string): string | null {
  if (!raw) return null;
  const s = raw.trim();

  // YYYY-MM-DD
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;

  // DD/MM/YYYY or DD-MM-YYYY
  const dmyMatch = s.match(/^(\d{1,2})[/-](\d{1,2})[/-](\d{4})$/);
  if (dmyMatch) {
    const day = dmyMatch[1].padStart(2, '0');
    const month = dmyMatch[2].padStart(2, '0');
    const year = dmyMatch[3];
    return `${year}-${month}-${day}`;
  }

  // DD-MMM-YYYY e.g. 05-Oct-2026 or 05 Oct 2026
  const monthNames: Record<string, string> = {
    jan: '01', feb: '02', mar: '03', apr: '04', may: '05', jun: '06',
    jul: '07', aug: '08', sep: '09', oct: '10', nov: '11', dec: '12',
  };
  const mmmMatch = s.match(/^(\d{1,2})[- ]([a-zA-Z]{3})[- ](\d{4})$/);
  if (mmmMatch) {
    const day = mmmMatch[1].padStart(2, '0');
    const mStr = mmmMatch[2].toLowerCase();
    const month = monthNames[mStr];
    const year = mmmMatch[3];
    if (month) return `${year}-${month}-${day}`;
  }

  return null;
}

/** Extract merchant name, UPI ID, or reference number from common Indian bank narrations */
export function cleanNarration(rawNarration: string): { merchant: string | null; reference: string | null; isSelfTransfer: boolean } {
  let narration = rawNarration.trim();
  let merchant: string | null = null;
  let reference: string | null = null;
  let isSelfTransfer = false;

  const lower = narration.toLowerCase();

  // Self transfer detection
  if (
    lower.includes('self transfer') ||
    lower.includes('own a/c') ||
    lower.includes('own account') ||
    lower.includes('to own') ||
    lower.includes('to self')
  ) {
    isSelfTransfer = true;
  }

  // UPI pattern: UPI/REV/123... or UPI/P2A/123.../Merchant/...
  if (narration.toUpperCase().startsWith('UPI')) {
    const parts = narration.split('/');
    if (parts.length >= 4) {
      reference = parts[2]?.trim() || null;
      merchant = parts[3]?.trim() || null;
    } else if (parts.length >= 3) {
      merchant = parts[2]?.trim() || null;
    }
  }

  // NEFT / IMPS pattern: NEFT/CMS... or IMPS/P2A/...
  if (!reference) {
    const refMatch = narration.match(/(?:UTR|CMS|REF|TXN|IMPS|NEFT)[\s/:-]*([A-Za-z0-9]{8,24})/i);
    if (refMatch) reference = refMatch[1];
  }

  // Fallback merchant extraction
  if (!merchant) {
    const cleaned = narration
      .replace(/^UPI\/[A-Z0-9]+\/[A-Z0-9]+\//i, '')
      .replace(/^POS\s+\d+\s+/i, '')
      .replace(/^ATM\s+WDL\s+/i, '')
      .replace(/^ACH\s+D\s+/i, '')
      .trim();
    if (cleaned.length > 2 && cleaned.length < 40) {
      merchant = cleaned;
    }
  }

  return { merchant, reference, isSelfTransfer };
}

/** Auto-suggest a WealthOS category and need/want classification based on narration */
export function suggestCategory(narration: string, type: TxnType): { categoryId: string | null; needWant: NeedWant | null } {
  const text = narration.toLowerCase();

  if (type === 'INCOME') {
    if (text.includes('salary') || text.includes('payroll') || text.includes('cms') || text.includes('wages')) {
      return { categoryId: 'cat_income_0', needWant: null };
    }
    if (text.includes('interest') || text.includes('int.pd') || text.includes('dividend')) {
      return { categoryId: 'cat_income_1', needWant: null };
    }
    if (text.includes('refund') || text.includes('cashback')) {
      return { categoryId: 'cat_income_2', needWant: null };
    }
    return { categoryId: 'cat_income_0', needWant: null };
  }

  // EXPENSE classifications
  if (text.includes('rent') || text.includes('house') || text.includes('pg') || text.includes('maintenance')) {
    return { categoryId: 'cat_housing', needWant: 'NEED' };
  }
  if (text.includes('swiggy') || text.includes('zomato') || text.includes('restaurant') || text.includes('cafe') || text.includes('starbucks')) {
    return { categoryId: 'cat_dining', needWant: 'WANT' };
  }
  if (text.includes('grocer') || text.includes('supermarket') || text.includes('zepto') || text.includes('blinkit') || text.includes('bigbasket') || text.includes('dmart')) {
    return { categoryId: 'cat_groceries', needWant: 'NEED' };
  }
  if (text.includes('petrol') || text.includes('fuel') || text.includes('hpcl') || text.includes('bpcl') || text.includes('iocl') || text.includes('uber') || text.includes('ola')) {
    return { categoryId: 'cat_transport', needWant: 'NEED' };
  }
  if (text.includes('bescom') || text.includes('tneb') || text.includes('electricity') || text.includes('airtel') || text.includes('jio') || text.includes('broadband') || text.includes('wifi')) {
    return { categoryId: 'cat_utilities', needWant: 'NEED' };
  }
  if (text.includes('emi') || text.includes('loan') || text.includes('hdfc loan') || text.includes('sbi loan') || text.includes('bajaj')) {
    return { categoryId: 'cat_debt', needWant: 'NEED' };
  }
  if (text.includes('chit') || text.includes('ksfe') || text.includes('shriram')) {
    return { categoryId: 'cat_savings', needWant: 'NEED' };
  }
  if (text.includes('apollo') || text.includes('pharmacy') || text.includes('hospital') || text.includes('clinic') || text.includes('medplus')) {
    return { categoryId: 'cat_health', needWant: 'NEED' };
  }
  if (text.includes('amazon') || text.includes('flipkart') || text.includes('myntra') || text.includes('zara')) {
    return { categoryId: 'cat_shopping', needWant: 'WANT' };
  }
  if (text.includes('netflix') || text.includes('prime') || text.includes('hotstar') || text.includes('movie') || text.includes('pvr') || text.includes('inox')) {
    return { categoryId: 'cat_entertainment', needWant: 'WANT' };
  }

  return { categoryId: 'cat_general', needWant: 'NEED' };
}

/** Check if row is a header candidate */
function isHeaderRow(row: string[]): boolean {
  const line = row.map(c => c.toLowerCase().trim()).join(' ');
  return (
    (line.includes('date') || line.includes('dt')) &&
    (line.includes('narration') || line.includes('particular') || line.includes('description') || line.includes('detail') || line.includes('remarks'))
  );
}

/** Split CSV lines properly accounting for quoted fields */
export function splitCsvLine(line: string, delimiter = ','): string[] {
  const result: string[] = [];
  let current = '';
  let inQuotes = false;

  for (let i = 0; i < line.length; i++) {
    const char = line[i];
    if (char === '"') {
      if (inQuotes && line[i + 1] === '"') {
        current += '"';
        i++;
      } else {
        inQuotes = !inQuotes;
      }
    } else if (char === delimiter && !inQuotes) {
      result.push(current.trim());
      current = '';
    } else {
      current += char;
    }
  }
  result.push(current.trim());
  return result;
}

/**
 * Main parser function: parses raw CSV or TSV text and returns structured rows
 * with duplicate detection and mathematical closing balance reconciliation.
 */
export function parseBankStatement(
  text: string,
  existingTxns: Transaction[] = [],
  accountId?: string
): StatementParseResult {
  const lines = text.split(/\r?\n/).map(l => l.trim()).filter(l => l.length > 0);
  if (lines.length === 0) {
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
    };
  }

  // Detect delimiter: comma, tab, or semicolon
  const firstFew = lines.slice(0, 10).join('\n');
  const commaCount = (firstFew.match(/,/g) || []).length;
  const tabCount = (firstFew.match(/\t/g) || []).length;
  const delimiter = tabCount > commaCount ? '\t' : ',';

  const rawRows = lines.map(line => splitCsvLine(line, delimiter));

  // Find header row
  let headerIndex = -1;
  let dateCol = -1;
  let descCol = -1;
  let refCol = -1;
  let debitCol = -1;
  let creditCol = -1;
  let amountCol = -1;
  let balanceCol = -1;

  for (let i = 0; i < Math.min(25, rawRows.length); i++) {
    if (isHeaderRow(rawRows[i])) {
      headerIndex = i;
      const headers = rawRows[i].map(h => h.toLowerCase().trim());

      dateCol = headers.findIndex(h => h.includes('date') || h === 'dt');
      descCol = headers.findIndex(h => h.includes('particular') || h.includes('narration') || h.includes('description') || h.includes('detail') || h.includes('remarks'));
      refCol = headers.findIndex(h => h.includes('chq') || h.includes('ref') || h.includes('cheque') || h.includes('utr'));
      debitCol = headers.findIndex(h => h.includes('withdrawal') || h.includes('debit') || h === 'dr');
      creditCol = headers.findIndex(h => h.includes('deposit') || h.includes('credit') || h === 'cr');
      amountCol = headers.findIndex(h => h === 'amount' || h.includes('net amount') || h.includes('txn amount'));
      balanceCol = headers.findIndex(h => h.includes('balance') || h.includes('closing'));
      break;
    }
  }

  if (headerIndex === -1) {
    // Fallback: assume row 0 has headers
    headerIndex = 0;
    dateCol = 0;
    descCol = 1;
    amountCol = 2;
  }

  const headers = rawRows[headerIndex] || [];
  const parsedRows: ParsedStatementRow[] = [];
  let runningBalances: number[] = [];

  // Build existing lookup for duplicate detection
  const existingSet = new Set<string>();
  existingTxns.forEach(t => {
    if (accountId && t.account_id !== accountId) return;
    existingSet.add(`${t.local_date}|${t.amount_minor}|${t.type}`);
  });

  for (let i = headerIndex + 1; i < rawRows.length; i++) {
    const cols = rawRows[i];
    if (cols.length < 2 || cols.every(c => !c)) continue;

    const rawDate = cols[dateCol] || '';
    const parsedDate = parseStatementDate(rawDate);
    if (!parsedDate) continue; // Skip footer or notes lines

    const narration = cols[descCol] || '';
    const rawRef = refCol >= 0 ? cols[refCol] : null;

    let debitMinor = debitCol >= 0 ? parseStatementAmount(cols[debitCol]) : 0;
    let creditMinor = creditCol >= 0 ? parseStatementAmount(cols[creditCol]) : 0;

    if (debitMinor === 0 && creditMinor === 0 && amountCol >= 0) {
      const amt = parseStatementAmount(cols[amountCol]);
      if (amt < 0) debitMinor = Math.abs(amt);
      else creditMinor = amt;
    }

    if (debitMinor === 0 && creditMinor === 0) continue;

    const balanceMinor = balanceCol >= 0 ? parseStatementAmount(cols[balanceCol]) : null;
    if (balanceMinor !== null) runningBalances.push(balanceMinor);

    const { merchant, reference: extractedRef, isSelfTransfer } = cleanNarration(narration);
    const reference = rawRef || extractedRef;

    let type: TxnType = 'EXPENSE';
    let amountMinor = debitMinor;

    if (creditMinor > 0) {
      type = 'INCOME';
      amountMinor = creditMinor;
    } else if (isSelfTransfer) {
      type = 'TRANSFER';
      amountMinor = debitMinor;
    }

    // Duplicate detection
    const matchKey = `${parsedDate}|${amountMinor}|${type}`;
    const isDuplicate = existingSet.has(matchKey);
    const duplicateReason = isDuplicate ? 'Exact match found in ledger on this date' : undefined;

    const { categoryId, needWant } = suggestCategory(narration, type);

    parsedRows.push({
      id: `stmt_${i}_${Date.now()}`,
      date: parsedDate,
      rawDate,
      description: narration,
      merchant,
      reference,
      debitMinor,
      creditMinor,
      amountMinor,
      type,
      balanceMinor,
      suggestedCategoryId: categoryId,
      needWant,
      isDuplicate,
      duplicateReason,
      isSelfTransfer,
      selected: !isDuplicate,
    });
  }

  // Summary calculations
  const totalCreditsMinor = parsedRows.reduce((sum, r) => sum + r.creditMinor, 0);
  const totalDebitsMinor = parsedRows.reduce((sum, r) => sum + r.debitMinor, 0);
  const netCashflowMinor = totalCreditsMinor - totalDebitsMinor;
  const duplicateCount = parsedRows.filter(r => r.isDuplicate).length;

  // Mathematical Reconciliation Check
  let openingBalanceMinor: number | null = null;
  let closingBalanceMinor: number | null = null;
  let expectedClosingMinor: number | null = null;
  let discrepancyMinor: number | null = null;
  let hasReconciliation = false;

  if (runningBalances.length >= 2) {
    // If chronological (oldest to newest): opening is first row balance minus its net effect
    const firstRow = parsedRows[0];
    const lastRow = parsedRows[parsedRows.length - 1];

    if (firstRow.balanceMinor !== null && lastRow.balanceMinor !== null) {
      // Determine if ascending or descending order
      const isAscending = firstRow.date <= lastRow.date;

      if (isAscending) {
        // First row's previous balance = row.balance - row.credit + row.debit
        openingBalanceMinor = firstRow.balanceMinor - firstRow.creditMinor + firstRow.debitMinor;
        closingBalanceMinor = lastRow.balanceMinor;
      } else {
        openingBalanceMinor = lastRow.balanceMinor - lastRow.creditMinor + lastRow.debitMinor;
        closingBalanceMinor = firstRow.balanceMinor;
      }

      expectedClosingMinor = openingBalanceMinor + totalCreditsMinor - totalDebitsMinor;
      discrepancyMinor = expectedClosingMinor - closingBalanceMinor;
      hasReconciliation = true;
    }
  }

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
  };
}
