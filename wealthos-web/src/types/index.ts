// ============================================================
// WealthOS Type Definitions — Mirrors Android Entities
// ============================================================

export type AccountType = 'BANK' | 'CASH' | 'UPI_WALLET' | 'SAVINGS' | 'CREDIT_CARD' | 'INVESTMENT' | 'LOAN' | 'OTHER_ASSET' | 'OTHER_LIABILITY';
export type TxnType = 'EXPENSE' | 'INCOME' | 'TRANSFER';
export type CategoryKind = 'EXPENSE' | 'INCOME';
export type NeedWant = 'NEED' | 'WANT' | 'SAVING';
export type Frequency = 'DAILY' | 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY';
export type ThemeMode = 'DARK' | 'LIGHT' | 'MIDNIGHT';
export type AccountRole = 'INCOME_VAULT' | 'DAILY_UPI_SPEND' | 'EMERGENCY_RESERVE' | 'WEALTH_PORTFOLIO' | 'LIABILITY_LOAN';

export interface Account {
  id: string;
  user_id: string;
  name: string;
  type: AccountType;
  institution: string | null;
  last_four: string | null;
  opening_balance_minor: number;
  currency: string;
  include_in_net_worth: boolean;
  include_in_stats: boolean;
  archived: boolean;
  created_at: number;
  updated_at: number;
  account_role?: AccountRole | null;
  bank_name?: string | null;
  interest_rate_percent_bps?: number;
  monthly_emi_paise?: number;
  outstanding_paise?: number;
  is_bnpl?: boolean;
  // computed
  current_balance_minor?: number;
}

export interface Category {
  id: string;
  user_id: string;
  parent_id: string | null;
  name: string;
  kind: CategoryKind;
  need_want: NeedWant | null;
  icon: string;
  color: number;
  archived: boolean;
  sort_order: number;
  budget_limit_minor?: number | null;
}

export interface Transaction {
  id: string;
  user_id: string;
  type: TxnType;
  amount_minor: number;
  currency: string;
  local_date: string;
  local_time: string;
  account_id: string;
  to_account_id: string | null;
  category_id: string | null;
  merchant: string | null;
  description: string | null;
  notes: string | null;
  payment_method: string | null;
  need_want: NeedWant | null;
  tags: string[];
  imported: boolean;
  source: string;
  created_at: number;
  updated_at: number;
  deleted_at: number | null;
  jar_bucket?: string | null;
  spend_emotion_tag?: 'NEED' | 'PLANNED_WANT' | 'REGRET_LEAK' | 'UNPLANNED_TRANSFER' | 'INCOME' | 'UNCATEGORISED' | null;
  payment_rail?: 'UPI_P2M' | 'UPI_P2A' | 'UPI_AUTOPAY' | 'IMPS_NEFT_RTGS' | 'NON_UPI' | null;
  rrn?: string | null;
  counterparty_name?: string | null;
  vpa?: string | null;
  import_batch_id?: string | null;
  user_overridden?: boolean;
  is_recurring_mandate?: boolean;
}

export interface SalaryCycle {
  id: string;
  user_id: string;
  cycle_start_date: string;
  salary_paise: number;
  baseline_paise: number;
  emi_paise: number;
  framework: 'SIX_JAR' | 'THREE_BOX' | 'FIFTY_THIRTY_TWENTY' | 'CUSTOM';
  jar_amounts: Record<string, number>;
  days_in_cycle: number;
  daily_safe_upi_paise: number;
  net_worth_snapshot_paise: number | null;
  checklist_state: Record<string, boolean>;
  created_at: number;
}

export interface UserFinancialProfile {
  user_id: string;
  income_schedule_type: 'VARIABLE_WINDOW' | 'FIXED_DATE' | 'IRREGULAR';
  payday_window_start_day: number;
  payday_window_end_day: number;
  expected_salary_min_paise: number | null;
  expected_salary_max_paise: number | null;
  fixed_baseline_paise: number | null;
  baseline_method: 'USER_ENTERED' | 'HISTORY_MEDIAN' | 'ESTIMATE_55_PERCENT';
  lifestyle_creep_lock: boolean;
  needs_buffer_paise: number;
  allocation_framework: 'SIX_JAR' | 'THREE_BOX' | 'FIFTY_THIRTY_TWENTY' | 'CUSTOM';
  custom_bps: Record<string, number> | null;
  play_to_long_term_shift: boolean;
  upi_needs_allowance_paise: number;
  emergency_target_months: number;
  survival_threshold_paise: number | null;
  small_scan_threshold_paise: number;
  ego_check_threshold_paise: number;
  cooldown_hours: number;
  currency: string;
  locale: string;
  timezone: string;
  language: 'en' | 'ta';
  updated_at: number;
}

