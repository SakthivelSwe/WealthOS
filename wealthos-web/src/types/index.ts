// ============================================================
// WealthOS Type Definitions — Mirrors Android Entities
// ============================================================

export type AccountType = 'BANK' | 'CASH' | 'UPI_WALLET' | 'SAVINGS' | 'CREDIT_CARD' | 'INVESTMENT' | 'LOAN' | 'OTHER_ASSET' | 'OTHER_LIABILITY';
export type TxnType = 'EXPENSE' | 'INCOME' | 'TRANSFER';
export type CategoryKind = 'EXPENSE' | 'INCOME';
export type NeedWant = 'NEED' | 'WANT' | 'SAVING';
export type Frequency = 'DAILY' | 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY';
export type ThemeMode = 'DARK' | 'LIGHT' | 'MIDNIGHT';

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
  const info = getCurrencyInfo(currencyCode);
  const num = parseFloat(text.replace(/[^0-9.-]/g, ''));
  if (isNaN(num)) return 0;
  return Math.round(num * Math.pow(10, info.minorUnit));
}

// Account type helpers
export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  BANK: '🏦 Bank',
  CASH: '💵 Cash',
  UPI_WALLET: '📱 UPI Wallet',
  SAVINGS: '🏧 Savings',
  CREDIT_CARD: '💳 Credit Card',
  INVESTMENT: '📈 Investment',
  LOAN: '🏠 Loan',
  OTHER_ASSET: '💎 Other Asset',
  OTHER_LIABILITY: '📋 Other Liability',
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

