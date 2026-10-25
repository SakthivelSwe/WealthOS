import { useState, useMemo, useEffect } from 'react';
import { useData } from '../../hooks/useData';
import { formatMoney, parseMoney, ACCOUNT_TYPE_LABELS, POPULAR_BANKS, getBankColor, type Account, type AccountType, type AccountRole } from '../../types';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { NightlyCheckInModal } from '../shared/NightlyCheckInModal';
import {
  Plus,
  Edit2,
  Archive,
  Wallet,
  TrendingUp,
  CreditCard,
  X,
  Shield,
  Building2,
  AlertTriangle,
  ArrowRight,
  ArrowDownRight,
  CheckCircle2,
  Moon,
  Zap,
  Sliders,
  Copy,
  Info,
} from 'lucide-react';

const ACCOUNT_TYPES: AccountType[] = ['BANK','CASH','UPI_WALLET','SAVINGS','CREDIT_CARD','INVESTMENT','LOAN','OTHER_ASSET','OTHER_LIABILITY'];

export const ACCOUNT_ROLES: { id: AccountRole; label: string; desc: string; badge: string; color: string }[] = [
  {
    id: 'INCOME_VAULT',
    label: 'Income Vault (Salary Account)',
    desc: 'Primary salary credit account. Do NOT link daily UPI apps here.',
    badge: 'Vault 🛡️',
    color: 'var(--neon-amber)',
  },
  {
    id: 'DAILY_UPI_SPEND',
    label: 'Daily UPI Spend Account',
    desc: 'Linked to UPI & UPI Lite apps for everyday scans and groceries.',
    badge: 'Daily UPI ⚡',
    color: 'var(--neon-cyan)',
  },
  {
    id: 'EMERGENCY_RESERVE',
    label: 'Emergency Reserve Account',
    desc: 'Auto-sweep FD, RD, or liquid fund for 3–7 months safety runway.',
    badge: 'Emergency 🛟',
    color: 'var(--neon-emerald)',
  },
  {
    id: 'WEALTH_PORTFOLIO',
    label: 'Wealth & Investment Account',
    desc: 'Mutual funds, index SIPs, gold, long-term wealth growth.',
    badge: 'Wealth 📈',
    color: 'var(--neon-purple)',
  },
  {
    id: 'LIABILITY_LOAN',
    label: 'Liability & Credit Cards',
    desc: 'Credit cards, personal loans, vehicle EMIs, BNPL accounts.',
    badge: 'Liability 💳',
    color: 'var(--neon-rose)',
  },
];