export interface SinkingFund {
  id: string;
  user_id: string;
  name: string;
  target_paise: number;
  current_paise: number;
  target_date: string;
  monthly_allocation_paise: number;
  archived: boolean;
  created_at: number;
  updated_at: number;
}

export interface MerchantRule {
  id: string;
  user_id: string;
  merchant_pattern: string;
  category_id: string | null;
  jar_bucket: string | null;
  spend_emotion_tag: string | null;
  is_recurring_mandate: boolean;
  created_at: number;
}

export interface Mandate {
  id: string;
  user_id: string;
  merchant_name: string;
  amount_paise: number;
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY';
  next_deduction_date: string;
  account_id: string | null;
  payment_rail: string;
  active: boolean;
  created_at: number;
  updated_at: number;
}

export interface JournalEntry {
  row_id?: number;
  user_id: string;
  txn_id: string;
  account_id: string | null;
  category_id: string | null;
  amount_minor: number;
}

export interface Goal {
  id: string;
  user_id: string;
  name: string;
  target_amount_minor: number;
  current_amount_minor: number;
  planned_monthly_contribution_minor: number;
  currency: string;
  target_date: string;
  archived: boolean;
  created_at: number;
  updated_at: number;
}

export interface Debt {
  id: string;
  user_id: string;
  name: string;
  principal_minor: number;
  current_balance_minor: number;
  annual_percent: string;
  expected_monthly_payment_minor: number;
  currency: string;
  archived: boolean;
  created_at: number;
  updated_at: number;
}

export interface ChitFund {
  id: string;
  user_id: string;
  name: string;
  total_months: number;
  start_month: string;
  monthly_contribution_minor: number;
  currency: string;
  active: boolean;
  created_at: number;
  updated_at: number;
}

export interface RecurringTransaction {
  id: string;
  user_id: string;
  type: TxnType;
  amount_minor: number;
  currency: string;
  account_id: string;
  category_id: string | null;
  description: string | null;
  frequency: Frequency;
  next_run_date: string;
  active: boolean;
  created_at: number;
  updated_at: number;
}

export interface Budget {
  id: string;
  user_id: string;
  name: string;
  period: string;
  basis_income_minor: number;
  needs_pct: number;
  wants_pct: number;
  savings_pct: number;
  currency: string;
  active: boolean;
  created_at: number;
  updated_at: number;
}

/** Monthly income plan — one per calendar month (YYYY-MM). Tracks actual salary received,
 *  planned allocation percentages, committed obligations, and safe-to-spend. */
export interface MonthlyPlan {
  id: string;
  user_id: string;
  month: string;             // 'YYYY-MM'
  currency: string;
  status: 'DRAFT' | 'ACTIVE' | 'CLOSED';
  // Income
  confirmed_income_minor: number;
  estimated_income_minor: number;
  income_source: string | null;        // 'Salary' | 'Bonus' | 'Freelance' | 'Other'
  income_received_date: string | null; // 'YYYY-MM-DD'
  // Allocation percentages
  needs_pct: number;
  wants_pct: number;
  savings_pct: number;
  // Committed outflows (minor units)
  committed_bills_minor: number;
  committed_debt_payments_minor: number;
  committed_savings_minor: number;
  buffer_pct: number;                  // cash buffer % of income (default 5)
  notes: string | null;
  created_at: number;
  updated_at: number;
}

export interface UserSettings {
  user_id: string;
  currency: string;
  theme_mode: ThemeMode;
  mask_amounts: boolean;
  user_name: string;
  phone_number: string | null;
  basis_income_minor: number;
  needs_pct: number;
  wants_pct: number;
  savings_pct: number;
  daily_reminder_enabled: boolean;
  daily_reminder_hour: number;
  updated_at: number;
}

export interface AppNotification {
  id: string;
  user_id: string;
  title: string;
  message: string;
  category: string;
  action_type: string | null;
  payload: string | null;
  read: boolean;
  created_at: number;
}

// Currency helpers
export interface CurrencyInfo {
  code: string;
  symbol: string;
  name: string;
  minorUnit: number;
}

