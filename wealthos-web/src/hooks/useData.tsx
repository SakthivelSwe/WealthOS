import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from './useAuth';
import type { Account, Transaction, JournalEntry, Goal, Debt, ChitFund, RecurringTransaction, Category, UserSettings, AppNotification, Budget } from '../types';
import { DEFAULT_CATEGORIES as SEED_CATEGORIES, sanitizeCategories } from '../types';
import { getSampleSeedData } from '../lib/mockData';

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
  updateSettings: (updates: Partial<UserSettings>) => Promise<void>;
  markNotificationRead: (id: string) => Promise<void>;
  markAllNotificationsRead: () => Promise<void>;
  importAllData: (data: any) => Promise<{ success: boolean; count: number }>;
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
  const [loading, setLoading] = useState(true);

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
        const cachedCats = sanitizeCategories(getLocal('categories', []));
        setCategories(cachedCats);
        setLocal('categories', cachedCats);
        setSettings(getLocal('settings', null));
        setNotifications(getLocal('notifications', []));
      }
      setLoading(false);
      return;
    }

    // REAL USER PATH: load from cloud, or start completely clean starting today
    let supabaseLoaded = false;
    try {
      const [accts, txns, entries, cats, gls, dts, chts, recs, bdgs, sttgs, notifs] = await Promise.allSettled([
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
        setCategories(sanitized);
        setLocal('categories', sanitized);
        if (cats.value.data.some(c => c.id === 'cat_salary') && cats.value.data.some(c => c.id === 'cat_income_0')) {
          supabase.from('user_categories').delete().eq('id', 'cat_salary').eq('user_id', userId).then(() => {}, () => {});
        }
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
    } else {
      // Supabase is active: Ensure default categories, settings, and welcome notification if new
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
      setNotifications(prev => {
        if (prev.length === 0) {
          setLocal('notifications', [welcomeNotification]);
          return [welcomeNotification];
        }
        return prev;
      });
    }

    setLoading(false);
  }, [userId, isDemo, user, getLocal, setLocal]);

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
  }, [userId]);

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
  }, [userId]);

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
  }, [userId]);

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
  }, [userId]);

  const updateTransaction = useCallback(async (id: string, updates: Partial<Transaction>, entries?: Omit<JournalEntry, 'user_id'>[]) => {
    setTransactions(prev => {
      const updated = prev.map(t => t.id === id ? { ...t, ...updates, updated_at: Date.now() } : t);
      setLocal('transactions', updated);
      return updated;
    });

    if (entries && entries.length > 0) {
      setJournalEntries(prev => {
        const filtered = prev.filter(je => je.txn_id !== id);
        const updated = [...filtered, ...entries.map(e => ({ ...e, user_id: userId }))];
        setLocal('journalEntries', updated);
        return updated;
      });
    }

    try {
      const { error } = await supabase.from('user_transactions').update({ ...updates, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
      if (error) {
        console.warn('Supabase update transaction error:', error.message);
      }
      if (entries && entries.length > 0) {
        const { error: delErr } = await supabase.from('user_journal_entries').delete().eq('txn_id', id).eq('user_id', userId);
        if (delErr) console.warn('Supabase delete old journal entries error:', delErr.message);
        const { error: insErr } = await supabase.from('user_journal_entries').insert(entries.map(e => ({ ...e, user_id: userId })));
        if (insErr) console.warn('Supabase insert new journal entries error:', insErr.message);
      }
    } catch (e) {
      console.warn('Transaction updated locally:', e);
    }
  }, [userId]);

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
  }, [userId]);

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
  }, [userId]);

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
  }, [userId]);

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
  }, [userId]);

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
  }, [userId]);

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
  }, [userId]);

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
  }, [userId]);

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
  }, [userId]);

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
  }, [userId, user?.phone]);

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
  }, [userId]);

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
  }, [userId]);

  const importAllData = useCallback(async (data: any) => {
    let count = 0;
    try {
      const incomingAccounts: Account[] = data.accounts || [];
      const incomingTxns: Transaction[] = data.txns || data.transactions || [];
      const incomingEntries: JournalEntry[] = data.journal_entries || data.journalEntries || [];
      const incomingGoals: Goal[] = data.goals || [];
      const incomingDebts: Debt[] = data.debts || [];
      const incomingChits: ChitFund[] = data.chits || data.chitFunds || [];
      const incomingRecs: RecurringTransaction[] = data.recurring || data.recurringTxns || [];
      const incomingBudgets: Budget[] = data.budgets || [];

      if (incomingAccounts.length > 0) {
        setAccounts(prev => {
          const map = new Map(prev.map(a => [a.id, a]));
          incomingAccounts.forEach(a => map.set(a.id, { ...a, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('accounts', combined);
          return combined;
        });
        count += incomingAccounts.length;
      }

      if (incomingTxns.length > 0) {
        setTransactions(prev => {
          const map = new Map(prev.map(t => [t.id, t]));
          incomingTxns.forEach(t => map.set(t.id, { ...t, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('transactions', combined);
          return combined;
        });
        count += incomingTxns.length;
      }

      if (incomingEntries.length > 0) {
        setJournalEntries(prev => {
          const combined = [...prev, ...incomingEntries.map(e => ({ ...e, user_id: userId }))];
          setLocal('journalEntries', combined);
          return combined;
        });
        count += incomingEntries.length;
      }

      if (incomingGoals.length > 0) {
        setGoals(prev => {
          const map = new Map(prev.map(g => [g.id, g]));
          incomingGoals.forEach(g => map.set(g.id, { ...g, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('goals', combined);
          return combined;
        });
        count += incomingGoals.length;
      }

      if (incomingDebts.length > 0) {
        setDebts(prev => {
          const map = new Map(prev.map(d => [d.id, d]));
          incomingDebts.forEach(d => map.set(d.id, { ...d, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('debts', combined);
          return combined;
        });
        count += incomingDebts.length;
      }

      if (incomingChits.length > 0) {
        setChitFunds(prev => {
          const map = new Map(prev.map(c => [c.id, c]));
          incomingChits.forEach(c => map.set(c.id, { ...c, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('chitFunds', combined);
          return combined;
        });
        count += incomingChits.length;
      }

      if (incomingRecs.length > 0) {
        setRecurringTxns(prev => {
          const map = new Map(prev.map(r => [r.id, r]));
          incomingRecs.forEach(r => map.set(r.id, { ...r, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('recurringTxns', combined);
          return combined;
        });
        count += incomingRecs.length;
      }

      if (incomingBudgets.length > 0) {
        setBudgets(prev => {
          const map = new Map(prev.map(b => [b.id, b]));
          incomingBudgets.forEach(b => map.set(b.id, { ...b, user_id: userId }));
          const combined = Array.from(map.values());
          setLocal('budgets', combined);
          return combined;
        });
        count += incomingBudgets.length;
      }

      // Try uploading to Supabase if connected
      try {
        if (incomingAccounts.length > 0) {
          const { error } = await supabase.from('user_accounts').upsert(incomingAccounts.map(a => ({ ...a, user_id: userId })));
          if (error) console.warn('Supabase upsert imported accounts warning:', error.message);
        }
        if (incomingTxns.length > 0) {
          const { error } = await supabase.from('user_transactions').upsert(incomingTxns.map(t => ({ ...t, user_id: userId })));
          if (error) console.warn('Supabase upsert imported transactions warning:', error.message);
        }
        if (incomingEntries.length > 0) {
          const { error } = await supabase.from('user_journal_entries').upsert(incomingEntries.map(e => ({ ...e, user_id: userId })));
          if (error) console.warn('Supabase upsert imported journal entries warning:', error.message);
        }
        if (incomingGoals.length > 0) {
          const { error } = await supabase.from('user_goals').upsert(incomingGoals.map(g => ({ ...g, user_id: userId })));
          if (error) console.warn('Supabase upsert imported goals warning:', error.message);
        }
        if (incomingDebts.length > 0) {
          const { error } = await supabase.from('user_debts').upsert(incomingDebts.map(d => ({ ...d, user_id: userId })));
          if (error) console.warn('Supabase upsert imported debts warning:', error.message);
        }
        if (incomingBudgets.length > 0) {
          const { error } = await supabase.from('user_budgets').upsert(incomingBudgets.map(b => ({ ...b, user_id: userId })));
          if (error) console.warn('Supabase upsert imported budgets warning:', error.message);
        }
      } catch (e) {
        console.warn('Cloud sync of imported items pending:', e);
      }

      return { success: true, count };
    } catch (e) {
      console.error('Failed to import data:', e);
      return { success: false, count: 0 };
    }
  }, [userId]);

  return (
    <DataContext.Provider value={{
      accounts, transactions, journalEntries, categories, goals, debts, chitFunds,
      recurringTxns, budgets, settings, notifications, loading,
      refresh: fetchAll,
      addAccount, updateAccount, deleteAccount,
      addTransaction, updateTransaction, deleteTransaction,
      addGoal, updateGoal, addDebt, updateDebt,
      addChitFund, addRecurring, addBudget,
      updateSettings, markNotificationRead, markAllNotificationsRead,
      importAllData,
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
