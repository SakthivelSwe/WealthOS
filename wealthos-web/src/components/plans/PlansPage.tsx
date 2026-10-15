import { useState } from 'react';
import { useData } from '../../hooks/useData';
import { formatMoney, parseMoney, type Goal, type Debt, type ChitFund, type RecurringTransaction } from '../../types';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { Target, CreditCard, Coins, Repeat, Plus, X, ArrowUpRight, Check, AlertCircle } from 'lucide-react';
import { format, addMonths } from 'date-fns';

// ===== GOAL MODAL =====
function GoalModal({ onClose }: { onClose: () => void }) {
  const { addGoal, settings } = useData();
  const currency = settings?.currency || 'INR';
  const [name, setName] = useState('');
  const [target, setTarget] = useState('');
  const [current, setCurrent] = useState('');
  const [monthly, setMonthly] = useState('');
  const [targetDate, setTargetDate] = useState(format(addMonths(new Date(), 12), 'yyyy-MM-dd'));
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !target) return;
    setLoading(true);
    const now = Date.now();
    await addGoal({
      id: Math.random().toString(36).substring(2) + now,
      name, currency,
      target_amount_minor: parseMoney(target, currency),
      current_amount_minor: parseMoney(current || '0', currency),
      planned_monthly_contribution_minor: parseMoney(monthly || '0', currency),
      target_date: targetDate,
      archived: false,
      created_at: now, updated_at: now,
    });
    setLoading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800 }}>Create Financial Goal</h2>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Goal Title *</label>
            <input value={name} onChange={e => setName(e.target.value)} placeholder="e.g. Emergency Fund (6 Months)" autoFocus />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Target Amount ({currency}) *</label>
              <input type="number" step="any" value={target} onChange={e => setTarget(e.target.value)} placeholder="300000" />
            </div>
            <div className="form-group">
              <label className="form-label">Current Savings</label>
              <input type="number" step="any" value={current} onChange={e => setCurrent(e.target.value)} placeholder="50000" />
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Monthly Target Contribution</label>
              <input type="number" step="any" value={monthly} onChange={e => setMonthly(e.target.value)} placeholder="10000" />
            </div>
            <div className="form-group">
              <label className="form-label">Target Completion Date</label>
              <input type="date" value={targetDate} onChange={e => setTargetDate(e.target.value)} />
            </div>
          </div>
          <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} style={{ flex: 1 }}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading} style={{ flex: 2 }}>
              <Check size={16} /> Save Goal
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ===== DEBT MODAL =====
function DebtModal({ onClose }: { onClose: () => void }) {
  const { addDebt, settings } = useData();
  const currency = settings?.currency || 'INR';
  const [name, setName] = useState('');
  const [principal, setPrincipal] = useState('');
  const [balance, setBalance] = useState('');
  const [annualPercent, setAnnualPercent] = useState('8.5');
  const [emi, setEmi] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !principal) return;
    setLoading(true);
    const now = Date.now();
    await addDebt({
      id: Math.random().toString(36).substring(2) + now,
      name, currency,
      principal_minor: parseMoney(principal, currency),
      current_balance_minor: parseMoney(balance || principal, currency),
      annual_percent: annualPercent,
      expected_monthly_payment_minor: parseMoney(emi || '0', currency),
      archived: false,
      created_at: now, updated_at: now,
    });
    setLoading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800 }}>Record Debt / Loan</h2>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Debt Name *</label>
            <input value={name} onChange={e => setName(e.target.value)} placeholder="e.g. HDFC Car Loan (Tata Nexon)" autoFocus />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Principal Amount *</label>
              <input type="number" step="any" value={principal} onChange={e => setPrincipal(e.target.value)} placeholder="800000" />
            </div>
            <div className="form-group">
              <label className="form-label">Current Outstanding</label>
              <input type="number" step="any" value={balance} onChange={e => setBalance(e.target.value)} placeholder="450000" />
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Interest Rate (% p.a.)</label>
              <input type="number" step="0.1" value={annualPercent} onChange={e => setAnnualPercent(e.target.value)} placeholder="8.5" />
            </div>
            <div className="form-group">
              <label className="form-label">Monthly EMI ({currency})</label>
              <input type="number" step="any" value={emi} onChange={e => setEmi(e.target.value)} placeholder="16500" />
            </div>
          </div>
          <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} style={{ flex: 1 }}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading} style={{ flex: 2 }}>
              <Check size={16} /> Record Debt
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ===== CHIT FUND MODAL =====
function ChitFundModal({ onClose }: { onClose: () => void }) {
  const { addChitFund, settings } = useData();
  const currency = settings?.currency || 'INR';
  const [name, setName] = useState('');
  const [totalMonths, setTotalMonths] = useState('25');
  const [monthlyContribution, setMonthlyContribution] = useState('10000');
  const [startMonth, setStartMonth] = useState(format(new Date(), 'yyyy-MM'));
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !monthlyContribution) return;
    setLoading(true);
    const now = Date.now();
    await addChitFund({
      id: Math.random().toString(36).substring(2) + now,
      name, currency,
      total_months: parseInt(totalMonths) || 20,
      monthly_contribution_minor: parseMoney(monthlyContribution, currency),
      start_month: startMonth,
      active: true,
      created_at: now, updated_at: now,
    });
    setLoading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800 }}>Add Chit Fund Scheme</h2>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Chit Scheme Name *</label>
            <input value={name} onChange={e => setName(e.target.value)} placeholder="e.g. KSFE Gold Chitty (₹5 Lakhs)" autoFocus />
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Monthly Installment ({currency}) *</label>
              <input type="number" step="any" value={monthlyContribution} onChange={e => setMonthlyContribution(e.target.value)} placeholder="10000" />
            </div>
            <div className="form-group">
              <label className="form-label">Tenure (Total Months)</label>
              <input type="number" value={totalMonths} onChange={e => setTotalMonths(e.target.value)} placeholder="20" />
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">Starting Month</label>
            <input type="month" value={startMonth} onChange={e => setStartMonth(e.target.value)} />
          </div>
          <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} style={{ flex: 1 }}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading} style={{ flex: 2 }}>
              <Check size={16} /> Save Chit Fund
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ===== RECURRING MODAL =====
function RecurringModal({ onClose }: { onClose: () => void }) {
  const { addRecurring, accounts, settings } = useData();
  const currency = settings?.currency || 'INR';
  const [type, setType] = useState<'EXPENSE' | 'INCOME'>('EXPENSE');
  const [amount, setAmount] = useState('');
  const [accountId, setAccountId] = useState(accounts[0]?.id || '');
  const [description, setDescription] = useState('');
  const [frequency, setFrequency] = useState<'MONTHLY' | 'WEEKLY' | 'YEARLY'>('MONTHLY');
  const [nextRunDate, setNextRunDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!amount || !accountId) return;
    setLoading(true);
    const now = Date.now();
    await addRecurring({
      id: Math.random().toString(36).substring(2) + now,
      type, currency,
      amount_minor: parseMoney(amount, currency),
      account_id: accountId,
      category_id: null,
      description: description || null,
      frequency, next_run_date: nextRunDate,
      active: true,
      created_at: now, updated_at: now,
    });
    setLoading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800 }}>Add Recurring Transaction</h2>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Amount ({currency}) *</label>
              <input type="number" step="any" value={amount} onChange={e => setAmount(e.target.value)} placeholder="2000" autoFocus />
            </div>
            <div className="form-group">
              <label className="form-label">Frequency</label>
              <select value={frequency} onChange={e => setFrequency(e.target.value as any)}>
                <option value="MONTHLY">Monthly</option>
                <option value="WEEKLY">Weekly</option>
                <option value="YEARLY">Yearly</option>
              </select>
            </div>
          </div>
          <div className="form-group">
            <label className="form-label">Source Account</label>
            <select value={accountId} onChange={e => setAccountId(e.target.value)}>
              {accounts.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
            </select>
          </div>
          <div className="form-group">
            <label className="form-label">Subscription / Description</label>
            <input value={description} onChange={e => setDescription(e.target.value)} placeholder="e.g. Netflix, Broadband, House Rent" />
          </div>
          <div className="form-group">
            <label className="form-label">Next Debit Date</label>
            <input type="date" value={nextRunDate} onChange={e => setNextRunDate(e.target.value)} />
          </div>
          <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} style={{ flex: 1 }}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading} style={{ flex: 2 }}>
              <Check size={16} /> Save Subscription
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ===== MAIN PLANS PAGE =====
export function PlansPage() {
  const { goals, debts, chitFunds, recurringTxns, settings } = useData();
  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;
  const [activeTab, setActiveTab] = useState<'goals' | 'debts' | 'chits' | 'recurring'>('goals');
  const [showGoalModal, setShowGoalModal] = useState(false);
  const [showDebtModal, setShowDebtModal] = useState(false);
  const [showChitModal, setShowChitModal] = useState(false);
  const [showRecurringModal, setShowRecurringModal] = useState(false);

  const totalDebt = debts.reduce((s, d) => s + d.current_balance_minor, 0);
  const totalGoalTarget = goals.reduce((s, g) => s + g.target_amount_minor, 0);
  const totalGoalCurrent = goals.reduce((s, g) => s + g.current_amount_minor, 0);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
            Wealth Horizons & Commitments
          </h1>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
            Long-term wealth targets, loan obligations, Chit Funds, and recurring subscriptions
          </p>
        </div>
        <button
          className="btn btn-primary"
          onClick={() => {
            if (activeTab === 'goals') setShowGoalModal(true);
            else if (activeTab === 'debts') setShowDebtModal(true);
            else if (activeTab === 'chits') setShowChitModal(true);
            else setShowRecurringModal(true);
          }}
        >
          <Plus size={16} /> Add {activeTab === 'goals' ? 'Goal' : activeTab === 'debts' ? 'Debt' : activeTab === 'chits' ? 'Chit Scheme' : 'Subscription'}
        </button>
      </div>

      {/* 3D Overview Metrics */}
      <div className="bento-grid">
        <div className="col-span-3">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-cyan)' }} />
              <div className="stat-label">
                <span>Goals Progress</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-cyan-glow)', color: 'var(--neon-cyan)' }}>
                  <Target size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-cyan)' }}>
                {formatMoney(totalGoalCurrent, currency, masked)}
              </div>
              <div className="stat-trend neutral">of {formatMoney(totalGoalTarget, currency, masked)}</div>
            </div>
          </Tilt3DCard>
        </div>

        <div className="col-span-3">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-rose)' }} />
              <div className="stat-label">
                <span>Debt Exposure</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-rose-glow)', color: 'var(--neon-rose)' }}>
                  <CreditCard size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-rose)' }}>
                {formatMoney(totalDebt, currency, masked)}
              </div>
              <div className="stat-trend negative">{debts.length} active loans</div>
            </div>
          </Tilt3DCard>
        </div>

        <div className="col-span-3">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-amber)' }} />
              <div className="stat-label">
                <span>Chit Fund Pools</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-amber-glow)', color: 'var(--neon-amber)' }}>
                  <Coins size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-amber)' }}>
                {chitFunds.length}
              </div>
              <div className="stat-trend neutral">Traditional wealth schemes</div>
            </div>
          </Tilt3DCard>
        </div>

        <div className="col-span-3">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-purple)' }} />
              <div className="stat-label">
                <span>Monthly Recurring</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-purple-glow)', color: 'var(--neon-purple)' }}>
                  <Repeat size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-purple)' }}>
                {formatMoney(recurringTxns.filter(r => r.type === 'EXPENSE').reduce((s, r) => s + r.amount_minor, 0), currency, masked)}
              </div>
              <div className="stat-trend neutral">{recurringTxns.length} active commitments</div>
            </div>
          </Tilt3DCard>
        </div>
      </div>

      {/* Cyber Tab Navigation */}
      <div style={{ display: 'flex', gap: 6, background: 'rgba(255, 255, 255, 0.04)', padding: 5, borderRadius: 'var(--radius-pill)', border: '1px solid var(--glass-border)', width: 'fit-content', maxWidth: '100%', flexWrap: 'wrap' }}>
        {[
          { id: 'goals', label: '🎯 Financial Goals', count: goals.length },
          { id: 'debts', label: '💳 Debt & Loans', count: debts.length },
          { id: 'chits', label: '🪙 Chit Funds (India)', count: chitFunds.length },
          { id: 'recurring', label: '🔄 Recurring Subscriptions', count: recurringTxns.length },
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`btn btn-sm ${activeTab === tab.id ? 'btn-primary' : 'btn-ghost'}`}
            style={{ borderRadius: 'var(--radius-pill)', fontSize: '0.82rem', padding: '6px 16px' }}
          >
            {tab.label} ({tab.count})
          </button>
        ))}
      </div>

      {/* Goals Tab */}
      {activeTab === 'goals' && (
        <div className="responsive-grid-cards">
          {goals.map(goal => {
            const pct = goal.target_amount_minor > 0 ? Math.min((goal.current_amount_minor / goal.target_amount_minor) * 100, 100) : 0;
            const remaining = goal.target_amount_minor - goal.current_amount_minor;

            return (
              <Tilt3DCard key={goal.id}>
                <div className="cyber-card" style={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                      <span className="badge badge-cyan">GOAL</span>
                      <span style={{ fontFamily: 'var(--font-outfit)', fontWeight: 800, fontSize: '1.2rem', color: pct >= 100 ? 'var(--neon-emerald)' : 'var(--neon-cyan)' }}>
                        {pct.toFixed(0)}%
                      </span>
                    </div>

                    <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                      {goal.name}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: 2 }}>Target Date: {goal.target_date}</div>

                    <div className="cyber-progress-track" style={{ height: 10, margin: '16px 0 10px 0' }}>
                      <div className="cyber-progress-fill" style={{ width: `${pct}%`, background: 'linear-gradient(90deg, var(--neon-cyan), var(--neon-emerald))' }} />
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                      <span style={{ fontWeight: 700, color: '#f8fafc' }}>{formatMoney(goal.current_amount_minor, currency, masked)}</span>
                      <span>of {formatMoney(goal.target_amount_minor, currency, masked)}</span>
                    </div>
                  </div>

                  <div style={{ marginTop: 16, paddingTop: 12, borderTop: '1px solid var(--glass-border)', fontSize: '0.78rem', color: 'var(--text-tertiary)' }}>
                    Remaining: <span style={{ color: 'var(--neon-cyan)', fontWeight: 600 }}>{formatMoney(Math.max(0, remaining), currency, masked)}</span>
                  </div>
                </div>
              </Tilt3DCard>
            );
          })}
        </div>
      )}

      {/* Debts Tab */}
      {activeTab === 'debts' && (
        <div className="responsive-grid-cards">
          {debts.map(debt => (
            <Tilt3DCard key={debt.id}>
              <div className="cyber-card" style={{ height: '100%', borderLeft: '3px solid var(--neon-rose)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                  <span className="badge badge-rose">LOAN / DEBT</span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.82rem', color: 'var(--neon-amber)' }}>{debt.annual_percent}% p.a.</span>
                </div>

                <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.15rem', fontWeight: 800 }}>{debt.name}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                  Monthly EMI: {formatMoney(debt.expected_monthly_payment_minor, currency, masked)}
                </div>

                <div style={{ marginTop: 20 }}>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Current Balance</div>
                  <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.6rem', fontWeight: 900, color: 'var(--neon-rose)' }}>
                    {formatMoney(debt.current_balance_minor, currency, masked)}
                  </div>
                </div>
              </div>
            </Tilt3DCard>
          ))}
        </div>
      )}

      {/* Chit Funds Tab */}
      {activeTab === 'chits' && (
        <div className="responsive-grid-cards">
          {chitFunds.map(chit => (
            <Tilt3DCard key={chit.id}>
              <div className="cyber-card" style={{ height: '100%', borderLeft: '3px solid var(--neon-amber)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                  <span className="badge badge-amber">CHIT FUND</span>
                  <span className="badge badge-green">Active</span>
                </div>
                <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.15rem', fontWeight: 800 }}>{chit.name}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: 2 }}>Duration: {chit.total_months} months • Start: {chit.start_month}</div>

                <div style={{ marginTop: 20 }}>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Monthly Installment</div>
                  <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.6rem', fontWeight: 900, color: 'var(--neon-amber)' }}>
                    {formatMoney(chit.monthly_contribution_minor, currency, masked)}
                  </div>
                </div>
              </div>
            </Tilt3DCard>
          ))}
        </div>
      )}

      {/* Recurring Tab */}
      {activeTab === 'recurring' && (
        <div className="responsive-grid-cards">
          {recurringTxns.map(rec => (
            <Tilt3DCard key={rec.id}>
              <div className="cyber-card" style={{ height: '100%', borderLeft: '3px solid var(--neon-purple)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                  <span className="badge badge-purple">{rec.frequency}</span>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>Next: {rec.next_run_date}</span>
                </div>
                <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.15rem', fontWeight: 800 }}>{rec.description || 'Subscription'}</div>

                <div style={{ marginTop: 20 }}>
                  <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.6rem', fontWeight: 900, color: 'var(--neon-purple)' }}>
                    {formatMoney(rec.amount_minor, currency, masked)}
                  </div>
                </div>
              </div>
            </Tilt3DCard>
          ))}
        </div>
      )}

      {showGoalModal && <GoalModal onClose={() => setShowGoalModal(false)} />}
      {showDebtModal && <DebtModal onClose={() => setShowDebtModal(false)} />}
      {showChitModal && <ChitFundModal onClose={() => setShowChitModal(false)} />}
      {showRecurringModal && <RecurringModal onClose={() => setShowRecurringModal(false)} />}
    </div>
  );
}
