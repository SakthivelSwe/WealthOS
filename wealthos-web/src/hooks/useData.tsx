import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from 'react';
import { supabase } from '../lib/supabase';
import { useAuth } from './useAuth';
import type { Account, Transaction, JournalEntry, Goal, Debt, ChitFund, RecurringTransaction, Category, UserSettings, AppNotification, Budget } from '../types';
import { DEFAULT_CATEGORIES as SEED_CATEGORIES } from '../types';
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
  updateTransaction: (id: string, updates: Partial<Transaction>) => Promise<void>;
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

const STORAGE_PREFIX = 'wealthos_cache_';

function getLocal<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

function setLocal<T>(key: string, value: T) {
  try {
    localStorage.setItem(STORAGE_PREFIX + key, JSON.stringify(value));
  } catch (e) {
    console.warn('LocalStorage save failed:', e);
  }
}

export function DataProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const userId = user?.id || 'demo_user';

  const [accounts, setAccounts] = useState<Account[]>(() => getLocal('accounts', []));
  const [transactions, setTransactions] = useState<Transaction[]>(() => getLocal('transactions', []));
  const [journalEntries, setJournalEntries] = useState<JournalEntry[]>(() => getLocal('journalEntries', []));
  const [categories, setCategories] = useState<Category[]>(() => getLocal('categories', []));
  const [goals, setGoals] = useState<Goal[]>(() => getLocal('goals', []));
  const [debts, setDebts] = useState<Debt[]>(() => getLocal('debts', []));
  const [chitFunds, setChitFunds] = useState<ChitFund[]>(() => getLocal('chitFunds', []));
  const [recurringTxns, setRecurringTxns] = useState<RecurringTransaction[]>(() => getLocal('recurringTxns', []));
  const [budgets, setBudgets] = useState<Budget[]>(() => getLocal('budgets', []));
  const [settings, setSettings] = useState<UserSettings | null>(() => getLocal('settings', null));
  const [notifications, setNotifications] = useState<AppNotification[]>(() => getLocal('notifications', []));
  const [loading, setLoading] = useState(true);

  const fetchAll = useCallback(async () => {
    if (!userId) return;
    setLoading(true);

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

      if (accts.status === 'fulfilled' && accts.value.data && accts.value.data.length > 0) {
        setAccounts(accts.value.data);
        setLocal('accounts', accts.value.data);
        supabaseLoaded = true;
      }
      if (txns.status === 'fulfilled' && txns.value.data && txns.value.data.length > 0) {
        setTransactions(txns.value.data);
        setLocal('transactions', txns.value.data);
        supabaseLoaded = true;
      }
      if (entries.status === 'fulfilled' && entries.value.data) {
        setJournalEntries(entries.value.data);
        setLocal('journalEntries', entries.value.data);
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
        setCategories(cats.value.data);
        setLocal('categories', cats.value.data);
      }
    } catch (err) {
      console.warn('Supabase fetch notice, falling back to local storage:', err);
    }

    // Seed defaults if empty
    const currentAccounts = getLocal<Account[]>('accounts', []);
    if (!supabaseLoaded && currentAccounts.length === 0) {
      const sample = getSampleSeedData(userId);
      const defaultCategories = SEED_CATEGORIES.map(c => ({ ...c, user_id: userId }));
      const defaultSettings: UserSettings = {
        user_id: userId,
        currency: 'INR',
        theme_mode: 'DARK',
        mask_amounts: false,
        user_name: 'WealthOS User',
        phone_number: user?.phone || '+91 98765 43210',
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
      setCategories(defaultCategories);
      setSettings(defaultSettings);

      setLocal('accounts', sample.accounts);
      setLocal('transactions', sample.transactions);
      setLocal('journalEntries', sample.journalEntries);
      setLocal('goals', sample.goals);
      setLocal('debts', sample.debts);
      setLocal('chitFunds', sample.chitFunds);
      setLocal('recurringTxns', sample.recurringTxns);
      setLocal('notifications', sample.notifications);
      setLocal('categories', defaultCategories);
      setLocal('settings', defaultSettings);
    } else if (!supabaseLoaded && currentAccounts.length > 0) {
      // Ensure Axis Bank is available in existing session cache
      const hasAxis = currentAccounts.some(a =>
        a.name.toLowerCase().includes('axis') || (a.institution && a.institution.toLowerCase().includes('axis'))
      );
      if (!hasAxis) {
        const axisAccount: Account = {
          id: 'acc_axis_salary',
          user_id: userId,
          name: 'Axis Bank Priority Account',
          type: 'BANK',
          institution: 'Axis Bank',
          last_four: '6512',
          opening_balance_minor: 18500000,
          currency: 'INR',
          include_in_net_worth: true,
          include_in_stats: true,
          archived: false,
          created_at: Date.now() - 95 * 86400000,
          updated_at: Date.now(),
        };
        const updated = [axisAccount, ...currentAccounts];
        setAccounts(updated);
        setLocal('accounts', updated);
      }
    }

    setLoading(false);
  }, [userId, user?.phone]);

  // Subscribe to real-time changes
  useEffect(() => {
    if (!userId) return;
    fetchAll();

    try {
      const channel = supabase.channel('db-changes')
        .on('postgres_changes', { event: '*', schema: 'public', table: 'user_transactions', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'user_accounts', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'user_goals', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'user_debts', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .on('postgres_changes', { event: '*', schema: 'public', table: 'user_settings', filter: `user_id=eq.${userId}` }, () => fetchAll())
        .subscribe();

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
      await supabase.from('user_accounts').insert(newAccount);
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
      await supabase.from('user_accounts').update({ ...updates, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
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
      await supabase.from('user_accounts').update({ archived: true, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
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
      await supabase.from('user_transactions').insert(newTxn);
      if (newEntries.length > 0) {
        await supabase.from('user_journal_entries').insert(newEntries);
      }
    } catch (e) {
      console.warn('Transaction stored locally:', e);
    }
  }, [userId]);

  const updateTransaction = useCallback(async (id: string, updates: Partial<Transaction>) => {
    setTransactions(prev => {
      const updated = prev.map(t => t.id === id ? { ...t, ...updates, updated_at: Date.now() } : t);
      setLocal('transactions', updated);
      return updated;
    });
    try {
      await supabase.from('user_transactions').update({ ...updates, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
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
    try {
      await supabase.from('user_transactions').update({ deleted_at: Date.now(), updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
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
      await supabase.from('user_goals').insert(newGoal);
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
      await supabase.from('user_goals').update({ ...updates, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
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
      await supabase.from('user_debts').insert(newDebt);
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
      await supabase.from('user_debts').update({ ...updates, updated_at: Date.now() }).eq('id', id).eq('user_id', userId);
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
      await supabase.from('user_chit_funds').insert(newChit);
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
      await supabase.from('user_recurring_transactions').insert(newRec);
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
      await supabase.from('user_budgets').insert(newBudget);
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
      await supabase.from('user_settings').upsert({ ...updates, user_id: userId, updated_at: Date.now() });
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
      await supabase.from('user_notifications').update({ read: true }).eq('id', id).eq('user_id', userId);
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
      await supabase.from('user_notifications').update({ read: true }).eq('user_id', userId).eq('read', false);
    } catch (e) {
      console.warn('All notifications marked read locally:', e);
    }
  }, [userId]);

  const importAllData = useCallback(async (data: any) => {
    let count = 0;
    try {
      const incomingAccounts: Account[] = data.accounts || [];
      const incomingTxns: Transaction[] = data.txns || data.transactions || [];
      const incomingGoals: Goal[] = data.goals || [];
      const incomingDebts: Debt[] = data.debts || [];
      const incomingChits: ChitFund[] = data.chits || data.chitFunds || [];
      const incomingRecs: RecurringTransaction[] = data.recurring || data.recurringTxns || [];

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

      // Try uploading to Supabase if connected
      try {
        if (incomingAccounts.length > 0) await supabase.from('user_accounts').upsert(incomingAccounts.map(a => ({ ...a, user_id: userId })));
        if (incomingTxns.length > 0) await supabase.from('user_transactions').upsert(incomingTxns.map(t => ({ ...t, user_id: userId })));
        if (incomingGoals.length > 0) await supabase.from('user_goals').upsert(incomingGoals.map(g => ({ ...g, user_id: userId })));
        if (incomingDebts.length > 0) await supabase.from('user_debts').upsert(incomingDebts.map(d => ({ ...d, user_id: userId })));
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