export const CURRENCIES: CurrencyInfo[] = [
  { code: 'INR', symbol: '₹', name: 'Indian Rupee', minorUnit: 2 },
  { code: 'USD', symbol: '$', name: 'US Dollar', minorUnit: 2 },
  { code: 'EUR', symbol: '€', name: 'Euro', minorUnit: 2 },
  { code: 'GBP', symbol: '£', name: 'British Pound', minorUnit: 2 },
  { code: 'JPY', symbol: '¥', name: 'Japanese Yen', minorUnit: 0 },
  { code: 'SGD', symbol: 'S$', name: 'Singapore Dollar', minorUnit: 2 },
  { code: 'AED', symbol: 'د.إ', name: 'UAE Dirham', minorUnit: 2 },
  { code: 'AUD', symbol: 'A$', name: 'Australian Dollar', minorUnit: 2 },
  { code: 'CAD', symbol: 'C$', name: 'Canadian Dollar', minorUnit: 2 },
];

export function getCurrencyInfo(code: string): CurrencyInfo {
  return CURRENCIES.find(c => c.code === code) || { code, symbol: code + ' ', name: code, minorUnit: 2 };
}

export function formatMoney(amountMinor: number, currencyCode: string = 'INR', masked: boolean = false): string {
  if (masked) return '••••••';
  const info = getCurrencyInfo(currencyCode);
  const amount = amountMinor / Math.pow(10, info.minorUnit);
  const locale = currencyCode === 'INR' ? 'en-IN' : 'en-US';
  return `${info.symbol}${amount.toLocaleString(locale, { minimumFractionDigits: info.minorUnit, maximumFractionDigits: info.minorUnit })}`;
}

export function parseMoney(text: string, currencyCode: string = 'INR'): number {
  if (!text) return 0;
  const info = getCurrencyInfo(currencyCode);
  const s = String(text).trim();
  if (!s || s === '-' || s === '—') return 0;

  const isNegative = (s.startsWith('(') && s.endsWith(')')) ||
    s.endsWith('Dr') || s.endsWith('DR') || s.endsWith('dr') ||
    s.startsWith('-');

  const clean = s.replace(/[^0-9.]/g, '');
  if (!clean) return 0;

  const parts = clean.split('.');
  const intPart = BigInt(parts[0] || '0');
  let fracPart = 0n;

  if (parts.length > 1 && info.minorUnit > 0) {
    const fracStr = parts[1].slice(0, info.minorUnit).padEnd(info.minorUnit, '0');
    fracPart = BigInt(fracStr || '0');
  }

  let multiplier = 1n;
  for (let i = 0; i < info.minorUnit; i++) {
    multiplier *= 10n;
  }

  const totalMinor = intPart * multiplier + fracPart;
  const num = Number(totalMinor);
  return isNegative ? -num : num;
}

// Account type helpers
export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  BANK: '🏦 Bank Account',
  CASH: '💵 Cash in Hand',
  UPI_WALLET: '📱 UPI Wallet (GPay / Paytm)',
  SAVINGS: '🏧 Savings Account',
  CREDIT_CARD: '💳 Credit Card',
  INVESTMENT: '📈 Investments & Stocks',
  LOAN: '🏠 Loans & EMI',
  OTHER_ASSET: '💎 Other Assets (Gold / Land)',
  OTHER_LIABILITY: '📋 Money I Owe Others',
};

export const ACCOUNT_TYPE_COLORS: Record<AccountType, string> = {
  BANK: 'var(--accent-blue)',
  CASH: 'var(--accent-green)',
  UPI_WALLET: 'var(--accent-purple)',
  SAVINGS: 'var(--accent-forest)',
  CREDIT_CARD: 'var(--accent-orange)',
  INVESTMENT: 'var(--accent-purple)',
  LOAN: 'var(--accent-red)',
  OTHER_ASSET: 'var(--accent-blue)',
  OTHER_LIABILITY: 'var(--accent-red)',
};

// Popular Indian Bank Institutions with authentic brand accents
export interface BankInstitution {
  id: string;
  name: string;
  code: string;
  color: string;
  badgeBg: string;
  defaultType: AccountType;
  sampleName: string;
}

