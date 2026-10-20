import { useState } from 'react';
import { useData } from '../../hooks/useData';
import { formatMoney, parseMoney, type Goal, type Debt, type ChitFund, type RecurringTransaction } from '../../types';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { Target, CreditCard, Coins, Repeat, Plus, X, Check, ShieldAlert, Sparkles, HelpCircle, Info } from 'lucide-react';
import { format, addMonths } from 'date-fns';

// ===== GOAL MODAL =====
interface GoalPreset {
  id: string;
  icon: string;
  badge: string;
  name: string;
  target: string;
  current: string;
  monthly: string;
  months: number;
  tip: string;
}

const GOAL_PRESETS: GoalPreset[] = [
  {
    id: 'emergency',
    icon: '🛡️',
    badge: 'Emergency Fund',
    name: 'Emergency Fund (6 Months Safety Net)',
    target: '300000',
    current: '25000',
    monthly: '10000',
    months: 12,
    tip: 'Emergency Fund protects you and your family against sudden hospital bills, job changes, or house repairs. Aim for 3 to 6 months of living expenses.',
  },
  {
    id: 'gold',
    icon: '🪙',
    badge: 'Gold Savings',
    name: 'Gold Jewelry & SGB Savings',
    target: '250000',
    current: '20000',
    monthly: '15000',
    months: 18,
    tip: 'Traditional family wealth preservation for weddings, Akshaya Tritiya, or Sovereign Gold Bonds (SGB).',
  },
  {
    id: 'house',
    icon: '🏠',
    badge: 'House / Land Plot',
    name: 'House Down Payment / Land Plot',
    target: '1000000',
    current: '100000',
    monthly: '25000',
    months: 36,
    tip: 'Accumulate the 20% down payment required to buy a flat, independent house, or residential plot.',
  },
  {
    id: 'vehicle',
    icon: '🚗',
    badge: 'Bike / Car Purchase',
    name: 'New Two-Wheeler / Car Purchase',
    target: '150000',
    current: '15000',
    monthly: '10000',
    months: 12,
    tip: 'Save for down payment or direct purchase of a scooter, bike, or car with lower EMI burden.',
  },
  {
    id: 'education',
    icon: '🎓',
    badge: 'Child Education',
    name: 'School Fees & College Fund',
    target: '500000',
    current: '50000',
    monthly: '15000',
    months: 24,
    tip: 'Annual school fees, entrance coaching, or higher degree college admission fees.',
  },
  {
    id: 'tour',
    icon: '✈️',
    badge: 'Tour & Vacation',
    name: 'Family Tour / Vacation Fund',
    target: '75000',
    current: '10000',
    monthly: '8000',
    months: 8,
    tip: 'Stress-free annual holiday, hill station trip, or family travel without taking credit card debt.',
  },
];

