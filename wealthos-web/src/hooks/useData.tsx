import { createContext, useContext, useEffect, useState, useCallback, useMemo, type ReactNode } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from './useAuth';
import type {
  Account,
  Transaction,
  JournalEntry,
  Goal,
  Debt,
  ChitFund,
  RecurringTransaction,
  Category,
  UserSettings,
  AppNotification,
  Budget,
  SalaryCycle,
  UserFinancialProfile,
  SinkingFund,
  MerchantRule,
  Mandate,
} from '../types';
import { DEFAULT_CATEGORIES as SEED_CATEGORIES, sanitizeCategories } from '../types';
import { getSampleSeedData } from '../lib/mockData';
import { isWithinPaydayWindow, type CycleDefinition } from '../lib/cycle';

/** Structured result returned by importAllData — F-03 fix: never return success:true when cloud writes failed. */
export interface ImportResult {
  success: boolean;
  /** True when local import succeeded but some or all cloud writes failed. */
  partialSuccess: boolean;
  localCount: number;
  cloudSyncedCount: number;
  cloudFailedCount: number;
  errors: string[];
  entityCounts: {
    accounts: number;
    transactions: number;
    journalEntries: number;
    categories: number;
    goals: number;
    debts: number;
    chits: number;
    recurring: number;
    budgets: number;
    settings: boolean;
    salaryCycles?: number;
    financialProfile?: boolean;
    sinkingFunds?: number;
    mandates?: number;
    merchantRules?: number;
  };
}

export interface DailyUpiMeter {
  guiltFreePaise: number;
  needsAllowancePaise: number;
  totalPaise: number;
}

interface DataContextType {
  accounts: Account[];
  transactions: Transaction[];
  journalEntries: JournalEntry[];
  categories: Category[];
  goals: Goal[];
  debts: Debt[];
  chitFunds: ChitFund[];
  recurringTxns: RecurringTransaction[];
  budgets: Budget[];
  settings: UserSettings | null;
  notifications: AppNotification[];
  salaryCycles: SalaryCycle[];
  activeCycle: SalaryCycle | null;
  financialProfile: UserFinancialProfile | null;
  sinkingFunds: SinkingFund[];
  mandates: Mandate[];
  merchantRules: MerchantRule[];
  dailyUpiMeter: DailyUpiMeter;
  isSurvivalMode: boolean;
  survivalDailyCapPaise: number | null;
  isSalaryWindowActive: boolean;
  loading: boolean;
  refresh: () => Promise<void>;
  // CRUD operations
  addAccount: (account: Omit<Account, 'user_id'>) => Promise<void>;
  updateAccount: (id: string, updates: Partial<Account>) => Promise<void>;
  deleteAccount: (id: string) => Promise<void>;
  addTransaction: (txn: Omit<Transaction, 'user_id'>, entries: Omit<JournalEntry, 'user_id'>[]) => Promise<void>;
  updateTransaction: (id: string, updates: Partial<Transaction>, entries?: Omit<JournalEntry, 'user_id'>[]) => Promise<void>;
  deleteTransaction: (id: string) => Promise<void>;
  addGoal: (goal: Omit<Goal, 'user_id'>) => Promise<void>;
  updateGoal: (id: string, updates: Partial<Goal>) => Promise<void>;
  addDebt: (debt: Omit<Debt, 'user_id'>) => Promise<void>;
  updateDebt: (id: string, updates: Partial<Debt>) => Promise<void>;
  addChitFund: (chit: Omit<ChitFund, 'user_id'>) => Promise<void>;
  addRecurring: (rec: Omit<RecurringTransaction, 'user_id'>) => Promise<void>;
  addBudget: (budget: Omit<Budget, 'user_id'>) => Promise<void>;
  updateCategoryBudget: (categoryId: string, limitMinor: number | null) => Promise<void>;
  updateSettings: (updates: Partial<UserSettings>) => Promise<void>;
  markNotificationRead: (id: string) => Promise<void>;
  markAllNotificationsRead: () => Promise<void>;
  importAllData: (data: any) => Promise<ImportResult>;
  // Phase 4 additions (§4.1 - §4.7, §5.4 - §5.5)
  addSalaryCycle: (cycle: Omit<SalaryCycle, 'user_id'>) => Promise<void>;
  updateFinancialProfile: (updates: Partial<UserFinancialProfile>) => Promise<void>;
  addSinkingFund: (fund: Omit<SinkingFund, 'user_id'>) => Promise<void>;
  updateSinkingFund: (id: string, updates: Partial<SinkingFund>) => Promise<void>;
  deleteSinkingFund: (id: string) => Promise<void>;
  addMandate: (mandate: Omit<Mandate, 'user_id'>) => Promise<void>;
  updateMandate: (id: string, updates: Partial<Mandate>) => Promise<void>;
  deleteMandate: (id: string) => Promise<void>;
  addMerchantRule: (rule: Omit<MerchantRule, 'user_id'>) => Promise<void>;
  deleteMerchantRule: (id: string) => Promise<void>;
}

const DataContext = createContext<DataContextType | null>(null);