export const POPULAR_BANKS: BankInstitution[] = [
  { id: 'axis', name: 'Axis Bank', code: 'AXIS', color: '#97144d', badgeBg: 'rgba(151, 20, 77, 0.22)', defaultType: 'BANK', sampleName: 'Axis Bank Salary Account' },
  { id: 'hdfc', name: 'HDFC Bank', code: 'HDFC', color: '#004c8f', badgeBg: 'rgba(0, 76, 143, 0.22)', defaultType: 'BANK', sampleName: 'HDFC Salary Account' },
  { id: 'icici', name: 'ICICI Bank', code: 'ICICI', color: '#e35205', badgeBg: 'rgba(227, 82, 5, 0.22)', defaultType: 'BANK', sampleName: 'ICICI Savings Account' },
  { id: 'sbi', name: 'State Bank of India', code: 'SBI', color: '#1e40af', badgeBg: 'rgba(30, 64, 175, 0.22)', defaultType: 'SAVINGS', sampleName: 'SBI Emergency Fund' },
  { id: 'kotak', name: 'Kotak Mahindra Bank', code: 'KOTAK', color: '#dc2626', badgeBg: 'rgba(220, 38, 38, 0.22)', defaultType: 'BANK', sampleName: 'Kotak 811 Account' },
  { id: 'bob', name: 'Bank of Baroda', code: 'BOB', color: '#ea580c', badgeBg: 'rgba(234, 88, 12, 0.22)', defaultType: 'BANK', sampleName: 'Bank of Baroda Account' },
  { id: 'pnb', name: 'Punjab National Bank', code: 'PNB', color: '#991b1b', badgeBg: 'rgba(153, 27, 27, 0.22)', defaultType: 'BANK', sampleName: 'PNB Savings Account' },
  { id: 'idfc', name: 'IDFC FIRST Bank', code: 'IDFC', color: '#9f1239', badgeBg: 'rgba(159, 18, 57, 0.22)', defaultType: 'BANK', sampleName: 'IDFC FIRST Savings' },
  { id: 'canara', name: 'Canara Bank', code: 'CANARA', color: '#0284c7', badgeBg: 'rgba(2, 132, 199, 0.22)', defaultType: 'BANK', sampleName: 'Canara Bank Savings' },
  { id: 'upi', name: 'UPI / Paytm / GPay', code: 'UPI', color: '#06b6d4', badgeBg: 'rgba(6, 182, 212, 0.22)', defaultType: 'UPI_WALLET', sampleName: 'Paytm / GPay Wallet' },
  { id: 'zerodha', name: 'Zerodha Coin / Kite', code: 'ZERODHA', color: '#8b5cf6', badgeBg: 'rgba(139, 92, 246, 0.22)', defaultType: 'INVESTMENT', sampleName: 'Zerodha Coin (Mutual Funds)' },
];

export function getBankColor(institution: string | null | undefined, accountType?: AccountType): string {
  if (institution) {
    const lower = institution.toLowerCase();
    const bank = POPULAR_BANKS.find(b => lower.includes(b.id) || lower.includes(b.code.toLowerCase()) || lower.includes(b.name.toLowerCase()));
    if (bank) return bank.color;
  }
  return accountType && ACCOUNT_TYPE_COLORS[accountType] ? ACCOUNT_TYPE_COLORS[accountType] : 'var(--accent-forest)';
}

export function formatAccountLabel(acc: Account): string {
  const typeLabel = ACCOUNT_TYPE_LABELS[acc.type] || acc.type;
  const digits = acc.last_four ? ` (•••• ${acc.last_four})` : '';
  return `${acc.name}${digits} • ${typeLabel}`;
}