function GoalModal({ initialPresetId, onClose }: { initialPresetId?: string; onClose: () => void }) {
  const { addGoal, settings } = useData();
  const currency = settings?.currency || 'INR';

  const defaultPreset = initialPresetId ? GOAL_PRESETS.find(p => p.id === initialPresetId) : undefined;

  const [name, setName] = useState(defaultPreset?.name || '');
  const [target, setTarget] = useState(defaultPreset?.target || '');
  const [current, setCurrent] = useState(defaultPreset?.current || '');
  const [monthly, setMonthly] = useState(defaultPreset?.monthly || '');
  const [targetDate, setTargetDate] = useState(
    format(addMonths(new Date(), defaultPreset?.months || 12), 'yyyy-MM-dd')
  );
  const [activeTip, setActiveTip] = useState(defaultPreset?.tip || '');
  const [loading, setLoading] = useState(false);

  const applyPreset = (p: GoalPreset) => {
    setName(p.name);
    setTarget(p.target);
    setCurrent(p.current);
    setMonthly(p.monthly);
    setTargetDate(format(addMonths(new Date(), p.months), 'yyyy-MM-dd'));
    setActiveTip(p.tip);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !target) return;
    setLoading(true);
    const now = Date.now();
    await addGoal({
      id: Math.random().toString(36).substring(2) + now,
      name,
      currency,
      target_amount_minor: parseMoney(target, currency),
      current_amount_minor: parseMoney(current || '0', currency),
      planned_monthly_contribution_minor: parseMoney(monthly || '0', currency),
      target_date: targetDate,
      archived: false,
      created_at: now,
      updated_at: now,
    });
    setLoading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 1200 }}>
      <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: 560 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <div>
            <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, margin: 0 }}>
              🎯 Create Financial Goal
            </h2>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', margin: '2px 0 0 0' }}>
              Choose a quick template or type your custom savings goal
            </p>
          </div>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        {/* 1-Tap Quick Goal Templates */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8, fontSize: '0.78rem', color: 'var(--text-tertiary)', fontWeight: 600 }}>
            <Sparkles size={13} style={{ color: 'var(--neon-cyan)' }} />
            <span>Popular 1-Tap Presets (Click to autofill):</span>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {GOAL_PRESETS.map(p => (
              <button
                key={p.id}
                type="button"
                onClick={() => applyPreset(p)}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '5px 11px',
                  borderRadius: 'var(--radius-pill)',
                  fontSize: '0.8rem',
                  fontWeight: name === p.name ? 700 : 500,
                  cursor: 'pointer',
                  border: name === p.name ? '1.5px solid var(--neon-cyan)' : '1px solid rgba(255, 255, 255, 0.08)',
                  background: name === p.name ? 'rgba(0, 240, 255, 0.16)' : 'rgba(255, 255, 255, 0.04)',
                  color: name === p.name ? '#ffffff' : 'var(--text-secondary)',
                  transition: 'all 0.16s ease',
                }}
              >
                <span>{p.icon}</span>
                <span>{p.badge}</span>
              </button>
            ))}
          </div>
        </div>

        {activeTip && (
          <div style={{ display: 'flex', gap: 10, padding: '10px 12px', borderRadius: 'var(--radius-md)', background: 'rgba(0, 240, 255, 0.08)', border: '1px solid rgba(0, 240, 255, 0.25)', marginBottom: 16, fontSize: '0.8rem', color: '#e2e8f0', lineHeight: 1.4 }}>
            <Info size={16} style={{ color: 'var(--neon-cyan)', flexShrink: 0, marginTop: 2 }} />
            <div>{activeTip}</div>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Goal Title / Name *</label>
            <input
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="e.g. Emergency Fund (6 Months)"
              autoFocus
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Target Amount ({currency}) *</label>
              <input
                type="number"
                step="any"
                value={target}
                onChange={e => setTarget(e.target.value)}
                placeholder="300000"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Current Savings Already Saved</label>
              <input
                type="number"
                step="any"
                value={current}
                onChange={e => setCurrent(e.target.value)}
                placeholder="25000"
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Monthly Target Contribution</label>
              <input
                type="number"
                step="any"
                value={monthly}
                onChange={e => setMonthly(e.target.value)}
                placeholder="10000"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Target Completion Date</label>
              <input type="date" value={targetDate} onChange={e => setTargetDate(e.target.value)} />
            </div>
          </div>

          <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} style={{ flex: 1 }}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading} style={{ flex: 2 }}>
              <Check size={16} /> Save Goal
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ===== CHIT FUND MODAL =====
interface ChitPreset {
  name: string;
  monthly: string;
  months: string;
  total: string;
}

const CHIT_PRESETS: ChitPreset[] = [
  { name: '₹5 Lakhs Chitty (KSFE / Shriram)', monthly: '25000', months: '20', total: '5,00,000' },
  { name: '₹2 Lakhs Monthly Chit', monthly: '10000', months: '20', total: '2,00,000' },
  { name: '₹1 Lakh Family / Friends Chit', monthly: '5000', months: '20', total: '1,00,000' },
  { name: '₹50,000 Mini Savings Chit', monthly: '2500', months: '20', total: '50,000' },
];

