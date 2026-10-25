/**
 * WealthOS Transaction Classifier & Jar Suggester
 * Conforms to WEALTHOS_FINAL_SPEC.md v3 §6.4.
 * Word boundary matching with deterministic precedence.
 */

import type { PaymentRail } from './narrationDecoder';

export type SpendEmotionTag =
  | 'NEED'
  | 'PLANNED_WANT'
  | 'REGRET_LEAK'
  | 'UNPLANNED_TRANSFER'
  | 'INCOME'
  | 'UNCATEGORISED';

export interface ClassificationResult {
  categoryId: string;
  jarBucket: string;
  emotionTag: SpendEmotionTag;
  isRecurringMandate: boolean;
  suggestedNote?: string;
}

function wordMatch(text: string, keywords: string[]): boolean {
  const lower = text.toLowerCase();
  return keywords.some(kw => {
    const re = new RegExp(`\\b${kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\b`, 'i');
    return re.test(lower);
  });
}

// 1. Investments -> FREEDOM_SIP
const INVESTMENT_KEYWORDS = [
  'zerodha', 'groww', 'kuvera', 'camsonline', 'kfintech', 'mfu', 'mutual fund',
  'uti mf', 'sbi mf', 'hdfc mf', 'icici pru', 'nippon', 'mirae', 'ppfas',
  'parag parikh', 'coin', 'etmoney', 'indmoney', 'smallcase', 'sip',
];

// 2. Education -> EDUCATION
const EDUCATION_KEYWORDS = [
  'udemy', 'coursera', 'edx', 'scaler', 'upgrad', 'simplilearn',
  'book', 'kindle', 'school', 'college', 'tuition', 'exam fee', 'unacademy',
];

// 3. Loans & EMI -> NEEDS
const LOAN_KEYWORDS = [
  'emi', 'loan', 'hdfc loan', 'sbi loan', 'bajaj finance', 'home credit',
  'cred', 'slice', 'uni card', 'stashfin', 'kreditbee', 'earlysalary', 'zestmoney',
];

// 4. Subscriptions & Autopay -> PLAY
const SUBSCRIPTION_KEYWORDS = [
  'netflix', 'spotify', 'youtube premium', 'prime video', 'hotstar', 'apple.com/bill',
  'google play', 'disney', 'chatgpt', 'openai', 'gym', 'cult.fit',
];

// 5. Essentials -> NEEDS
const UTILITY_KEYWORDS = [
  'bescom', 'tneb', 'tangedco', 'mseb', 'uppcl', 'electricity', 'water board',
  'airtel', 'jio', 'vi', 'act fibernet', 'broadband', 'cylinder', 'indane', 'hp gas', 'bharat gas',
];
const GROCERY_KEYWORDS = [
  'dmart', 'reliance fresh', 'bigbasket', 'bb daily', 'blinkit', 'zepto', 'instamart',
  'grofers', 'supermarket', 'provision', 'vegetables', 'milk', 'dairy',
];
const TRANSPORT_KEYWORDS = [
  'uber', 'ola', 'rapido', 'namma yatri', 'metro', 'irctc', 'petrol', 'fuel', 'hpcl', 'bpcl', 'iocl',
];
const HEALTH_KEYWORDS = [
  'apollo', 'medplus', '1mg', 'pharmeasy', 'netmeds', 'hospital', 'clinic', 'pharmacy', 'doctor', 'lab',
];
const HOUSING_KEYWORDS = [
  'rent', 'maintenance', 'house', 'pg', 'society',
];

// 6. Food Delivery & Dining -> PLAY (PLANNED_WANT)
const DINING_KEYWORDS = [
  'swiggy', 'zomato', 'restaurant', 'starbucks', 'mcdonalds', 'kfc', 'dominos', 'pizza', 'burger', 'cafe',
];

// 7. Shopping -> PLAY (PLANNED_WANT)
const SHOPPING_KEYWORDS = [
  'amazon', 'flipkart', 'myntra', 'meesho', 'ajio', 'nykaa', 'tata cliq', 'zara', 'h&m',
];

// Income keywords
const SALARY_KEYWORDS = ['salary', 'payroll', 'cms', 'wages', 'stipend'];
const REFUND_KEYWORDS = ['refund', 'cashback', 'reversal'];
const INTEREST_KEYWORDS = ['interest', 'int.pd', 'dividend'];

