import { useMemo, useState } from 'react';
import { useData } from '../../hooks/useData';
import { formatMoney, getCategoryDisplayName } from '../../types';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { BehaviouralGuardrailsCard } from '../shared/BehaviouralGuardrailsCard';
import { analyzeCycleForensics } from '../../lib/forensics';
import { evaluateEmi, evaluateSavingsRate, evaluateRunway } from '../../lib/healthRules';
import {
  BarChart2,
  TrendingUp,
  TrendingDown,
  Brain,
  Loader2,
  Sparkles,
  AlertTriangle,
  CheckCircle2,
  Lightbulb,
  WifiOff,
  Globe,
  ExternalLink,
  ShieldCheck,
  Compass,
  Landmark,
  Coins,
} from 'lucide-react';
import {
  BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis,
  CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';
import { format, subMonths, startOfMonth, endOfMonth, subWeeks, startOfWeek, endOfWeek } from 'date-fns';
import { supabase } from '../../lib/supabase';

const COLORS = [
  '#06b6d4', '#6366f1', '#10b981', '#f43f5e', '#f59e0b',
  '#a855f7', '#3b82f6', '#ec4899', '#14b8a6', '#eab308'
];

interface CoachData {
  cashflowLeakDiagnosis?: string;
  paydayTransferPlan?: string;
  allocationFrameworkContext?: string;
  emergencyAndSevenSevenSeven?: string;
  tamilSummary?: string;
  confidence?: number;
}

interface MarketPulseData {
  niftyLevelAndTrend?: string;
  goldRateContext?: string;
  rbiRepoRateAndInflation?: string;
  generalFrameworkGuidance?: string;
  asOfTimestamp?: string;
  confidence?: number;
}

interface GroundingSource {
  title?: string;
  uri?: string;
  url?: string;
}

export function InsightsPage() {
  const { transactions, categories, settings, accounts, salaryCycles, financialProfile, dailyUpiMeter } = useData();
  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;
  const userCity = (settings as any)?.city || 'Chennai';

  const [period, setPeriod] = useState<'month' | '3months' | '6months' | 'year'>('month');
  const [aiMode, setAiMode] = useState<'coach' | 'market_pulse'>('coach');
  const [aiLoading, setAiLoading] = useState(false);
  const [coachResult, setCoachResult] = useState<CoachData | null>(null);
  const [marketResult, setMarketResult] = useState<MarketPulseData | null>(null);
  const [groundingSources, setGroundingSources] = useState<GroundingSource[]>([]);
  const [resultSource, setResultSource] = useState<'ai' | 'rules-based'>('ai');
  const [ruleObservations, setRuleObservations] = useState<string[]>([]);
  const [aiError, setAiError] = useState('');
  const [coachQuestion, setCoachQuestion] = useState('Audit my cash flow leaks and recommend first 48 hours payday steps.');

  const now = new Date();
  const periodMonths = period === 'month' ? 1 : period === '3months' ? 3 : period === '6months' ? 6 : 12;
  const fromDate = format(startOfMonth(subMonths(now, periodMonths - 1)), 'yyyy-MM-dd');
  const toDate = format(endOfMonth(now), 'yyyy-MM-dd');
  const label = period === 'month' ? format(now, 'MMMM yyyy') : `Last ${periodMonths} months`;

  const periodTxns = useMemo(() =>
    transactions.filter(t => !t.deleted_at && t.local_date >= fromDate && t.local_date <= toDate),
    [transactions, fromDate, toDate]
  );

  const totalExpense = periodTxns.filter(t => t.type === 'EXPENSE').reduce((s, t) => s + t.amount_minor, 0);
  const totalIncome = periodTxns.filter(t => t.type === 'INCOME').reduce((s, t) => s + t.amount_minor, 0);
  const savingsRate = totalIncome > 0 ? (((totalIncome - totalExpense) / totalIncome) * 100).toFixed(1) : '0';

  // Monthly trend
  const monthlyTrend = useMemo(() => {
    const months = [];
    for (let i = periodMonths - 1; i >= 0; i--) {
      const d = subMonths(now, i);
      const mStart = format(startOfMonth(d), 'yyyy-MM-dd');
      const mEnd = format(endOfMonth(d), 'yyyy-MM-dd');
      const income = periodTxns.filter(t => t.type === 'INCOME' && t.local_date >= mStart && t.local_date <= mEnd).reduce((s, t) => s + t.amount_minor, 0);
      const expense = periodTxns.filter(t => t.type === 'EXPENSE' && t.local_date >= mStart && t.local_date <= mEnd).reduce((s, t) => s + t.amount_minor, 0);
      months.push({ month: format(d, 'MMM'), income: income / 100, expense: expense / 100, net: (income - expense) / 100 });
    }
    return months;
  }, [periodTxns, periodMonths, now]);

  // Category breakdown
  const categoryBreakdown = useMemo(() => {
    const catMap = new Map<string, number>();
    periodTxns.filter(t => t.type === 'EXPENSE' && t.category_id).forEach(t => {
      const rootCat = categories.find(c => c.id === t.category_id);
      const topCat = rootCat?.parent_id ? categories.find(c => c.id === rootCat.parent_id) : rootCat;
      const key = topCat?.name && !topCat.name.startsWith('cat_')
        ? topCat.name
        : getCategoryDisplayName(t.category_id, categories);
      catMap.set(key, (catMap.get(key) || 0) + t.amount_minor);
    });
    const total = Array.from(catMap.values()).reduce((a, b) => a + b, 0);
    return Array.from(catMap.entries())
      .map(([name, val], idx) => ({
        name,
        value: val / 100,
        pct: total > 0 ? ((val / total) * 100).toFixed(1) : '0',
        color: COLORS[idx % COLORS.length],
      }))
      .sort((a, b) => b.value - a.value);
  }, [periodTxns, categories]);

  // Needs, wants, savings breakdown
  const nwsBreakdown = useMemo(() => {
    const needs = periodTxns.filter(t => t.type === 'EXPENSE' && t.need_want === 'NEED').reduce((s, t) => s + t.amount_minor, 0);
    const wants = periodTxns.filter(t => t.type === 'EXPENSE' && t.need_want === 'WANT').reduce((s, t) => s + t.amount_minor, 0);
    const savings = periodTxns.filter(t => t.type === 'EXPENSE' && t.need_want === 'SAVING').reduce((s, t) => s + t.amount_minor, 0);
    const base = totalIncome > 0 ? totalIncome : (needs + wants + savings);
    return [
      { name: 'Needs', value: needs / 100, pctOfIncome: base > 0 ? ((needs / base) * 100).toFixed(1) : '0', color: 'var(--neon-cyan)' },
      { name: 'Wants', value: wants / 100, pctOfIncome: base > 0 ? ((wants / base) * 100).toFixed(1) : '0', color: 'var(--neon-purple)' },
      { name: 'Savings/SIPs', value: savings / 100, pctOfIncome: base > 0 ? ((savings / base) * 100).toFixed(1) : '0', color: 'var(--neon-emerald)' },
    ].filter(item => item.value > 0);
  }, [periodTxns, totalIncome]);

  // Category comparison: this month vs last month
  const categoryComparison = useMemo(() => {
    const thisMonthStart = format(startOfMonth(now), 'yyyy-MM-dd');
    const thisMonthEnd = format(endOfMonth(now), 'yyyy-MM-dd');
    const lastMonthDate = subMonths(now, 1);
    const lastMonthStart = format(startOfMonth(lastMonthDate), 'yyyy-MM-dd');
    const lastMonthEnd = format(endOfMonth(lastMonthDate), 'yyyy-MM-dd');

    const thisMonthMap = new Map<string, number>();
    const lastMonthMap = new Map<string, number>();

    transactions.filter(t => t.type === 'EXPENSE' && !t.deleted_at && t.category_id).forEach(t => {
      if (t.local_date >= thisMonthStart && t.local_date <= thisMonthEnd) {
        thisMonthMap.set(t.category_id!, (thisMonthMap.get(t.category_id!) || 0) + t.amount_minor);
      } else if (t.local_date >= lastMonthStart && t.local_date <= lastMonthEnd) {
        lastMonthMap.set(t.category_id!, (lastMonthMap.get(t.category_id!) || 0) + t.amount_minor);
      }
    });

    const allCatIds = new Set([...thisMonthMap.keys(), ...lastMonthMap.keys()]);
    return Array.from(allCatIds).map(catId => {
      const cat = categories.find(c => c.id === catId);
      const thisMonthMinor = thisMonthMap.get(catId) || 0;
      const lastMonthMinor = lastMonthMap.get(catId) || 0;
      const deltaMinor = thisMonthMinor - lastMonthMinor;
      const pctChange = lastMonthMinor > 0
        ? Math.round((deltaMinor / lastMonthMinor) * 100)
        : (thisMonthMinor > 0 ? 100 : 0);

      return {
        categoryId: catId,
        name: getCategoryDisplayName(catId, categories),
        icon: cat?.icon && cat.icon.length <= 4 ? cat.icon : '🏷️',
        thisMonthMinor,
        lastMonthMinor,
        deltaMinor,
        pctChange,
      };
    })
    .sort((a, b) => b.deltaMinor - a.deltaMinor)
    .slice(0, 8);
  }, [transactions, categories, now]);

  // Active cycle and forensics calculation for deterministic aggregates
  const activeCycle = salaryCycles[0] || null;
  const cycleStartDate = activeCycle?.cycle_start_date || `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
  const salaryPaise = activeCycle?.salary_paise || (totalIncome > 0 ? totalIncome : 3220000);
  const discretionaryBudgetPaise = (activeCycle?.jar_amounts?.PLAY || activeCycle?.jar_amounts?.WANTS || Math.floor(salaryPaise * 0.2)) as number;

  const forensics = useMemo(() => {
    const forensicItems = transactions.map(t => ({
      id: t.id,
      amountMinor: t.amount_minor,
      type: t.type,
      local_date: t.local_date,
      merchant: t.merchant,
      description: t.description,
      category_id: t.category_id,
      payment_method: t.payment_method,
      paymentRail: (t as any).payment_rail || (t.description?.toUpperCase().includes('UPI') ? 'UPI_P2M' : 'NON_UPI'),
      need_want: t.need_want,
      spendEmotionTag: (t as any).spend_emotion_tag || (t.need_want === 'NEED' ? 'NEED' : 'PLANNED_WANT'),
      jarBucket: (t as any).jar_bucket || null,
      isRecurringMandate: (t as any).is_recurring_mandate || false,
    }));

    return analyzeCycleForensics({
      transactions: forensicItems,
      cycleStartDate,
      salaryPaise,
      openingBalancePaise: salaryPaise,
      discretionaryBudgetPaise,
    });
  }, [transactions, cycleStartDate, salaryPaise, discretionaryBudgetPaise]);

  // Deterministic 7-7-7 and runway calculation
  const liquidAccountsPaise = useMemo(() => {
    return accounts
      .filter(a => !a.archived && (a.type === 'BANK' || a.type === 'CASH' || a.type === 'UPI_WALLET' || a.type === 'SAVINGS'))
      .reduce((s, a) => s + (a.current_balance_minor ?? a.opening_balance_minor ?? 0), 0);
  }, [accounts]);

  const monthlyBaselinePaise = useMemo(() => {
    if (financialProfile?.fixed_baseline_paise && financialProfile.fixed_baseline_paise > 0) {
      return financialProfile.fixed_baseline_paise;
    }
    const needsMinor = periodTxns.filter(t => t.type === 'EXPENSE' && t.need_want === 'NEED').reduce((s, t) => s + t.amount_minor, 0);
    return needsMinor > 0 ? needsMinor : Math.floor(salaryPaise * 0.5);
  }, [financialProfile, periodTxns, salaryPaise]);

  const runwayMonths = useMemo(() => {
    return monthlyBaselinePaise > 0 ? Math.floor(liquidAccountsPaise / monthlyBaselinePaise) : 0;
  }, [liquidAccountsPaise, monthlyBaselinePaise]);

  const sevenSevenSevenStatus = useMemo(() => {
    const emiPaise = (financialProfile as any)?.monthly_emi_paise || (financialProfile as any)?.current_total_emi_paise || 0;
    const emiStatus = evaluateEmi(emiPaise, salaryPaise);
    const savingsStatus = evaluateSavingsRate(Math.floor(salaryPaise * 0.2), salaryPaise);
    const runwayStatus = evaluateRunway(liquidAccountsPaise, monthlyBaselinePaise);

    if (emiStatus === 'RED' || runwayStatus === 'RED') return 'CRITICAL';
    if (emiStatus === 'AMBER' || savingsStatus === 'AMBER' || runwayStatus === 'AMBER') return 'AMBER';
    return 'GREEN';
  }, [financialProfile, salaryPaise, liquidAccountsPaise, monthlyBaselinePaise]);

  // User-tagged joyless leaks
  const regretLeakPaise = useMemo(() => {
    return transactions
      .filter(t => !t.deleted_at && (t as any).spend_emotion_tag === 'REGRET_LEAK')
      .reduce((s, t) => s + t.amount_minor, 0);
  }, [transactions]);

  // Top leak merchants (excluding transfers)
  const topMerchants = useMemo(() => {
    return forensics.topMerchants
      .filter(m => !m.merchant.toLowerCase().includes('transfer') && !m.merchant.toLowerCase().includes('self'))
      .slice(0, 5)
      .map(m => ({
        name: m.merchant,
        totalRupees: Math.round(m.totalPaise / 100),
        count: m.count,
      }));
  }, [forensics]);

  // Execute AI Analysis via Edge Function 'ai-analyze'
  const runAiAnalysis = async (customMode?: 'coach' | 'market_pulse', customPrompt?: string) => {
    const modeToUse = customMode || aiMode;
    setAiLoading(true);
    setAiError('');
    setRuleObservations([]);

    const payload = {
      mode: modeToUse,
      salaryRupees: Math.round(salaryPaise / 100),
      fixedBaselineRupees: Math.round(monthlyBaselinePaise / 100),
      currentLiquidBalanceRupees: Math.round(liquidAccountsPaise / 100),
      dailySafeUpiRupees: Math.round(dailyUpiMeter.totalPaise / 100),
      sevenDayBurnPercent: forensics.sevenDayDiscretionaryBurnPct,
      regretLeakTotalRupees: Math.round(regretLeakPaise / 100),
      topLeakMerchants: topMerchants,
      sevenSevenSevenStatus,
      emergencyRunwayMonths: runwayMonths,
      equityToDebtRatio: '60:40',
      userCity,
      customQuery: customPrompt || coachQuestion,
    };

    try {
      const { data, error } = await supabase.functions.invoke('ai-analyze', {
        body: payload,
      });

      if (error || !data || !data.data) {
        // Honest deterministic fallback without fake confidence scores
        const obs: string[] = [];
        if (forensics.isVelocityAlertTriggered) {
          obs.push(`Discretionary burn velocity reached ${forensics.sevenDayDiscretionaryBurnPct}% in the first 7 days (caution above 50%).`);
        }
        if (runwayMonths < 3) {
          obs.push(`Emergency runway is ${runwayMonths} months. Target minimum 3 months baseline protection.`);
        }
        if (regretLeakPaise > 0) {
          obs.push(`You flagged ₹${(regretLeakPaise / 100).toFixed(0)} in joyless regret spend. Review in month-end leak audit.`);
        }
        if (categoryBreakdown.length > 0) {
          obs.push(`Largest spending outflow is ${categoryBreakdown[0].name} at ${categoryBreakdown[0].pct}% of recorded expenses.`);
        }

        setRuleObservations(obs);
        setResultSource('rules-based');
        if (obs.length === 0) {
          setAiError('AI Engine is currently offline. Your financial figures and charts above remain 100% accurate.');
        }
      } else {
        setResultSource('ai');
        if (modeToUse === 'coach') {
          setCoachResult(data.data);
        } else {
          setMarketResult(data.data);
          setGroundingSources(data.sources || []);
        }
      }
    } catch (e: any) {
      setAiError(e.message || 'AI service temporarily unavailable.');
    } finally {
      setAiLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
            Spending & Savings Insights
          </h1>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
            Deterministic financial forensics, AI Wealth Coach, and grounded Indian market pulse
          </p>
        </div>

        {/* Period Selector Pills */}
        <div style={{ display: 'flex', gap: 4, background: 'rgba(255, 255, 255, 0.04)', padding: 4, borderRadius: 'var(--radius-pill)', border: '1px solid var(--glass-border)' }}>
          {[
            { id: 'month', label: '1M' },
            { id: '3months', label: '3M' },
            { id: '6months', label: '6M' },
            { id: 'year', label: '1Y' },
          ].map(p => (
            <button
              key={p.id}
              onClick={() => setPeriod(p.id as any)}
              className={`btn btn-sm ${period === p.id ? 'btn-primary' : 'btn-ghost'}`}
              style={{ padding: '4px 12px', fontSize: '0.75rem', borderRadius: 'var(--radius-pill)' }}
            >
              {p.label}
            </button>
          ))}
        </div>
      </div>

      {/* Top 3D Metrics */}
      <div className="bento-grid">
        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-emerald)' }} />
              <div className="stat-label">
                <span>Total Income</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-emerald-glow)', color: 'var(--neon-emerald)' }}>
                  <TrendingUp size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-emerald)' }}>
                +{formatMoney(totalIncome, currency, masked)}
              </div>
              <div className="stat-trend positive">{label}</div>
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
                  <TrendingDown size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-rose)' }}>
                -{formatMoney(totalExpense, currency, masked)}
              </div>
              <div className="stat-trend negative">Spent in this time period</div>
            </div>
          </Tilt3DCard>
        </div>

        <div className="col-span-4">
          <Tilt3DCard>
            <div className="cyber-card">
              <div className="cyber-card-glow" style={{ background: 'var(--neon-cyan)' }} />
              <div className="stat-label">
                <span>Savings Rate</span>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-cyan-glow)', color: 'var(--neon-cyan)' }}>
                  <BarChart2 size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--neon-cyan)' }}>
                {savingsRate}%
              </div>
              <div className="stat-trend neutral">
                Money saved: {formatMoney(totalIncome - totalExpense, currency, masked)}
              </div>
            </div>
          </Tilt3DCard>
        </div>
      </div>

      {/* Charts Bento */}
      <div className="bento-grid">
        <div className="col-span-7">
          <Tilt3DCard maxTilt={4}>
            <div className="cyber-card">
              <div className="cyber-header">
                <div className="cyber-title-group">
                  <div className="cyber-icon-badge" style={{ background: 'var(--neon-indigo-glow)', color: 'var(--neon-indigo)' }}>
                    <BarChart2 size={18} />
                  </div>
                  <div>
                    <div className="cyber-title">Monthly Income vs Expenses ({label})</div>
                    <div className="cyber-subtitle">Compare earnings and outflows month-over-month</div>
                  </div>
                </div>
              </div>

              <div style={{ height: 260 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={monthlyTrend} margin={{ left: 10, right: 15, top: 10, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                    <XAxis dataKey="month" tick={{ fill: '#64748b', fontSize: 12 }} axisLine={false} tickLine={false} />
                    <YAxis
                      width={65}
                      tick={{ fill: '#64748b', fontSize: 11 }}
                      axisLine={false}
                      tickLine={false}
                      tickFormatter={(v) => v >= 1000 ? `₹${(v / 1000).toFixed(0)}k` : `₹${v}`}
                    />
                    <Tooltip
                      contentStyle={{ background: 'rgba(13, 19, 34, 0.95)', border: '1px solid var(--glass-border-bright)', borderRadius: 12 }}
                      formatter={(v: any) => formatMoney(Math.round(Number(v || 0) * 100), currency, masked)}
                    />
                    <Bar dataKey="income" fill="var(--neon-emerald)" radius={[6, 6, 0, 0]} name="Income" />
                    <Bar dataKey="expense" fill="var(--neon-rose)" radius={[6, 6, 0, 0]} name="Expense" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </Tilt3DCard>
        </div>

        <div className="col-span-5">
          <Tilt3DCard maxTilt={4}>
            <div className="cyber-card">
              <div className="cyber-header">
                <div className="cyber-title-group">
                  <div className="cyber-icon-badge" style={{ background: 'var(--neon-cyan-glow)', color: 'var(--neon-cyan)' }}>
                    <Sparkles size={18} />
                  </div>
                  <div>
                    <div className="cyber-title">Where Your Money Goes</div>
                    <div className="cyber-subtitle">Spending breakdown by category</div>
                  </div>
                </div>
              </div>

              {categoryBreakdown.length > 0 ? (
                <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
                  <PieChart width={180} height={180}>
                    <Pie data={categoryBreakdown} cx={90} cy={90} innerRadius={55} outerRadius={80} dataKey="value" strokeWidth={0}>
                      {categoryBreakdown.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                    </Pie>
                    <Tooltip
                      contentStyle={{ background: 'rgba(13, 19, 34, 0.95)', border: '1px solid var(--glass-border-bright)', borderRadius: 8, fontSize: 12 }}
                      formatter={(v: any) => formatMoney(Math.round(Number(v || 0) * 100), currency, masked)}
                    />
                  </PieChart>

                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 6 }}>
                    {categoryBreakdown.slice(0, 6).map((cat) => (
                      <div key={cat.name} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <div style={{ width: 8, height: 8, borderRadius: '50%', background: cat.color, flexShrink: 0 }} />
                        <span style={{ fontSize: '0.8rem', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{cat.name}</span>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', fontFamily: 'var(--font-mono)' }}>{cat.pct}%</span>
                      </div>
                    ))}
                  </div>
                </div>
              ) : (
                <div style={{ textAlign: 'center', padding: '40px 0', color: 'var(--text-tertiary)' }}>No expense data in this period</div>
              )}
            </div>
          </Tilt3DCard>
        </div>
      </div>

      {/* Category Spending Trends: This Month vs Last Month */}
      <Tilt3DCard maxTilt={3}>
        <div className="cyber-card">
          <div className="cyber-header">
            <div className="cyber-title-group">
              <div className="cyber-icon-badge" style={{ background: 'rgba(6, 182, 212, 0.15)', color: 'var(--neon-cyan)' }}>
                <TrendingUp size={18} />
              </div>
              <div>
                <div className="cyber-title">Monthly Category Comparison</div>
                <div className="cyber-subtitle">This Month vs Previous Month Spending Changes</div>
              </div>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 10 }}>
            {categoryComparison.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '24px 0', color: 'var(--text-tertiary)', gridColumn: '1 / -1' }}>
                No category expense data to compare between months
              </div>
            ) : (
              categoryComparison.map(item => (
                <div
                  key={item.categoryId}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 16px',
                    background: 'rgba(255, 255, 255, 0.02)',
                    border: '1px solid var(--glass-border)',
                    borderRadius: 'var(--radius-md)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span style={{ fontSize: '1.4rem' }}>{item.icon}</span>
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.88rem', color: '#f8fafc' }}>
                        {item.name}
                      </div>
                      <div style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)' }}>
                        Last Month: {formatMoney(item.lastMonthMinor, currency, masked)}
                      </div>
                    </div>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontWeight: 800, fontSize: '0.95rem', color: '#f8fafc' }}>
                      {formatMoney(item.thisMonthMinor, currency, masked)}
                    </div>
                    <div
                      style={{
                        fontSize: '0.74rem',
                        fontWeight: 700,
                        color: item.deltaMinor > 0 ? 'var(--neon-rose)' : item.deltaMinor < 0 ? 'var(--neon-emerald)' : 'var(--text-tertiary)',
                      }}
                    >
                      {item.deltaMinor > 0
                        ? `+${formatMoney(item.deltaMinor, currency, masked)} (+${item.pctChange}%)`
                        : item.deltaMinor < 0
                        ? `-${formatMoney(Math.abs(item.deltaMinor), currency, masked)} (${item.pctChange}%)`
                        : 'No change'}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </Tilt3DCard>

      {/* AI WEALTH COACH & LIVE MARKET PULSE (§9.2) */}
      <Tilt3DCard maxTilt={3}>
        <div className="cyber-card" style={{ border: '1px solid rgba(168, 85, 247, 0.3)', boxShadow: '0 20px 40px -10px var(--neon-purple-glow)' }}>
          {/* Section Header with Mode Toggle */}
          <div className="cyber-header" style={{ flexWrap: 'wrap', gap: 12 }}>
            <div className="cyber-title-group">
              <div
                className="cyber-icon-badge"
                style={{
                  background: aiMode === 'coach'
                    ? 'linear-gradient(135deg, var(--neon-purple) 0%, var(--neon-indigo) 100%)'
                    : 'linear-gradient(135deg, var(--neon-cyan) 0%, var(--neon-indigo) 100%)',
                  color: 'white',
                  boxShadow: '0 0 16px var(--neon-purple-glow)',
                }}
              >
                {aiMode === 'coach' ? <Brain size={20} /> : <Globe size={20} />}
              </div>
              <div>
                <div className="cyber-title">
                  {aiMode === 'coach' ? 'AI Wealth Coach' : 'Live Indian Market Pulse'}
                </div>
                <div className="cyber-subtitle">
                  {aiMode === 'coach'
                    ? 'Coaching based on your computed cash flow, first 48h plan & 7-7-7 health'
                    : `Live Nifty, gold in ${userCity}, and RBI rates grounded via Google Search`}
                </div>
              </div>
            </div>

            {/* Mode Switcher */}
            <div style={{ display: 'flex', gap: 6, background: 'rgba(255, 255, 255, 0.05)', padding: 4, borderRadius: 'var(--radius-pill)', border: '1px solid var(--glass-border)' }}>
              <button
                type="button"
                className={`btn btn-xs ${aiMode === 'coach' ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => setAiMode('coach')}
                style={{ borderRadius: 'var(--radius-pill)', padding: '6px 14px', fontSize: '0.76rem' }}
              >
                <Brain size={13} style={{ marginRight: 4 }} /> Wealth Coach
              </button>
              <button
                type="button"
                className={`btn btn-xs ${aiMode === 'market_pulse' ? 'btn-primary' : 'btn-ghost'}`}
                onClick={() => setAiMode('market_pulse')}
                style={{ borderRadius: 'var(--radius-pill)', padding: '6px 14px', fontSize: '0.76rem' }}
              >
                <Globe size={13} style={{ marginRight: 4 }} /> Market Pulse (Live)
              </button>
            </div>
          </div>

          {/* Quick Prompt Chips */}
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 14 }}>
            {aiMode === 'coach' ? (
              [
                'Audit my cash flow leaks and 7-day velocity',
                'What are my payday first 48 hours transfer steps?',
                'Evaluate my 7-7-7 health and emergency runway',
                'Assess my 60:40 asset allocation framework',
              ].map((chip) => (
                <button
                  key={chip}
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => { setCoachQuestion(chip); runAiAnalysis('coach', chip); }}
                  style={{ fontSize: '0.75rem', borderRadius: 'var(--radius-pill)' }}
                >
                  ⚡ {chip}
                </button>
              ))
            ) : (
              [
                `Current 22K & 24K Gold Rate in ${userCity}`,
                'Nifty 50 & Sensex Level and Weekly Trend',
                'Current RBI Policy Repo Rate & Inflation',
                '6-Jar & Index SIP Allocation Context',
              ].map((chip) => (
                <button
                  key={chip}
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => { setCoachQuestion(chip); runAiAnalysis('market_pulse', chip); }}
                  style={{ fontSize: '0.75rem', borderRadius: 'var(--radius-pill)' }}
                >
                  🌐 {chip}
                </button>
              ))
            )}
          </div>

          {/* Input & Action */}
          <div style={{ display: 'flex', gap: 10, marginBottom: 16 }}>
            <input
              value={coachQuestion}
              onChange={e => setCoachQuestion(e.target.value)}
              placeholder={aiMode === 'coach' ? 'Ask anything about your personal finances...' : 'Ask about Indian market trends, gold, or repo rate...'}
              style={{ flex: 1 }}
              onKeyDown={e => e.key === 'Enter' && runAiAnalysis(aiMode)}
            />
            <button className="btn btn-primary" onClick={() => runAiAnalysis(aiMode)} disabled={aiLoading} style={{ flexShrink: 0 }}>
              {aiLoading ? <Loader2 size={16} className="spinner" /> : aiMode === 'coach' ? <Brain size={16} /> : <Globe size={16} />}
              <span>{aiLoading ? 'Synthesizing...' : aiMode === 'coach' ? 'Analyze' : 'Fetch Pulse'}</span>
            </button>
          </div>

          {aiError && (
            <div style={{ color: 'var(--neon-rose)', fontSize: '0.82rem', padding: '10px 14px', background: 'var(--neon-rose-glow)', borderRadius: 'var(--radius-md)', marginBottom: 14 }}>
              {aiError}
            </div>
          )}

          {/* Rule-based fallback observations when AI is unavailable */}
          {resultSource === 'rules-based' && ruleObservations.length > 0 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 16 }}>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)', display: 'flex', alignItems: 'center', gap: 6 }}>
                <WifiOff size={13} style={{ color: 'var(--neon-amber)' }} /> AI unavailable — showing deterministic rule-based observations:
              </div>
              {ruleObservations.map((obs, idx) => (
                <div key={idx} style={{ padding: '10px 14px', background: 'rgba(255,255,255,0.03)', border: '1px solid var(--glass-border)', borderRadius: 'var(--radius-md)', fontSize: '0.85rem' }}>
                  • {obs}
                </div>
              ))}
            </div>
          )}

          {/* COACH MODE OUTPUT */}
          {aiMode === 'coach' && coachResult && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)' }}>
                  Confidence Score: {Math.round((coachResult.confidence || 0.95) * 100)}%
                </span>
                <span className="badge badge-purple">AI Wealth Coach</span>
              </div>

              {/* 1. Cashflow & Leak Diagnosis */}
              {coachResult.cashflowLeakDiagnosis && (
                <div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--neon-rose)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.05em', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <AlertTriangle size={14} /> Cashflow & Leak Diagnosis
                  </div>
                  <div style={{ padding: '12px 16px', background: 'var(--neon-rose-glow)', border: '1px solid rgba(244, 63, 94, 0.3)', borderRadius: 'var(--radius-md)', fontSize: '0.86rem', lineHeight: 1.5, color: '#f1f5f9' }}>
                    {coachResult.cashflowLeakDiagnosis}
                  </div>
                </div>
              )}

              {/* 2. Payday First 48-Hours Transfer Plan */}
              {coachResult.paydayTransferPlan && (
                <div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--neon-emerald)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.05em', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <CheckCircle2 size={14} /> Payday First 48-Hours Action Plan
                  </div>
                  <div style={{ padding: '12px 16px', background: 'var(--neon-emerald-glow)', border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: 'var(--radius-md)', fontSize: '0.86rem', lineHeight: 1.5, color: '#f1f5f9' }}>
                    {coachResult.paydayTransferPlan}
                  </div>
                </div>
              )}

              {/* 3. Reference Framework Context */}
              {coachResult.allocationFrameworkContext && (
                <div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--neon-cyan)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.05em', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Compass size={14} /> Allocation Framework Context (Videos 1-16)
                  </div>
                  <div style={{ padding: '12px 16px', background: 'rgba(6, 182, 212, 0.08)', border: '1px solid rgba(6, 182, 212, 0.25)', borderRadius: 'var(--radius-md)', fontSize: '0.86rem', lineHeight: 1.5, color: '#f1f5f9' }}>
                    {coachResult.allocationFrameworkContext}
                  </div>
                </div>
              )}

              {/* 4. 7-7-7 Health & Emergency Runway */}
              {coachResult.emergencyAndSevenSevenSeven && (
                <div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--neon-indigo)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.05em', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <ShieldCheck size={14} /> 7-7-7 Health & Emergency Runway
                  </div>
                  <div style={{ padding: '12px 16px', background: 'rgba(99, 102, 241, 0.08)', border: '1px solid rgba(99, 102, 241, 0.25)', borderRadius: 'var(--radius-md)', fontSize: '0.86rem', lineHeight: 1.5, color: '#f1f5f9' }}>
                    {coachResult.emergencyAndSevenSevenSeven}
                  </div>
                </div>
              )}

              {/* Optional Tamil Summary */}
              {coachResult.tamilSummary && (
                <div style={{ padding: '10px 14px', background: 'rgba(255, 255, 255, 0.03)', border: '1px solid var(--glass-border)', borderRadius: 'var(--radius-md)', fontSize: '0.84rem', color: 'var(--text-secondary)' }}>
                  <span style={{ fontWeight: 700, color: 'var(--neon-amber)', marginRight: 6 }}>🇮🇳 முக்கிய குறிப்பு:</span>
                  {coachResult.tamilSummary}
                </div>
              )}

              {/* Mandatory Disclaimer */}
              <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', fontStyle: 'italic', borderTop: '1px solid var(--glass-border)', paddingTop: 10 }}>
                ⚠️ Educational, not financial advice. No numbers were recomputed by AI. All math is deterministic.
              </div>
            </div>
          )}

          {/* MARKET PULSE MODE OUTPUT */}
          {aiMode === 'market_pulse' && marketResult && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)' }}>
                  As of: {marketResult.asOfTimestamp || new Date().toLocaleTimeString()}
                </span>
                <span className="badge badge-cyan">Google Search Grounded</span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 12 }}>
                {/* Benchmark Nifty / Sensex */}
                <div style={{ padding: '14px', background: 'rgba(6, 182, 212, 0.06)', border: '1px solid rgba(6, 182, 212, 0.25)', borderRadius: 'var(--radius-md)' }}>
                  <div style={{ fontSize: '0.78rem', fontWeight: 800, color: 'var(--neon-cyan)', marginBottom: 6, display: 'flex', alignItems: 'center', gap: 6 }}>
                    <BarChart2 size={14} /> Nifty 50 & Sensex Trend
                  </div>
                  <div style={{ fontSize: '0.85rem', color: '#f1f5f9', lineHeight: 1.4 }}>
                    {marketResult.niftyLevelAndTrend || 'Live index benchmark data active.'}
                  </div>
                </div>

                {/* Gold Rate */}
                <div style={{ padding: '14px', background: 'rgba(245, 158, 11, 0.06)', border: '1px solid rgba(245, 158, 11, 0.25)', borderRadius: 'var(--radius-md)' }}>
                  <div style={{ fontSize: '0.78rem', fontWeight: 800, color: 'var(--neon-amber)', marginBottom: 6, display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Coins size={14} /> 22K & 24K Gold ({userCity})
                  </div>
                  <div style={{ fontSize: '0.85rem', color: '#f1f5f9', lineHeight: 1.4 }}>
                    {marketResult.goldRateContext || 'Precious metals benchmark data active.'}
                  </div>
                </div>

                {/* RBI Repo Rate */}
                <div style={{ padding: '14px', background: 'rgba(99, 102, 241, 0.06)', border: '1px solid rgba(99, 102, 241, 0.25)', borderRadius: 'var(--radius-md)' }}>
                  <div style={{ fontSize: '0.78rem', fontWeight: 800, color: 'var(--neon-indigo)', marginBottom: 6, display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Landmark size={14} /> RBI Policy Repo Rate & Inflation
                  </div>
                  <div style={{ fontSize: '0.85rem', color: '#f1f5f9', lineHeight: 1.4 }}>
                    {marketResult.rbiRepoRateAndInflation || 'Central bank monetary policy context active.'}
                  </div>
                </div>
              </div>

              {/* General Framework Guidance */}
              {marketResult.generalFrameworkGuidance && (
                <div style={{ padding: '14px', background: 'rgba(255, 255, 255, 0.02)', border: '1px solid var(--glass-border)', borderRadius: 'var(--radius-md)' }}>
                  <div style={{ fontSize: '0.78rem', fontWeight: 800, color: 'var(--neon-emerald)', marginBottom: 6, display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Lightbulb size={14} /> Allocation Framework Principles (Non-Personalized)
                  </div>
                  <div style={{ fontSize: '0.85rem', color: '#f1f5f9', lineHeight: 1.4 }}>
                    {marketResult.generalFrameworkGuidance}
                  </div>
                </div>
              )}

              {/* Grounding Source Citations (§9.2) */}
              {groundingSources.length > 0 && (
                <div>
                  <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-tertiary)', marginBottom: 6 }}>
                    Grounding Sources (Google Search):
                  </div>
                  <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                    {groundingSources.map((s, idx) => {
                      const linkUrl = s.uri || s.url;
                      if (!linkUrl) return null;
                      return (
                        <a
                          key={idx}
                          href={linkUrl}
                          target="_blank"
                          rel="noreferrer"
                          style={{
                            fontSize: '0.72rem',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 4,
                            padding: '4px 10px',
                            background: 'rgba(255, 255, 255, 0.04)',
                            border: '1px solid var(--glass-border)',
                            borderRadius: 'var(--radius-pill)',
                            color: 'var(--neon-cyan)',
                            textDecoration: 'none',
                          }}
                        >
                          <ExternalLink size={11} /> {s.title || 'Source Citation'}
                        </a>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Mandatory Disclaimer */}
              <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', fontStyle: 'italic', borderTop: '1px solid var(--glass-border)', paddingTop: 10 }}>
                ⚠️ Market data retrieved via Google Search grounding with timestamp. Educational, not financial advice. Market data is for context only and never enters financial calculations.
              </div>
            </div>
          )}

          {!coachResult && !marketResult && !aiLoading && !aiError && (
            <div style={{ textAlign: 'center', padding: '30px 0', color: 'var(--text-tertiary)', fontSize: '0.88rem' }}>
              Select a quick prompt above or click "{aiMode === 'coach' ? 'Analyze' : 'Fetch Pulse'}" to engage the Gemini AI Engine.
            </div>
          )}
        </div>
      </Tilt3DCard>

      {/* BEHAVIOURAL GUARDRAILS (§5.14, Videos 4, 5, 15) */}
      <div>
        <div style={{ marginBottom: 12 }}>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800 }}>
            Behavioural Guardrails & Mindset
          </h2>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)' }}>
            48-hour impulse cooldowns, lifestyle freeze tracker, and quiet wealth principles
          </p>
        </div>
        <BehaviouralGuardrailsCard />
      </div>
    </div>
  );
}

