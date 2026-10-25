import { useState, useMemo } from 'react';
import { useData } from '../../hooks/useData';
import { formatMoney, parseMoney, getCategoryDisplayName, formatAccountLabel, type Transaction, type TxnType, type NeedWant } from '../../types';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import {
  Plus,
  Search,
  Edit2,
  Trash2,
  ArrowUpRight,
  ArrowDownRight,
  ArrowLeftRight,
  Clock,
  X,
  Check,
  Upload,
  HeartCrack,
  Activity,
  Flame,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { format } from 'date-fns';
import { CategoryPicker } from '../shared/CategoryPicker';
import { ImportStatementModal } from './ImportStatementModal';
import { CycleForensicsCard } from './CycleForensicsCard';
import { JoylessLeakModal } from './JoylessLeakModal';

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
  const [categoryId, setCategoryId] = useState(txn?.category_id === 'cat_salary' ? 'cat_income_0' : (txn?.category_id || ''));
  const [merchant, setMerchant] = useState(txn?.merchant || '');
  const [description, setDescription] = useState(txn?.description || '');
  const [paymentMethod, setPaymentMethod] = useState(txn?.payment_method || 'UPI');
  const [needWant, setNeedWant] = useState<NeedWant | ''>(txn?.need_want || 'NEED');
  const [spendEmotionTag, setSpendEmotionTag] = useState<string>((txn as any)?.spend_emotion_tag || 'NEED');
  const [paymentRail, setPaymentRail] = useState<string>((txn as any)?.payment_rail || 'UPI_P2M');
  const [rrn, setRrn] = useState<string>((txn as any)?.rrn || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const filteredCategories = useMemo(() => {
    const list = categories.filter(c => (type === 'INCOME' ? c.kind === 'INCOME' : c.kind === 'EXPENSE') && !c.archived);
    const seen = new Set<string>();
    return list.filter(c => {
      if (c.id === 'cat_salary' && list.some(o => o.id === 'cat_income_0')) return false;
      const label = getCategoryDisplayName(c.id, categories).trim();
      if (seen.has(label)) return false;
      seen.add(label);
      return true;
    });
  }, [categories, type]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!amount || parseFloat(amount) <= 0) { setError('Enter a valid amount'); return; }
    if (!accountId) { setError('Select an account'); return; }
    setLoading(true);
    const amountMinor = parseMoney(amount, currency);
    const now = Date.now();
    const id = txn?.id || ('txn_' + Date.now());

    if (txn) {
      const updatedEntries = [
        { txn_id: txn.id, account_id: accountId, category_id: null, amount_minor: type === 'INCOME' ? amountMinor : -amountMinor },
        { txn_id: txn.id, account_id: type === 'TRANSFER' ? toAccountId : null, category_id: categoryId || null, amount_minor: type === 'INCOME' ? -amountMinor : amountMinor },
      ];
      await updateTransaction(txn.id, {
        type, amount_minor: amountMinor, local_date: date, local_time: time,
        account_id: accountId, to_account_id: type === 'TRANSFER' ? toAccountId : null,
        category_id: type !== 'TRANSFER' ? (categoryId || null) : null, merchant: merchant || null,
        description: description || null, notes: null,
        payment_method: paymentMethod || null, need_want: type === 'EXPENSE' ? (needWant as NeedWant || null) : null,
        updated_at: now,
        // Spec §4 & §6 fields
        rrn: rrn || null,
        payment_rail: paymentRail || null,
        spend_emotion_tag: type === 'EXPENSE' ? spendEmotionTag : (type === 'INCOME' ? 'INCOME' : 'UNPLANNED_TRANSFER'),
        user_overridden: true,
      } as any, updatedEntries);
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
        rrn: rrn || null,
        payment_rail: paymentRail || null,
        spend_emotion_tag: type === 'EXPENSE' ? spendEmotionTag : (type === 'INCOME' ? 'INCOME' : 'UNPLANNED_TRANSFER'),
        user_overridden: true,
      } as any, [
        { txn_id: id, account_id: accountId, category_id: null, amount_minor: type === 'INCOME' ? amountMinor : -amountMinor },
        { txn_id: id, account_id: type === 'TRANSFER' ? toAccountId : null, category_id: categoryId || null, amount_minor: type === 'INCOME' ? -amountMinor : amountMinor },
      ]);
    }
    setLoading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: 540 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800 }}>
            {txn ? 'Edit Transaction' : 'Record Transaction'}
          </h2>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          {error && <div style={{ color: 'var(--semantic-expense, #f43f5e)', fontSize: '0.82rem', marginBottom: 14 }}>{error}</div>}

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
                  border: type === t ? '1px solid var(--border-default)' : '1px solid var(--border-subtle)',
                  color: type === t ? 'var(--text-primary)' : 'var(--text-secondary)',
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
            <label className="form-label">Amount ({currency}) *</label>
            <input
              type="number"
              step="any"
              placeholder="0.00"
              value={amount}
              onChange={e => setAmount(e.target.value)}
              required
              autoFocus
              style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'var(--font-outfit)' }}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Date</label>
              <input type="date" value={date} onChange={e => setDate(e.target.value)} required />
            </div>
            <div className="form-group">
              <label className="form-label">Account *</label>
              <select value={accountId} onChange={e => setAccountId(e.target.value)} required>
                {accounts.map(a => <option key={a.id} value={a.id}>{formatAccountLabel(a)}</option>)}
              </select>
            </div>
          </div>

          {type === 'TRANSFER' && (
            <div className="form-group">
              <label className="form-label">Destination Account *</label>
              <select value={toAccountId} onChange={e => setToAccountId(e.target.value)} required>
                <option value="">Select recipient account</option>
                {accounts.filter(a => a.id !== accountId).map(a => (
                  <option key={a.id} value={a.id}>{formatAccountLabel(a)}</option>
                ))}
              </select>
            </div>
          )}

          {type !== 'TRANSFER' && (
            <div className="form-group">
              <label className="form-label">Category</label>
              <select value={categoryId} onChange={e => setCategoryId(e.target.value)}>
                <option value="">Uncategorised</option>
                {filteredCategories.map(c => (
                  <option key={c.id} value={c.id}>{getCategoryDisplayName(c.id, categories)}</option>
                ))}
              </select>
            </div>
          )}

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Merchant / Payee</label>
              <input placeholder="e.g. Swiggy, Amazon, Tea Stall" value={merchant} onChange={e => setMerchant(e.target.value)} />
            </div>

            <div className="form-group">
              <label className="form-label">UPI Reference (RRN / UTR)</label>
              <input placeholder="12-digit RRN" value={rrn} onChange={e => setRrn(e.target.value)} />
            </div>
          </div>

          {/* Emotion Tag Picker for Expenses */}
          {type === 'EXPENSE' && (
            <div className="form-group">
              <label className="form-label">Psychology &amp; Emotion Tag (Video 12)</label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 6 }}>
                {[
                  { tag: 'NEED', label: '🛡️ Need' },
                  { tag: 'PLANNED_WANT', label: '✨ Planned Want' },
                  { tag: 'REGRET_LEAK', label: '💔 Joyless Leak' },
                ].map(item => (
                  <button
                    key={item.tag}
                    type="button"
                    onClick={() => {
                      setSpendEmotionTag(item.tag);
                      setNeedWant(item.tag === 'NEED' ? 'NEED' : 'WANT');
                    }}
                    style={{
                      padding: '8px 4px',
                      borderRadius: 'var(--radius-md)',
                      background: spendEmotionTag === item.tag ? 'rgba(255,255,255,0.12)' : 'rgba(255,255,255,0.02)',
                      border: spendEmotionTag === item.tag ? '1px solid var(--border-default)' : '1px solid var(--border-subtle)',
                      color: spendEmotionTag === item.tag ? '#fff' : 'var(--text-secondary)',
                      fontSize: '0.74rem',
                      fontWeight: 700,
                      cursor: 'pointer',
                    }}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}>
            <button type="button" className="btn btn-ghost" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              <Check size={16} /> Save Record
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export function LedgerPage() {
  const { transactions, accounts, categories, deleteTransaction, salaryCycles, settings } = useData();
  const [search, setSearch] = useState('');
  const [filterType, setFilterType] = useState<string>('ALL');
  const [filterAccount, setFilterAccount] = useState<string>('');
  const [filterCategory, setFilterCategory] = useState<string>('');
  const [filterEmotion, setFilterEmotion] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [showJoylessModal, setShowJoylessModal] = useState(false);
  const [showForensics, setShowForensics] = useState(true);
  const [editTxn, setEditTxn] = useState<Transaction | undefined>();

  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;

  const activeCycle = salaryCycles[0] || null;
  const cycleSalary = activeCycle?.salary_paise || 3220000;
  const cyclePlayJar = activeCycle?.jar_amounts?.PLAY || activeCycle?.jar_amounts?.WANTS || Math.floor(cycleSalary * 0.1);

  // 7-Day Velocity calculation
  const velocityMetrics = useMemo(() => {
    const cycleStart = activeCycle?.cycle_start_date || '2026-10-01';
    const startMs = new Date(cycleStart + 'T00:00:00Z').getTime();
    const sevenDaysEndMs = startMs + 6 * 24 * 60 * 60 * 1000;
    const sevenDaysEndDate = new Date(sevenDaysEndMs).toISOString().split('T')[0];

    const first7DaysSpend = transactions
      .filter(t => t.type === 'EXPENSE' && t.local_date >= cycleStart && t.local_date <= sevenDaysEndDate)
      .reduce((sum, t) => sum + t.amount_minor, 0);

    const velocityPct = cyclePlayJar > 0
      ? Math.min(100, Math.floor((first7DaysSpend * 100) / cyclePlayJar))
      : 0;

    return {
      first7DaysSpend,
      velocityPct,
      isAlert: velocityPct > 50,
    };
  }, [transactions, activeCycle, cyclePlayJar]);

  const uniqueFilterCategories = useMemo(() => {
    const seen = new Set<string>();
    return categories.filter(c => {
      if (c.id === 'cat_salary' && categories.some(o => o.id === 'cat_income_0')) return false;
      const label = (c.name || '').trim();
      if (seen.has(label)) return false;
      seen.add(label);
      return true;
    });
  }, [categories]);

  const filtered = useMemo(() => {
    let txns = transactions.filter(t => !t.deleted_at);
    if (filterType !== 'ALL') txns = txns.filter(t => t.type === filterType);
    if (filterAccount) txns = txns.filter(t => t.account_id === filterAccount);
    if (filterCategory) txns = txns.filter(t => t.category_id === filterCategory);
    if (filterEmotion !== 'ALL') {
      txns = txns.filter(t => (t as any).spend_emotion_tag === filterEmotion);
    }
    if (search) {
      const q = search.toLowerCase();
      txns = txns.filter(t =>
        t.merchant?.toLowerCase().includes(q) ||
        t.description?.toLowerCase().includes(q) ||
        (t as any).rrn?.toLowerCase().includes(q)
      );
    }
    return txns;
  }, [transactions, filterType, filterAccount, filterCategory, filterEmotion, search]);

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
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
            Transactions
          </h1>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
            All your income, expense, and transfer records with emotion tags &amp; bank reconciliation
          </p>
        </div>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <button
            className="btn btn-ghost"
            onClick={() => setShowForensics(!showForensics)}
            style={{ display: 'inline-flex', alignItems: 'center', gap: 6, color: 'var(--neon-cyan)' }}
          >
            <Activity size={16} /> {showForensics ? 'Hide Forensics' : 'Show Forensics'}
          </button>
          <button
            className="btn btn-secondary"
            onClick={() => setShowJoylessModal(true)}
            style={{ display: 'inline-flex', alignItems: 'center', gap: 6, color: 'var(--neon-rose)' }}
          >
            <HeartCrack size={16} /> Joyless Leak Review
          </button>
          <button
            className="btn btn-secondary"
            onClick={() => setShowImportModal(true)}
            style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
          >
            <Upload size={16} /> Import Statement
          </button>
          <button
            className="btn btn-primary"
            onClick={() => { setEditTxn(undefined); setShowModal(true); }}
          >
            <Plus size={16} /> New Transaction
          </button>
        </div>
      </div>

      {/* 7-Day Velocity Bar (Spec §5.14 & §8.4) */}
      <div
        className="cyber-card"
        style={{
          padding: '12px 18px',
          background: velocityMetrics.isAlert ? 'rgba(239, 68, 68, 0.08)' : 'rgba(6, 182, 212, 0.05)',
          border: velocityMetrics.isAlert ? '1px solid rgba(239, 68, 68, 0.3)' : '1px solid var(--glass-border)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {velocityMetrics.isAlert ? <Flame size={16} style={{ color: 'var(--neon-rose)' }} /> : <Activity size={16} style={{ color: 'var(--neon-cyan)' }} />}
            <span style={{ fontSize: '0.82rem', fontWeight: 700, color: '#f1f5f9' }}>
              7-Day Discretionary Velocity Indicator (Video 12)
            </span>
            {velocityMetrics.isAlert && (
              <span className="badge badge-rose" style={{ fontSize: '0.68rem' }}>
                Alert: &gt;50% spent in first 7 days
              </span>
            )}
          </div>
          <span style={{ fontFamily: 'var(--font-outfit)', fontSize: '0.85rem', fontWeight: 800, color: velocityMetrics.isAlert ? 'var(--neon-rose)' : 'var(--neon-cyan)' }}>
            {velocityMetrics.velocityPct}% of Play Budget Spent
          </span>
        </div>

        <div style={{ height: 6, borderRadius: 3, background: 'rgba(255,255,255,0.06)', overflow: 'hidden' }}>
          <div
            style={{
              height: '100%',
              width: `${Math.min(100, velocityMetrics.velocityPct)}%`,
              background: velocityMetrics.isAlert ? 'var(--neon-rose)' : 'var(--neon-cyan)',
              transition: 'width 0.4s ease',
            }}
          />
        </div>
      </div>

      {/* Cycle Forensics Card (§7) */}
      {showForensics && (
        <CycleForensicsCard onOpenJoylessReview={() => setShowJoylessModal(true)} />
      )}

      {/* 3D Summary Cards */}
      <div className="bento-grid">
        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-emerald)' }} />
              <div className="stat-label">
                <span>Total Income</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-emerald-glow)', color: 'var(--neon-emerald)' }}>
                  <ArrowDownRight size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-emerald)' }}>
                +{formatMoney(totalIncome, currency, masked)}
              </div>
              <div className="stat-trend positive">{filtered.filter(t => t.type === 'INCOME').length} income records</div>
            </div>
          </Tilt3DCard>
        </div>

        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-rose)' }} />
              <div className="stat-label">
                <span>Total Expenses</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-rose-glow)', color: 'var(--neon-rose)' }}>
                  <ArrowUpRight size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-rose)' }}>
                -{formatMoney(totalExpense, currency, masked)}
              </div>
              <div className="stat-trend negative">{filtered.filter(t => t.type === 'EXPENSE').length} expense records</div>
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
              <div className="stat-trend neutral">Remaining cashflow in selected view</div>
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
              placeholder="Search merchant, RRN, tags..."
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

          {/* Emotion Tag Filter */}
          <select value={filterEmotion} onChange={e => setFilterEmotion(e.target.value)} style={{ width: 150 }}>
            <option value="ALL">All Emotion Tags</option>
            <option value="NEED">🛡️ Needs</option>
            <option value="PLANNED_WANT">✨ Planned Wants</option>
            <option value="REGRET_LEAK">💔 Joyless Leaks</option>
            <option value="UNPLANNED_TRANSFER">💸 Transfers</option>
          </select>

          {/* Account Filter */}
          <select value={filterAccount} onChange={e => setFilterAccount(e.target.value)} style={{ width: 160 }}>
            <option value="">All Accounts</option>
            {accounts.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
          </select>

          {/* Category Filter */}
          <select value={filterCategory} onChange={e => setFilterCategory(e.target.value)} style={{ width: 160 }}>
            <option value="">All Categories</option>
            {uniqueFilterCategories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
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

                const emotionTag = (txn as any).spend_emotion_tag || (txn.need_want === 'NEED' ? 'NEED' : 'PLANNED_WANT');
                const paymentRail = (txn as any).payment_rail;
                const rrn = (txn as any).rrn;

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
                          {txn.merchant || txn.description || 'Transaction'}
                        </div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', display: 'flex', alignItems: 'center', gap: 8, marginTop: 4, flexWrap: 'wrap' }}>
                          <span>{acc?.name || 'Account'}</span>
                          {txn.category_id && <span>• {getCategoryDisplayName(txn.category_id, categories)}</span>}

                          {/* Payment Rail Badge */}
                          {paymentRail && (
                            <span className="badge badge-purple" style={{ fontSize: '0.68rem' }}>
                              {paymentRail}
                            </span>
                          )}

                          {/* Emotion Tag Badge */}
                          {isExpense && (
                            <span
                              className={`badge ${
                                emotionTag === 'REGRET_LEAK'
                                  ? 'badge-rose'
                                  : emotionTag === 'NEED'
                                  ? 'badge-cyan'
                                  : 'badge-purple'
                              }`}
                              style={{ fontSize: '0.68rem' }}
                            >
                              {emotionTag === 'REGRET_LEAK' ? '💔 JOYLESS' : emotionTag}
                            </span>
                          )}

                          {/* RRN Badge */}
                          {rrn && (
                            <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)' }}>
                              RRN: {rrn}
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
                          onClick={() => { if (confirm('Delete this transaction?')) deleteTransaction(txn.id); }}
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
            <Clock size={36} style={{ margin: '0 auto 12px auto', color: 'var(--neon-cyan)', opacity: 0.7 }} />
            <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.2rem', fontWeight: 700, color: '#f1f5f9' }}>No Transactions Found</div>
            <p style={{ fontSize: '0.85rem', marginTop: 4, maxWidth: 400, marginLeft: 'auto', marginRight: 'auto', marginBottom: 20 }}>
              No transactions recorded yet. Add an expense or income to track your cashflow.
            </p>
            <button
              className="btn btn-primary btn-md"
              onClick={() => { setEditTxn(undefined); setShowModal(true); }}
            >
              <Plus size={16} /> Add First Transaction
            </button>
          </div>
        )}
      </div>

      {showModal && <TxnModal txn={editTxn} onClose={() => setShowModal(false)} />}
      {showImportModal && <ImportStatementModal onClose={() => setShowImportModal(false)} />}
      {showJoylessModal && <JoylessLeakModal onClose={() => setShowJoylessModal(false)} />}
    </div>
  );
}
