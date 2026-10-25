/**
 * WealthOS Duplicate Detection Engine
 * Conforms to WEALTHOS_FINAL_SPEC.md v3 §4.6.
 * Primary: unique (user_id, rrn) where rrn is not null.
 * Fallback: hash of (date, amountPaise, normalisedCounterparty, rail).
 */

export interface DuplicateCandidate {
  date: string; // YYYY-MM-DD
  amountMinor: number;
  merchant?: string | null;
  reference?: string | null;
  accountId?: string | null;
  description?: string | null;
}

export type Likelihood = 'NONE' | 'POSSIBLE' | 'LIKELY' | 'EXACT';

export function normalizeText(text?: string | null): string {
  return (text || '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
}

export function levenshteinDistance(a: string, b: string): number {
  let prev = Array.from({ length: b.length + 1 }, (_, i) => i);
  let cur = new Array(b.length + 1);

  for (let i = 1; i <= a.length; i++) {
    cur[0] = i;
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      cur[j] = Math.min(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost);
    }
    const temp = prev;
    prev = cur;
    cur = temp;
  }

  return prev[b.length];
}

export function stringSimilarity(a: string, b: string): number {
  if (a === b) return 1.0;
  const maxLen = Math.max(a.length, b.length);
  if (maxLen === 0) return 1.0;
  return 1.0 - levenshteinDistance(a, b) / maxLen;
}

/**
 * Computes deterministic dedup key conforming to Spec 4.6.
 */
export function computeDedupKey(
  date: string,
  amountMinor: number,
  counterparty: string,
  rail: string,
  rrn?: string | null
): { rrn: string | null; fallbackKey: string } {
  const cleanRrn = rrn?.trim() || null;
  const canonical = `${date}|${amountMinor}|${normalizeText(counterparty)}|${rail.toUpperCase().trim()}`;
  return {
    rrn: cleanRrn,
    fallbackKey: canonical,
  };
}

export function compareCandidates(a: DuplicateCandidate, b: DuplicateCandidate): Likelihood {
  if (a.amountMinor !== b.amountMinor) return 'NONE';
  if (a.accountId && b.accountId && a.accountId !== b.accountId) return 'NONE';

  const refA = normalizeText(a.reference);
  const refB = normalizeText(b.reference);
  if (refA && refA === refB) return 'LIKELY';

  const dateA = new Date(a.date).getTime();
  const dateB = new Date(b.date).getTime();
  const diffDays = Math.abs((dateA - dateB) / (1000 * 60 * 60 * 24));
  if (diffDays > 1) return 'NONE';

  const textA = normalizeText(a.merchant || a.description);
  const textB = normalizeText(b.merchant || b.description);

  if (!textA || !textB) {
    return diffDays === 0 ? 'POSSIBLE' : 'NONE';
  }

  const sim = stringSimilarity(textA, textB);
  if (diffDays === 0 && sim >= 0.8) return 'LIKELY';
  if (sim >= 0.6) return 'POSSIBLE';
  return 'NONE';
}
