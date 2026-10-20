import { useState, useMemo } from 'react';
import { useData } from '../../hooks/useData';
import { parseMoney, getCategoryDisplayName, formatAccountLabel, POPULAR_BANKS, type TxnType, type NeedWant } from '../../types';
import { X, Check, ArrowRightLeft, TrendingDown, TrendingUp, Plus, Building2 } from 'lucide-react';
import { format } from 'date-fns';
import { CategoryPicker } from './CategoryPicker';

interface NewTransactionModalProps {
  onClose: () => void;
}

export function NewTransactionModal({ onClose }: NewTransactionModalProps) {
  const { accounts, categories, addTransaction, addAccount, settings } = useData();
  const currency = settings?.currency || 'INR';
  const [showQuickAddBank, setShowQuickAddBank] = useState(false);

  const [type, setType] = useState<TxnType>('EXPENSE');
  const [amountStr, setAmountStr] = useState('');
  const [accountId, setAccountId] = useState(accounts[0]?.id || '');
  const [toAccountId, setToAccountId] = useState(accounts[1]?.id || '');
  const [categoryId, setCategoryId] = useState('');
  const [merchant, setMerchant] = useState('');
  const [description, setDescription] = useState('');
  const [needWant, setNeedWant] = useState<NeedWant | null>('NEED');
  const [date, setDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [error, setError] = useState('');

  const categoriesForType = useMemo(() => {
    const list = categories.filter(c => c.kind === type && !c.archived);
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
    if (!amountStr || parseFloat(amountStr) <= 0) {
      setError('Please enter a valid amount');
      return;
    }
    if (!accountId) {
      setError('Please select an account');
      return;
    }

    const amountMinor = parseMoney(amountStr, currency);
    const txnId = 'txn_' + Date.now();

    const journalEntries = [];
    if (type === 'EXPENSE') {
      journalEntries.push({ txn_id: txnId, account_id: accountId, category_id: null, amount_minor: -amountMinor });
      journalEntries.push({ txn_id: txnId, account_id: null, category_id: categoryId || null, amount_minor: amountMinor });
    } else if (type === 'INCOME') {
      journalEntries.push({ txn_id: txnId, account_id: accountId, category_id: null, amount_minor: amountMinor });
      journalEntries.push({ txn_id: txnId, account_id: null, category_id: categoryId || null, amount_minor: -amountMinor });
    } else if (type === 'TRANSFER') {
      journalEntries.push({ txn_id: txnId, account_id: accountId, category_id: null, amount_minor: -amountMinor });
      if (toAccountId) {
        journalEntries.push({ txn_id: txnId, account_id: toAccountId, category_id: null, amount_minor: amountMinor });
      }
    }

    await addTransaction({
      id: txnId,
      type,
      amount_minor: amountMinor,
      currency,
      local_date: date,
      local_time: format(new Date(), 'HH:mm'),
      account_id: accountId,
      to_account_id: type === 'TRANSFER' ? toAccountId : null,
      category_id: type !== 'TRANSFER' ? (categoryId || null) : null,
      merchant: merchant || null,
      description: description || null,
      notes: null,
      payment_method: 'UPI',
      need_want: type === 'EXPENSE' ? needWant : null,
      tags: [],
      imported: false,
      source: 'WEB_MANUAL',
      created_at: Date.now(),
      updated_at: Date.now(),
      deleted_at: null,
    }, journalEntries);

    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800 }}>Add Transaction</h2>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        {/* Transaction Type Selector */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, marginBottom: 18 }}>
          {[
            { id: 'EXPENSE', label: 'Expense', icon: TrendingDown, color: 'var(--semantic-expense, #f43f5e)' },
            { id: 'INCOME', label: 'Income', icon: TrendingUp, color: 'var(--semantic-income, #10b981)' },
            { id: 'TRANSFER', label: 'Transfer', icon: ArrowRightLeft, color: 'var(--semantic-info, #0ea5e9)' },
          ].map(item => {
            const Icon = item.icon;
            const isSelected = type === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setType(item.id as TxnType)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 6,
                  padding: '10px 8px',
                  borderRadius: 'var(--radius-md)',
                  background: isSelected ? 'rgba(255, 255, 255, 0.12)' : 'rgba(255, 255, 255, 0.03)',
                  border: isSelected ? `1px solid ${item.color}` : '1px solid var(--border-subtle)',
                  color: isSelected ? item.color : 'var(--text-secondary)',
                  cursor: 'pointer',
                  fontWeight: 700,
                  fontSize: '0.85rem',
                  transition: 'all 0.2s',
                }}
              >
                <Icon size={16} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>

        <form onSubmit={handleSubmit}>
          {/* Amount Input */}
          <div className="form-group">
            <label className="form-label">Amount ({currency})</label>
            <input
              type="number"
              step="any"
              placeholder="0.00"
              value={amountStr}
              onChange={e => setAmountStr(e.target.value)}
              autoFocus
              style={{
                fontSize: '1.4rem',
                fontFamily: 'var(--font-outfit)',
                fontWeight: 800,
                color: type === 'EXPENSE' ? 'var(--semantic-expense, #f43f5e)' : type === 'INCOME' ? 'var(--semantic-income, #10b981)' : 'var(--semantic-info, #0ea5e9)',
              }}
            />
          </div>

          {/* Account Dropdown with Quick Bank Add */}
          <div className="form-group">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
              <label className="form-label" style={{ marginBottom: 0 }}>
                {type === 'TRANSFER' ? 'From Account' : 'Account'}
              </label>
              <button
                type="button"
                onClick={() => setShowQuickAddBank(!showQuickAddBank)}
                className="btn-ghost"
                style={{
                  fontSize: '0.75rem',
                  color: 'var(--neon-cyan)',
                  padding: '3px 8px',
                  borderRadius: 'var(--radius-pill)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 4,
                  background: showQuickAddBank ? 'var(--neon-cyan-glow)' : 'transparent',
                }}
              >
                <Plus size={12} /> {showQuickAddBank ? 'Close' : 'Add Bank / Account'}
              </button>
            </div>

            {/* Quick Add Bank Panel */}
            {showQuickAddBank && (
              <div
                style={{
                  padding: 12,
                  marginBottom: 10,
                  borderRadius: 'var(--radius-md)',
                  background: 'rgba(255, 255, 255, 0.04)',
                  border: '1px solid var(--glass-border-bright)',
                }}
              >
                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Building2 size={13} style={{ color: 'var(--neon-cyan)' }} />
                  Tap an institution to create & select immediately:
                </div>
                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
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
                          last_four: '4291',
                          opening_balance_minor: 0,
                          currency,
                          include_in_net_worth: true,
                          include_in_stats: true,
                          archived: false,
                          created_at: Date.now(),
                          updated_at: Date.now(),
                        });
                        setAccountId(newId);
                        setShowQuickAddBank(false);
                      }}
                      style={{
                        padding: '4px 10px',
                        borderRadius: 'var(--radius-pill)',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                        border: `1px solid ${bank.color}55`,
                        background: bank.badgeBg,
                        color: bank.color,
                        cursor: 'pointer',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 5,
                        transition: 'transform 0.15s ease',
                      }}
                      onMouseEnter={e => { e.currentTarget.style.transform = 'scale(1.05)'; }}
                      onMouseLeave={e => { e.currentTarget.style.transform = 'scale(1)'; }}
                    >
                      <span style={{ width: 6, height: 6, borderRadius: '50%', background: bank.color }} />
                      {bank.name}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <select value={accountId} onChange={e => setAccountId(e.target.value)}>
              {accounts.map(acc => (
                <option key={acc.id} value={acc.id}>
                  {formatAccountLabel(acc)}
                </option>
              ))}
            </select>
          </div>

          {type === 'TRANSFER' && (
            <div className="form-group">
              <label className="form-label">To Account</label>
              <select value={toAccountId} onChange={e => setToAccountId(e.target.value)}>
                {accounts.filter(a => a.id !== accountId).map(acc => (
                  <option key={acc.id} value={acc.id}>
                    {formatAccountLabel(acc)}
                  </option>
                ))}
              </select>
            </div>
          )}

          {type !== 'TRANSFER' && (
            <div className="form-group">
              <label className="form-label">Category</label>
              <CategoryPicker
                type={type}
                value={categoryId}
                onChange={setCategoryId}
                categories={categories}
              />
            </div>
          )}

          {/* Merchant / Description */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Merchant / Payee</label>
              <input placeholder="e.g. Swiggy, Uber" value={merchant} onChange={e => setMerchant(e.target.value)} />
            </div>
            <div className="form-group">
              <label className="form-label">Date</label>
              <input type="date" value={date} onChange={e => setDate(e.target.value)} />
            </div>
          </div>

          {/* Need / Want / Saving Classification for Expenses */}
          {type === 'EXPENSE' && (
            <div className="form-group">
              <label className="form-label">50/30/20 Classification</label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8 }}>
                {[
                  { id: 'NEED', label: 'Need (50%)', color: 'var(--semantic-info, #0ea5e9)' },
                  { id: 'WANT', label: 'Want (30%)', color: '#a855f7' },
                  { id: 'SAVING', label: 'Saving (20%)', color: 'var(--semantic-income, #10b981)' },
                ].map(item => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => setNeedWant(item.id as NeedWant)}
                    style={{
                      padding: '8px',
                      borderRadius: 'var(--radius-sm)',
                      background: needWant === item.id ? 'rgba(255, 255, 255, 0.12)' : 'rgba(255, 255, 255, 0.03)',
                      border: needWant === item.id ? `1px solid ${item.color}` : '1px solid var(--border-subtle)',
                      color: needWant === item.id ? item.color : 'var(--text-tertiary)',
                      fontSize: '0.78rem',
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

          {error && <div style={{ color: 'var(--semantic-expense, #f43f5e)', fontSize: '0.82rem', marginBottom: 12 }}>{error}</div>}

          <div style={{ display: 'flex', gap: 10, marginTop: 10 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} style={{ flex: 1 }}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" style={{ flex: 2 }}>
              <Check size={16} /> Save Transaction
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