export function classifyTransaction(
  merchantOrNarration: string,
  amountMinor: number,
  rail: PaymentRail,
  isCredit: boolean,
  userOverride?: ClassificationResult
): ClassificationResult {
  if (userOverride) return userOverride;

  const text = merchantOrNarration.trim();

  // Credits
  if (isCredit) {
    if (wordMatch(text, SALARY_KEYWORDS)) {
      return { categoryId: 'cat_income_0', jarBucket: 'INCOME', emotionTag: 'INCOME', isRecurringMandate: true };
    }
    if (wordMatch(text, REFUND_KEYWORDS)) {
      return { categoryId: 'cat_income_2', jarBucket: 'INCOME', emotionTag: 'INCOME', isRecurringMandate: false };
    }
    if (wordMatch(text, INTEREST_KEYWORDS)) {
      return { categoryId: 'cat_income_1', jarBucket: 'INCOME', emotionTag: 'INCOME', isRecurringMandate: false };
    }
    return { categoryId: 'cat_income_0', jarBucket: 'INCOME', emotionTag: 'INCOME', isRecurringMandate: false };
  }

  // Investments
  if (wordMatch(text, INVESTMENT_KEYWORDS)) {
    return {
      categoryId: 'cat_savings',
      jarBucket: 'FREEDOM_SIP',
      emotionTag: 'NEED',
      isRecurringMandate: rail === 'UPI_AUTOPAY' || rail === 'ACH_NACH',
    };
  }

  // Education
  if (wordMatch(text, EDUCATION_KEYWORDS)) {
    return { categoryId: 'cat_education', jarBucket: 'EDUCATION', emotionTag: 'NEED', isRecurringMandate: false };
  }

  // Loan / EMI
  if (wordMatch(text, LOAN_KEYWORDS)) {
    return { categoryId: 'cat_debt', jarBucket: 'NEEDS', emotionTag: 'NEED', isRecurringMandate: true };
  }

  // Subscriptions & Autopay
  if (rail === 'UPI_AUTOPAY' || wordMatch(text, SUBSCRIPTION_KEYWORDS)) {
    return { categoryId: 'cat_entertainment', jarBucket: 'PLAY', emotionTag: 'PLANNED_WANT', isRecurringMandate: true };
  }

  // Essentials
  if (wordMatch(text, HOUSING_KEYWORDS)) {
    return { categoryId: 'cat_housing', jarBucket: 'NEEDS', emotionTag: 'NEED', isRecurringMandate: false };
  }
  if (wordMatch(text, GROCERY_KEYWORDS)) {
    return { categoryId: 'cat_groceries', jarBucket: 'NEEDS', emotionTag: 'NEED', isRecurringMandate: false };
  }
  if (wordMatch(text, UTILITY_KEYWORDS)) {
    return { categoryId: 'cat_utilities', jarBucket: 'NEEDS', emotionTag: 'NEED', isRecurringMandate: false };
  }
  if (wordMatch(text, TRANSPORT_KEYWORDS)) {
    return { categoryId: 'cat_transport', jarBucket: 'NEEDS', emotionTag: 'NEED', isRecurringMandate: false };
  }
  if (wordMatch(text, HEALTH_KEYWORDS)) {
    return { categoryId: 'cat_health', jarBucket: 'NEEDS', emotionTag: 'NEED', isRecurringMandate: false };
  }

  // Food Delivery
  if (wordMatch(text, DINING_KEYWORDS)) {
    return { categoryId: 'cat_dining', jarBucket: 'PLAY', emotionTag: 'PLANNED_WANT', isRecurringMandate: false };
  }

  // Shopping
  if (wordMatch(text, SHOPPING_KEYWORDS)) {
    return { categoryId: 'cat_shopping', jarBucket: 'PLAY', emotionTag: 'PLANNED_WANT', isRecurringMandate: false };
  }

  // P2A Heuristics
  if (rail === 'UPI_P2A' || rail === 'UPI_P2P') {
    if (amountMinor >= 500000) {
      return {
        categoryId: 'cat_general',
        jarBucket: 'NEEDS',
        emotionTag: 'NEED',
        isRecurringMandate: false,
        suggestedNote: 'Possible rent or family support',
      };
    } else {
      return {
        categoryId: 'cat_general',
        jarBucket: 'PLAY',
        emotionTag: 'UNCATEGORISED',
        isRecurringMandate: false,
        suggestedNote: 'P2A transfer for review',
      };
    }
  }

  // Default: never auto-tag REGRET_LEAK
  return {
    categoryId: 'cat_general',
    jarBucket: 'PLAY',
    emotionTag: 'PLANNED_WANT',
    isRecurringMandate: false,
  };
}