function ChitFundModal({ onClose }: { onClose: () => void }) {
  const { addChitFund, settings } = useData();
  const currency = settings?.currency || 'INR';

  const [name, setName] = useState('');
  const [totalMonths, setTotalMonths] = useState('20');
  const [monthlyContribution, setMonthlyContribution] = useState('10000');
  const [startMonth, setStartMonth] = useState(format(new Date(), 'yyyy-MM'));
  const [loading, setLoading] = useState(false);

  const applyPreset = (p: ChitPreset) => {
    setName(p.name);
    setMonthlyContribution(p.monthly);
    setTotalMonths(p.months);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !monthlyContribution) return;
    setLoading(true);
    const now = Date.now();
    await addChitFund({
      id: Math.random().toString(36).substring(2) + now,
      name,
      currency,
      total_months: parseInt(totalMonths) || 20,
      monthly_contribution_minor: parseMoney(monthlyContribution, currency),
      start_month: startMonth,
      active: true,
      created_at: now,
      updated_at: now,
    });
    setLoading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 1200 }}>
      <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: 540 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <div>
            <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, margin: 0 }}>
              🪙 Add Chit Fund Scheme
            </h2>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', margin: '2px 0 0 0' }}>
              Track monthly chits, installments paid, and auction payouts
            </p>
          </div>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        {/* 1-Tap Chit Presets */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8, fontSize: '0.78rem', color: 'var(--text-tertiary)', fontWeight: 600 }}>
            <Sparkles size={13} style={{ color: 'var(--neon-amber)' }} />
            <span>Popular Chit Schemes:</span>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {CHIT_PRESETS.map((p, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => applyPreset(p)}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '5px 11px',
                  borderRadius: 'var(--radius-pill)',
                  fontSize: '0.8rem',
                  fontWeight: name === p.name ? 700 : 500,
                  cursor: 'pointer',
                  border: name === p.name ? '1.5px solid var(--neon-amber)' : '1px solid rgba(255, 255, 255, 0.08)',
                  background: name === p.name ? 'rgba(245, 158, 11, 0.18)' : 'rgba(255, 255, 255, 0.04)',
                  color: name === p.name ? '#ffffff' : 'var(--text-secondary)',
                  transition: 'all 0.16s ease',
                }}
              >
                <span>🪙</span>
                <span>{p.name}</span>
              </button>
            ))}
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10, padding: '10px 12px', borderRadius: 'var(--radius-md)', background: 'rgba(245, 158, 11, 0.08)', border: '1px solid rgba(245, 158, 11, 0.25)', marginBottom: 16, fontSize: '0.8rem', color: '#e2e8f0', lineHeight: 1.4 }}>
          <Info size={16} style={{ color: 'var(--neon-amber)', flexShrink: 0, marginTop: 2 }} />
          <div>
            <strong>How Chit Funds work:</strong> You pay a fixed monthly installment with a group. Each month, members bid for the prize money or receive dividend discounts.
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Chit Scheme Name *</label>
            <input
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="e.g. KSFE Gold Chitty (₹5 Lakhs) or Shriram Chits"
              autoFocus
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Monthly Installment ({currency}) *</label>
              <input
                type="number"
                step="any"
                value={monthlyContribution}
                onChange={e => setMonthlyContribution(e.target.value)}
                placeholder="10000"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Total Tenure (Months)</label>
              <input
                type="number"
                value={totalMonths}
                onChange={e => setTotalMonths(e.target.value)}
                placeholder="20"
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Starting Month</label>
            <input type="month" value={startMonth} onChange={e => setStartMonth(e.target.value)} />
          </div>

          <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} style={{ flex: 1 }}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading} style={{ flex: 2, background: 'var(--neon-amber)', color: '#000' }}>
              <Check size={16} /> Save Chit Fund
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ===== DEBT MODAL =====
interface LoanPreset {
  name: string;
  principal: string;
  balance: string;
  rate: string;
  emi: string;
}

const LOAN_PRESETS: LoanPreset[] = [
  { name: 'SBI / HDFC Home Loan', principal: '3500000', balance: '3200000', rate: '8.5', emi: '30375' },
  { name: 'Car Loan (Tata / Hyundai)', principal: '800000', balance: '550000', rate: '8.75', emi: '16500' },
  { name: 'Two-Wheeler / Bike Loan', principal: '120000', balance: '85000', rate: '10.5', emi: '3800' },
  { name: 'Jewel Loan / Gold Loan', principal: '200000', balance: '180000', rate: '8.5', emi: '1450' },
  { name: 'Personal Loan', principal: '300000', balance: '220000', rate: '12.0', emi: '9900' },
];