// Default categories (mirrors Android catalog with vibrant, clear emojis)
export const DEFAULT_CATEGORIES: Omit<Category, 'user_id'>[] = [
  { id: 'cat_food', parent_id: null, name: '🍔 Food & Dining', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9A66B, archived: false, sort_order: 0 },
  { id: 'cat_food_groceries', parent_id: 'cat_food', name: '🥦 Groceries & Veggies', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9A66B, archived: false, sort_order: 1 },
  { id: 'cat_food_restaurant', parent_id: 'cat_food', name: '🍽️ Hotel & Restaurant', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9A66B, archived: false, sort_order: 2 },
  { id: 'cat_food_snacks', parent_id: 'cat_food', name: '☕ Snacks & Tea', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9A66B, archived: false, sort_order: 3 },
  { id: 'cat_food_coffee', parent_id: 'cat_food', name: '☕ Coffee & Cafe', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9A66B, archived: false, sort_order: 4 },
  { id: 'cat_food_delivery', parent_id: 'cat_food', name: '🛵 Food Delivery (Zomato/Swiggy)', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9A66B, archived: false, sort_order: 5 },
  { id: 'cat_transport', parent_id: null, name: '🚗 Travel & Commute', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFF7FA6D9, archived: false, sort_order: 6 },
  { id: 'cat_transport_fuel', parent_id: 'cat_transport', name: '⛽ Petrol & Fuel', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFF7FA6D9, archived: false, sort_order: 7 },
  { id: 'cat_transport_metro', parent_id: 'cat_transport', name: '🚇 Metro & Train', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFF7FA6D9, archived: false, sort_order: 8 },
  { id: 'cat_shopping', parent_id: null, name: '🛍️ Shopping & Spends', kind: 'EXPENSE', need_want: 'WANT', icon: 'default', color: 0xFFB58BC9, archived: false, sort_order: 9 },
  { id: 'cat_shopping_clothing', parent_id: 'cat_shopping', name: '👕 Clothes & Fashion', kind: 'EXPENSE', need_want: 'WANT', icon: 'default', color: 0xFFB58BC9, archived: false, sort_order: 10 },
  { id: 'cat_shopping_electronics', parent_id: 'cat_shopping', name: '📱 Mobiles & Electronics', kind: 'EXPENSE', need_want: 'WANT', icon: 'default', color: 0xFFB58BC9, archived: false, sort_order: 11 },
  { id: 'cat_bills', parent_id: null, name: '🧾 Bills & Utilities', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFD1A85A, archived: false, sort_order: 12 },
  { id: 'cat_bills_rent', parent_id: 'cat_bills', name: '🏠 Rent & Housing', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFD1A85A, archived: false, sort_order: 13 },
  { id: 'cat_bills_electricity', parent_id: 'cat_bills', name: '⚡ Electricity Bill (EB)', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFD1A85A, archived: false, sort_order: 14 },
  { id: 'cat_bills_internet', parent_id: 'cat_bills', name: '🌐 WiFi & Broadband', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFD1A85A, archived: false, sort_order: 15 },
  { id: 'cat_bills_subscriptions', parent_id: 'cat_bills', name: '📱 Mobile Recharge & OTT', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFD1A85A, archived: false, sort_order: 16 },
  { id: 'cat_health', parent_id: null, name: '❤️ Health & Medicine', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFE0736B, archived: false, sort_order: 17 },
  { id: 'cat_education', parent_id: null, name: '📚 School & College Fees', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFF6FB7C4, archived: false, sort_order: 18 },
  { id: 'cat_entertainment', parent_id: null, name: '🍿 Movies & Entertainment', kind: 'EXPENSE', need_want: 'WANT', icon: 'default', color: 0xFFC98B9E, archived: false, sort_order: 19 },
  { id: 'cat_travel', parent_id: null, name: '✈️ Travel & Vacation', kind: 'EXPENSE', need_want: 'WANT', icon: 'default', color: 0xFF8BC9A6, archived: false, sort_order: 20 },
  { id: 'cat_family', parent_id: null, name: '👨‍👩‍👧 Family & Home', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9B58B, archived: false, sort_order: 21 },
  { id: 'cat_investment', parent_id: null, name: '📈 Mutual Funds & Stocks (SIP)', kind: 'EXPENSE', need_want: 'SAVING', icon: 'default', color: 0xFF7FD1AE, archived: false, sort_order: 22 },
  { id: 'cat_loan_emi', parent_id: null, name: '🏦 Bank Loan EMI & Repayment', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFA3A3AD, archived: false, sort_order: 23 },
  { id: 'cat_misc', parent_id: null, name: '📦 Other Expenses', kind: 'EXPENSE', need_want: 'WANT', icon: 'default', color: 0xFF8E8E96, archived: false, sort_order: 24 },
  { id: 'cat_income_0', parent_id: null, name: '💰 Salary', kind: 'INCOME', need_want: null, icon: 'default', color: 0xFF7FD1AE, archived: false, sort_order: 25 },
  { id: 'cat_income_1', parent_id: null, name: '💼 Business & Freelance', kind: 'INCOME', need_want: null, icon: 'default', color: 0xFF7FD1AE, archived: false, sort_order: 26 },
  { id: 'cat_income_2', parent_id: null, name: '📈 Interest & Dividends', kind: 'INCOME', need_want: null, icon: 'default', color: 0xFF7FD1AE, archived: false, sort_order: 27 },
  { id: 'cat_income_3', parent_id: null, name: '🎁 Bonus, Gift & Cashback', kind: 'INCOME', need_want: null, icon: 'default', color: 0xFF7FD1AE, archived: false, sort_order: 28 },
];

