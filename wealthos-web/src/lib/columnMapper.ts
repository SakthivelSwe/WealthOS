/**
 * WealthOS Bank Statement Column Discovery & Mapper
 * Conforms to WEALTHOS_FINAL_SPEC.md v3 §6.2.
 * Exact and whole-word matching to eliminate false positives.
 */

export interface ColumnMapping {
  headerRowIndex: number;
  dateCol: number;
  descriptionCol: number;
  debitCol: number;
  creditCol: number;
  amountCol: number;
  indicatorCol: number;
  balanceCol: number;
  referenceCol: number;
  confidence: 'HIGH' | 'MEDIUM' | 'LOW';
}

const DATE_HEADERS = [
  'tran date', 'transaction date', 'value date', 'txn date', 'posting date', 'date', 'txn_date', 'trans date',
];

const NARRATION_HEADERS = [
  'particulars', 'narration', 'description', 'remarks', 'transaction remarks', 'details', 'desc', 'memo',
];

const DEBIT_HEADERS = [
  'withdrawal amt.', 'withdrawal amt', 'withdrawal', 'debit amt.', 'debit amt', 'debit amount', 'debit', 'dr amt', 'dr',
];

const CREDIT_HEADERS = [
  'deposit amt.', 'deposit amt', 'deposit', 'credit amt.', 'credit amt', 'credit amount', 'credit', 'cr amt', 'cr',
];

const AMOUNT_HEADERS = [
  'amount', 'net amount', 'txn amount', 'transaction amount', 'amt',
];

const INDICATOR_HEADERS = [
  'type', 'cr/dr', 'dr/cr', 'cr_dr', 'dr_cr', 'indicator',
];

const BALANCE_HEADERS = [
  'balance', 'closing balance', 'running balance', 'closing bal', 'bal',
];

const REFERENCE_HEADERS = [
  'chq no', 'cheque no', 'ref no', 'reference no', 'utr', 'chq / ref no', 'ref/chq no', 'reference', 'rrn',
];

function matchesHeader(colHeader: string, dictionary: string[]): boolean {
  const h = colHeader.toLowerCase().trim();
  return dictionary.some(target => {
    if (h === target) return true;
    const re = new RegExp(`\\b${target.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`, 'i');
    return re.test(h);
  });
}

export function discoverMapping(rows: string[][]): ColumnMapping | null {
  const maxScan = Math.min(40, rows.length);

  for (let i = 0; i < maxScan; i++) {
    const row = rows[i];
    if (row.length < 2) continue;

    const dCol = row.findIndex(c => matchesHeader(c, DATE_HEADERS));
    const nCol = row.findIndex(c => matchesHeader(c, NARRATION_HEADERS));

    if (dCol >= 0 && nCol >= 0 && dCol !== nCol) {
      const debitCol = row.findIndex(c => matchesHeader(c, DEBIT_HEADERS));
      const creditCol = row.findIndex(c => matchesHeader(c, CREDIT_HEADERS));
      const amountCol = row.findIndex(c => matchesHeader(c, AMOUNT_HEADERS));
      const indicatorCol = row.findIndex(c => matchesHeader(c, INDICATOR_HEADERS));
      const balanceCol = row.findIndex(c => matchesHeader(c, BALANCE_HEADERS));
      const refCol = row.findIndex(c => matchesHeader(c, REFERENCE_HEADERS));

      const hasAmount = (debitCol >= 0 && creditCol >= 0) || amountCol >= 0;
      const confidence = hasAmount ? 'HIGH' : 'MEDIUM';

      return {
        headerRowIndex: i,
        dateCol: dCol,
        descriptionCol: nCol,
        debitCol,
        creditCol,
        amountCol,
        indicatorCol,
        balanceCol,
        referenceCol: refCol,
        confidence,
      };
    }
  }

  return null;
}

const MONTH_NAMES: Record<string, string> = {
  jan: '01', feb: '02', mar: '03', apr: '04', may: '05', jun: '06',
  jul: '07', aug: '08', sep: '09', oct: '10', nov: '11', dec: '12',
};

export function parseStatementDate(raw: string): string | null {
  if (!raw) return null;
  const s = raw.trim();

  // YYYY-MM-DD
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;

  // DD/MM/YYYY or DD-MM-YYYY
  const dmy = s.match(/^(\d{1,2})[/-](\d{1,2})[/-](\d{4})$/);
  if (dmy) {
    const d = dmy[1].padStart(2, '0');
    const m = dmy[2].padStart(2, '0');
    const y = dmy[3];
    return `${y}-${m}-${d}`;
  }

  // DD-MMM-YYYY or DD MMM YYYY (e.g. 05-Oct-2026 or 05 Oct 2026)
  const dmmm = s.match(/^(\d{1,2})[- ]([a-zA-Z]{3})[- ](\d{4})$/);
  if (dmmm) {
    const d = dmmm[1].padStart(2, '0');
    const mStr = dmmm[2].toLowerCase();
    const m = MONTH_NAMES[mStr];
    const y = dmmm[3];
    if (m) return `${y}-${m}-${d}`;
  }

  return null;
}
