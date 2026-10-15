import { useMemo } from 'react';
import { useData } from '../../hooks/useData';
import { formatMoney, getCategoryDisplayName } from '../../types';
import { HoloCard3D } from '../shared/HoloCard3D';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import {
  TrendingUp,
  TrendingDown,
  ArrowUpRight,
  ArrowDownRight,
  Shield,
  Zap,
  Plus,
  Flame,
  PieChart as PieIcon,
  CreditCard,
  ChevronRight,
  Clock,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { format, subMonths, startOfMonth, endOfMonth } from 'date-fns';
import { useNavigate } from 'react-router-dom';

interface DashboardProps {
  onOpenNewTxn?: () => void;
}

export function Dashboard({ onOpenNewTxn }: DashboardProps) {
  const navigate = useNavigate();
  const { accounts, transactions, journalEntries, settings, categories, updateSettings } = useData();
  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;
  const userName = settings?.user_name || 'WealthOS User';

  const now = new Date();
  const currentMonthStart = format(startOfMonth(now), 'yyyy-MM-dd');
  const currentMonthEnd = format(endOfMonth(now), 'yyyy-MM-dd');

  // Compute live account balances
  const computedAccounts = useMemo(() => accounts.map(acc => {
    const journalSum = journalEntries
      .filter(je => je.account_id === acc.id)
      .reduce((sum, je) => {
        const txn = transactions.find(t => t.id === je.txn_id);
        if (txn && !txn.deleted_at) return sum + je.amount_minor;
        return sum;
      }, 0);
    return { ...acc, current_balance_minor: acc.opening_balance_minor + journalSum };
  }), [accounts, journalEntries, transactions]);

  const liquidTypes = ['BANK', 'CASH', 'UPI_WALLET', 'SAVINGS'];
  const liquidBalance = computedAccounts
    .filter(a => liquidTypes.includes(a.type))
    .reduce((s, a) => s + (a.current_balance_minor || 0), 0);

  const netWorth = computedAccounts
    .filter(a => a.include_in_net_worth)
    .reduce((s, a) => s + (a.current_balance_minor || 0), 0);

  // Monthly totals
  const monthlyExpense = transactions
    .filter(t => t.type === 'EXPENSE' && !t.deleted_at && t.local_date >= currentMonthStart && t.local_date <= currentMonthEnd)
    .reduce((s, t) => s + t.amount_minor, 0);

  const monthlyIncome = transactions
    .filter(t => t.type === 'INCOME' && !t.deleted_at && t.local_date >= currentMonthStart && t.local_date <= currentMonthEnd)
    .reduce((s, t) => s + t.amount_minor, 0);

  // 50/30/20 breakdown — compare against take-home income (falls back to settings income)
  const basisIncome = settings?.basis_income_minor || 0;
  const incomeBase = monthlyIncome > 0 ? monthlyIncome : basisIncome;

  const needsSpend = transactions
    .filter(t => t.type === 'EXPENSE' && !t.deleted_at && t.need_want === 'NEED' && t.local_date >= currentMonthStart && t.local_date <= currentMonthEnd)
    .reduce((s, t) => s + t.amount_minor, 0);
  const wantsSpend = transactions
    .filter(t => t.type === 'EXPENSE' && !t.deleted_at && t.need_want === 'WANT' && t.local_date >= currentMonthStart && t.local_date <= currentMonthEnd)
    .reduce((s, t) => s + t.amount_minor, 0);
  // Savings = tagged SAVING type (SIPs, investments, goal transfers)
  const savingsSpend = transactions
    .filter(t => t.type === 'EXPENSE' && !t.deleted_at && t.need_want === 'SAVING' && t.local_date >= currentMonthStart && t.local_date <= currentMonthEnd)
    .reduce((s, t) => s + t.amount_minor, 0);

  // Safe-to-spend: this month's income minus this month's total expenses
  // (not liquid balance, which accumulates historical months)
  const monthNetSurplus = monthlyIncome - monthlyExpense;
  const safeToSpend = monthNetSurplus > 0 ? monthNetSurplus : 0;
  const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
  const daysRemaining = daysInMonth - now.getDate() + 1;
  const dailyBudget = daysRemaining > 0 ? Math.floor(safeToSpend / daysRemaining) : 0;

  // 6-month historical area trend
  const trendData = useMemo(() => {
    const months = [];
    for (let i = 5; i >= 0; i--) {
      const d = subMonths(now, i);
      const mStart = format(startOfMonth(d), 'yyyy-MM-dd');
      const mEnd = format(endOfMonth(d), 'yyyy-MM-dd');
      const label = format(d, 'MMM');
      const income = transactions
        .filter(t => t.type === 'INCOME' && !t.deleted_at && t.local_date >= mStart && t.local_date <= mEnd)
        .reduce((s, t) => s + t.amount_minor, 0);
      const expense = transactions
        .filter(t => t.type === 'EXPENSE' && !t.deleted_at && t.local_date >= mStart && t.local_date <= mEnd)
        .reduce((s, t) => s + t.amount_minor, 0);
      months.push({ month: label, income: income / 100, expense: expense / 100 });
    }
    return months;
  }, [transactions, now]);

  // Top Category Spending
  const topCategories = useMemo(() => {
    const catMap = new Map<string, number>();
    transactions
      .filter(t => t.type === 'EXPENSE' && !t.deleted_at && t.local_date >= currentMonthStart && t.local_date <= currentMonthEnd && t.category_id)
      .forEach(t => catMap.set(t.category_id!, (catMap.get(t.category_id!) || 0) + t.amount_minor));
    return Array.from(catMap.entries())
      .sort((a, b) => b[1] - a[1])
      .slice(0, 5)
      .map(([catId, total]) => {
        return {
          name: getCategoryDisplayName(catId, categories),
          amount: total,
          pct: monthlyExpense > 0 ? ((total / monthlyExpense) * 100).toFixed(0) : '0',
        };
      });
  }, [transactions, categories, currentMonthStart, currentMonthEnd, monthlyExpense]);

  const recentTxns = transactions.filter(t => !t.deleted_at).slice(0, 6);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* ========================================================
          HERO ROW: 3D HOLOGRAPHIC CARD + SPATIAL TELEMETRY
          ======================================================== */}
      <div className="bento-grid">
        {/* Left Column: Interactive 3D Card */}
        <div className="col-span-5" style={{ display: 'flex', alignItems: 'center' }}>
          <HoloCard3D
            netWorth={netWorth}
            liquidBalance={liquidBalance}
            userName={userName}
            currency={currency}
            masked={masked}
            onToggleMask={() => updateSettings({ mask_amounts: !masked })}
          />
        </div>

        {/* Right Column: Key Financial Metrics Bento */}
        <div className="col-span-7">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            {/* Monthly Inflow */}
            <Tilt3DCard>
              <div className="cyber-card" style={{ height: '100%' }}>
                <div className="cyber-card-glow" style={{ background: 'var(--neon-emerald)' }} />
                <div className="stat-label">
                  <span>Inflow This Month</span>
                  <div className="cyber-icon-badge" style={{ background: 'var(--neon-emerald-glow)', color: 'var(--neon-emerald)' }}>
                    <ArrowDownRight size={18} />
                  </div>
                </div>
                <div className="stat-value" style={{ color: 'var(--neon-emerald)' }}>
                  +{formatMoney(monthlyIncome, currency, masked)}
                </div>
                <div className="stat-trend positive">
                  <TrendingUp size={14} /> Active stream
                </div>
              </div>
            </Tilt3DCard>

            {/* Monthly Outflow */}
            <Tilt3DCard>
              <div className="cyber-card" style={{ height: '100%' }}>
                <div className="cyber-card-glow" style={{ background: 'var(--neon-rose)' }} />
                <div className="stat-label">
                  <span>Outflow This Month</span>
                  <div className="cyber-icon-badge" style={{ background: 'var(--neon-rose-glow)', color: 'var(--neon-rose)' }}>
                    <ArrowUpRight size={18} />
                  </div>
                </div>
                <div className="stat-value" style={{ color: 'var(--neon-rose)' }}>
                  -{formatMoney(monthlyExpense, currency, masked)}
                </div>
                <div className="stat-trend negative">
                  <TrendingDown size={14} /> Burn rate monitored
                </div>
              </div>
            </Tilt3DCard>

            {/* Safe to Spend & Daily Velocity */}
            <Tilt3DCard>
              <div className="cyber-card" style={{ height: '100%' }}>
                <div className="cyber-card-glow" style={{ background: 'var(--neon-cyan)' }} />
                <div className="stat-label">
                  <span>Safe To Spend</span>
                  <div className="cyber-icon-badge" style={{ background: 'var(--neon-cyan-glow)', color: 'var(--neon-cyan)' }}>
                    <Zap size={18} />
                  </div>
                </div>
                <div className="stat-value" style={{ color: 'var(--neon-cyan)' }}>
                  {formatMoney(safeToSpend, currency, masked)}
                </div>
                <div className="stat-trend neutral">
                  ≈ {formatMoney(dailyBudget, currency, masked)} / day ({daysRemaining} days left)
                </div>
              </div>
            </Tilt3DCard>

            {/* 50/30/20 Budget Telemetry */}
            <Tilt3DCard>
              <div className="cyber-card" style={{ height: '100%' }}>
                <div className="cyber-card-glow" style={{ background: 'var(--neon-purple)' }} />
                <div className="stat-label">
                  <span>50/30/20 vs Income</span>
                  <div className="cyber-icon-badge" style={{ background: 'var(--neon-purple-glow)', color: 'var(--neon-purple)' }}>
                    <PieIcon size={18} />
                  </div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 10 }}>
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: 'var(--text-secondary)' }}>
                      <span>Needs: {incomeBase > 0 ? Math.round((needsSpend / incomeBase) * 100) : 0}%</span>
                      <span style={{ color: (incomeBase > 0 && needsSpend / incomeBase > 0.55) ? 'var(--neon-rose)' : 'inherit' }}>Target: {settings?.needs_pct ?? 50}%</span>
                    </div>
                    <div className="cyber-progress-track" style={{ height: 6, marginTop: 3 }}>
                      <div className="cyber-progress-fill" style={{ width: `${incomeBase > 0 ? Math.min(100, (needsSpend / incomeBase) * 100) : 0}%`, background: 'var(--neon-cyan)' }} />
                    </div>
                  </div>
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: 'var(--text-secondary)' }}>
                      <span>Wants: {incomeBase > 0 ? Math.round((wantsSpend / incomeBase) * 100) : 0}%</span>
                      <span style={{ color: (incomeBase > 0 && wantsSpend / incomeBase > 0.35) ? 'var(--neon-rose)' : 'inherit' }}>Target: {settings?.wants_pct ?? 30}%</span>
                    </div>
                    <div className="cyber-progress-track" style={{ height: 6, marginTop: 3 }}>
                      <div className="cyber-progress-fill" style={{ width: `${incomeBase > 0 ? Math.min(100, (wantsSpend / incomeBase) * 100) : 0}%`, background: 'var(--neon-purple)' }} />
                    </div>
                  </div>
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: 'var(--text-secondary)' }}>
                      <span>Savings: {incomeBase > 0 ? Math.round((savingsSpend / incomeBase) * 100) : 0}%</span>
                      <span>Target: {settings?.savings_pct ?? 20}%</span>
                    </div>
                    <div className="cyber-progress-track" style={{ height: 6, marginTop: 3 }}>
                      <div className="cyber-progress-fill" style={{ width: `${incomeBase > 0 ? Math.min(100, (savingsSpend / incomeBase) * 100) : 0}%`, background: 'var(--neon-emerald)' }} />
                    </div>
                  </div>
                </div>
              </div>
            </Tilt3DCard>
          </div>
        </div>
      </div>

      {/* ========================================================
          ANALYTICS BENTO: CASHFLOW STREAM + TOP CATEGORIES
          ======================================================== */}
      <div className="bento-grid">
        {/* 6-Month Interactive Cashflow Stream */}
        <div className="col-span-8">
          <Tilt3DCard maxTilt={4}>
            <div className="cyber-card">
              <div className="cyber-header">
                <div className="cyber-title-group">
                  <div className="cyber-icon-badge" style={{ background: 'rgba(99, 102, 241, 0.2)', color: 'var(--neon-indigo)' }}>
                    <Flame size={18} />
                  </div>
                  <div>
                    <div className="cyber-title">6-Month Liquidity & Flow Dynamics</div>
                    <div className="cyber-subtitle">Income credits vs operating expenditures</div>
                  </div>
                </div>
                <button className="btn btn-secondary btn-sm" onClick={() => navigate('/insights')}>
                  Deep Analytics <ChevronRight size={14} />
                </button>
              </div>

              <div style={{ height: 260, width: '100%' }}>
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={trendData} margin={{ top: 10, right: 15, left: 10, bottom: 0 }}>
                    <defs>
                      <linearGradient id="incomeGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="var(--neon-emerald)" stopOpacity={0.4} />
                        <stop offset="95%" stopColor="var(--neon-emerald)" stopOpacity={0} />
                      </linearGradient>
                      <linearGradient id="expenseGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="var(--neon-rose)" stopOpacity={0.4} />
                        <stop offset="95%" stopColor="var(--neon-rose)" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                    <XAxis dataKey="month" tick={{ fill: '#64748b', fontSize: 12 }} axisLine={false} tickLine={false} />
                    <YAxis
                      width={65}
                      tick={{ fill: '#64748b', fontSize: 11 }}
                      axisLine={false}
                      tickLine={false}
                      tickFormatter={(val: number) => val >= 1000 ? `₹${(val / 1000).toFixed(0)}k` : `₹${val}`}
                    />
                    <Tooltip
                      contentStyle={{
                        background: 'rgba(13, 19, 34, 0.95)',
                        border: '1px solid var(--glass-border-bright)',
                        borderRadius: 12,
                        backdropFilter: 'blur(16px)',
                        boxShadow: '0 10px 25px rgba(0,0,0,0.5)',
                        fontSize: 12,
                      }}
                      formatter={(val: any) => formatMoney(Math.round(Number(val) * 100), currency, masked)}
                    />
                    <Area type="monotone" dataKey="income" stroke="var(--neon-emerald)" strokeWidth={2.5} fill="url(#incomeGrad)" name="Income" />
                    <Area type="monotone" dataKey="expense" stroke="var(--neon-rose)" strokeWidth={2.5} fill="url(#expenseGrad)" name="Expense" />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>
          </Tilt3DCard>
        </div>

        {/* Top Spending Categories */}
        <div className="col-span-4">
          <Tilt3DCard maxTilt={5}>
            <div className="cyber-card" style={{ height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <div className="cyber-header">
                  <div className="cyber-title-group">
                    <div className="cyber-icon-badge" style={{ background: 'rgba(245, 158, 11, 0.2)', color: 'var(--neon-amber)' }}>
                      <PieIcon size={18} />
                    </div>
                    <div>
                      <div className="cyber-title">Top Expenditures</div>
                      <div className="cyber-subtitle">This month's primary outflows</div>
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                  {topCategories.map((item, i) => (
                    <div key={item.name}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', marginBottom: 4 }}>
                        <span style={{ fontWeight: 600 }}>{item.name}</span>
                        <span style={{ color: 'var(--text-secondary)', fontFamily: 'var(--font-mono)' }}>
                          {item.pct}% ({formatMoney(item.amount, currency, masked)})
                        </span>
                      </div>
                      <div className="cyber-progress-track">
                        <div
                          className="cyber-progress-fill"
                          style={{
                            width: `${item.pct}%`,
                            background: [
                              'linear-gradient(90deg, #06b6d4, #3b82f6)',
                              'linear-gradient(90deg, #a855f7, #6366f1)',
                              'linear-gradient(90deg, #f43f5e, #fb923c)',
                              'linear-gradient(90deg, #10b981, #14b8a6)',
                              'linear-gradient(90deg, #f59e0b, #eab308)',
                            ][i % 5],
                          }}
                        />
                      </div>
                    </div>
                  ))}

                  {topCategories.length === 0 && (
                    <div style={{ textAlign: 'center', padding: '30px 0', color: 'var(--text-tertiary)' }}>
                      No expenses recorded this month
                    </div>
                  )}
                </div>
              </div>

              {onOpenNewTxn && (
                <button className="btn btn-secondary btn-sm" onClick={onOpenNewTxn} style={{ marginTop: 16, width: '100%' }}>
                  <Plus size={14} /> Quick Entry
                </button>
              )}
            </div>
          </Tilt3DCard>
        </div>
      </div>

      {/* ========================================================
          RECENT TRANSACTIONS STREAM
          ======================================================== */}
      <div className="cyber-card">
        <div className="cyber-header">
          <div className="cyber-title-group">
            <div className="cyber-icon-badge" style={{ background: 'rgba(6, 182, 212, 0.2)', color: 'var(--neon-cyan)' }}>
              <Clock size={18} />
            </div>
            <div>
              <div className="cyber-title">Real-Time Transaction Stream</div>
              <div className="cyber-subtitle">Instant synchronization with your Android device</div>
            </div>
          </div>
          <button className="btn btn-secondary btn-sm" onClick={() => navigate('/ledger')}>
            View Full Ledger <ChevronRight size={14} />
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {recentTxns.map((txn) => {
            const isExpense = txn.type === 'EXPENSE';
            const isIncome = txn.type === 'INCOME';
            const sign = isExpense ? '-' : isIncome ? '+' : '';
            const color = isExpense ? 'var(--neon-rose)' : isIncome ? 'var(--neon-emerald)' : 'var(--neon-cyan)';

            return (
              <div
                key={txn.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '12px 16px',
                  background: 'rgba(255, 255, 255, 0.02)',
                  border: '1px solid var(--glass-border)',
                  borderRadius: 'var(--radius-md)',
                  transition: 'all 0.2s ease',
                }}
                onMouseEnter={e => {
                  e.currentTarget.style.background = 'rgba(255, 255, 255, 0.05)';
                  e.currentTarget.style.borderColor = 'var(--glass-border-bright)';
                  e.currentTarget.style.transform = 'translateX(4px)';
                }}
                onMouseLeave={e => {
                  e.currentTarget.style.background = 'rgba(255, 255, 255, 0.02)';
                  e.currentTarget.style.borderColor = 'var(--glass-border)';
                  e.currentTarget.style.transform = 'translateX(0)';
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                  <div
                    style={{
                      width: 40,
                      height: 40,
                      borderRadius: 12,
                      background: isExpense ? 'var(--neon-rose-glow)' : isIncome ? 'var(--neon-emerald-glow)' : 'var(--neon-cyan-glow)',
                      color,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 800,
                    }}
                  >
                    {isExpense ? <ArrowUpRight size={18} /> : isIncome ? <ArrowDownRight size={18} /> : <CreditCard size={18} />}
                  </div>
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.92rem', color: 'var(--text-primary)' }}>
                      {txn.merchant || txn.description || 'Transaction'}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span>{txn.local_date}</span>
                      <span>•</span>
                      <span>{accounts.find(a => a.id === txn.account_id)?.name || 'Account'}</span>
                      {txn.category_id && <span>• {getCategoryDisplayName(txn.category_id, categories)}</span>}
                      {txn.need_want && (
                        <>
                          <span>•</span>
                          <span className={`badge ${txn.need_want === 'NEED' ? 'badge-cyan' : txn.need_want === 'WANT' ? 'badge-purple' : 'badge-green'}`}>
                            {txn.need_want}
                          </span>
                        </>
                      )}
                    </div>
                  </div>
                </div>

                <div
                  style={{
                    fontFamily: 'var(--font-outfit)',
                    fontWeight: 800,
                    fontSize: '1.1rem',
                    color,
                  }}
                >
                  {sign}{formatMoney(txn.amount_minor, currency, masked)}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
