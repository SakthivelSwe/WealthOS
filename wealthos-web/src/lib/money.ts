/**
 * WealthOS Integer Money & Precision Engine
 * Zero floating-point representation. All calculations in integer minor units (paise/cents).
 * Conforms to WEALTHOS_FINAL_SPEC.md v3 §2.1.
 */

/**
 * Non-negative integer HALF_UP rounding: (2 * num + den) / (2 * den).
 * Guarantees identical rounding behavior between Kotlin and TypeScript.
 */
export function roundHalfUp(num: number | bigint, den: number | bigint): number {
  const n = typeof num === 'bigint' ? num : BigInt(Math.floor(num));
  const d = typeof den === 'bigint' ? den : BigInt(Math.floor(den));
  if (n < 0n) throw new Error(`Numerator must be non-negative: ${num}`);
  if (d <= 0n) throw new Error(`Denominator must be positive: ${den}`);
  return Number((2n * n + d) / (2n * d));
}

/**
 * Calculates integer percentage using explicit rounding (HALF_UP).
 * Safely handles zero/negative denominator, no income/spend, and negative balances.
 */
export function calculatePercentage(numeratorMinor: number, denominatorMinor: number): number {
  if (denominatorMinor <= 0 || numeratorMinor <= 0) return 0;
  return roundHalfUp(numeratorMinor * 100, denominatorMinor);
}

/**
 * Parses monetary strings (e.g. "₹ 1,23,970.50", "12,500 Dr", "(500.00)")
 * into exact integer minor units without floating-point conversion.
 */
export function parseMoney(raw: string | number | null | undefined, minorUnit: number = 2): number {
  if (raw === null || raw === undefined) return 0;
  if (typeof raw === 'number') {
    if (isNaN(raw)) return 0;
    // Already an integer minor amount if passed as integer number
    return Math.trunc(raw);
  }

  let s = String(raw).trim();
  if (!s || s === '-' || s === '—') return 0;

  const isNegative = (s.startsWith('(') && s.endsWith(')')) ||
    s.endsWith('Dr') || s.endsWith('DR') || s.endsWith('dr') ||
    s.startsWith('-');

  // Keep only digits and decimal point
  const clean = s.replace(/[^0-9.]/g, '');
  if (!clean) return 0;

  const parts = clean.split('.');
  const intPart = BigInt(parts[0] || '0');
  let fracPart = 0n;

  if (parts.length > 1 && minorUnit > 0) {
    const fracStr = parts[1].slice(0, minorUnit).padEnd(minorUnit, '0');
    fracPart = BigInt(fracStr || '0');
  }

  let multiplier = 1n;
  for (let i = 0; i < minorUnit; i++) {
    multiplier *= 10n;
  }

  const totalMinor = intPart * multiplier + fracPart;
  const num = Number(totalMinor);
  return isNegative ? -num : num;
}

/**
 * Formats minor units into Indian digit grouped format: ₹1,23,970 or ₹1,23,970.50.
 */
export function formatIndian(
  amountMinor: number,
  minorUnit: number = 2,
  showFraction: boolean = false,
  symbol: string = '₹'
): string {
  const isNeg = amountMinor < 0;
  const absMinor = Math.abs(Math.trunc(amountMinor));
  const divisor = Math.pow(10, minorUnit);

  const whole = minorUnit > 0 ? Math.floor(absMinor / divisor) : absMinor;
  const fraction = minorUnit > 0 ? absMinor % divisor : 0;

  const wholeStr = String(whole);
  let grouped: string;

  if (wholeStr.length <= 3) {
    grouped = wholeStr;
  } else {
    const last3 = wholeStr.slice(-3);
    let remaining = wholeStr.slice(0, -3);
    const chunks: string[] = [];
    while (remaining.length > 2) {
      chunks.unshift(remaining.slice(-2));
      remaining = remaining.slice(0, -2);
    }
    if (remaining.length > 0) {
      chunks.unshift(remaining);
    }
    grouped = `${chunks.join(',')},${last3}`;
  }

  const fracStr = (showFraction && minorUnit > 0)
    ? `.${String(fraction).padStart(minorUnit, '0')}`
    : '';

  return `${isNeg ? '-' : ''}${symbol}${grouped}${fracStr}`;
}