function AccountModal({ account, onClose }: { account?: Account; onClose: () => void }) {
  const { addAccount, updateAccount, settings } = useData();
  const [name, setName] = useState(account?.name || '');
  const [type, setType] = useState<AccountType>(account?.type || 'BANK');
  const [role, setRole] = useState<AccountRole | ''>(account?.account_role || '');
  const [balance, setBalance] = useState(account ? String(account.opening_balance_minor / 100) : '');
  const [institution, setInstitution] = useState(account?.institution || '');
  const [lastFour, setLastFour] = useState(account?.last_four || '');
  const [includeNW, setIncludeNW] = useState(account?.include_in_net_worth ?? true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const currency = settings?.currency || 'INR';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) { setError('Name is required'); return; }
    setLoading(true);
    const now = Date.now();
    const roleToSave = (role as AccountRole) || null;

    if (account) {
      await updateAccount(account.id, {
        name: name.trim(),
        type,
        account_role: roleToSave,
        institution: institution || null,
        last_four: lastFour || null,
        include_in_net_worth: includeNW,
        updated_at: now,
      });
    } else {
      await addAccount({
        id: Math.random().toString(36).substring(2) + Date.now(),
        name: name.trim(),
        type,
        account_role: roleToSave,
        institution: institution || null,
        last_four: lastFour || null,
        opening_balance_minor: parseMoney(balance || '0', currency),
        currency,
        include_in_net_worth: includeNW,
        include_in_stats: true,
        archived: false,
        created_at: now,
        updated_at: now,
      });
    }
    setLoading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: 540 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800 }}>
            {account ? 'Edit Account' : 'Add New Account'}
          </h2>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          {error && <div style={{ color: 'var(--neon-rose)', fontSize: '0.82rem', marginBottom: 14 }}>{error}</div>}

          <div className="form-group">
            <label className="form-label">Account / Wallet Name *</label>
            <input value={name} onChange={e => setName(e.target.value)} placeholder="e.g. Axis Bank, HDFC Salary, Cash" autoFocus />
          </div>

          {/* Account Role Selector (§8.6) */}
          <div className="form-group">
            <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Shield size={14} style={{ color: 'var(--neon-cyan)' }} /> Strategic Account Role (§8.6)
            </label>
            <select value={role} onChange={e => setRole(e.target.value as AccountRole | '')}>
              <option value="">General Account (No designated role)</option>
              {ACCOUNT_ROLES.map(r => (
                <option key={r.id} value={r.id}>{r.label}</option>
              ))}
            </select>
          </div>

          {/* Vault Isolation Warning (§8.6, Video 1 & 14) */}
          {role === 'INCOME_VAULT' && (
            <div
              style={{
                padding: '10px 14px',
                borderRadius: 'var(--radius-md)',
                background: 'rgba(245, 158, 11, 0.1)',
                border: '1px solid rgba(245, 158, 11, 0.3)',
                fontSize: '0.78rem',
                color: '#fef3c7',
                lineHeight: 1.4,
                marginBottom: 14,
                display: 'flex',
                gap: 8,
                alignItems: 'flex-start',
              }}
            >
              <AlertTriangle size={16} style={{ color: 'var(--neon-amber)', flexShrink: 0, marginTop: 2 }} />
              <div>
                <strong>Vault Isolation Protocol:</strong> Do not link everyday UPI apps (GPay, PhonePe, Paytm) to this vault account. Keeps your primary salary isolated from continuous micro-leaks and balance drain.
              </div>
            </div>
          )}

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Account Type</label>
              <select value={type} onChange={e => setType(e.target.value as AccountType)}>
                {ACCOUNT_TYPES.map(t => <option key={t} value={t}>{ACCOUNT_TYPE_LABELS[t]}</option>)}
              </select>
            </div>
            {!account && (
              <div className="form-group">
                <label className="form-label">Starting Balance ({currency})</label>
                <input type="number" step="any" value={balance} onChange={e => setBalance(e.target.value)} placeholder="0.00" />
              </div>
            )}
          </div>

          {/* Quick Bank Presets */}
          <div className="form-group">
            <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Building2 size={13} style={{ color: 'var(--neon-cyan)' }} /> Popular Banks (1-Click Fill)
            </label>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 4 }}>
              {POPULAR_BANKS.map(bank => {
                const isSelected = institution.toLowerCase() === bank.name.toLowerCase();
                return (
                  <button
                    key={bank.id}
                    type="button"
                    onClick={() => {
                      setInstitution(bank.name);
                      if (!name || name.includes('Account')) {
                        setName(bank.sampleName);
                      }
                      setType(bank.defaultType);
                    }}
                    style={{
                      padding: '5px 10px',
                      borderRadius: 'var(--radius-pill)',
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      border: isSelected ? `1px solid ${bank.color}` : '1px solid var(--glass-border)',
                      background: isSelected ? bank.badgeBg : 'rgba(255, 255, 255, 0.04)',
                      color: isSelected ? bank.color : 'var(--text-secondary)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 4,
                      transition: 'all 0.15s ease',
                      boxShadow: isSelected ? `0 0 10px ${bank.color}44` : 'none',
                    }}
                  >
                    <span style={{ width: 6, height: 6, borderRadius: '50%', background: bank.color }} />
                    {bank.name}
                  </button>
                );
              })}
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Financial Institution</label>
              <input value={institution} onChange={e => setInstitution(e.target.value)} placeholder="e.g. Axis Bank, HDFC, SBI" />
            </div>
            <div className="form-group">
              <label className="form-label">Last 4 Digits</label>
              <input value={lastFour} onChange={e => setLastFour(e.target.value.slice(0, 4))} placeholder="4821" maxLength={4} />
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 10, margin: '14px 0 20px 0' }}>
            <input type="checkbox" id="nw" checked={includeNW} onChange={e => setIncludeNW(e.target.checked)} style={{ width: 18, height: 18, accentColor: 'var(--neon-cyan)' }} />
            <label htmlFor="nw" style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', cursor: 'pointer' }}>
              Include balance in Aggregate Net Worth calculations
            </label>
          </div>

          <div style={{ display: 'flex', gap: 10 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} style={{ flex: 1 }}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading} style={{ flex: 2 }}>
              {account ? 'Update Account' : 'Create Account'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export function AccountsPage() {
  const { accounts, journalEntries, transactions, salaryCycles, financialProfile, dailyUpiMeter, addAccount, deleteAccount, settings } = useData();
  const [showModal, setShowModal] = useState(false);
  const [showNightlyModal, setShowNightlyModal] = useState(false);
  const [editAccount, setEditAccount] = useState<Account | undefined>();
  const [copiedPlan, setCopiedPlan] = useState(false);
  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;

  // Configurable UPI Lite Settings (§8.6)
  const [upiLitePerTxn, setUpiLitePerTxn] = useState<number>(() => {
    try {
      const s = localStorage.getItem('wealthos_upi_lite_per_txn');
      return s ? Number(s) : 500;
    } catch {
      return 500;
    }
  });

  const [upiLiteBalanceCap, setUpiLiteBalanceCap] = useState<number>(() => {
    try {
      const s = localStorage.getItem('wealthos_upi_lite_balance_cap');
      return s ? Number(s) : 2000;
    } catch {
      return 2000;
    }
  });

  const [editingUpiLite, setEditingUpiLite] = useState(false);

  const handleSaveUpiLite = (perTxn: number, cap: number) => {
    setUpiLitePerTxn(perTxn);
    setUpiLiteBalanceCap(cap);
    try {
      localStorage.setItem('wealthos_upi_lite_per_txn', String(perTxn));
      localStorage.setItem('wealthos_upi_lite_balance_cap', String(cap));
    } catch (e) {
      console.warn(e);
    }
    setEditingUpiLite(false);
  };

  const deletedTxnIds = useMemo(() => new Set(transactions.filter(t => t.deleted_at).map(t => t.id)), [transactions]);

  const computedAccounts = useMemo(() => accounts.map(acc => {
    const journalSum = journalEntries
      .filter(je => je.account_id === acc.id && !deletedTxnIds.has(je.txn_id))
      .reduce((sum, je) => sum + je.amount_minor, 0);
    return { ...acc, current_balance_minor: acc.opening_balance_minor + journalSum };
  }), [accounts, journalEntries, deletedTxnIds]);

  const totalAssets = useMemo(() => computedAccounts
    .filter(a => a.include_in_net_worth && (a.currency || currency) === currency && (a.current_balance_minor || 0) > 0)
    .reduce((s, a) => s + (a.current_balance_minor || 0), 0), [computedAccounts, currency]);

  const totalLiabilities = useMemo(() => computedAccounts
    .filter(a => a.include_in_net_worth && (a.currency || currency) === currency && (a.current_balance_minor || 0) < 0)
    .reduce((s, a) => s + (a.current_balance_minor || 0), 0), [computedAccounts, currency]);

  const netWorth = totalAssets + totalLiabilities;

  const groupedByType = useMemo(() => {
    const map: Record<string, typeof computedAccounts> = {};
    computedAccounts.forEach(a => {
      if (!map[a.type]) map[a.type] = [];
      map[a.type].push(a);
    });
    return map;
  }, [computedAccounts]);

  // Strategic Account Roles Finder (§8.6)
  const vaultAccount = computedAccounts.find(a => a.account_role === 'INCOME_VAULT') || computedAccounts.find(a => a.type === 'BANK');
  const dailyUpiAccount = computedAccounts.find(a => a.account_role === 'DAILY_UPI_SPEND') || computedAccounts.find(a => a.type === 'UPI_WALLET');
  const emergencyAccount = computedAccounts.find(a => a.account_role === 'EMERGENCY_RESERVE') || computedAccounts.find(a => a.type === 'SAVINGS');
  const wealthAccount = computedAccounts.find(a => a.account_role === 'WEALTH_PORTFOLIO') || computedAccounts.find(a => a.type === 'INVESTMENT');

  // Payday Transfer Plan Calculations (§8.6, Videos 3, 4)
  const activeCycle = salaryCycles[0] || null;
  const cycleSalaryPaise = activeCycle?.salary_paise || (financialProfile?.expected_salary_min_paise || 3220000);
  const baselinePaise = activeCycle?.baseline_paise || (financialProfile?.fixed_baseline_paise || 1771000);
  const emergencyTransferPaise = activeCycle?.jar_amounts?.LONG_TERM || Math.floor(cycleSalaryPaise * 0.1);
  const wealthTransferPaise = activeCycle?.jar_amounts?.FREEDOM_SIP || Math.floor(cycleSalaryPaise * 0.1);
  const weeklyUpiReloadPaise = (dailyUpiMeter.totalPaise > 0 ? dailyUpiMeter.totalPaise : 115000) * 7;

  const copyTransferSteps = () => {
    const text = [
      `WealthOS Payday Transfer Plan (${vaultAccount?.name || 'Income Vault'}):`,
      `1. Move ${formatMoney(emergencyTransferPaise, currency, false)} -> ${emergencyAccount?.name || 'Emergency Reserve'}`,
      `2. Move ${formatMoney(wealthTransferPaise, currency, false)} -> ${wealthAccount?.name || 'Wealth Portfolio'}`,
      `3. Move ${formatMoney(weeklyUpiReloadPaise, currency, false)} -> ${dailyUpiAccount?.name || 'Daily UPI Spend'} / UPI Lite`,
      `4. Retain ${formatMoney(baselinePaise, currency, false)} in Vault for Rent & EMI`,
    ].join('\n');

    navigator.clipboard.writeText(text);
    setCopiedPlan(true);
    setTimeout(() => setCopiedPlan(false), 2000);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
            Bank Accounts & Wallets
          </h1>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
            Multi-account role segregation, vault isolation protocol & payday transfer plan (§8.6)
          </p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => setShowNightlyModal(true)}
            style={{ borderColor: 'var(--neon-purple)', color: 'var(--neon-purple)' }}
          >
            <Moon size={15} /> Nightly Check-in
          </button>
          <button
            className="btn btn-primary"
            onClick={() => { setEditAccount(undefined); setShowModal(true); }}
          >
            <Plus size={16} /> Add Account
          </button>
        </div>
      </div>

      {/* Top 3D Spatial Stat Cards */}
      <div className="bento-grid">
        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-cyan)' }} />
              <div className="stat-label">
                <span>Total Net Worth</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-cyan-glow)', color: 'var(--neon-cyan)' }}>
                  <Wallet size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-cyan)' }}>
                {formatMoney(netWorth, currency, masked)}
              </div>
              <div className="stat-trend neutral">Total balance minus loans & cards</div>
            </div>
          </Tilt3DCard>
        </div>

        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-emerald)' }} />
              <div className="stat-label">
                <span>Total Money & Savings</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-emerald-glow)', color: 'var(--neon-emerald)' }}>
                  <TrendingUp size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-emerald)' }}>
                {formatMoney(totalAssets, currency, masked)}
              </div>
              <div className="stat-trend positive">Bank accounts, cash, and investments</div>
            </div>
          </Tilt3DCard>
        </div>

        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-rose)' }} />
              <div className="stat-label">
                <span>Total Loans & Credit Cards</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-rose-glow)', color: 'var(--neon-rose)' }}>
                  <CreditCard size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-rose)' }}>
                {formatMoney(Math.abs(totalLiabilities), currency, masked)}
              </div>
              <div className="stat-trend negative">Pending loan amounts & card bills</div>
            </div>
          </Tilt3DCard>
        </div>
      </div>

      {/* PAYDAY TRANSFER PLAN & CONFIGURABLE UPI LITE (§8.6) */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: 16 }}>
        {/* Payday Transfer Plan Card */}
        <div className="cyber-card" style={{ padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: '50%',
                  background: 'rgba(245, 158, 11, 0.15)',
                  color: 'var(--neon-amber)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Shield size={16} />
              </div>
              <div>
                <h3 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.05rem', fontWeight: 800, margin: 0, color: '#f8fafc' }}>
                  Payday Transfer Plan (§8.6)
                </h3>
                <div style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)' }}>
                  First 48-hour routing from Salary Vault to daily & wealth buckets
                </div>
              </div>
            </div>

            <button
              type="button"
              className="btn btn-ghost btn-xs"
              onClick={copyTransferSteps}
              style={{ color: 'var(--neon-cyan)', border: '1px solid rgba(0, 240, 255, 0.25)' }}
            >
              {copiedPlan ? <CheckCircle2 size={12} /> : <Copy size={12} />}
              {copiedPlan ? 'Copied Steps!' : 'Copy Plan'}
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {/* Source Vault */}
            <div style={{ padding: '8px 12px', borderRadius: 'var(--radius-sm)', background: 'rgba(245, 158, 11, 0.08)', border: '1px solid rgba(245, 158, 11, 0.25)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.78rem' }}>
                <span style={{ fontWeight: 800, color: 'var(--neon-amber)' }}>SOURCE VAULT:</span>
                <span style={{ color: '#f8fafc', fontWeight: 700 }}>{vaultAccount?.name || 'Salary Bank Account'}</span>
              </div>
              <span className="badge badge-amber">Do Not Link Daily UPI</span>
            </div>

            {/* Transfer Steps */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 8 }}>
              {/* Step 1: Emergency Reserve */}
              <div style={{ padding: '10px 12px', borderRadius: 'var(--radius-sm)', background: 'rgba(255, 255, 255, 0.02)', border: '1px solid var(--glass-border)' }}>
                <div style={{ fontSize: '0.72rem', color: 'var(--neon-emerald)', fontWeight: 700, textTransform: 'uppercase' }}>
                  1. To Emergency Reserve
                </div>
                <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.05rem', fontWeight: 800, color: '#f8fafc', marginTop: 2 }}>
                  {formatMoney(emergencyTransferPaise, currency, masked)}
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                  Target: {emergencyAccount?.name || 'Auto-sweep / Liquid Account'}
                </div>
              </div>

              {/* Step 2: Freedom SIP */}
              <div style={{ padding: '10px 12px', borderRadius: 'var(--radius-sm)', background: 'rgba(255, 255, 255, 0.02)', border: '1px solid var(--glass-border)' }}>
                <div style={{ fontSize: '0.72rem', color: 'var(--neon-purple)', fontWeight: 700, textTransform: 'uppercase' }}>
                  2. To Wealth / SIP
                </div>
                <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.05rem', fontWeight: 800, color: '#f8fafc', marginTop: 2 }}>
                  {formatMoney(wealthTransferPaise, currency, masked)}
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                  Target: {wealthAccount?.name || 'Investment Account'}
                </div>
              </div>

              {/* Step 3: Weekly UPI Lite */}
              <div style={{ padding: '10px 12px', borderRadius: 'var(--radius-sm)', background: 'rgba(255, 255, 255, 0.02)', border: '1px solid var(--glass-border)' }}>
                <div style={{ fontSize: '0.72rem', color: 'var(--neon-cyan)', fontWeight: 700, textTransform: 'uppercase' }}>
                  3. Weekly UPI Lite Reload
                </div>
                <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.05rem', fontWeight: 800, color: '#f8fafc', marginTop: 2 }}>
                  {formatMoney(weeklyUpiReloadPaise, currency, masked)}
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                  Target: {dailyUpiAccount?.name || 'Daily UPI Account'}
                </div>
              </div>

              {/* Step 4: Retained in Vault */}
              <div style={{ padding: '10px 12px', borderRadius: 'var(--radius-sm)', background: 'rgba(255, 255, 255, 0.02)', border: '1px solid var(--glass-border)' }}>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)', fontWeight: 700, textTransform: 'uppercase' }}>
                  4. Retain in Vault (Bills)
                </div>
                <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.05rem', fontWeight: 800, color: '#f8fafc', marginTop: 2 }}>
                  {formatMoney(baselinePaise, currency, masked)}
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                  Stays in Vault for Rent & EMI debits
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Configurable UPI Lite Settings Card (§8.6) */}
        <div className="cyber-card" style={{ padding: 20, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Zap size={18} style={{ color: 'var(--neon-cyan)' }} />
                <h3 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.05rem', fontWeight: 800, margin: 0, color: '#f8fafc' }}>
                  UPI Lite Settings
                </h3>
              </div>
              <button
                type="button"
                className="btn btn-ghost btn-xs"
                onClick={() => setEditingUpiLite(!editingUpiLite)}
                style={{ color: 'var(--neon-cyan)' }}
              >
                <Sliders size={12} /> {editingUpiLite ? 'Close' : 'Configure'}
              </button>
            </div>

            <p style={{ fontSize: '0.76rem', color: 'var(--text-tertiary)', margin: '0 0 12px 0' }}>
              RBI limits must be configurable settings, not constants (§8.6). UPI Lite prevents PIN exhaustion and isolates daily micro-spends.
            </p>

            {editingUpiLite ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                <div>
                  <label className="form-label" style={{ fontSize: '0.74rem' }}>Per-Transaction Cap (₹)</label>
                  <input
                    type="number"
                    value={upiLitePerTxn}
                    onChange={e => setUpiLitePerTxn(Number(e.target.value))}
                    style={{ fontSize: '0.8rem', padding: '6px 10px' }}
                  />
                </div>
                <div>
                  <label className="form-label" style={{ fontSize: '0.74rem' }}>Wallet Balance Cap (₹)</label>
                  <input
                    type="number"
                    value={upiLiteBalanceCap}
                    onChange={e => setUpiLiteBalanceCap(Number(e.target.value))}
                    style={{ fontSize: '0.8rem', padding: '6px 10px' }}
                  />
                </div>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={() => handleSaveUpiLite(upiLitePerTxn, upiLiteBalanceCap)}
                >
                  Save UPI Lite Limits
                </button>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 10px', background: 'rgba(255, 255, 255, 0.02)', borderRadius: 'var(--radius-sm)' }}>
                  <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>Per-Txn Pinless Cap</span>
                  <span style={{ fontWeight: 800, fontSize: '0.84rem', color: 'var(--neon-cyan)' }}>₹{upiLitePerTxn}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 10px', background: 'rgba(255, 255, 255, 0.02)', borderRadius: 'var(--radius-sm)' }}>
                  <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>Max Balance Cap</span>
                  <span style={{ fontWeight: 800, fontSize: '0.84rem', color: 'var(--neon-cyan)' }}>₹{upiLiteBalanceCap}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 10px', background: 'rgba(255, 255, 255, 0.02)', borderRadius: 'var(--radius-sm)' }}>
                  <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>Suggested Weekly Reload</span>
                  <span style={{ fontWeight: 800, fontSize: '0.84rem', color: 'var(--neon-emerald)' }}>
                    {formatMoney(weeklyUpiReloadPaise, currency, masked)}
                  </span>
                </div>
              </div>
            )}
          </div>

          <div style={{ marginTop: 12, fontSize: '0.7rem', color: 'var(--text-tertiary)', display: 'flex', alignItems: 'center', gap: 4 }}>
            <Info size={12} /> Reload weekly to enforce automatic impulse guardrails
          </div>
        </div>
      </div>

      {/* Account Groups */}
      {computedAccounts.length === 0 ? (
        <div
          className="cyber-card"
          style={{
            padding: '48px 24px',
            textAlign: 'center',
            background: 'rgba(255, 255, 255, 0.02)',
            borderRadius: 'var(--radius-lg)',
            border: '1px dashed var(--glass-border-bright)',
          }}
        >
          <Building2 size={44} style={{ color: 'var(--neon-cyan)', marginBottom: 16, opacity: 0.8 }} />
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.35rem', fontWeight: 800, color: '#fff', marginBottom: 8 }}>
            No Accounts Added Yet
          </h2>
          <p style={{ color: 'var(--text-tertiary)', fontSize: '0.88rem', maxWidth: 480, margin: '0 auto 24px auto', lineHeight: 1.6 }}>
            Add your bank account, credit card, or cash wallet to start tracking your money.
          </p>

          <button
            className="btn btn-primary btn-lg"
            onClick={() => { setEditAccount(undefined); setShowModal(true); }}
            style={{ marginBottom: 28 }}
          >
            <Plus size={18} /> Add Your First Account
          </button>

          {/* Quick-add popular Indian banks */}
          <div style={{ maxWidth: 640, margin: '0 auto', textAlign: 'left' }}>
            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: 12, textAlign: 'center' }}>
              Or quick-add with 1 click:
            </div>
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', justifyContent: 'center' }}>
              {POPULAR_BANKS.map(bank => (
                <button
                  key={bank.id}
                  type="button"
                  onClick={async () => {
                    const newId = 'acc_' + bank.id + '_' + Date.now();
                    await addAccount({
                      id: newId,
                      name: bank.sampleName,
                      type: bank.defaultType,
                      account_role: bank.id === 'axis' ? 'INCOME_VAULT' : 'DAILY_UPI_SPEND',
                      institution: bank.name,
                      last_four: '0000',
                      opening_balance_minor: 0,
                      currency,
                      include_in_net_worth: true,
                      include_in_stats: true,
                      archived: false,
                      created_at: Date.now(),
                      updated_at: Date.now(),
                    });
                  }}
                  style={{
                    padding: '8px 14px',
                    borderRadius: 'var(--radius-pill)',
                    fontSize: '0.82rem',
                    fontWeight: 600,
                    border: `1px solid ${bank.color}55`,
                    background: bank.badgeBg,
                    color: bank.color,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 6,
                    transition: 'all 0.2s ease',
                  }}
                  onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-2px)'; }}
                  onMouseLeave={e => { e.currentTarget.style.transform = 'translateY(0)'; }}
                >
                  <Plus size={14} /> {bank.name}
                </button>
              ))}
            </div>
          </div>
        </div>
      ) : (
        Object.entries(groupedByType).map(([type, accs]) => (
        <div key={type}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
            <span style={{ fontFamily: 'var(--font-outfit)', fontSize: '0.92rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
              {ACCOUNT_TYPE_LABELS[type as AccountType]}
            </span>
            <span className="badge badge-cyan">{accs.length}</span>
          </div>

          <div className="responsive-grid-cards">
            {accs.map(acc => {
              const balanceMinor = acc.current_balance_minor || 0;
              const isPositive = balanceMinor >= 0;
              const accentColor = getBankColor(acc.institution, acc.type);
              const isVault = acc.account_role === 'INCOME_VAULT';
              const roleMeta = ACCOUNT_ROLES.find(r => r.id === acc.account_role);

              return (
                <Tilt3DCard key={acc.id}>
                  <div
                    className="cyber-card"
                    style={{
                      height: '100%',
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      borderLeft: `4px solid ${isVault ? 'var(--neon-amber)' : accentColor}`,
                      boxShadow: isVault
                        ? '0 10px 30px -10px rgba(245, 158, 11, 0.3)'
                        : `0 10px 30px -10px ${accentColor}25`,
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                          <span className="badge" style={{ background: `${accentColor}18`, color: accentColor, border: `1px solid ${accentColor}33` }}>
                            {ACCOUNT_TYPE_LABELS[acc.type]}
                          </span>

                          {/* Role Badge (§8.6) */}
                          {roleMeta && (
                            <span
                              className="badge"
                              style={{
                                background: `${roleMeta.color}22`,
                                color: roleMeta.color,
                                border: `1px solid ${roleMeta.color}55`,
                                fontWeight: 800,
                              }}
                            >
                              {roleMeta.badge}
                            </span>
                          )}

                          {acc.institution && (
                            <span className="badge" style={{ background: `${accentColor}22`, color: accentColor, border: `1px solid ${accentColor}44`, fontSize: '0.7rem', fontWeight: 700 }}>
                              {acc.institution}
                            </span>
                          )}
                        </div>
                        <div style={{ display: 'flex', gap: 4, flexShrink: 0 }}>
                          <button
                            type="button"
                            className="btn-ghost btn-sm"
                            style={{ padding: 6, borderRadius: 'var(--radius-sm)', color: 'var(--neon-cyan)' }}
                            onClick={() => { setEditAccount(acc); setShowModal(true); }}
                          >
                            <Edit2 size={13} />
                          </button>
                          <button
                            type="button"
                            className="btn-ghost btn-sm"
                            style={{ padding: 6, borderRadius: 'var(--radius-sm)', color: 'var(--neon-rose)' }}
                            onClick={() => { if (confirm(`Archive ${acc.name}?`)) deleteAccount(acc.id); }}
                          >
                            <Archive size={13} />
                          </button>
                        </div>
                      </div>

                      <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.15rem', fontWeight: 800, marginTop: 12, color: 'var(--text-primary)' }}>
                        {acc.name}
                      </div>

                      {acc.institution && (
                        <div style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)', marginTop: 2, display: 'flex', alignItems: 'center', gap: 6 }}>
                          <span>{acc.institution}</span>
                          {acc.last_four && <span>•••• {acc.last_four}</span>}
                        </div>
                      )}

                      {/* Vault Isolation Warning Banner (§8.6) */}
                      {isVault && (
                        <div
                          style={{
                            marginTop: 10,
                            padding: '6px 10px',
                            background: 'rgba(245, 158, 11, 0.12)',
                            border: '1px solid rgba(245, 158, 11, 0.3)',
                            borderRadius: 'var(--radius-sm)',
                            fontSize: '0.7rem',
                            color: '#fef3c7',
                            lineHeight: 1.35,
                          }}
                        >
                          ⚠️ <strong>Vault Protected:</strong> Do not link daily UPI apps here (GPay, PhonePe).
                        </div>
                      )}
                    </div>

                    <div style={{ marginTop: 20 }}>
                      <div
                        style={{
                          fontFamily: 'var(--font-outfit)',
                          fontSize: '1.6rem',
                          fontWeight: 900,
                          letterSpacing: '-0.02em',
                          color: isPositive ? 'var(--text-primary)' : 'var(--neon-rose)',
                        }}
                      >
                        {formatMoney(balanceMinor, acc.currency, masked)}
                      </div>
                      {!acc.include_in_net_worth && (
                        <div style={{ fontSize: '0.68rem', color: 'var(--text-tertiary)', marginTop: 4 }}>
                          Excluded from net worth
                        </div>
                      )}
                    </div>
                  </div>
                </Tilt3DCard>
              );
            })}
          </div>
        </div>
      )))}

      {showModal && <AccountModal account={editAccount} onClose={() => setShowModal(false)} />}
      {showNightlyModal && <NightlyCheckInModal onClose={() => setShowNightlyModal(false)} />}
    </div>
  );
}

