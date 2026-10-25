/**
 * WealthOS Bank Narration Decoder & Rail Extractor
 * Conforms to WEALTHOS_FINAL_SPEC.md v3 §6.3.
 * Data-driven rule table for Indian banking narrations.
 */

export type PaymentRail =
  | 'UPI_P2M'
  | 'UPI_P2A'
  | 'UPI_P2P'
  | 'UPI_AUTOPAY'
  | 'IMPS'
  | 'NEFT'
  | 'RTGS'
  | 'ACH_NACH'
  | 'CARD_POS'
  | 'ATM'
  | 'NON_UPI';

export interface DecodedNarration {
  cleanMerchantOrPayee: string;
  referenceOrRrn: string | null;
  vpa: string | null;
  paymentRail: PaymentRail;
  isRecurringMandate: boolean;
  isSelfTransfer: boolean;
  formatName: string;
}

const UPI_MODES = new Set(['P2M', 'P2A', 'P2P', 'DR', 'CR', 'MANDATE', 'AUTOPAY', 'REV']);
const RRN_REGEX = /^[0-9]{9,16}$/;

export function cleanMerchantName(s: string, reference?: string | null): string {
  let str = s;
  if (reference && reference.trim()) {
    str = str.replace(reference, ' ');
  }
  const cleaned = str
    .replace(/\b(upi|pos|atm|ach|nach|neft|imps|rtgs|cms|bil|inf)\b/gi, ' ')
    .replace(/[0-9]{8,}/g, ' ')
    .replace(/[/\\-_:*]+/g, ' ')
    .trim();

  const tokens = cleaned.split(/\s+/).filter(t => t.length > 1);
  return tokens.slice(0, 4).join(' ') || s.slice(0, 30);
}

function extractFirst12Digit(s: string): string | null {
  const m = s.match(/\b[0-9]{9,16}\b/);
  return m ? m[0] : null;
}

function extractReference(s: string, prefix: string): string | null {
  const re = new RegExp(`(?:${prefix})[\\s/:-]*([A-Za-z0-9]{8,24})`, 'i');
  const m = s.match(re);
  return m ? m[1] : extractFirst12Digit(s);
}

