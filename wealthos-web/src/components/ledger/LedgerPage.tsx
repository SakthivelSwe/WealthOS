import { useState, useMemo } from 'react';
import { useData } from '../../hooks/useData';
import { formatMoney, parseMoney, getCategoryDisplayName, formatAccountLabel, type Transaction, type TxnType, type NeedWant } from '../../types';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { Plus, Search, Edit2, Trash2, ArrowUpRight, ArrowDownRight, ArrowLeftRight, Clock, X, Check } from 'lucide-react';
import { format } from 'date-fns';

const PAGE_SIZE = 40;

function TxnModal({ txn, onClose }: { txn?: Transaction; onClose: () => void }) {
  const { addTransaction, updateTransaction, accounts, categories, settings } = useData();
  const currency = settings?.currency || 'INR';
  const [type, setType] = useState<TxnType>(txn?.type || 'EXPENSE');
  const [amount, setAmount] = useState(txn ? String(txn.amount_minor / 100) : '');
  const [date, setDate] = useState(txn?.local_date || format(new Date(), 'yyyy-MM-dd'));
  const [time, setTime] = useState(txn?.local_time || format(new Date(), 'HH:mm'));
  const [accountId, setAccountId] = useState(txn?.account_id || accounts[0]?.id || '');
  const [toAccountId, setToAccountId] = useState(txn?.to_account_id || '');
  const [categoryId, setCategoryId] = useState(txn?.category_id || '');
  const [merchant, setMerchant] = useState(txn?.merchant || '');
  const [description, setDescription] = useState(txn?.description || '');
  const [paymentMethod, setPaymentMethod] = useState(txn?.payment_method || 'UPI');
  const [needWant, setNeedWant] = useState<NeedWant | ''>(txn?.need_want || 'NEED');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const filteredCategories = categories.filter(c => type === 'INCOME' ? c.kind === 'INCOME' : c.kind === 'EXPENSE');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!amount || parseFloat(amount) <= 0) { setError('Enter a valid amount'); return; }
    if (!accountId) { setError('Select an account'); return; }
    setLoading(true);
    const amountMinor = parseMoney(amount, currency);
    const now = Date.now();
    const id = txn?.id || ('txn_' + Date.now());

    if (txn) {
      await updateTransaction(txn.id, {
        type, amount_minor: amountMinor, local_date: date, local_time: time,
        account_id: accountId, to_account_id: toAccountId || null,
        category_id: categoryId || null, merchant: merchant || null,
        description: description || null, notes: null,
        payment_method: paymentMethod || null, need_want: needWant as NeedWant || null,
        updated_at: now,
      });
    } else {
      await addTransaction({
        id, type, amount_minor: amountMinor, currency,
        local_date: date, local_time: time, account_id: accountId,
        to_account_id: type === 'TRANSFER' ? toAccountId : null,
        category_id: categoryId || null, merchant: merchant || null,
        description: description || null, notes: null,
        payment_method: paymentMethod || null, need_want: needWant as NeedWant || null,
        tags: [], imported: false, source: 'WEB_MANUAL',
        created_at: now, updated_at: now, deleted_at: null,
      }, [
        { txn_id: id, account_id: accountId, category_id: null, amount_minor: type === 'INCOME' ? amountMinor : -amountMinor },
        { txn_id: id, account_id: type === 'TRANSFER' ? toAccountId : null, category_id: categoryId || null, amount_minor: type === 'INCOME' ? -amountMinor : amountMinor },
      ]);
    }
    setLoading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800 }}>
            {txn ? 'Edit Transaction' : 'Record Transaction'}
          </h2>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          {error && <div style={{ color: 'var(--neon-rose)', fontSize: '0.82rem', marginBottom: 14 }}>{error}</div>}

          {/* Type Selector */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, marginBottom: 16 }}>
            {(['EXPENSE', 'INCOME', 'TRANSFER'] as TxnType[]).map(t => (
              <button
                key={t}
                type="button"
                onClick={() => setType(t)}
                style={{
                  padding: '10px 8px',
                  borderRadius: 'var(--radius-md)',
                  background: type === t ? 'rgba(255, 255, 255, 0.12)' : 'rgba(255, 255, 255, 0.03)',
                  border: type === t ? '1px solid var(--neon-cyan)' : '1px solid var(--glass-border)',
                  color: type === t ? 'var(--neon-cyan)' : 'var(--text-secondary)',
                  fontWeight: 700,
                  fontSize: '0.82rem',
                  cursor: 'pointer',
                }}
              >
                {t}
              </button>
            ))}
          </div>

          <div className="form-group">
            <label className="form-label">Amount ({currency})</label>
            <input
              type="number"
              step="any"
              value={amount}
              onChange={e => setAmount(e.target.value)}
              placeholder="0.00"
              autoFocus
              style={{
                fontFamily: 'var(--font-outfit)',
                fontSize: '1.35rem',
                fontWeight: 800,
                color: type === 'EXPENSE' ? 'var(--neon-rose)' : type === 'INCOME' ? 'var(--neon-emerald)' : 'var(--neon-cyan)',
              }}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Account</label>
              <select value={accountId} onChange={e => setAccountId(e.target.value)}>
                {accounts.map(a => <option key={a.id} value={a.id}>{formatAccountLabel(a)}</option>)}
              </select>
            </div>
            {type === 'TRANSFER' ? (
              <div className="form-group">
                <label className="form-label">Destination Account</label>
                <select value={toAccountId} onChange={e => setToAccountId(e.target.value)}>
                  <option value="">Select destination...</option>
                  {accounts.filter(a => a.id !== accountId).map(a => <option key={a.id} value={a.id}>{formatAccountLabel(a)}</option>)}
                </select>
              </div>
            ) : (
              <div className="form-group">
                <label className="form-label">Category</label>
                <select value={categoryId} onChange={e => setCategoryId(e.target.value)}>
                  <option value="">Select category...</option>
                  {filteredCategories.map(c => <option key={c.id} value={c.id}>{getCategoryDisplayName(c.id, categories)}</option>)}
                </select>
              </div>
            )}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Merchant / Description</label>
              <input value={merchant} onChange={e => setMerchant(e.target.value)} placeholder="e.g. Swiggy, Apple Store" />
            </div>
            <div className="form-group">
              <label className="form-label">Date</label>
              <input type="date" value={date} onChange={e => setDate(e.target.value)} />
            </div>
          </div>

          <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} style={{ flex: 1 }}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading} style={{ flex: 2 }}>
              <Check size={16} /> Save Record
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export function LedgerPage() {
  const { transactions, accounts, categories, deleteTransaction, settings } = useData();
  const [search, setSearch] = useState('');
  const [filterType, setFilterType] = useState<string>('ALL');
  const [filterAccount, setFilterAccount] = useState<string>('');
  const [filterCategory, setFilterCategory] = useState<string>('');
  const [page, setPage] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [editTxn, setEditTxn] = useState<Transaction | undefined>();

  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;

  const filtered = useMemo(() => {
    let txns = transactions.filter(t => !t.deleted_at);
    if (filterType !== 'ALL') txns = txns.filter(t => t.type === filterType);
    if (filterAccount) txns = txns.filter(t => t.account_id === filterAccount);
    if (filterCategory) txns = txns.filter(t => t.category_id === filterCategory);
    if (search) {
      const q = search.toLowerCase();
      txns = txns.filter(t =>
        t.merchant?.toLowerCase().includes(q) ||
        t.description?.toLowerCase().includes(q)
      );
    }
    return txns;
  }, [transactions, filterType, filterAccount, filterCategory, search]);

  const paginated = filtered.slice(0, (page + 1) * PAGE_SIZE);
  const totalExpense = filtered.filter(t => t.type === 'EXPENSE').reduce((s, t) => s + t.amount_minor, 0);
  const totalIncome = filtered.filter(t => t.type === 'INCOME').reduce((s, t) => s + t.amount_minor, 0);

  // Group by date
  const grouped = useMemo(() => {
    const groups: Record<string, typeof filtered> = {};
    paginated.forEach(txn => {
      const d = txn.local_date;
      if (!groups[d]) groups[d] = [];
      groups[d].push(txn);
    });
    return Object.entries(groups).sort((a, b) => b[0].localeCompare(a[0]));
  }, [paginated]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
            Financial Ledger
          </h1>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
            Synchronized journal entries, categorized cashflows, and live double-entry records
          </p>
        </div>
        <button
          className="btn btn-primary"
          onClick={() => { setEditTxn(undefined); setShowModal(true); }}
        >
          <Plus size={16} /> New Transaction
        </button>
      </div>

      {/* 3D Summary Cards */}
      <div className="bento-grid">
        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-emerald)' }} />
              <div className="stat-label">
                <span>Total Inflow</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-emerald-glow)', color: 'var(--neon-emerald)' }}>
                  <ArrowDownRight size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-emerald)' }}>
                +{formatMoney(totalIncome, currency, masked)}
              </div>
              <div className="stat-trend positive">{filtered.filter(t => t.type === 'INCOME').length} credits</div>
            </div>
          </Tilt3DCard>
        </div>

        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-rose)' }} />
              <div className="stat-label">
                <span>Total Outflow</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-rose-glow)', color: 'var(--neon-rose)' }}>
                  <ArrowUpRight size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-rose)' }}>
                -{formatMoney(totalExpense, currency, masked)}
              </div>
              <div className="stat-trend negative">{filtered.filter(t => t.type === 'EXPENSE').length} debits</div>
            </div>
          </Tilt3DCard>
        </div>

        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-cyan)' }} />
              <div className="stat-label">
                <span>Net Cashflow</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-cyan-glow)', color: 'var(--neon-cyan)' }}>
                  <ArrowLeftRight size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: totalIncome - totalExpense >= 0 ? 'var(--neon-cyan)' : 'var(--neon-rose)' }}>
                {formatMoney(totalIncome - totalExpense, currency, masked)}
              </div>
              <div className="stat-trend neutral">Net margin across selected query</div>
            </div>
          </Tilt3DCard>
        </div>
      </div>

      {/* Cyber Filter Bar */}
      <div className="cyber-card" style={{ padding: '16px 20px' }}>
        <div style={{ display: 'flex', gap: 14, flexWrap: 'wrap', alignItems: 'center' }}>
          {/* Search Box */}
          <div style={{ position: 'relative', flex: 1, minWidth: 220 }}>
            <Search size={16} style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-tertiary)' }} />
            <input
              placeholder="Search merchant, tags, notes..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              style={{ paddingLeft: 42 }}
            />
          </div>

          {/* Type Selector Pills */}
          <div style={{ display: 'flex', gap: 4 }}>
            {['ALL', 'EXPENSE', 'INCOME', 'TRANSFER'].map(t => (
              <button
                key={t}
                onClick={() => setFilterType(t)}
                className={`btn btn-sm ${filterType === t ? 'btn-primary' : 'btn-ghost'}`}
                style={{ fontSize: '0.78rem' }}
              >
                {t}
              </button>
            ))}
          </div>

          {/* Account Filter */}
          <select value={filterAccount} onChange={e => setFilterAccount(e.target.value)} style={{ width: 170 }}>
            <option value="">All Vaults</option>
            {accounts.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
          </select>

          {/* Category Filter */}
          <select value={filterCategory} onChange={e => setFilterCategory(e.target.value)} style={{ width: 170 }}>
            <option value="">All Categories</option>
            {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
      </div>

      {/* Grouped Timeline Stream */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
        {grouped.map(([dateStr, txns]) => (
          <div key={dateStr}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
              <div style={{ width: 8, height: 8, borderRadius: '50%', background: 'var(--neon-cyan)', boxShadow: '0 0 8px var(--neon-cyan)' }} />
              <span style={{ fontFamily: 'var(--font-outfit)', fontWeight: 800, fontSize: '0.95rem', color: 'var(--text-secondary)' }}>
                {dateStr}
              </span>
              <span className="badge badge-cyan">{txns.length} entries</span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {txns.map(txn => {
                const isExpense = txn.type === 'EXPENSE';
                const isIncome = txn.type === 'INCOME';
                const sign = isExpense ? '-' : isIncome ? '+' : '';
                const color = isExpense ? 'var(--neon-rose)' : isIncome ? 'var(--neon-emerald)' : 'var(--neon-cyan)';
                const acc = accounts.find(a => a.id === txn.account_id);
                const cat = categories.find(c => c.id === txn.category_id);

                return (
                  <div
                    key={txn.id}
                    className="cyber-card"
                    style={{
                      padding: '12px 18px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      borderRadius: 'var(--radius-md)',
                      transition: 'transform 0.15s ease',
                    }}
                    onMouseEnter={e => { e.currentTarget.style.transform = 'translateX(4px)'; }}
                    onMouseLeave={e => { e.currentTarget.style.transform = 'translateX(0)'; }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                      <div
                        style={{
                          width: 38,
                          height: 38,
                          borderRadius: 10,
                          background: isExpense ? 'var(--neon-rose-glow)' : isIncome ? 'var(--neon-emerald-glow)' : 'var(--neon-cyan-glow)',
                          color,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          fontWeight: 800,
                        }}
                      >
                        {isExpense ? <ArrowUpRight size={18} /> : isIncome ? <ArrowDownRight size={18} /> : <ArrowLeftRight size={18} />}
                      </div>

                      <div>
                        <div style={{ fontWeight: 700, fontSize: '0.95rem', color: 'var(--text-primary)' }}>
                          {txn.merchant || txn.description || 'Ledger Entry'}
                        </div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', display: 'flex', alignItems: 'center', gap: 8, marginTop: 2 }}>
                          <span>{acc?.name || 'Vault'}</span>
                          {txn.category_id && <span>• {getCategoryDisplayName(txn.category_id, categories)}</span>}
                          {txn.payment_method && <span>• {txn.payment_method}</span>}
                          {txn.need_want && (
                            <span className={`badge ${txn.need_want === 'NEED' ? 'badge-cyan' : txn.need_want === 'WANT' ? 'badge-purple' : 'badge-green'}`}>
                              {txn.need_want}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                      <div
                        style={{
                          fontFamily: 'var(--font-outfit)',
                          fontWeight: 800,
                          fontSize: '1.2rem',
                          color,
                          letterSpacing: '-0.01em',
                        }}
                      >
                        {sign}{formatMoney(txn.amount_minor, currency, masked)}
                      </div>

                      <div style={{ display: 'flex', gap: 4 }}>
                        <button
                          type="button"
                          className="btn-ghost btn-sm"
                          style={{ padding: 6, borderRadius: 'var(--radius-sm)' }}
                          onClick={() => { setEditTxn(txn); setShowModal(true); }}
                        >
                          <Edit2 size={13} />
                        </button>
                        <button
                          type="button"
                          className="btn-ghost btn-sm"
                          style={{ padding: 6, borderRadius: 'var(--radius-sm)', color: 'var(--neon-rose)' }}
                          onClick={() => { if (confirm('Delete this ledger record?')) deleteTransaction(txn.id); }}
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ))}

        {grouped.length === 0 && (
          <div className="cyber-card" style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-tertiary)' }}>
            <Clock size={32} style={{ margin: '0 auto 12px auto', opacity: 0.5 }} />
            <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.1rem', fontWeight: 700 }}>No Transactions Found</div>
            <p style={{ fontSize: '0.82rem', marginTop: 4 }}>Adjust your search filter or record a new transaction.</p>
          </div>
        )}
      </div>

      {showModal && <TxnModal txn={editTxn} onClose={() => setShowModal(false)} />}
    </div>
  );
}
