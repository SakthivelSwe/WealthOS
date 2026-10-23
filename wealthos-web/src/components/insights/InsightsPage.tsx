import { useMemo, useState } from 'react';
import { useData } from '../../hooks/useData';
import { formatMoney, getCategoryDisplayName } from '../../types';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { BarChart2, TrendingUp, TrendingDown, Brain, Loader2, Sparkles, AlertTriangle, CheckCircle2, Lightbulb, WifiOff } from 'lucide-react';
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis,
  CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';
import { format, subMonths, startOfMonth, endOfMonth, subWeeks, startOfWeek, endOfWeek } from 'date-fns';
import { supabase } from '../../lib/supabase';

const COLORS = [
  '#06b6d4', '#6366f1', '#10b981', '#f43f5e', '#f59e0b',
  '#a855f7', '#3b82f6', '#ec4899', '#14b8a6', '#eab308'
];

export function InsightsPage() {
  const { transactions, categories, settings } = useData();
  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;
  const [period, setPeriod] = useState<'month' | '3months' | '6months' | 'year'>('month');
  const [aiLoading, setAiLoading] = useState(false);
  const [aiResult, setAiResult] = useState<{ insights: string[]; recommendations: string[]; warnings: string[]; confidence: number } | null>(null);
  const [aiError, setAiError] = useState('');
  const [aiQuestion, setAiQuestion] = useState('Analyze my spending patterns and recommend 3 ways to boost savings.');

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

  // F-11 fix: Weekly spend — previously wStart and wEnd were the same single day.
  // Now each week spans Mon–Sun using startOfWeek/endOfWeek.
  const weeklyData = useMemo(() => {
    const weeks = [];
    for (let i = 3; i >= 0; i--) {
      const weekAnchor = subWeeks(now, i);
      const wStart = format(startOfWeek(weekAnchor, { weekStartsOn: 1 }), 'yyyy-MM-dd');
      const wEnd = format(endOfWeek(weekAnchor, { weekStartsOn: 1 }), 'yyyy-MM-dd');
      const amount = periodTxns
        .filter(t => t.type === 'EXPENSE' && t.local_date >= wStart && t.local_date <= wEnd)
        .reduce((s, t) => s + t.amount_minor, 0);
      weeks.push({ week: `Wk ${4 - i}`, amount: amount / 100 });
    }
    return weeks;
  }, [periodTxns, now]);

  // Need / Want / Saving breakdown — percentages are of income, not outflows
  const nwsBreakdown = useMemo(() => {
    const needs = periodTxns.filter(t => t.type === 'EXPENSE' && t.need_want === 'NEED').reduce((s, t) => s + t.amount_minor, 0);
    const wants = periodTxns.filter(t => t.type === 'EXPENSE' && t.need_want === 'WANT').reduce((s, t) => s + t.amount_minor, 0);
    // Savings/Investments tagged as SAVING type (SIPs, FDs, RDs, goal transfers)
    const savings = periodTxns.filter(t => t.type === 'EXPENSE' && t.need_want === 'SAVING').reduce((s, t) => s + t.amount_minor, 0);
    const base = totalIncome > 0 ? totalIncome : (needs + wants + savings);
    return [
      { name: 'Needs', value: needs / 100, pctOfIncome: base > 0 ? ((needs / base) * 100).toFixed(1) : '0', color: 'var(--neon-cyan)' },
      { name: 'Wants', value: wants / 100, pctOfIncome: base > 0 ? ((wants / base) * 100).toFixed(1) : '0', color: 'var(--neon-purple)' },
      { name: 'Savings/SIPs', value: savings / 100, pctOfIncome: base > 0 ? ((savings / base) * 100).toFixed(1) : '0', color: 'var(--neon-emerald)' },
    ].filter(item => item.value > 0);
  }, [periodTxns, totalIncome]);

  // Phase 7.6: This month vs Last month category spending comparison
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

  const runAiAnalysis = async (customPrompt?: string) => {
    const promptToUse = customPrompt || aiQuestion;
    setAiLoading(true);
    setAiError('');
    try {
      const prompt = `You are a private wealth manager for WealthOS. Analyze this data:
- Income: ₹${(totalIncome / 100).toFixed(0)}
- Expense: ₹${(totalExpense / 100).toFixed(0)}
- Savings Rate: ${savingsRate}%
- Period: ${label}
- Top Categories: ${categoryBreakdown.slice(0, 4).map(c => `${c.name}: ₹${c.value.toFixed(0)} (${c.pct}%)`).join(', ')}
- Need/Want Split: ${nwsBreakdown.map(n => `${n.name}: ₹${n.value.toFixed(0)}`).join(', ')}

Question: ${promptToUse}`;

      const { data, error } = await supabase.functions.invoke('ai-financial-coach', {
        body: { prompt, userQuery: promptToUse },
      });

      if (error || !data) {
        // F-12 fix: Do NOT show fabricated AI insights when the Edge Function is offline.
        // Previously this returned hardcoded confidence: 0.94 and invented advice,
        // which was indistinguishable from real AI output.
        // Now we show an honest unavailability message and only genuine rule-based warnings.
        const ruleWarnings: string[] = [];
        const ruleInsights: string[] = [];

        if (totalExpense > totalIncome && totalIncome > 0) {
          ruleWarnings.push('Your expenses exceeded income in this period. Review the category breakdown above.');
        }
        if (totalIncome === 0) {
          ruleInsights.push('No income recorded for this period. Add your salary or other income to see meaningful analysis.');
        }
        if (categoryBreakdown.length > 0) {
          ruleInsights.push(`Your largest spending category is ${categoryBreakdown[0].name} at ${categoryBreakdown[0].pct}% of total expenses.`);
        }

        if (ruleWarnings.length > 0 || ruleInsights.length > 0) {
          setAiResult({
            insights: ruleInsights,
            recommendations: [],
            warnings: ruleWarnings,
            confidence: 0,  // 0 = rules-based, not AI
            source: 'rules-based',
          } as any);
        } else {
          setAiError('AI Money Coach is currently unavailable. Your financial charts and data above are still accurate.');
        }
      } else {
        setAiResult(data);
      }
    } catch (e: any) {
      setAiError(e.message || 'AI engine is currently warming up.');
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
            Charts and smart tips to understand your spending and save more money
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

      {/* Charts Bento 1 */}
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
                    <div className="cyber-subtitle">Compare your monthly earnings and spending</div>
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

      {/* Category Spending Trends: This Month vs Last Month (Phase 7.6) */}
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

      {/* 3D NEURAL AI FINANCIAL COACH (GEMINI) */}
      <Tilt3DCard maxTilt={3}>
        <div className="cyber-card" style={{ border: '1px solid rgba(168, 85, 247, 0.3)', boxShadow: '0 20px 40px -10px var(--neon-purple-glow)' }}>
          <div className="cyber-header">
            <div className="cyber-title-group">
              <div
                className="cyber-icon-badge"
                style={{
                  background: 'linear-gradient(135deg, var(--neon-purple) 0%, var(--neon-indigo) 100%)',
                  color: 'white',
                  boxShadow: '0 0 16px var(--neon-purple-glow)',
                }}
              >
                <Brain size={20} />
              </div>
              <div>
                <div className="cyber-title">
                  AI Money Coach
                </div>
                <div className="cyber-subtitle">Helpful recommendations based on your spending habits</div>
              </div>
            </div>
          </div>

          {/* Quick Prompt Chips */}
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 14 }}>
            {[
              'Audit my discretionary spending',
              'How can I save ₹25,000 more each month?',
              'Evaluate my 50/30/20 balance',
              'Check debt payoff timeline',
            ].map((chip) => (
              <button
                key={chip}
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => { setAiQuestion(chip); runAiAnalysis(chip); }}
                style={{ fontSize: '0.75rem', borderRadius: 'var(--radius-pill)' }}
              >
                ⚡ {chip}
              </button>
            ))}
          </div>

          {/* Input & Action */}
          <div style={{ display: 'flex', gap: 10, marginBottom: 16 }}>
            <input
              value={aiQuestion}
              onChange={e => setAiQuestion(e.target.value)}
              placeholder="Ask anything about your financial health..."
              style={{ flex: 1 }}
              onKeyDown={e => e.key === 'Enter' && runAiAnalysis()}
            />
            <button className="btn btn-primary" onClick={() => runAiAnalysis()} disabled={aiLoading} style={{ flexShrink: 0 }}>
              {aiLoading ? <Loader2 size={16} className="spinner" /> : <Brain size={16} />}
              <span>{aiLoading ? 'Synthesizing...' : 'Analyze'}</span>
            </button>
          </div>

          {aiError && <div style={{ color: 'var(--neon-rose)', fontSize: '0.82rem', padding: '10px 14px', background: 'var(--neon-rose-glow)', borderRadius: 'var(--radius-md)', marginBottom: 14 }}>{aiError}</div>}

          {aiResult && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                {(aiResult as any).source === 'rules-based' ? (
                  <span style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <WifiOff size={12} /> AI unavailable — showing rule-based observations only
                  </span>
                ) : (
                  <span style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)' }}>Confidence Score: {(aiResult.confidence * 100).toFixed(0)}%</span>
                )}
                <span className="badge badge-cyan">{label}</span>
              </div>

              {aiResult.insights.length > 0 && (
                <div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--neon-cyan)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.05em', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Lightbulb size={14} /> Key Strategic Observations
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {aiResult.insights.map((ins, i) => (
                      <div key={i} style={{ padding: '10px 14px', background: 'rgba(255,255,255,0.03)', border: '1px solid var(--glass-border)', borderRadius: 'var(--radius-md)', fontSize: '0.85rem', lineHeight: 1.5 }}>
                        • {ins}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {aiResult.recommendations.length > 0 && (
                <div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--neon-emerald)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.05em', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <CheckCircle2 size={14} /> Actionable Recommendations
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {aiResult.recommendations.map((rec, i) => (
                      <div key={i} style={{ padding: '10px 14px', background: 'var(--neon-emerald-glow)', border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: 'var(--radius-md)', fontSize: '0.85rem', color: '#f1f5f9', lineHeight: 1.5 }}>
                        ✓ {rec}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {aiResult.warnings.length > 0 && (
                <div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--neon-rose)', marginBottom: 8, textTransform: 'uppercase', letterSpacing: '0.05em', display: 'flex', alignItems: 'center', gap: 6 }}>
                    <AlertTriangle size={14} /> Risk Alerts
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                    {aiResult.warnings.map((w, i) => (
                      <div key={i} style={{ padding: '10px 14px', background: 'var(--neon-rose-glow)', border: '1px solid rgba(244, 63, 94, 0.3)', borderRadius: 'var(--radius-md)', fontSize: '0.85rem', color: '#f1f5f9' }}>
                        ⚠️ {w}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {!aiResult && !aiLoading && !aiError && (
            <div style={{ textAlign: 'center', padding: '30px 0', color: 'var(--text-tertiary)', fontSize: '0.88rem' }}>
              Select a prompt above or click "Analyze" to engage the Gemini AI Neural Coach.
            </div>
          )}
        </div>
      </Tilt3DCard>
    </div>
  );
}
