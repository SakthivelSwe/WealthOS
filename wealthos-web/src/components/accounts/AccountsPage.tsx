import { useState, useMemo } from 'react';
import { useData } from '../../hooks/useData';
import { formatMoney, parseMoney, ACCOUNT_TYPE_LABELS, POPULAR_BANKS, getBankColor, type Account, type AccountType } from '../../types';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { Plus, Edit2, Archive, Wallet, TrendingUp, CreditCard, X, Shield, Building2 } from 'lucide-react';

const ACCOUNT_TYPES: AccountType[] = ['BANK','CASH','UPI_WALLET','SAVINGS','CREDIT_CARD','INVESTMENT','LOAN','OTHER_ASSET','OTHER_LIABILITY'];
const ACCOUNT_TYPE_COLORS: Record<string, string> = {
  BANK: 'var(--neon-cyan)',
  CASH: 'var(--neon-emerald)',
  UPI_WALLET: 'var(--neon-indigo)',
  SAVINGS: 'var(--neon-cyan)',
  CREDIT_CARD: 'var(--neon-amber)',
  INVESTMENT: 'var(--neon-purple)',
  LOAN: 'var(--neon-rose)',
  OTHER_ASSET: 'var(--neon-cyan)',
  OTHER_LIABILITY: 'var(--neon-rose)',
};

function AccountModal({ account, onClose }: { account?: Account; onClose: () => void }) {
  const { addAccount, updateAccount, settings } = useData();
  const [name, setName] = useState(account?.name || '');
  const [type, setType] = useState<AccountType>(account?.type || 'BANK');
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
    if (account) {
      await updateAccount(account.id, { name: name.trim(), type, institution: institution || null, last_four: lastFour || null, include_in_net_worth: includeNW, updated_at: now });
    } else {
      await addAccount({
        id: Math.random().toString(36).substring(2) + Date.now(),
        name: name.trim(), type,
        institution: institution || null,
        last_four: lastFour || null,
        opening_balance_minor: parseMoney(balance || '0', currency),
        currency,
        include_in_net_worth: includeNW,
        include_in_stats: true,
        archived: false,
        created_at: now, updated_at: now,
      });
    }
    setLoading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800 }}>
            {account ? 'Edit Vault Entity' : 'Add New Vault Account'}
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

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Asset Classification</label>
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
              <Building2 size={13} style={{ color: 'var(--neon-cyan)' }} /> Popular Bank / Institution Presets
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
                      if (!name || name === 'HDFC Salary Account' || name.includes('Account')) {
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
  const { accounts, journalEntries, transactions, addAccount, deleteAccount, settings } = useData();
  const [showModal, setShowModal] = useState(false);
  const [editAccount, setEditAccount] = useState<Account | undefined>();
  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;

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

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
            Vaults & Portfolios
          </h1>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
            Manage liquid bank accounts, physical cash, credit lines, and investments
          </p>
        </div>
        <button
          className="btn btn-primary"
          onClick={() => { setEditAccount(undefined); setShowModal(true); }}
        >
          <Plus size={16} /> Add Vault Account
        </button>
      </div>

      {/* Top 3D Spatial Stat Cards */}
      <div className="bento-grid">
        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-cyan)' }} />
              <div className="stat-label">
                <span>Aggregate Net Worth</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-cyan-glow)', color: 'var(--neon-cyan)' }}>
                  <Wallet size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-cyan)' }}>
                {formatMoney(netWorth, currency, masked)}
              </div>
              <div className="stat-trend neutral">All active assets minus liabilities</div>
            </div>
          </Tilt3DCard>
        </div>

        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-emerald)' }} />
              <div className="stat-label">
                <span>Total Assets</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-emerald-glow)', color: 'var(--neon-emerald)' }}>
                  <TrendingUp size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-emerald)' }}>
                {formatMoney(totalAssets, currency, masked)}
              </div>
              <div className="stat-trend positive">Positive balances & reserves</div>
            </div>
          </Tilt3DCard>
        </div>

        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-rose)' }} />
              <div className="stat-label">
                <span>Total Liabilities</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-rose-glow)', color: 'var(--neon-rose)' }}>
                  <CreditCard size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-rose)' }}>
                {formatMoney(Math.abs(totalLiabilities), currency, masked)}
              </div>
              <div className="stat-trend negative">Credit cards & debt balances</div>
            </div>
          </Tilt3DCard>
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
            No Vault Accounts Added Yet
          </h2>
          <p style={{ color: 'var(--text-tertiary)', fontSize: '0.88rem', maxWidth: 480, margin: '0 auto 24px auto', lineHeight: 1.6 }}>
            Your ledger starts fresh today. Connect your first primary bank account, credit card, cash wallet, or savings account to start monitoring balances.
          </p>

          <button
            className="btn btn-primary btn-lg"
            onClick={() => { setEditAccount(undefined); setShowModal(true); }}
            style={{ marginBottom: 28 }}
          >
            <Plus size={18} /> Add Your First Vault Account
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

              return (
                <Tilt3DCard key={acc.id}>
                  <div
                    className="cyber-card"
                    style={{
                      height: '100%',
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      borderLeft: `4px solid ${accentColor}`,
                      boxShadow: `0 10px 30px -10px ${accentColor}25`,
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                          <span className="badge" style={{ background: `${accentColor}18`, color: accentColor, border: `1px solid ${accentColor}33` }}>
                            {ACCOUNT_TYPE_LABELS[acc.type]}
                          </span>
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
    </div>
  );
}