export const CATEGORY_EMOJI_MAP: Record<string, string> = {
  cat_housing: '🏠 Rent & Housing',
  cat_bills_rent: '🏠 Rent & Housing',
  cat_bills_electricity: '⚡ Electricity Bill (EB)',
  cat_bills_internet: '🌐 WiFi & Broadband',
  cat_bills_subscriptions: '📱 Mobile Recharge & OTT',
  cat_bills: '🧾 Bills & Utilities',
  cat_food: '🍔 Food & Dining',
  cat_food_groceries: '🥦 Groceries & Veggies',
  cat_food_restaurant: '🍽️ Hotel & Restaurant',
  cat_food_snacks: '☕ Snacks & Tea',
  cat_food_coffee: '☕ Coffee & Cafe',
  cat_food_delivery: '🛵 Food Delivery',
  cat_transport: '🚗 Travel & Commute',
  cat_transport_fuel: '⛽ Petrol & Fuel',
  cat_transport_metro: '🚇 Metro & Train',
  cat_shopping: '🛍️ Shopping & Spends',
  cat_shopping_clothing: '👕 Clothes & Fashion',
  cat_shopping_electronics: '📱 Mobiles & Electronics',
  cat_health: '❤️ Health & Medicine',
  cat_education: '📚 School & College Fees',
  cat_entertainment: '🍿 Movies & Entertainment',
  cat_travel: '✈️ Travel & Vacation',
  cat_family: '👨‍👩‍👧 Family & Home',
  cat_investments: '📈 Mutual Funds & Stocks (SIP)',
  cat_investment: '📈 Mutual Funds & Stocks (SIP)',
  cat_loan_emi: '🏦 Bank Loan EMI & Repayment',
  cat_misc: '📦 Other Expenses',
  cat_salary: '💰 Salary',
  cat_income_0: '💰 Salary',
  cat_income_1: '💼 Business & Freelance',
  cat_income_2: '📈 Interest & Dividends',
  cat_income_3: '🎁 Bonus, Gift & Cashback',
};

export function getCategoryDisplayName(catId: string | null | undefined, categories: Category[] = []): string {
  if (!catId) return '📦 General';

  const found = categories.find(c => c.id === catId);
  if (found) {
    if (/^\p{Extended_Pictographic}/u.test(found.name.trim())) {
      return found.name;
    }
    if (CATEGORY_EMOJI_MAP[found.id]) {
      return CATEGORY_EMOJI_MAP[found.id];
    }
    return `🏷️ ${found.name}`;
  }

  if (CATEGORY_EMOJI_MAP[catId]) return CATEGORY_EMOJI_MAP[catId];
  return `🏷️ ${catId.replace(/^cat_/, '').replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())}`;
}

export function getCategoryEmoji(catId: string | null | undefined, categories: Category[] = []): string {
  const label = getCategoryDisplayName(catId, categories);
  const match = label.match(/^\p{Extended_Pictographic}/u);
  return match ? match[0] : '🏷️';
}

export function sanitizeCategories<T extends { id: string; name: string; kind?: string; archived?: boolean; sort_order?: number }>(cats: T[]): T[] {
  if (!cats || !Array.isArray(cats)) return [];
  const seenIds = new Set<string>();
  const seenNames = new Set<string>();
  const result: T[] = [];

  // Sort canonical IDs first so cat_income_0 takes precedence over legacy cat_salary
  const sorted = [...cats].sort((a, b) => {
    if (a.id === 'cat_income_0' && b.id === 'cat_salary') return -1;
    if (a.id === 'cat_salary' && b.id === 'cat_income_0') return 1;
    return (a.sort_order ?? 0) - (b.sort_order ?? 0);
  });

  for (const cat of sorted) {
    if (!cat || !cat.id) continue;
    // Omit duplicate salary category if cat_income_0 exists
    if (cat.id === 'cat_salary' && sorted.some(c => c.id === 'cat_income_0')) {
      continue;
    }
    if (seenIds.has(cat.id)) continue;
    seenIds.add(cat.id);

    // Filter duplicate display names within same kind
    const key = `${cat.kind || 'EXPENSE'}_${cat.name.trim().toLowerCase()}`;
    if (seenNames.has(key)) continue;
    seenNames.add(key);

    result.push(cat);
  }

  return result;
}