export function DataProvider({ children }: { children: ReactNode }) {
  const { user, isDemo } = useAuth();
  const userId = user?.id || (isDemo ? 'demo_user' : 'guest_user');

  const getStorageKey = useCallback((key: string) => `wealthos_v2_${userId}_${key}`, [userId]);

  const getLocal = useCallback(<T,>(key: string, fallback: T): T => {
    try {
      const raw = localStorage.getItem(getStorageKey(key));
      return raw ? JSON.parse(raw) : fallback;
    } catch {
      return fallback;
    }
  }, [getStorageKey]);

  const setLocal = useCallback(<T,>(key: string, value: T) => {
    try {
      localStorage.setItem(getStorageKey(key), JSON.stringify(value));
    } catch (e) {
      console.warn('LocalStorage save failed:', e);
    }
  }, [getStorageKey]);

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [journalEntries, setJournalEntries] = useState<JournalEntry[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [goals, setGoals] = useState<Goal[]>([]);
  const [debts, setDebts] = useState<Debt[]>([]);
  const [chitFunds, setChitFunds] = useState<ChitFund[]>([]);
  const [recurringTxns, setRecurringTxns] = useState<RecurringTransaction[]>([]);
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [settings, setSettings] = useState<UserSettings | null>(null);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [salaryCycles, setSalaryCycles] = useState<SalaryCycle[]>([]);
  const [financialProfile, setFinancialProfile] = useState<UserFinancialProfile | null>(null);
  const [sinkingFunds, setSinkingFunds] = useState<SinkingFund[]>([]);
  const [mandates, setMandates] = useState<Mandate[]>([]);
  const [merchantRules, setMerchantRules] = useState<MerchantRule[]>([]);
  const [loading, setLoading] = useState(true);

  const defaultFinancialProfile: UserFinancialProfile = useMemo(() => ({
    user_id: userId,
    income_schedule_type: 'VARIABLE_WINDOW',
    payday_window_start_day: 2,
    payday_window_end_day: 5,
    expected_salary_min_paise: null,
    expected_salary_max_paise: null,
    fixed_baseline_paise: null,
    baseline_method: 'ESTIMATE_55_PERCENT',
    lifestyle_creep_lock: true,
    needs_buffer_paise: 150000,
    allocation_framework: 'SIX_JAR',
    custom_bps: null,
    play_to_long_term_shift: false,
    upi_needs_allowance_paise: 0,
    emergency_target_months: 7,
    survival_threshold_paise: null,
    small_scan_threshold_paise: 20000,
    ego_check_threshold_paise: 200000,
    cooldown_hours: 48,
    currency: 'INR',
    locale: 'en-IN',
    timezone: 'Asia/Kolkata',
    language: 'en',
    updated_at: Date.now(),
  }), [userId]);

  const fetchAll = useCallback(async () => {
    if (!userId) return;
    setLoading(true);

    if (isDemo) {
      // Demo session gets simulated seed data
      const cachedAccounts = getLocal<Account[]>('accounts', []);
      if (cachedAccounts.length === 0) {
        const sample = getSampleSeedData(userId);
        const defaultCategories = SEED_CATEGORIES.map(c => ({ ...c, user_id: userId }));
        const defaultSettings: UserSettings = {
          user_id: userId,
          currency: 'INR',
          theme_mode: 'DARK',
          mask_amounts: false,
          user_name: 'Demo User',
          phone_number: '+91 98765 43210',
          basis_income_minor: 9500000,
          needs_pct: 50,
          wants_pct: 30,
          savings_pct: 20,
          daily_reminder_enabled: true,
          daily_reminder_hour: 21,
          updated_at: Date.now(),
        };

        setAccounts(sample.accounts);
        setTransactions(sample.transactions);
        setJournalEntries(sample.journalEntries);
        setGoals(sample.goals);
        setDebts(sample.debts);
        setChitFunds(sample.chitFunds);
        setRecurringTxns(sample.recurringTxns);
        setNotifications(sample.notifications);
        const sanitizedSeed = sanitizeCategories(defaultCategories);
        setCategories(sanitizedSeed);
        setSettings(defaultSettings);
        setFinancialProfile(defaultFinancialProfile);

        setLocal('accounts', sample.accounts);
        setLocal('transactions', sample.transactions);
        setLocal('journalEntries', sample.journalEntries);
        setLocal('goals', sample.goals);
        setLocal('debts', sample.debts);
        setLocal('chitFunds', sample.chitFunds);
        setLocal('recurringTxns', sample.recurringTxns);
        setLocal('notifications', sample.notifications);
        setLocal('categories', sanitizedSeed);
        setLocal('settings', defaultSettings);
        setLocal('financialProfile', defaultFinancialProfile);
      } else {
        setAccounts(cachedAccounts);
        const cachedTxns = getLocal<Transaction[]>('transactions', []);
        const normalizedTxns = cachedTxns.map(t => t.category_id === 'cat_salary' ? { ...t, category_id: 'cat_income_0' } : t);
        setTransactions(normalizedTxns);
        const cachedEntries = getLocal<JournalEntry[]>('journalEntries', []);
        const normalizedEntries = cachedEntries.map(e => e.category_id === 'cat_salary' ? { ...e, category_id: 'cat_income_0' } : e);
        setJournalEntries(normalizedEntries);
        setGoals(getLocal('goals', []));
        setDebts(getLocal('debts', []));
        setChitFunds(getLocal('chitFunds', []));
        setRecurringTxns(getLocal('recurringTxns', []));
        setBudgets(getLocal('budgets', []));
        const rawCachedCats = sanitizeCategories(getLocal<Category[]>('categories', []));
        const savedBudgets = getLocal<Record<string, number>>('category_budgets', {});
        const cachedCats = rawCachedCats.map((c: Category) => ({
          ...c,
          budget_limit_minor: savedBudgets[c.id] ?? c.budget_limit_minor ?? null,
        }));
        setCategories(cachedCats);
        setLocal('categories', cachedCats);
        setSettings(getLocal('settings', null));
        setNotifications(getLocal('notifications', []));
        setSalaryCycles(getLocal('salaryCycles', []));
        setFinancialProfile(getLocal('financialProfile', defaultFinancialProfile));
        setSinkingFunds(getLocal('sinkingFunds', []));
        setMandates(getLocal('mandates', []));
        setMerchantRules(getLocal('merchantRules', []));
      }
      setLoading(false);
      return;
    }

    // REAL USER PATH: load from cloud, or start completely clean starting today
    let supabaseLoaded = false;
    try {
      const [
        accts, txns, entries, cats, gls, dts, chts, recs, bdgs, sttgs, notifs,
        cycles, profile, funds, mnds, rules
      ] = await Promise.allSettled([
        supabase.from('user_accounts').select('*').eq('user_id', userId).eq('archived', false).order('name'),
        supabase.from('user_transactions').select('*').eq('user_id', userId).is('deleted_at', null).order('local_date', { ascending: false }).limit(500),
        supabase.from('user_journal_entries').select('*').eq('user_id', userId),
        supabase.from('user_categories').select('*').eq('user_id', userId).eq('archived', false).order('sort_order'),
        supabase.from('user_goals').select('*').eq('user_id', userId).eq('archived', false).order('created_at', { ascending: false }),
        supabase.from('user_debts').select('*').eq('user_id', userId).eq('archived', false).order('created_at', { ascending: false }),
        supabase.from('user_chit_funds').select('*').eq('user_id', userId).eq('active', true).order('created_at', { ascending: false }),
        supabase.from('user_recurring_transactions').select('*').eq('user_id', userId).eq('active', true).order('next_run_date'),
        supabase.from('user_budgets').select('*').eq('user_id', userId).eq('active', true),
        supabase.from('user_settings').select('*').eq('user_id', userId).single(),
        supabase.from('user_notifications').select('*').eq('user_id', userId).order('created_at', { ascending: false }).limit(50),
        supabase.from('salary_cycles').select('*').eq('user_id', userId).order('cycle_start_date', { ascending: false }),
        supabase.from('user_financial_profiles').select('*').eq('user_id', userId).single(),
        supabase.from('sinking_funds').select('*').eq('user_id', userId).eq('archived', false).order('created_at', { ascending: false }),
        supabase.from('mandates').select('*').eq('user_id', userId).eq('active', true).order('next_deduction_date'),
        supabase.from('merchant_rules').select('*').eq('user_id', userId).order('created_at', { ascending: false }),
      ]);

      if (accts.status === 'fulfilled' && accts.value.data) {
        setAccounts(accts.value.data);
        setLocal('accounts', accts.value.data);
        supabaseLoaded = true;
      }
      if (txns.status === 'fulfilled' && txns.value.data) {
        const normalizedTxns = txns.value.data.map(t => t.category_id === 'cat_salary' ? { ...t, category_id: 'cat_income_0' } : t);
        setTransactions(normalizedTxns);
        setLocal('transactions', normalizedTxns);
        supabaseLoaded = true;
      }
      if (entries.status === 'fulfilled' && entries.value.data) {
        const normalizedEntries = entries.value.data.map(e => e.category_id === 'cat_salary' ? { ...e, category_id: 'cat_income_0' } : e);
        setJournalEntries(normalizedEntries);
        setLocal('journalEntries', normalizedEntries);
      }
      if (gls.status === 'fulfilled' && gls.value.data) {
        setGoals(gls.value.data);
        setLocal('goals', gls.value.data);
      }
      if (dts.status === 'fulfilled' && dts.value.data) {
        setDebts(dts.value.data);
        setLocal('debts', dts.value.data);
      }
      if (chts.status === 'fulfilled' && chts.value.data) {
        setChitFunds(chts.value.data);
        setLocal('chitFunds', chts.value.data);
      }
      if (recs.status === 'fulfilled' && recs.value.data) {
        setRecurringTxns(recs.value.data);
        setLocal('recurringTxns', recs.value.data);
      }
      if (bdgs.status === 'fulfilled' && bdgs.value.data) {
        setBudgets(bdgs.value.data);
        setLocal('budgets', bdgs.value.data);
      }
      if (sttgs.status === 'fulfilled' && sttgs.value.data) {
        setSettings(sttgs.value.data);
        setLocal('settings', sttgs.value.data);
      }
      if (notifs.status === 'fulfilled' && notifs.value.data) {
        setNotifications(notifs.value.data);
        setLocal('notifications', notifs.value.data);
      }
      if (cats.status === 'fulfilled' && cats.value.data && cats.value.data.length > 0) {
        const sanitized = sanitizeCategories(cats.value.data);
        const savedBudgets = getLocal<Record<string, number>>('category_budgets', {});
        const mergedCats = sanitized.map(c => ({
          ...c,
          budget_limit_minor: savedBudgets[c.id] ?? (c as any).budget_limit_minor ?? null,
        }));
        setCategories(mergedCats);
        setLocal('categories', mergedCats);
        if (cats.value.data.some(c => c.id === 'cat_salary') && cats.value.data.some(c => c.id === 'cat_income_0')) {
          supabase.from('user_categories').delete().eq('id', 'cat_salary').eq('user_id', userId).then(() => {}, () => {});
        }
      }
      if (cycles.status === 'fulfilled' && cycles.value.data) {
        setSalaryCycles(cycles.value.data);
        setLocal('salaryCycles', cycles.value.data);
      }
      if (profile.status === 'fulfilled' && profile.value.data) {
        setFinancialProfile(profile.value.data);
        setLocal('financialProfile', profile.value.data);
      }
      if (funds.status === 'fulfilled' && funds.value.data) {
        setSinkingFunds(funds.value.data);
        setLocal('sinkingFunds', funds.value.data);
      }
      if (mnds.status === 'fulfilled' && mnds.value.data) {
        setMandates(mnds.value.data);
        setLocal('mandates', mnds.value.data);
      }
      if (rules.status === 'fulfilled' && rules.value.data) {
        setMerchantRules(rules.value.data);
        setLocal('merchantRules', rules.value.data);
      }
    } catch (err) {
      console.warn('Supabase fetch notice, falling back to local storage:', err);
    }

    // Default categories & settings fallback
    const defaultCategories = SEED_CATEGORIES.map(c => ({ ...c, user_id: userId }));
    const fallbackName = user?.user_metadata?.name || (user?.email ? user.email.split('@')[0] : 'My Vault');
    const defaultSettings: UserSettings = {
      user_id: userId,
      currency: 'INR',
      theme_mode: 'DARK',
      mask_amounts: false,
      user_name: fallbackName,
      phone_number: user?.phone || '',
      basis_income_minor: 0,
      needs_pct: 50,
      wants_pct: 30,
      savings_pct: 20,
      daily_reminder_enabled: true,
      daily_reminder_hour: 21,
      updated_at: Date.now(),
    };
    const welcomeNotification: AppNotification = {
      id: `notif_welcome_${Date.now()}`,
      user_id: userId,
      title: 'Welcome to WealthOS',
      message: 'Your sovereign financial vault is ready starting today. Add your first bank account or vault to get started.',
      category: 'SYSTEM',
      action_type: null,
      payload: null,
      read: false,
      created_at: Date.now(),
    };

    if (!supabaseLoaded) {
      // Offline fallback: load from user-scoped local storage
      const cachedAccounts = getLocal<Account[]>('accounts', []);
      setAccounts(cachedAccounts);
      const rawTxns = getLocal<Transaction[]>('transactions', []);
      const normalizedTxns = rawTxns.map(t => t.category_id === 'cat_salary' ? { ...t, category_id: 'cat_income_0' } : t);
      setTransactions(normalizedTxns);
      setLocal('transactions', normalizedTxns);

      const rawEntries = getLocal<JournalEntry[]>('journalEntries', []);
      const normalizedEntries = rawEntries.map(e => e.category_id === 'cat_salary' ? { ...e, category_id: 'cat_income_0' } : e);
      setJournalEntries(normalizedEntries);
      setLocal('journalEntries', normalizedEntries);

      setGoals(getLocal('goals', []));
      setDebts(getLocal('debts', []));
      setChitFunds(getLocal('chitFunds', []));
      setRecurringTxns(getLocal('recurringTxns', []));
      setBudgets(getLocal('budgets', []));
      const cachedCats = sanitizeCategories(getLocal('categories', defaultCategories));
      setCategories(cachedCats);
      setLocal('categories', cachedCats);
      setSettings(getLocal('settings', defaultSettings));
      setNotifications(getLocal('notifications', [welcomeNotification]));
      setSalaryCycles(getLocal('salaryCycles', []));
      setFinancialProfile(getLocal('financialProfile', defaultFinancialProfile));
      setSinkingFunds(getLocal('sinkingFunds', []));
      setMandates(getLocal('mandates', []));
      setMerchantRules(getLocal('merchantRules', []));
    } else {
      // Supabase is active: Ensure default categories, settings, profile and welcome notification if new
      setCategories(prev => {
        const sanitized = sanitizeCategories(prev);
        if (sanitized.length === 0) {
          const seeded = sanitizeCategories(defaultCategories);
          setLocal('categories', seeded);
          supabase.from('user_categories').upsert(seeded).then(() => {}, () => {});
          return seeded;
        }
        setLocal('categories', sanitized);
        return sanitized;
      });
      setSettings(prev => {
        if (!prev) {
          setLocal('settings', defaultSettings);
          supabase.from('user_settings').upsert(defaultSettings).then(() => {}, () => {});
          return defaultSettings;
        }
        return prev;
      });
      setFinancialProfile(prev => {
        if (!prev) {
          setLocal('financialProfile', defaultFinancialProfile);
          supabase.from('user_financial_profiles').upsert(defaultFinancialProfile).then(() => {}, () => {});
          return defaultFinancialProfile;
        }
        return prev;
      });
      setNotifications(prev => {
        if (prev.length === 0) {
          setLocal('notifications', [welcomeNotification]);
          return [welcomeNotification];
        }
        return prev;
      });
    }

    setLoading(false);
  }, [userId, isDemo, user, defaultFinancialProfile, getLocal, setLocal]);

  // Subscribe to real-time changes
  useEffect(() => {
    if (!userId) return;
    fetchAll();

    try {
      const channel = supabase.channel(`user-sync-${userId}`)
        .on('postgres_changes', { event: '*', schema: 'public', table: 'user_transactions', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'user_accounts', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'user_goals', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'user_debts', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'user_settings', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'salary_cycles', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'user_financial_profiles', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'sinking_funds', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'mandates', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'merchant_rules', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .subscribe((status) => {
          console.log('[Supabase Realtime] Channel status:', status);
        });

      return () => { supabase.removeChannel(channel); };
    } catch (e) {
      console.warn('Realtime subscription not active:', e);
    }
  }, [userId, fetchAll]);

  // CRUD operations
  const addAccount = useCallback(async (account: Omit<Account, 'user_id'>) => {
    const newAccount: Account = { ...account, user_id: userId };
    setAccounts(prev => {
      const updated = [...prev, newAccount];
      setLocal('accounts', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_accounts').insert(newAccount);
      if (error) {
        console.warn('Supabase insert account failed, stored in local cache:', error.message);
      }
    } catch (e) {
      console.warn('Saved to local storage, cloud sync pending:', e);
    }
  }, [userId, setLocal]);

  const updateAccount = useCallback(async (id: string, updates: Partial<Account>) => {
    setAccounts(prev => {
      const updated = prev.map(a => a.id === id ? { ...a, ...updates, updated_at: Date.now() } : a);
      setLocal('accounts', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_accounts').update({ ...updates, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
      if (error) {
        console.warn('Supabase update account failed:', error.message);
      }
    } catch (e) {
      console.warn('Updated in local storage, cloud sync pending:', e);
    }
  }, [userId, setLocal]);

  const deleteAccount = useCallback(async (id: string) => {
    setAccounts(prev => {
      const updated = prev.filter(a => a.id !== id);
      setLocal('accounts', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_accounts').update({ archived: true, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
      if (error) {
        console.warn('Supabase delete account failed:', error.message);
      }
    } catch (e) {
      console.warn('Deleted locally, cloud sync pending:', e);
    }
  }, [userId, setLocal]);

  const addTransaction = useCallback(async (txn: Omit<Transaction, 'user_id'>, entries: Omit<JournalEntry, 'user_id'>[]) => {
    const newTxn: Transaction = { ...txn, user_id: userId };
    const newEntries: JournalEntry[] = entries.map(e => ({ ...e, user_id: userId }));

    setTransactions(prev => {
      const updated = [newTxn, ...prev];
      setLocal('transactions', updated);
      return updated;
    });
    if (newEntries.length > 0) {
      setJournalEntries(prev => {
        const updated = [...prev, ...newEntries];
        setLocal('journalEntries', updated);
        return updated;
      });
    }

    try {
      const { error: txnErr } = await supabase.from('user_transactions').insert(newTxn);
      if (txnErr) {
        console.warn('Supabase insert transaction error:', txnErr.message);
      }
      if (newEntries.length > 0) {
        const { error: jeErr } = await supabase.from('user_journal_entries').insert(newEntries);
        if (jeErr) {
          console.warn('Supabase insert journal entries error:', jeErr.message);
        }
      }
    } catch (e) {
      console.warn('Transaction stored locally:', e);
    }
  }, [userId, setLocal]);

  const updateTransaction = useCallback(async (id: string, updates: Partial<Transaction>, entries?: Omit<JournalEntry, 'user_id'>[]) => {
    setTransactions(prev => {
      const updated = prev.map(t => t.id === id ? { ...t, ...updates, updated_at: Date.now() } : t);
      setLocal('transactions', updated);
      return updated;
    });

    if (entries !== undefined) {
      const newEntries: JournalEntry[] = entries.map(e => ({ ...e, user_id: userId, txn_id: id }));
      setJournalEntries(prev => {
        const withoutOld = prev.filter(je => je.txn_id !== id);
        const updated = [...withoutOld, ...newEntries];
        setLocal('journalEntries', updated);
        return updated;
      });
    }

    try {
      const { error: txnErr } = await supabase.from('user_transactions').update({ ...updates, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
      if (txnErr) {
        console.warn('Supabase update transaction error:', txnErr.message);
      }

      if (entries !== undefined) {
        const newEntries: JournalEntry[] = entries.map(e => ({ ...e, user_id: userId, txn_id: id }));
        const { error: rpcErr } = await supabase.rpc('replace_journal_entries', {
          p_user_id: userId,
          p_txn_id: id,
          p_entries: newEntries,
        });

        if (rpcErr) {
          console.warn('replace_journal_entries RPC failed, falling back to delete+insert:', rpcErr.message);
          const { error: delErr } = await supabase.from('user_journal_entries').delete().eq('txn_id', id).eq('user_id', userId);
          if (delErr) {
            console.warn('Supabase delete old journal entries error:', delErr.message);
          } else if (newEntries.length > 0) {
            const { error: insErr } = await supabase.from('user_journal_entries').insert(newEntries);
            if (insErr) {
              console.warn('Supabase insert replacement journal entries error:', insErr.message);
            }
          }
        }
      }
    } catch (e) {
      console.warn('Transaction updated locally, cloud sync pending:', e);
    }
  }, [userId, setLocal]);

  const deleteTransaction = useCallback(async (id: string) => {
    setTransactions(prev => {
      const updated = prev.filter(t => t.id !== id);
      setLocal('transactions', updated);
      return updated;
    });
    setJournalEntries(prev => {
      const updated = prev.filter(je => je.txn_id !== id);
      setLocal('journalEntries', updated);
      return updated;
    });
    try {
      const { error: txnErr } = await supabase.from('user_transactions').update({ deleted_at: Date.now(), updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
      if (txnErr) {
        console.warn('Supabase soft-delete transaction error:', txnErr.message);
      }
      const { error: jeErr } = await supabase.from('user_journal_entries').delete().eq('txn_id', id).eq('user_id', userId);
      if (jeErr) {
        console.warn('Supabase delete journal entries error:', jeErr.message);
      }
    } catch (e) {
      console.warn('Transaction deleted locally:', e);
    }
  }, [userId, setLocal]);

  const addGoal = useCallback(async (goal: Omit<Goal, 'user_id'>) => {
    const newGoal: Goal = { ...goal, user_id: userId };
    setGoals(prev => {
      const updated = [newGoal, ...prev];
      setLocal('goals', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_goals').insert(newGoal);
      if (error) console.warn('Supabase insert goal error:', error.message);
    } catch (e) {
      console.warn('Goal saved locally:', e);
    }
  }, [userId, setLocal]);

  const updateGoal = useCallback(async (id: string, updates: Partial<Goal>) => {
    setGoals(prev => {
      const updated = prev.map(g => g.id === id ? { ...g, ...updates, updated_at: Date.now() } : g);
      setLocal('goals', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_goals').update({ ...updates, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
      if (error) console.warn('Supabase update goal error:', error.message);
    } catch (e) {
      console.warn('Goal updated locally:', e);
    }
  }, [userId, setLocal]);

  const addDebt = useCallback(async (debt: Omit<Debt, 'user_id'>) => {
    const newDebt: Debt = { ...debt, user_id: userId };
    setDebts(prev => {
      const updated = [newDebt, ...prev];
      setLocal('debts', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_debts').insert(newDebt);
      if (error) console.warn('Supabase insert debt error:', error.message);
    } catch (e) {
      console.warn('Debt saved locally:', e);
    }
  }, [userId, setLocal]);

  const updateDebt = useCallback(async (id: string, updates: Partial<Debt>) => {
    setDebts(prev => {
      const updated = prev.map(d => d.id === id ? { ...d, ...updates, updated_at: Date.now() } : d);
      setLocal('debts', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_debts').update({ ...updates, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
      if (error) console.warn('Supabase update debt error:', error.message);
    } catch (e) {
      console.warn('Debt updated locally:', e);
    }
  }, [userId, setLocal]);

  const addChitFund = useCallback(async (chit: Omit<ChitFund, 'user_id'>) => {
    const newChit: ChitFund = { ...chit, user_id: userId };
    setChitFunds(prev => {
      const updated = [newChit, ...prev];
      setLocal('chitFunds', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_chit_funds').insert(newChit);
      if (error) console.warn('Supabase insert chit fund error:', error.message);
    } catch (e) {
      console.warn('Chit fund saved locally:', e);
    }
  }, [userId, setLocal]);

  const addRecurring = useCallback(async (rec: Omit<RecurringTransaction, 'user_id'>) => {
    const newRec: RecurringTransaction = { ...rec, user_id: userId };
    setRecurringTxns(prev => {
      const updated = [newRec, ...prev];
      setLocal('recurringTxns', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_recurring_transactions').insert(newRec);
      if (error) console.warn('Supabase insert recurring txn error:', error.message);
    } catch (e) {
      console.warn('Recurring txn saved locally:', e);
    }
  }, [userId, setLocal]);

  const addBudget = useCallback(async (budget: Omit<Budget, 'user_id'>) => {
    const newBudget: Budget = { ...budget, user_id: userId };
    setBudgets(prev => {
      const updated = [...prev, newBudget];
      setLocal('budgets', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_budgets').insert(newBudget);
      if (error) console.warn('Supabase insert budget error:', error.message);
    } catch (e) {
      console.warn('Budget saved locally:', e);
    }
  }, [userId, setLocal]);

  const updateCategoryBudget = useCallback(async (categoryId: string, limitMinor: number | null) => {
    const savedBudgets = getLocal<Record<string, number>>('category_budgets', {});
    if (limitMinor === null || limitMinor <= 0) {
      delete savedBudgets[categoryId];
    } else {
      savedBudgets[categoryId] = limitMinor;
    }
    setLocal('category_budgets', savedBudgets);

    setCategories(prev => {
      const updated = prev.map(c => c.id === categoryId ? { ...c, budget_limit_minor: limitMinor } : c);
      setLocal('categories', updated);
      return updated;
    });

    try {
      if (userId && !isDemo) {
        await supabase.from('user_categories').update({ budget_limit_minor: limitMinor }).eq('id', categoryId).eq('user_id', userId);
      }
    } catch (e) {
      console.warn('Category budget stored in local storage:', e);
    }
  }, [userId, isDemo, getLocal, setLocal]);

  const updateSettings = useCallback(async (updates: Partial<UserSettings>) => {
    setSettings(prev => {
      const updated: UserSettings = prev ? { ...prev, ...updates, updated_at: Date.now() } : {
        user_id: userId,
        currency: 'INR',
        theme_mode: 'DARK',
        mask_amounts: false,
        user_name: 'WealthOS User',
        phone_number: user?.phone || null,
        basis_income_minor: 0,
        needs_pct: 50,
        wants_pct: 30,
        savings_pct: 20,
        daily_reminder_enabled: true,
        daily_reminder_hour: 21,
        updated_at: Date.now(),
        ...updates,
      };
      setLocal('settings', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_settings').upsert({ ...updates, user_id: userId, updated_at: Date.now() });
      if (error) console.warn('Supabase upsert settings error:', error.message);
    } catch (e) {
      console.warn('Settings updated locally:', e);
    }
  }, [userId, user?.phone, setLocal]);

  const markNotificationRead = useCallback(async (id: string) => {
    setNotifications(prev => {
      const updated = prev.map(n => n.id === id ? { ...n, read: true } : n);
      setLocal('notifications', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_notifications').update({ read: true }).eq('id', id).eq('user_id', userId);
      if (error) console.warn('Supabase mark notification read error:', error.message);
    } catch (e) {
      console.warn('Notification marked read locally:', e);
    }
  }, [userId, setLocal]);

  const markAllNotificationsRead = useCallback(async () => {
    setNotifications(prev => {
      const updated = prev.map(n => ({ ...n, read: true }));
      setLocal('notifications', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('user_notifications').update({ read: true }).eq('user_id', userId).eq('read', false);
      if (error) console.warn('Supabase mark all notifications read error:', error.message);
    } catch (e) {
      console.warn('All notifications marked read locally:', e);
    }
  }, [userId, setLocal]);

  // Phase 4 CRUD operations (§4.1 - §4.7)
  const addSalaryCycle = useCallback(async (cycle: Omit<SalaryCycle, 'user_id'>) => {
    const newCycle: SalaryCycle = { ...cycle, user_id: userId };
    setSalaryCycles(prev => {
      const filtered = prev.filter(c => c.id !== newCycle.id);
      const updated = [newCycle, ...filtered].sort((a, b) => b.cycle_start_date.localeCompare(a.cycle_start_date));
      setLocal('salaryCycles', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('salary_cycles').upsert(newCycle);
      if (error) console.warn('Supabase upsert salary_cycle failed:', error.message);
    } catch (e) {
      console.warn('Salary cycle saved locally, sync pending:', e);
    }
  }, [userId, setLocal]);

  const updateFinancialProfile = useCallback(async (updates: Partial<UserFinancialProfile>) => {
    setFinancialProfile(prev => {
      const base = prev || defaultFinancialProfile;
      const updated: UserFinancialProfile = {
        ...base,
        ...updates,
        user_id: userId,
        updated_at: Date.now(),
      };
      setLocal('financialProfile', updated);
      return updated;
    });
    try {
      const profileRow = {
        ...(financialProfile || defaultFinancialProfile),
        ...updates,
        user_id: userId,
        updated_at: Date.now(),
      };
      const { error } = await supabase.from('user_financial_profiles').upsert(profileRow);
      if (error) console.warn('Supabase upsert financial_profile failed:', error.message);
    } catch (e) {
      console.warn('Financial profile saved locally, sync pending:', e);
    }
  }, [userId, financialProfile, defaultFinancialProfile, setLocal]);

  const addSinkingFund = useCallback(async (fund: Omit<SinkingFund, 'user_id'>) => {
    const newFund: SinkingFund = { ...fund, user_id: userId };
    setSinkingFunds(prev => {
      const updated = [...prev, newFund];
      setLocal('sinkingFunds', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('sinking_funds').insert(newFund);
      if (error) console.warn('Supabase insert sinking_fund failed:', error.message);
    } catch (e) {
      console.warn('Sinking fund saved locally, sync pending:', e);
    }
  }, [userId, setLocal]);

  const updateSinkingFund = useCallback(async (id: string, updates: Partial<SinkingFund>) => {
    setSinkingFunds(prev => {
      const updated = prev.map(f => f.id === id ? { ...f, ...updates, updated_at: Date.now() } : f);
      setLocal('sinkingFunds', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('sinking_funds').update({ ...updates, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
      if (error) console.warn('Supabase update sinking_fund failed:', error.message);
    } catch (e) {
      console.warn('Sinking fund updated locally, sync pending:', e);
    }
  }, [userId, setLocal]);

  const deleteSinkingFund = useCallback(async (id: string) => {
    setSinkingFunds(prev => {
      const updated = prev.filter(f => f.id !== id);
      setLocal('sinkingFunds', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('sinking_funds').update({ archived: true, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
      if (error) console.warn('Supabase delete sinking_fund failed:', error.message);
    } catch (e) {
      console.warn('Sinking fund archived locally, sync pending:', e);
    }
  }, [userId, setLocal]);

  const addMandate = useCallback(async (mandate: Omit<Mandate, 'user_id'>) => {
    const newMandate: Mandate = { ...mandate, user_id: userId };
    setMandates(prev => {
      const updated = [...prev, newMandate];
      setLocal('mandates', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('mandates').insert(newMandate);
      if (error) console.warn('Supabase insert mandate failed:', error.message);
    } catch (e) {
      console.warn('Mandate saved locally, sync pending:', e);
    }
  }, [userId, setLocal]);

  const updateMandate = useCallback(async (id: string, updates: Partial<Mandate>) => {
    setMandates(prev => {
      const updated = prev.map(m => m.id === id ? { ...m, ...updates, updated_at: Date.now() } : m);
      setLocal('mandates', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('mandates').update({ ...updates, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
      if (error) console.warn('Supabase update mandate failed:', error.message);
    } catch (e) {
      console.warn('Mandate updated locally, sync pending:', e);
    }
  }, [userId, setLocal]);

  const deleteMandate = useCallback(async (id: string) => {
    setMandates(prev => {
      const updated = prev.filter(m => m.id !== id);
      setLocal('mandates', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('mandates').update({ active: false, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
      if (error) console.warn('Supabase delete mandate failed:', error.message);
    } catch (e) {
      console.warn('Mandate deactivated locally, sync pending:', e);
    }
  }, [userId, setLocal]);

  const addMerchantRule = useCallback(async (rule: Omit<MerchantRule, 'user_id'>) => {
    const newRule: MerchantRule = { ...rule, user_id: userId };
    setMerchantRules(prev => {
      const updated = [...prev, newRule];
      setLocal('merchantRules', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('merchant_rules').insert(newRule);
      if (error) console.warn('Supabase insert merchant_rule failed:', error.message);
    } catch (e) {
      console.warn('Merchant rule saved locally, sync pending:', e);
    }
  }, [userId, setLocal]);

  const deleteMerchantRule = useCallback(async (id: string) => {
    setMerchantRules(prev => {
      const updated = prev.filter(r => r.id !== id);
      setLocal('merchantRules', updated);
      return updated;
    });
    try {
      const { error } = await supabase.from('merchant_rules').delete().eq('id', id).eq('user_id', userId);
      if (error) console.warn('Supabase delete merchant_rule failed:', error.message);
    } catch (e) {
      console.warn('Merchant rule deleted locally, sync pending:', e);
    }
  }, [userId, setLocal]);

  // Reactive state computations (§5.3 - §5.5)
  const activeCycle = useMemo(() => {
    if (salaryCycles.length === 0) return null;
    return [...salaryCycles].sort((a, b) => b.cycle_start_date.localeCompare(a.cycle_start_date))[0] || null;
  }, [salaryCycles]);

  const dailyUpiMeter = useMemo((): DailyUpiMeter => {
    const guiltFreePaise = activeCycle ? (activeCycle.daily_safe_upi_paise || 0) : 0;
    const upiNeeds = financialProfile?.upi_needs_allowance_paise || 0;
    const daysInCycle = activeCycle?.days_in_cycle || 30;
    const needsAllowancePaise = (activeCycle && daysInCycle > 0)
      ? Math.floor(upiNeeds / daysInCycle)
      : 0;
    return {
      guiltFreePaise,
      needsAllowancePaise,
      totalPaise: guiltFreePaise + needsAllowancePaise,
    };
  }, [activeCycle, financialProfile]);

  // Compute live account balances for liquid and survival spendable calculation
  const computedAccounts = useMemo(() => {
    const deletedTxnIds = new Set(transactions.filter(t => t.deleted_at).map(t => t.id));
    return accounts.map(acc => {
      const journalSum = journalEntries
        .filter(je => je.account_id === acc.id && !deletedTxnIds.has(je.txn_id))
        .reduce((sum, je) => sum + je.amount_minor, 0);
      return { ...acc, current_balance_minor: acc.opening_balance_minor + journalSum };
    });
  }, [accounts, journalEntries, transactions]);

  const liquidBalance = useMemo(() => {
    const liquidTypes = ['BANK', 'CASH', 'UPI_WALLET', 'SAVINGS'];
    return computedAccounts
      .filter(a => liquidTypes.includes(a.type))
      .reduce((sum, a) => sum + (a.current_balance_minor || 0), 0);
  }, [computedAccounts]);

  const effectiveSpendable = useMemo(() => {
    const vaultAndUpiBalance = computedAccounts
      .filter(a => a.account_role === 'DAILY_UPI_SPEND' || a.account_role === 'INCOME_VAULT')
      .reduce((sum, a) => sum + Math.max(0, a.current_balance_minor || 0), 0);
    return vaultAndUpiBalance > 0 ? vaultAndUpiBalance : liquidBalance;
  }, [computedAccounts, liquidBalance]);

  const survivalThreshold = useMemo(() => {
    if (financialProfile?.survival_threshold_paise != null) {
      return financialProfile.survival_threshold_paise;
    }
    const baseline = financialProfile?.fixed_baseline_paise;
    return Math.min(200000, baseline ? Math.floor(baseline / 10) : 200000);
  }, [financialProfile]);

  const isSurvivalMode = useMemo(() => {
    return effectiveSpendable > 0 && effectiveSpendable <= survivalThreshold;
  }, [effectiveSpendable, survivalThreshold]);

  const survivalDailyCapPaise = useMemo(() => {
    if (!isSurvivalMode) return null;
    let daysUntilNextPayday = 30;
    if (activeCycle && activeCycle.days_in_cycle > 0) {
      try {
        const start = new Date(activeCycle.cycle_start_date);
        const now = new Date();
        const diffDays = Math.floor((now.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
        daysUntilNextPayday = Math.max(1, activeCycle.days_in_cycle - diffDays);
      } catch {
        daysUntilNextPayday = 30;
      }
    }
    return Math.floor(effectiveSpendable / daysUntilNextPayday);
  }, [isSurvivalMode, effectiveSpendable, activeCycle]);

  const isSalaryWindowActive = useMemo(() => {
    const cycleDef: CycleDefinition = {
      scheduleType: financialProfile?.income_schedule_type || 'VARIABLE_WINDOW',
      paydayWindowStartDay: financialProfile?.payday_window_start_day ?? 2,
      paydayWindowEndDay: financialProfile?.payday_window_end_day ?? 5,
    };
    return isWithinPaydayWindow(new Date(), cycleDef);
  }, [financialProfile]);

  const importAllData = useCallback(async (data: any): Promise<ImportResult> => {
    const errors: string[] = [];
    let cloudSyncedCount = 0;
    let cloudFailedCount = 0;

    try {
      // ---- Parse incoming data ----
      const incomingAccounts: Account[] = data.accounts || [];
      const incomingTxns: Transaction[] = data.txns || data.transactions || [];
      const rawEntries: JournalEntry[] = data.journal_entries || data.journalEntries || [];
      const incomingCategories: Category[] = data.categories || [];
      const incomingGoals: Goal[] = data.goals || [];
      const incomingDebts: Debt[] = data.debts || [];
      const incomingChits: ChitFund[] = data.chits || data.chitFunds || [];
      const incomingRecs: RecurringTransaction[] = data.recurring || data.recurringTxns || [];
      const incomingBudgets: Budget[] = data.budgets || [];
      const incomingSettings: any = data.settings || null;
      const incomingCycles: SalaryCycle[] = data.salary_cycles || data.salaryCycles || [];
      const incomingProfile: UserFinancialProfile | null = data.user_financial_profiles || data.financialProfile || null;
      const incomingFunds: SinkingFund[] = data.sinking_funds || data.sinkingFunds || [];
      const incomingMandates: Mandate[] = data.mandates || [];
      const incomingRules: MerchantRule[] = data.merchant_rules || data.merchantRules || [];

      // ---- F-02: Deduplicate journal entries by composite key ----
      const entryKey = (e: JournalEntry) =>
        `${e.txn_id}|${e.account_id ?? ''}|${e.category_id ?? ''}`;

      let dedupedEntries: JournalEntry[] = [];
      setJournalEntries(prev => {
        const existingKeys = new Set(prev.map(entryKey));
        dedupedEntries = rawEntries
          .map(e => ({ ...e, user_id: userId }))
          .filter(e => !existingKeys.has(entryKey(e)));
        const combined = [...prev, ...dedupedEntries];
        setLocal('journalEntries', combined);
        return combined;
      });

      const skippedEntries = rawEntries.length - dedupedEntries.length;
      if (skippedEntries > 0) {
        console.info(`[Import] Skipped ${skippedEntries} duplicate journal entries.`);
      }

      // ---- Local state merges (deduplicate by id for all entities with stable IDs) ----
      if (incomingAccounts.length > 0) {
        setAccounts(prev => {
          const map = new Map(prev.map(a => [a.id, a]));
          incomingAccounts.forEach(a => map.set(a.id, { ...a, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('accounts', combined);
          return combined;
        });
      }

      if (incomingTxns.length > 0) {
        setTransactions(prev => {
          const map = new Map(prev.map(t => [t.id, t]));
          incomingTxns.forEach(t => map.set(t.id, { ...t, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('transactions', combined);
          return combined;
        });
      }

      if (incomingCategories.length > 0) {
        setCategories(prev => {
          const map = new Map(prev.map(c => [c.id, c]));
          incomingCategories.forEach(c => map.set(c.id, { ...c, user_id: userId }));
          const combined = sanitizeCategories(Array.from(map.values()));
          setLocal('categories', combined);
          return combined;
        });
      }

      if (incomingGoals.length > 0) {
        setGoals(prev => {
          const map = new Map(prev.map(g => [g.id, g]));
          incomingGoals.forEach(g => map.set(g.id, { ...g, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('goals', combined);
          return combined;
        });
      }

      if (incomingDebts.length > 0) {
        setDebts(prev => {
          const map = new Map(prev.map(d => [d.id, d]));
          incomingDebts.forEach(d => map.set(d.id, { ...d, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('debts', combined);
          return combined;
        });
      }

      if (incomingChits.length > 0) {
        setChitFunds(prev => {
          const map = new Map(prev.map(c => [c.id, c]));
          incomingChits.forEach(c => map.set(c.id, { ...c, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('chitFunds', combined);
          return combined;
        });
      }

      if (incomingRecs.length > 0) {
        setRecurringTxns(prev => {
          const map = new Map(prev.map(r => [r.id, r]));
          incomingRecs.forEach(r => map.set(r.id, { ...r, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('recurringTxns', combined);
          return combined;
        });
      }

      if (incomingBudgets.length > 0) {
        setBudgets(prev => {
          const map = new Map(prev.map(b => [b.id, b]));
          incomingBudgets.forEach(b => map.set(b.id, { ...b, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('budgets', combined);
          return combined;
        });
      }

      if (incomingCycles.length > 0) {
        setSalaryCycles(prev => {
          const map = new Map(prev.map(c => [c.id, c]));
          incomingCycles.forEach(c => map.set(c.id, { ...c, user_id: userId }));
          const combined = Array.from(map.values()).sort((a, b) => b.cycle_start_date.localeCompare(a.cycle_start_date));
          setLocal('salaryCycles', combined);
          return combined;
        });
      }

      if (incomingProfile) {
        const mergedProfile = { ...incomingProfile, user_id: userId, updated_at: Date.now() };
        setFinancialProfile(mergedProfile);
        setLocal('financialProfile', mergedProfile);
      }

      if (incomingFunds.length > 0) {
        setSinkingFunds(prev => {
          const map = new Map(prev.map(f => [f.id, f]));
          incomingFunds.forEach(f => map.set(f.id, { ...f, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('sinkingFunds', combined);
          return combined;
        });
      }

      if (incomingMandates.length > 0) {
        setMandates(prev => {
          const map = new Map(prev.map(m => [m.id, m]));
          incomingMandates.forEach(m => map.set(m.id, { ...m, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('mandates', combined);
          return combined;
        });
      }

      if (incomingRules.length > 0) {
        setMerchantRules(prev => {
          const map = new Map(prev.map(r => [r.id, r]));
          incomingRules.forEach(r => map.set(r.id, { ...r, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('merchantRules', combined);
          return combined;
        });
      }

      if (incomingSettings) {
        setSettings(prev => {
          const combined: UserSettings = {
            ...(prev || {}),
            ...incomingSettings,
            user_id: userId,
            updated_at: Date.now(),
          };
          setLocal('settings', combined);
          return combined;
        });
      }

      // ---- Cloud upsert: track failures per entity ----
      const tryUpsert = async (table: string, rows: any[], label: string) => {
        if (rows.length === 0) return;
        try {
          const { error } = await supabase.from(table).upsert(rows);
          if (error) {
            errors.push(`${label}: ${error.message}`);
            cloudFailedCount += rows.length;
          } else {
            cloudSyncedCount += rows.length;
          }
        } catch (e: any) {
          errors.push(`${label} (network): ${e?.message || 'unknown'}`);
          cloudFailedCount += rows.length;
        }
      };

      await tryUpsert('user_accounts', incomingAccounts.map(a => ({ ...a, user_id: userId })), 'accounts');
      await tryUpsert('user_transactions', incomingTxns.map(t => ({ ...t, user_id: userId })), 'transactions');
      await tryUpsert('user_categories', incomingCategories.map(c => ({ ...c, user_id: userId })), 'categories');
      await tryUpsert('user_goals', incomingGoals.map(g => ({ ...g, user_id: userId })), 'goals');
      await tryUpsert('user_debts', incomingDebts.map(d => ({ ...d, user_id: userId })), 'debts');
      await tryUpsert('user_chit_funds', incomingChits.map(c => ({ ...c, user_id: userId })), 'chitFunds');
      await tryUpsert('user_recurring_transactions', incomingRecs.map(r => ({ ...r, user_id: userId })), 'recurring');
      await tryUpsert('user_budgets', incomingBudgets.map(b => ({ ...b, user_id: userId })), 'budgets');
      await tryUpsert('salary_cycles', incomingCycles.map(c => ({ ...c, user_id: userId })), 'salaryCycles');
      await tryUpsert('sinking_funds', incomingFunds.map(f => ({ ...f, user_id: userId })), 'sinkingFunds');
      await tryUpsert('mandates', incomingMandates.map(m => ({ ...m, user_id: userId })), 'mandates');
      await tryUpsert('merchant_rules', incomingRules.map(r => ({ ...r, user_id: userId })), 'merchantRules');

      if (incomingProfile) {
        await tryUpsert('user_financial_profiles', [{ ...incomingProfile, user_id: userId, updated_at: Date.now() }], 'financialProfile');
      }

      // Journal entries: INSERT only the deduplicated new entries
      if (dedupedEntries.length > 0) {
        try {
          const { error } = await supabase.from('user_journal_entries').insert(dedupedEntries);
          if (error) {
            errors.push(`journalEntries: ${error.message}`);
            cloudFailedCount += dedupedEntries.length;
          } else {
            cloudSyncedCount += dedupedEntries.length;
          }
        } catch (e: any) {
          errors.push(`journalEntries (network): ${e?.message || 'unknown'}`);
          cloudFailedCount += dedupedEntries.length;
        }
      }

      // Settings: upsert
      let settingsSynced = false;
      if (incomingSettings) {
        try {
          const settingsRow = { ...incomingSettings, user_id: userId, updated_at: Date.now() };
          const { error } = await supabase.from('user_settings').upsert(settingsRow);
          if (error) {
            errors.push(`settings: ${error.message}`);
          } else {
            settingsSynced = true;
            cloudSyncedCount++;
          }
        } catch (e: any) {
          errors.push(`settings (network): ${e?.message || 'unknown'}`);
        }
      }

      const localCount = incomingAccounts.length + incomingTxns.length + dedupedEntries.length +
        incomingGoals.length + incomingDebts.length + incomingChits.length +
        incomingRecs.length + incomingBudgets.length + incomingCategories.length +
        incomingCycles.length + incomingFunds.length + incomingMandates.length + incomingRules.length +
        (incomingProfile ? 1 : 0) + (incomingSettings ? 1 : 0);

      const hasCloudFailures = errors.length > 0;

      return {
        success: localCount > 0 && !hasCloudFailures,
        partialSuccess: localCount > 0 && hasCloudFailures,
        localCount,
        cloudSyncedCount,
        cloudFailedCount,
        errors,
        entityCounts: {
          accounts: incomingAccounts.length,
          transactions: incomingTxns.length,
          journalEntries: dedupedEntries.length,
          categories: incomingCategories.length,
          goals: incomingGoals.length,
          debts: incomingDebts.length,
          chits: incomingChits.length,
          recurring: incomingRecs.length,
          budgets: incomingBudgets.length,
          settings: settingsSynced,
          salaryCycles: incomingCycles.length,
          financialProfile: incomingProfile ? !errors.some(e => e.startsWith('financialProfile')) : undefined,
          sinkingFunds: incomingFunds.length,
          mandates: incomingMandates.length,
          merchantRules: incomingRules.length,
        },
      };
    } catch (e: any) {
      console.error('[WealthOS] Import failed unexpectedly:', e);
      return {
        success: false,
        partialSuccess: false,
        localCount: 0,
        cloudSyncedCount: 0,
        cloudFailedCount: 0,
        errors: [e?.message || 'Unexpected import error'],
        entityCounts: {
          accounts: 0, transactions: 0, journalEntries: 0, categories: 0,
          goals: 0, debts: 0, chits: 0, recurring: 0, budgets: 0, settings: false,
        },
      };
    }
  }, [userId, setLocal]);

  return (
    <DataContext.Provider value={{
      accounts,
      transactions,
      journalEntries,
      categories,
      goals,
      debts,
      chitFunds,
      recurringTxns,
      budgets,
      settings,
      notifications,
      salaryCycles,
      activeCycle,
      financialProfile,
      sinkingFunds,
      mandates,
      merchantRules,
      dailyUpiMeter,
      isSurvivalMode,
      survivalDailyCapPaise,
      isSalaryWindowActive,
      loading,
      refresh: fetchAll,
      addAccount,
      updateAccount,
      deleteAccount,
      addTransaction,
      updateTransaction,
      deleteTransaction,
      addGoal,
      updateGoal,
      addDebt,
      updateDebt,
      addChitFund,
      addRecurring,
      addBudget,
      updateCategoryBudget,
      updateSettings,
      markNotificationRead,
      markAllNotificationsRead,
      importAllData,
      addSalaryCycle,
      updateFinancialProfile,
      addSinkingFund,
      updateSinkingFund,
      deleteSinkingFund,
      addMandate,
      updateMandate,
      deleteMandate,
      addMerchantRule,
      deleteMerchantRule,
    }}>
      {children}
    </DataContext.Provider>
  );
}

export function useData() {
  const context = useContext(DataContext);
  if (!context) throw new Error('useData must be used within DataProvider');
  return context;
}