export function decodeNarration(rawNarration: string): DecodedNarration {
  const s = rawNarration.trim();
  if (!s) {
    return {
      cleanMerchantOrPayee: 'Unknown',
      referenceOrRrn: null,
      vpa: null,
      paymentRail: 'NON_UPI',
      isRecurringMandate: false,
      isSelfTransfer: false,
      formatName: 'EMPTY',
    };
  }

  const lower = s.toLowerCase();
  const isSelf =
    lower.includes('self transfer') ||
    lower.includes('own a/c') ||
    lower.includes('own account') ||
    lower.includes('to own') ||
    lower.includes('to self');

  // 1. Slash UPI: UPI/<mode?>/<rrn 9-16 digits>/<payee>/...
  if (s.toUpperCase().startsWith('UPI/')) {
    const parts = s.split('/');
    if (parts.length >= 3) {
      let idx = 1;
      let mode: string | null = null;
      if (UPI_MODES.has(parts[idx].toUpperCase())) {
        mode = parts[idx].toUpperCase();
        idx++;
      }

      const rrnCandidate = parts[idx]?.trim();
      let rrn: string | null = null;
      if (rrnCandidate && RRN_REGEX.test(rrnCandidate)) {
        rrn = rrnCandidate;
        idx++;
      }

      const payee = parts[idx]?.trim() || '';
      const isMandate = mode === 'MANDATE' || mode === 'AUTOPAY' || lower.includes('mandate') || lower.includes('autopay');
      let rail: PaymentRail = 'UPI_P2M';
      if (isMandate) rail = 'UPI_AUTOPAY';
      else if (mode === 'P2M') rail = 'UPI_P2M';
      else if (mode === 'P2A') rail = 'UPI_P2A';
      else if (mode === 'P2P') rail = 'UPI_P2P';

      return {
        cleanMerchantOrPayee: cleanMerchantName(payee || s),
        referenceOrRrn: rrn,
        vpa: null,
        paymentRail: rail,
        isRecurringMandate: isMandate,
        isSelfTransfer: isSelf,
        formatName: 'UPI_SLASH',
      };
    }
  }

  // 2. Hyphen UPI: UPI-<payee>-<vpa>-<ifsc?>-<rrn>-...
  if (s.toUpperCase().startsWith('UPI-')) {
    const parts = s.split('-');
    if (parts.length >= 3) {
      const payee = parts[1].trim();
      const vpa = parts[2]?.trim().includes('@') ? parts[2].trim() : null;
      const rrn = parts.find(p => RRN_REGEX.test(p.trim()))?.trim() || null;
      const isMandate = lower.includes('mandate') || lower.includes('autopay');
      return {
        cleanMerchantOrPayee: cleanMerchantName(payee),
        referenceOrRrn: rrn,
        vpa,
        paymentRail: isMandate ? 'UPI_AUTOPAY' : 'UPI_P2M',
        isRecurringMandate: isMandate,
        isSelfTransfer: isSelf,
        formatName: 'UPI_HYPHEN',
      };
    }
  }

  // 3. Slash Bank Channels: NEFT/CMS/<ref>/<payee>, IMPS/<ref>/<payee>, ACH/<payee>
  if (s.includes('/')) {
    const slashParts = s.split('/').map(p => p.trim());
    const firstUpper = (slashParts[0] || '').toUpperCase();
    if (['NEFT', 'IMPS', 'RTGS', 'ACH', 'NACH'].includes(firstUpper)) {
      let idx = 1;
      const channelTags = new Set(['CMS', 'BIL', 'INF', 'P2A', 'P2P', 'P2M', 'DR', 'CR']);
      while (idx < slashParts.length && channelTags.has(slashParts[idx].toUpperCase())) {
        idx++;
      }
      let ref: string | null = null;
      const candidateRef = slashParts[idx];
      if (candidateRef && /^[A-Za-z0-9]{8,24}$/.test(candidateRef)) {
        ref = candidateRef;
        idx++;
      }
      const rawPayee = slashParts.slice(idx).join(' ');
      const cleanPayee = cleanMerchantName(rawPayee || s, ref);
      let rail: PaymentRail = 'NON_UPI';
      if (firstUpper === 'NEFT') rail = 'NEFT';
      else if (firstUpper === 'IMPS') rail = 'IMPS';
      else if (firstUpper === 'RTGS') rail = 'RTGS';
      else if (firstUpper === 'ACH' || firstUpper === 'NACH') rail = 'ACH_NACH';

      return {
        cleanMerchantOrPayee: cleanPayee,
        referenceOrRrn: ref || extractFirst12Digit(s),
        vpa: null,
        paymentRail: rail,
        isRecurringMandate: rail === 'ACH_NACH' || lower.includes('mandate') || lower.includes('autopay'),
        isSelfTransfer: isSelf,
        formatName: firstUpper,
      };
    }
  }

  // 4. Other IMPS / NEFT / RTGS / ACH
  if (lower.includes('neft')) {
    const ref = extractReference(s, 'NEFT');
    return {
      cleanMerchantOrPayee: cleanMerchantName(s.replace(/neft[/-]?[A-Za-z0-9]*[/-]?/gi, ' '), ref),
      referenceOrRrn: ref,
      vpa: null,
      paymentRail: 'NEFT',
      isRecurringMandate: false,
      isSelfTransfer: isSelf,
      formatName: 'NEFT',
    };
  }
  if (lower.includes('imps')) {
    const ref = extractReference(s, 'IMPS');
    return {
      cleanMerchantOrPayee: cleanMerchantName(s.replace(/imps[/-]?[A-Za-z0-9]*[/-]?/gi, ' '), ref),
      referenceOrRrn: ref,
      vpa: null,
      paymentRail: 'IMPS',
      isRecurringMandate: false,
      isSelfTransfer: isSelf,
      formatName: 'IMPS',
    };
  }
  if (lower.includes('rtgs')) {
    const ref = extractReference(s, 'RTGS');
    return {
      cleanMerchantOrPayee: cleanMerchantName(s.replace(/rtgs[/-]?[A-Za-z0-9]*[/-]?/gi, ' '), ref),
      referenceOrRrn: ref,
      vpa: null,
      paymentRail: 'RTGS',
      isRecurringMandate: false,
      isSelfTransfer: isSelf,
      formatName: 'RTGS',
    };
  }
  if (lower.includes('ach') || lower.includes('nach') || lower.includes('ecs')) {
    const ref = extractFirst12Digit(s);
    return {
      cleanMerchantOrPayee: cleanMerchantName(s, ref),
      referenceOrRrn: ref,
      vpa: null,
      paymentRail: 'ACH_NACH',
      isRecurringMandate: true,
      isSelfTransfer: isSelf,
      formatName: 'ACH_NACH',
    };
  }

  // 4. Card POS / ATM
  if (lower.startsWith('pos ') || lower.includes(' pos ') || lower.includes('card')) {
    return {
      cleanMerchantOrPayee: cleanMerchantName(s.replace(/^pos\s+\d+\s+/i, '')),
      referenceOrRrn: extractFirst12Digit(s),
      vpa: null,
      paymentRail: 'CARD_POS',
      isRecurringMandate: false,
      isSelfTransfer: isSelf,
      formatName: 'CARD_POS',
    };
  }
  if (lower.includes('atm') || lower.includes('wdl')) {
    return {
      cleanMerchantOrPayee: 'ATM Cash Withdrawal',
      referenceOrRrn: extractFirst12Digit(s),
      vpa: null,
      paymentRail: 'ATM',
      isRecurringMandate: false,
      isSelfTransfer: isSelf,
      formatName: 'ATM',
    };
  }

  // 5. Generic fallback
  return {
    cleanMerchantOrPayee: cleanMerchantName(s),
    referenceOrRrn: extractFirst12Digit(s),
    vpa: null,
    paymentRail: 'NON_UPI',
    isRecurringMandate: lower.includes('mandate') || lower.includes('autopay') || lower.includes('sub'),
    isSelfTransfer: isSelf,
    formatName: 'GENERIC_FALLBACK',
  };
}