function DebtModal({ onClose }: { onClose: () => void }) {
  const { addDebt, settings } = useData();
  const currency = settings?.currency || 'INR';

  const [name, setName] = useState('');
  const [principal, setPrincipal] = useState('');
  const [balance, setBalance] = useState('');
  const [annualPercent, setAnnualPercent] = useState('8.5');
  const [emi, setEmi] = useState('');
  const [loading, setLoading] = useState(false);

  const applyPreset = (p: LoanPreset) => {
    setName(p.name);
    setPrincipal(p.principal);
    setBalance(p.balance);
    setAnnualPercent(p.rate);
    setEmi(p.emi);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !principal) return;
    setLoading(true);
    const now = Date.now();
    await addDebt({
      id: Math.random().toString(36).substring(2) + now,
      name,
      currency,
      principal_minor: parseMoney(principal, currency),
      current_balance_minor: parseMoney(balance || principal, currency),
      annual_percent: annualPercent,
      expected_monthly_payment_minor: parseMoney(emi || '0', currency),
      archived: false,
      created_at: now,
      updated_at: now,
    });
    setLoading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 1200 }}>
      <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: 540 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <div>
            <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, margin: 0 }}>
              💳 Record Debt / Loan (EMI)
            </h2>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', margin: '2px 0 0 0' }}>
              Track home loans, vehicle EMIs, gold loans, and credit repayments
            </p>
          </div>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        {/* 1-Tap Loan Presets */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8, fontSize: '0.78rem', color: 'var(--text-tertiary)', fontWeight: 600 }}>
            <Sparkles size={13} style={{ color: 'var(--neon-rose)' }} />
            <span>Common Loan Types in India:</span>
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {LOAN_PRESETS.map((p, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => applyPreset(p)}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '5px 11px',
                  borderRadius: 'var(--radius-pill)',
                  fontSize: '0.8rem',
                  fontWeight: name === p.name ? 700 : 500,
                  cursor: 'pointer',
                  border: name === p.name ? '1.5px solid var(--neon-rose)' : '1px solid rgba(255, 255, 255, 0.08)',
                  background: name === p.name ? 'rgba(244, 63, 94, 0.18)' : 'rgba(255, 255, 255, 0.04)',
                  color: name === p.name ? '#ffffff' : 'var(--text-secondary)',
                  transition: 'all 0.16s ease',
                }}
              >
                <span>💳</span>
                <span>{p.name}</span>
              </button>
            ))}
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Loan / Debt Name *</label>
            <input
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="e.g. HDFC Home Loan or Tata Car EMI"
              autoFocus
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Original Principal Borrowed ({currency}) *</label>
              <input
                type="number"
                step="any"
                value={principal}
                onChange={e => setPrincipal(e.target.value)}
                placeholder="800000"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Remaining Balance Still Due ({currency})</label>
              <input
                type="number"
                step="any"
                value={balance}
                onChange={e => setBalance(e.target.value)}
                placeholder="450000"
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Interest Rate (% p.a.)</label>
              <input
                type="number"
                step="0.1"
                value={annualPercent}
                onChange={e => setAnnualPercent(e.target.value)}
                placeholder="8.5"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Monthly EMI ({currency})</label>
              <input
                type="number"
                step="any"
                value={emi}
                onChange={e => setEmi(e.target.value)}
                placeholder="16500"
              />
            </div>
          </div>

          <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} style={{ flex: 1 }}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading} style={{ flex: 2, background: 'var(--neon-rose)', color: '#fff' }}>
              <Check size={16} /> Record Loan / EMI
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
      type,
      currency,
      amount_minor: parseMoney(amount, currency),
      account_id: accountId,
      category_id: null,
      description: description || null,
      frequency,
      next_run_date: nextRunDate,
      active: true,
      created_at: now,
      updated_at: now,
    });
    setLoading(false);
    onClose();
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 1200 }}>
      <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: 520 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <div>
            <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, margin: 0 }}>
              🔄 Add Recurring Bill / Subscription
            </h2>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', margin: '2px 0 0 0' }}>
              Monthly rent, broadband, OTT plans, or SIP auto-debits
            </p>
          </div>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <div className="form-group">
              <label className="form-label">Amount ({currency}) *</label>
              <input
                type="number"
                step="any"
                value={amount}
                onChange={e => setAmount(e.target.value)}
                placeholder="2000"
                autoFocus
              />
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
              {accounts.map(a => (
                <option key={a.id} value={a.id}>
                  {a.name}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Subscription / Bill Description</label>
            <input
              value={description}
              onChange={e => setDescription(e.target.value)}
              placeholder="e.g. Netflix, Airtel Fiber, House Rent, Gym"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Next Debit Date</label>
            <input type="date" value={nextRunDate} onChange={e => setNextRunDate(e.target.value)} />
          </div>

          <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
            <button type="button" className="btn btn-secondary" onClick={onClose} style={{ flex: 1 }}>
              Cancel
            </button>
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
  const [goalPresetId, setGoalPresetId] = useState<string | undefined>();
  const [showDebtModal, setShowDebtModal] = useState(false);
  const [showChitModal, setShowChitModal] = useState(false);
  const [showRecurringModal, setShowRecurringModal] = useState(false);

  const totalDebt = debts.reduce((s, d) => s + d.current_balance_minor, 0);
  const totalGoalTarget = goals.reduce((s, g) => s + g.target_amount_minor, 0);
  const totalGoalCurrent = goals.reduce((s, g) => s + g.current_amount_minor, 0);

  const openGoalWithPreset = (presetId?: string) => {
    setGoalPresetId(presetId);
    setShowGoalModal(true);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 14 }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.02em', margin: 0 }}>
            Plans, Goals & Loans
          </h1>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)', marginTop: 4 }}>
            Save for goals, track chit funds, manage loans, and organize monthly bills
          </p>
        </div>

        {/* Direct Action Buttons for Each Commitment Type */}
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <button
            className="btn btn-sm btn-primary"
            onClick={() => openGoalWithPreset()}
            style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
          >
            <Plus size={15} /> 🎯 New Goal / Emergency Fund
          </button>
          <button
            className="btn btn-sm btn-secondary"
            onClick={() => setShowChitModal(true)}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              borderColor: 'rgba(245, 158, 11, 0.45)',
              background: 'rgba(245, 158, 11, 0.08)',
              color: '#f8fafc',
            }}
          >
            <Plus size={15} /> 🪙 Add Chit Fund
          </button>
          <button
            className="btn btn-sm btn-secondary"
            onClick={() => setShowDebtModal(true)}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              borderColor: 'rgba(244, 63, 94, 0.45)',
              background: 'rgba(244, 63, 94, 0.08)',
              color: '#f8fafc',
            }}
          >
            <Plus size={15} /> 💳 Add Loan / EMI
          </button>
          <button
            className="btn btn-sm btn-secondary"
            onClick={() => setShowRecurringModal(true)}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              borderColor: 'rgba(168, 85, 247, 0.45)',
              background: 'rgba(168, 85, 247, 0.08)',
              color: '#f8fafc',
            }}
          >
            <Plus size={15} /> 🔄 Add Monthly Bill
          </button>
        </div>
      </div>

      {/* 3D Overview Metrics — CLICKABLE INTERACTIVE TABS */}
      <div className="bento-grid">
        {/* Card 1: Goals Progress */}
        <div className="col-span-3" onClick={() => setActiveTab('goals')} style={{ cursor: 'pointer' }}>
          <Tilt3DCard>
            <div
              className="cyber-card"
              style={{
                border: activeTab === 'goals' ? '2px solid var(--neon-cyan)' : '1px solid var(--glass-border)',
                boxShadow: activeTab === 'goals' ? '0 0 20px rgba(0, 240, 255, 0.35)' : 'none',
                transition: 'all 0.2s ease',
              }}
            >
              <div className="cyber-card-glow" style={{ background: 'var(--neon-cyan)' }} />
              <div className="stat-label">
                <span>🎯 Goals & Emergency Fund</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-cyan-glow)', color: 'var(--neon-cyan)' }}>
                  <Target size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-cyan)' }}>
                {formatMoney(totalGoalCurrent, currency, masked)}
              </div>
              <div className="stat-trend neutral">of {formatMoney(totalGoalTarget, currency, masked)} target</div>
              {activeTab === 'goals' && (
                <div style={{ marginTop: 8, fontSize: '0.72rem', color: 'var(--neon-cyan)', fontWeight: 700 }}>
                  ● Active View
                </div>
              )}
            </div>
          </Tilt3DCard>
        </div>

        {/* Card 2: Outstanding Debt / Loans */}
        <div className="col-span-3" onClick={() => setActiveTab('debts')} style={{ cursor: 'pointer' }}>
          <Tilt3DCard>
            <div
              className="cyber-card"
              style={{
                border: activeTab === 'debts' ? '2px solid var(--neon-rose)' : '1px solid var(--glass-border)',
                boxShadow: activeTab === 'debts' ? '0 0 20px rgba(244, 63, 94, 0.35)' : 'none',
                transition: 'all 0.2s ease',
              }}
            >
              <div className="cyber-card-glow" style={{ background: 'var(--neon-rose)' }} />
              <div className="stat-label">
                <span>💳 Loans & Outstanding EMI</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-rose-glow)', color: 'var(--neon-rose)' }}>
                  <CreditCard size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-rose)' }}>
                {formatMoney(totalDebt, currency, masked)}
              </div>
              <div className="stat-trend negative">{debts.length} active loans / EMIs</div>
              {activeTab === 'debts' && (
                <div style={{ marginTop: 8, fontSize: '0.72rem', color: 'var(--neon-rose)', fontWeight: 700 }}>
                  ● Active View
                </div>
              )}
            </div>
          </Tilt3DCard>
        </div>

        {/* Card 3: Chit Fund Pools */}
        <div className="col-span-3" onClick={() => setActiveTab('chits')} style={{ cursor: 'pointer' }}>
          <Tilt3DCard>
            <div
              className="cyber-card"
              style={{
                border: activeTab === 'chits' ? '2px solid var(--neon-amber)' : '1px solid var(--glass-border)',
                boxShadow: activeTab === 'chits' ? '0 0 20px rgba(245, 158, 11, 0.35)' : 'none',
                transition: 'all 0.2s ease',
              }}
            >
              <div className="cyber-card-glow" style={{ background: 'var(--neon-amber)' }} />
              <div className="stat-label">
                <span>🪙 Chit Funds</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-amber-glow)', color: 'var(--neon-amber)' }}>
                  <Coins size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-amber)' }}>
                {chitFunds.length}
              </div>
              <div className="stat-trend neutral">Traditional wealth schemes</div>
              {activeTab === 'chits' && (
                <div style={{ marginTop: 8, fontSize: '0.72rem', color: 'var(--neon-amber)', fontWeight: 700 }}>
                  ● Active View
                </div>
              )}
            </div>
          </Tilt3DCard>
        </div>

        {/* Card 4: Monthly Recurring */}
        <div className="col-span-3" onClick={() => setActiveTab('recurring')} style={{ cursor: 'pointer' }}>
          <Tilt3DCard>
            <div
              className="cyber-card"
              style={{
                border: activeTab === 'recurring' ? '2px solid var(--neon-purple)' : '1px solid var(--glass-border)',
                boxShadow: activeTab === 'recurring' ? '0 0 20px rgba(168, 85, 247, 0.35)' : 'none',
                transition: 'all 0.2s ease',
              }}
            >
              <div className="cyber-card-glow" style={{ background: 'var(--neon-purple)' }} />
              <div className="stat-label">
                <span>🔄 Monthly Subscriptions & Bills</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-purple-glow)', color: 'var(--neon-purple)' }}>
                  <Repeat size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-purple)' }}>
                {formatMoney(
                  recurringTxns.filter(r => r.type === 'EXPENSE').reduce((s, r) => s + r.amount_minor, 0),
                  currency,
                  masked
                )}
              </div>
              <div className="stat-trend neutral">{recurringTxns.length} active bills</div>
              {activeTab === 'recurring' && (
                <div style={{ marginTop: 8, fontSize: '0.72rem', color: 'var(--neon-purple)', fontWeight: 700 }}>
                  ● Active View
                </div>
              )}
            </div>
          </Tilt3DCard>
        </div>
      </div>

      {/* Cyber Tab Navigation */}
      <div
        style={{
          display: 'flex',
          gap: 6,
          background: 'rgba(255, 255, 255, 0.04)',
          padding: 5,
          borderRadius: 'var(--radius-pill)',
          border: '1px solid var(--glass-border)',
          width: 'fit-content',
          maxWidth: '100%',
          flexWrap: 'wrap',
        }}
      >
        {[
          { id: 'goals', label: '🎯 Savings Goals & Emergency Fund', count: goals.length },
          { id: 'debts', label: '💳 Loans & EMIs', count: debts.length },
          { id: 'chits', label: '🪙 Chit Funds', count: chitFunds.length },
          { id: 'recurring', label: '🔄 Monthly Subscriptions & Bills', count: recurringTxns.length },
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`btn btn-sm ${activeTab === tab.id ? 'btn-primary' : 'btn-ghost'}`}
            style={{ borderRadius: 'var(--radius-pill)', fontSize: '0.82rem', padding: '7px 18px', fontWeight: 700 }}
          >
            {tab.label} ({tab.count})
          </button>
        ))}
      </div>

      {/* Goals Tab */}
      {activeTab === 'goals' && (
        goals.length > 0 ? (
          <div className="responsive-grid-cards">
            {goals.map(goal => {
              const pct = goal.target_amount_minor > 0 ? Math.min((goal.current_amount_minor / goal.target_amount_minor) * 100, 100) : 0;
              const remaining = goal.target_amount_minor - goal.current_amount_minor;

              return (
                <Tilt3DCard key={goal.id}>
                  <div className="cyber-card" style={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 12 }}>
                        <span className="badge badge-cyan">SAVINGS GOAL</span>
                        <span style={{ fontFamily: 'var(--font-outfit)', fontWeight: 800, fontSize: '1.2rem', color: pct >= 100 ? 'var(--neon-emerald)' : 'var(--neon-cyan)' }}>
                          {pct.toFixed(0)}%
                        </span>
                      </div>

                      <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                        {goal.name}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                        Target Date: {goal.target_date}
                      </div>

                      <div className="cyber-progress-track" style={{ height: 10, margin: '16px 0 10px 0' }}>
                        <div className="cyber-progress-fill" style={{ width: `${pct}%`, background: 'linear-gradient(90deg, var(--neon-cyan), var(--neon-emerald))' }} />
                      </div>

                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                        <span style={{ fontWeight: 700, color: '#f8fafc' }}>{formatMoney(goal.current_amount_minor, currency, masked)}</span>
                        <span>of {formatMoney(goal.target_amount_minor, currency, masked)}</span>
                      </div>
                    </div>

                    <div style={{ marginTop: 16, paddingTop: 12, borderTop: '1px solid var(--glass-border)', fontSize: '0.78rem', color: 'var(--text-tertiary)' }}>
                      Remaining to save: <span style={{ color: 'var(--neon-cyan)', fontWeight: 600 }}>{formatMoney(Math.max(0, remaining), currency, masked)}</span>
                    </div>
                  </div>
                </Tilt3DCard>
              );
            })}
          </div>
        ) : (
          <div className="cyber-card" style={{ textAlign: 'center', padding: '48px 24px', color: 'var(--text-tertiary)' }}>
            <Target size={42} style={{ margin: '0 auto 12px auto', color: 'var(--neon-cyan)', opacity: 0.85 }} />
            <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, color: '#f1f5f9' }}>
              No Financial Goals Set Yet
            </div>
            <p style={{ fontSize: '0.88rem', marginTop: 4, maxWidth: 520, marginLeft: 'auto', marginRight: 'auto', marginBottom: 24, lineHeight: 1.5 }}>
              Choose a popular goal template below to start tracking your emergency fund, gold savings, house down payment, or child education.
            </p>

            {/* Quick Starter Cards */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 12, maxWidth: 860, margin: '0 auto 24px auto', textAlign: 'left' }}>
              {GOAL_PRESETS.slice(0, 4).map(p => (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => openGoalWithPreset(p.id)}
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    padding: 14,
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid rgba(255, 255, 255, 0.08)',
                    background: 'rgba(255, 255, 255, 0.03)',
                    cursor: 'pointer',
                    transition: 'all 0.16s ease',
                  }}
                >
                  <div style={{ fontSize: '1.6rem', marginBottom: 6 }}>{p.icon}</div>
                  <div style={{ fontWeight: 700, fontSize: '0.88rem', color: '#f8fafc', marginBottom: 2 }}>{p.badge}</div>
                  <div style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)', lineHeight: 1.3 }}>{p.tip}</div>
                </button>
              ))}
            </div>

            <button className="btn btn-primary btn-md" onClick={() => openGoalWithPreset()}>
              <Plus size={16} /> Create Custom Goal
            </button>
          </div>
        )
      )}

      {/* Debts Tab */}
      {activeTab === 'debts' && (
        debts.length > 0 ? (
          <div className="responsive-grid-cards">
            {debts.map(debt => (
              <Tilt3DCard key={debt.id}>
                <div className="cyber-card" style={{ height: '100%', borderLeft: '3px solid var(--neon-rose)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                    <span className="badge badge-rose">LOAN / EMI</span>
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.82rem', color: 'var(--neon-amber)' }}>
                      {debt.annual_percent}% p.a.
                    </span>
                  </div>

                  <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.15rem', fontWeight: 800 }}>{debt.name}</div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                    Monthly EMI: {formatMoney(debt.expected_monthly_payment_minor, currency, masked)}
                  </div>

                  <div style={{ marginTop: 20 }}>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Current Balance Due</div>
                    <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.6rem', fontWeight: 900, color: 'var(--neon-rose)' }}>
                      {formatMoney(debt.current_balance_minor, currency, masked)}
                    </div>
                  </div>
                </div>
              </Tilt3DCard>
            ))}
          </div>
        ) : (
          <div className="cyber-card" style={{ textAlign: 'center', padding: '48px 24px', color: 'var(--text-tertiary)' }}>
            <CreditCard size={42} style={{ margin: '0 auto 12px auto', color: 'var(--neon-rose)', opacity: 0.85 }} />
            <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, color: '#f1f5f9' }}>
              No Debt or Loans Recorded
            </div>
            <p style={{ fontSize: '0.88rem', marginTop: 4, maxWidth: 500, marginLeft: 'auto', marginRight: 'auto', marginBottom: 20, lineHeight: 1.5 }}>
              Track home loans, car loans, two-wheeler EMIs, jewel loans, or personal loans to know your exact payoff schedule.
            </p>
            <button className="btn btn-primary btn-md" onClick={() => setShowDebtModal(true)} style={{ background: 'var(--neon-rose)' }}>
              <Plus size={16} /> Record Debt or Loan
            </button>
          </div>
        )
      )}

      {/* Chit Funds Tab */}
      {activeTab === 'chits' && (
        chitFunds.length > 0 ? (
          <div className="responsive-grid-cards">
            {chitFunds.map(chit => {
              const totalPoolValue = chit.monthly_contribution_minor * chit.total_months;

              return (
                <Tilt3DCard key={chit.id}>
                  <div className="cyber-card" style={{ height: '100%', borderLeft: '3px solid var(--neon-amber)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                      <span className="badge badge-amber">🪙 CHIT FUND</span>
                      <span className="badge badge-green">Active</span>
                    </div>

                    <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.2rem', fontWeight: 800, color: '#f8fafc' }}>
                      {chit.name}
                    </div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)', marginTop: 4 }}>
                      Duration: <strong>{chit.total_months} Months</strong> • Start Month: {chit.start_month}
                    </div>

                    <div style={{ marginTop: 18, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, padding: '10px 12px', borderRadius: 'var(--radius-md)', background: 'rgba(255, 255, 255, 0.03)' }}>
                      <div>
                        <div style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Monthly Installment</div>
                        <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, color: 'var(--neon-amber)' }}>
                          {formatMoney(chit.monthly_contribution_minor, currency, masked)}
                        </div>
                      </div>
                      <div>
                        <div style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Total Chit Value</div>
                        <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, color: '#f8fafc' }}>
                          {formatMoney(totalPoolValue, currency, masked)}
                        </div>
                      </div>
                    </div>
                  </div>
                </Tilt3DCard>
              );
            })}
          </div>
        ) : (
          <div className="cyber-card" style={{ textAlign: 'center', padding: '48px 24px', color: 'var(--text-tertiary)' }}>
            <Coins size={42} style={{ margin: '0 auto 12px auto', color: 'var(--neon-amber)', opacity: 0.85 }} />
            <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, color: '#f1f5f9' }}>
              No Chit Fund Schemes Added
            </div>
            <p style={{ fontSize: '0.88rem', marginTop: 4, maxWidth: 520, marginLeft: 'auto', marginRight: 'auto', marginBottom: 20, lineHeight: 1.5 }}>
              Chit Funds (like KSFE, Shriram Chits, or local group chits) are widely used for disciplined monthly savings and lump sum payouts.
            </p>

            {/* Quick 1-Click Starter Chit Buttons */}
            <div style={{ display: 'flex', gap: 8, justifyContent: 'center', flexWrap: 'wrap', marginBottom: 24 }}>
              {CHIT_PRESETS.map((p, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setShowChitModal(true)}
                  style={{
                    padding: '8px 14px',
                    borderRadius: 'var(--radius-pill)',
                    border: '1px solid rgba(245, 158, 11, 0.35)',
                    background: 'rgba(245, 158, 11, 0.08)',
                    color: '#f8fafc',
                    fontSize: '0.82rem',
                    cursor: 'pointer',
                  }}
                >
                  🪙 {p.name}
                </button>
              ))}
            </div>

            <button className="btn btn-primary btn-md" onClick={() => setShowChitModal(true)} style={{ background: 'var(--neon-amber)', color: '#000' }}>
              <Plus size={16} /> Add Your First Chit Fund
            </button>
          </div>
        )
      )}

      {/* Recurring Tab */}
      {activeTab === 'recurring' && (
        recurringTxns.length > 0 ? (
          <div className="responsive-grid-cards">
            {recurringTxns.map(rec => (
              <Tilt3DCard key={rec.id}>
                <div className="cyber-card" style={{ height: '100%', borderLeft: '3px solid var(--neon-purple)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                    <span className="badge badge-purple">{rec.frequency}</span>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>Next Debit: {rec.next_run_date}</span>
                  </div>
                  <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.15rem', fontWeight: 800 }}>
                    {rec.description || 'Subscription'}
                  </div>

                  <div style={{ marginTop: 20 }}>
                    <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.6rem', fontWeight: 900, color: 'var(--neon-purple)' }}>
                      {formatMoney(rec.amount_minor, currency, masked)}
                    </div>
                  </div>
                </div>
              </Tilt3DCard>
            ))}
          </div>
        ) : (
          <div className="cyber-card" style={{ textAlign: 'center', padding: '48px 24px', color: 'var(--text-tertiary)' }}>
            <Repeat size={42} style={{ margin: '0 auto 12px auto', color: 'var(--neon-purple)', opacity: 0.85 }} />
            <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, color: '#f1f5f9' }}>
              No Recurring Subscriptions or Bills
            </div>
            <p style={{ fontSize: '0.88rem', marginTop: 4, maxWidth: 500, marginLeft: 'auto', marginRight: 'auto', marginBottom: 20, lineHeight: 1.5 }}>
              Track automatic recurring charges like monthly house rent, broadband WiFi, OTT packs (Netflix, Prime), and gym memberships.
            </p>
            <button className="btn btn-primary btn-md" onClick={() => setShowRecurringModal(true)}>
              <Plus size={16} /> Add Recurring Subscription
            </button>
          </div>
        )
      )}

      {showGoalModal && <GoalModal initialPresetId={goalPresetId} onClose={() => setShowGoalModal(false)} />}
      {showDebtModal && <DebtModal onClose={() => setShowDebtModal(false)} />}
      {showChitModal && <ChitFundModal onClose={() => setShowChitModal(false)} />}
      {showRecurringModal && <RecurringModal onClose={() => setShowRecurringModal(false)} />}
    </div>
  );
}