// Default categories (mirrors Android)
export const DEFAULT_CATEGORIES: Omit<Category, 'user_id'>[] = [
  { id: 'cat_food', parent_id: null, name: '🍔 Food', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9A66B, archived: false, sort_order: 0 },
  { id: 'cat_food_groceries', parent_id: 'cat_food', name: 'Groceries', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9A66B, archived: false, sort_order: 1 },
  { id: 'cat_food_restaurant', parent_id: 'cat_food', name: 'Restaurant', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9A66B, archived: false, sort_order: 2 },
  { id: 'cat_food_snacks', parent_id: 'cat_food', name: 'Snacks', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9A66B, archived: false, sort_order: 3 },
  { id: 'cat_food_coffee', parent_id: 'cat_food', name: 'Coffee', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9A66B, archived: false, sort_order: 4 },
  { id: 'cat_food_delivery', parent_id: 'cat_food', name: 'Delivery', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9A66B, archived: false, sort_order: 5 },
  { id: 'cat_transport', parent_id: null, name: '🚗 Transport', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFF7FA6D9, archived: false, sort_order: 6 },
  { id: 'cat_transport_fuel', parent_id: 'cat_transport', name: 'Fuel', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFF7FA6D9, archived: false, sort_order: 7 },
  { id: 'cat_transport_metro', parent_id: 'cat_transport', name: 'Metro', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFF7FA6D9, archived: false, sort_order: 8 },
  { id: 'cat_shopping', parent_id: null, name: '🛍️ Shopping', kind: 'EXPENSE', need_want: 'WANT', icon: 'default', color: 0xFFB58BC9, archived: false, sort_order: 9 },
  { id: 'cat_shopping_clothing', parent_id: 'cat_shopping', name: 'Clothing', kind: 'EXPENSE', need_want: 'WANT', icon: 'default', color: 0xFFB58BC9, archived: false, sort_order: 10 },
  { id: 'cat_shopping_electronics', parent_id: 'cat_shopping', name: 'Electronics', kind: 'EXPENSE', need_want: 'WANT', icon: 'default', color: 0xFFB58BC9, archived: false, sort_order: 11 },
  { id: 'cat_bills', parent_id: null, name: '🧾 Bills', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFD1A85A, archived: false, sort_order: 12 },
  { id: 'cat_bills_rent', parent_id: 'cat_bills', name: 'Rent', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFD1A85A, archived: false, sort_order: 13 },
  { id: 'cat_bills_electricity', parent_id: 'cat_bills', name: 'Electricity', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFD1A85A, archived: false, sort_order: 14 },
  { id: 'cat_bills_internet', parent_id: 'cat_bills', name: 'Internet', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFD1A85A, archived: false, sort_order: 15 },
  { id: 'cat_bills_subscriptions', parent_id: 'cat_bills', name: 'Subscriptions', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFD1A85A, archived: false, sort_order: 16 },
  { id: 'cat_health', parent_id: null, name: '❤️ Health', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFE0736B, archived: false, sort_order: 17 },
  { id: 'cat_education', parent_id: null, name: '📚 Education', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFF6FB7C4, archived: false, sort_order: 18 },
  { id: 'cat_entertainment', parent_id: null, name: '🍿 Entertainment', kind: 'EXPENSE', need_want: 'WANT', icon: 'default', color: 0xFFC98B9E, archived: false, sort_order: 19 },
  { id: 'cat_travel', parent_id: null, name: '✈️ Travel', kind: 'EXPENSE', need_want: 'WANT', icon: 'default', color: 0xFF8BC9A6, archived: false, sort_order: 20 },
  { id: 'cat_family', parent_id: null, name: '👨‍👩‍👧 Family', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFC9B58B, archived: false, sort_order: 21 },
  { id: 'cat_investment', parent_id: null, name: '📈 Investment', kind: 'EXPENSE', need_want: 'SAVING', icon: 'default', color: 0xFF7FD1AE, archived: false, sort_order: 22 },
  { id: 'cat_loan_emi', parent_id: null, name: '🏦 Loan / EMI', kind: 'EXPENSE', need_want: 'NEED', icon: 'default', color: 0xFFA3A3AD, archived: false, sort_order: 23 },
  { id: 'cat_misc', parent_id: null, name: '📦 Misc', kind: 'EXPENSE', need_want: 'WANT', icon: 'default', color: 0xFF8E8E96, archived: false, sort_order: 24 },
  { id: 'cat_income_0', parent_id: null, name: '💰 Salary', kind: 'INCOME', need_want: null, icon: 'default', color: 0xFF7FD1AE, archived: false, sort_order: 25 },
  { id: 'cat_income_1', parent_id: null, name: '💼 Business', kind: 'INCOME', need_want: null, icon: 'default', color: 0xFF7FD1AE, archived: false, sort_order: 26 },
  { id: 'cat_income_2', parent_id: null, name: '📈 Interest', kind: 'INCOME', need_want: null, icon: 'default', color: 0xFF7FD1AE, archived: false, sort_order: 27 },
  { id: 'cat_income_3', parent_id: null, name: '🎁 Other Income', kind: 'INCOME', need_want: null, icon: 'default', color: 0xFF7FD1AE, archived: false, sort_order: 28 },
];

export function getCategoryDisplayName(catId: string | null | undefined, categories: Category[] = []): string {
  if (!catId) return 'General';
  const found = categories.find(c => c.id === catId);
  if (found && !found.name.startsWith('cat_')) return found.name;

  const KNOWN_MAP: Record<string, string> = {
    cat_housing: '🏠 Housing & Rent',
    cat_bills_rent: '🏠 Housing & Rent',
    cat_investments: '📈 Investments & SIP',
    cat_investment: '📈 Investments & SIP',
    cat_utilities: '⚡ Utilities & Bills',
    cat_bills: '🧾 Bills',
    cat_food: '🍔 Food & Dining',
    cat_food_groceries: '🥦 Groceries',
    cat_food_restaurant: '🍽️ Restaurant',
    cat_transport: '🚗 Fuel & Transport',
    cat_shopping: '🛍️ Shopping',
    cat_entertainment: '🍿 Entertainment',
    cat_salary: '💰 Salary',
    cat_income_0: '💰 Salary',
    cat_health: '❤️ Health & Wellness',
    cat_education: '📚 Education',
    cat_travel: '✈️ Travel',
  };

  if (KNOWN_MAP[catId]) return KNOWN_MAP[catId];
  return catId.replace(/^cat_/, '').replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
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

