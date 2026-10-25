import { useMemo, useState } from 'react';
import { useData } from '../../hooks/useData';
import { analyzeCycleForensics, type CrisisBucketKey } from '../../lib/forensics';
import { formatMoney } from '../../types';
import {
  Search,
  AlertTriangle,
  CheckCircle2,
  TrendingDown,
  Coffee,
  Store,
  Calendar,
  ShieldAlert,
  ArrowRight,
  Flame,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

interface CycleForensicsCardProps {
  onOpenJoylessReview?: () => void;
}

export function CycleForensicsCard({ onOpenJoylessReview }: CycleForensicsCardProps) {
  const { transactions, salaryCycles, financialProfile, settings } = useData();
  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;

  const [expanded, setExpanded] = useState(true);

  // Determine active cycle or fall back to recent 30 days
  const activeCycle = salaryCycles[0] || null;

  const cycleStartDate = useMemo(() => {
    if (activeCycle?.cycle_start_date) return activeCycle.cycle_start_date;
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`;
  }, [activeCycle]);

  const salaryPaise = activeCycle?.salary_paise || 3220000; // default ₹32,200 if not set

  // Run full cycle forensics
  const forensics = useMemo(() => {
    // Map transactions to forensic items
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

    // Opening balance default: salary + buffer or specified in cycle
    const opening = activeCycle?.salary_paise ? (activeCycle.salary_paise + 500000) : 3720000;
    const discretionary = activeCycle?.jar_amounts?.PLAY || activeCycle?.jar_amounts?.WANTS || Math.floor(salaryPaise * 0.1);

    return analyzeCycleForensics({
      transactions: forensicItems,
      cycleStartDate,
      salaryPaise,
      openingBalancePaise: opening,
      discretionaryBudgetPaise: discretionary,
      smallScanThresholdPaise: 20000, // ₹200
    });
  }, [transactions, activeCycle, cycleStartDate, salaryPaise]);

  const bucketIcons: Record<CrisisBucketKey, string> = {
    FIXED_BILLS_RENT_EMI: '🏛️',
    FOOD_TEA_SNACKS: '🍔',
    SHOPPING_SUBSCRIPTIONS: '🛍️',
    TRANSFERS_FRIENDS_FAMILY: '💸',
  };

  const bucketColors: Record<CrisisBucketKey, string> = {
    FIXED_BILLS_RENT_EMI: '#06b6d4', // Cyan
    FOOD_TEA_SNACKS: '#f59e0b',     // Amber
    SHOPPING_SUBSCRIPTIONS: '#ec4899', // Pink
    TRANSFERS_FRIENDS_FAMILY: '#a855f7', // Purple
  };

  return (
    <div
      className="cyber-card"
      style={{
        border: forensics.isVelocityAlertTriggered
          ? '1px solid rgba(239, 68, 68, 0.4)'
          : '1px solid var(--glass-border)',
        background: 'rgba(17, 24, 39, 0.85)',
        marginBottom: 20,
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Top Banner Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '16px 20px',
          cursor: 'pointer',
          borderBottom: expanded ? '1px solid var(--glass-border)' : 'none',
        }}
        onClick={() => setExpanded(!expanded)}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div
            style={{
              width: 40,
              height: 40,
              borderRadius: 10,
              background: forensics.isVelocityAlertTriggered ? 'rgba(239, 68, 68, 0.15)' : 'rgba(0, 240, 255, 0.15)',
              color: forensics.isVelocityAlertTriggered ? 'var(--neon-rose)' : 'var(--neon-cyan)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {forensics.isVelocityAlertTriggered ? <Flame size={22} /> : <Search size={22} />}
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <h3 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.15rem', fontWeight: 800, margin: 0 }}>
                Cycle Forensics: Where Did My Money Go?
              </h3>
              {forensics.isVelocityAlertTriggered && (
                <span
                  className="badge badge-rose"
                  style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: '0.72rem' }}
                >
                  <AlertTriangle size={12} /> High Burn Alert
                </span>
              )}
            </div>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', margin: '2px 0 0 0' }}>
              Cycle started {cycleStartDate} · {forensics.daysElapsed} days elapsed · Single-source truth from bank statement
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Daily Average Burn</div>
            <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.2rem', fontWeight: 800, color: 'var(--neon-rose)' }}>
              {formatMoney(forensics.averageBurnPerDayPaise, currency, masked)}
              <span style={{ fontSize: '0.75rem', fontWeight: 500, color: 'var(--text-tertiary)' }}> / day</span>
            </div>
          </div>
          <button className="btn-ghost" style={{ padding: 6 }}>
            {expanded ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
          </button>
        </div>
      </div>

      {expanded && (
        <div style={{ padding: 20 }}>
          {/* Key Metric Highlights Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 14, marginBottom: 20 }}>
            {/* Total Debited */}
            <div className="cyber-card" style={{ padding: 14, borderLeft: '3px solid var(--neon-rose)' }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--neon-rose)', fontWeight: 700, textTransform: 'uppercase' }}>
                Total Debited in Cycle
              </div>
              <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.35rem', fontWeight: 800, color: 'var(--neon-rose)', marginTop: 4 }}>
                -{formatMoney(forensics.totalDebitsPaise, currency, masked)}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: 4 }}>
                Across {forensics.daysElapsed} days elapsed
              </div>
            </div>

            {/* 7-Day Velocity */}
            <div
              className="cyber-card"
              style={{
                padding: 14,
                borderLeft: `3px solid ${forensics.sevenDaySalarySpendPct > 50 ? 'var(--neon-rose)' : 'var(--neon-cyan)'}`,
              }}
            >
              <div style={{ fontSize: '0.72rem', color: forensics.sevenDaySalarySpendPct > 50 ? 'var(--neon-rose)' : 'var(--neon-cyan)', fontWeight: 700, textTransform: 'uppercase' }}>
                First 7 Days Spend
              </div>
              <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.35rem', fontWeight: 800, color: '#f1f5f9', marginTop: 4 }}>
                {formatMoney(forensics.first7DaysSpendPaise, currency, masked)}
              </div>
              <div style={{ fontSize: '0.75rem', color: forensics.sevenDaySalarySpendPct > 50 ? 'var(--neon-rose)' : 'var(--text-tertiary)', marginTop: 4 }}>
                {forensics.sevenDaySalarySpendPct}% of salary gone in 7 days
              </div>
            </div>

            {/* Small Scans Leak */}
            <div className="cyber-card" style={{ padding: 14, borderLeft: '3px solid var(--neon-amber)' }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--neon-amber)', fontWeight: 700, textTransform: 'uppercase' }}>
                Small UPI Scans (≤ ₹200)
              </div>
              <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.35rem', fontWeight: 800, color: 'var(--neon-amber)', marginTop: 4 }}>
                {formatMoney(forensics.smallScanTotalPaise, currency, masked)}
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: 4 }}>
                ☕ {forensics.smallScanCount} micro-scans accumulated
              </div>
            </div>

            {/* Math Reconciliation */}
            <div
              className="cyber-card"
              style={{
                padding: 14,
                borderLeft: `3px solid ${forensics.isReconciled ? 'var(--neon-emerald)' : 'var(--neon-amber)'}`,
              }}
            >
              <div style={{ fontSize: '0.72rem', color: forensics.isReconciled ? 'var(--neon-emerald)' : 'var(--neon-amber)', fontWeight: 700, textTransform: 'uppercase' }}>
                Ledger Reconciliation
              </div>
              <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.1rem', fontWeight: 800, color: forensics.isReconciled ? 'var(--neon-emerald)' : 'var(--neon-amber)', marginTop: 4 }}>
                {forensics.isReconciled ? '✅ 100% Balanced' : `⚠️ Discrepancy ${formatMoney(Math.abs(forensics.discrepancyPaise), currency, masked)}`}
              </div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', marginTop: 4 }}>
                Expected: {formatMoney(forensics.expectedClosingPaise, currency, masked)} · Actual: {formatMoney(forensics.actualClosingPaise, currency, masked)}
              </div>
            </div>
          </div>

          {/* 4 Crisis Buckets (§7) */}
          <div style={{ marginBottom: 20 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
              <h4 style={{ fontFamily: 'var(--font-outfit)', fontSize: '0.92rem', fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.04em', margin: 0 }}>
                Four Crisis Buckets Diagnostic
              </h4>
              <span style={{ fontSize: '0.76rem', color: 'var(--text-tertiary)' }}>
                Based on jar &amp; emotion tag mapping
              </span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 10 }}>
              {(Object.keys(forensics.crisisBuckets) as CrisisBucketKey[]).map(key => {
                const b = forensics.crisisBuckets[key];
                const color = bucketColors[key];
                return (
                  <div
                    key={key}
                    style={{
                      padding: 12,
                      borderRadius: 'var(--radius-md)',
                      background: 'rgba(255, 255, 255, 0.03)',
                      border: '1px solid var(--glass-border)',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span style={{ fontSize: '0.85rem', fontWeight: 700, color: '#f1f5f9' }}>
                        {bucketIcons[key]} {b.label}
                      </span>
                      <span style={{ fontSize: '0.78rem', fontWeight: 800, color }}>{b.percentage}%</span>
                    </div>

                    <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.15rem', fontWeight: 800, color, marginTop: 6 }}>
                      {formatMoney(b.totalPaise, currency, masked)}
                    </div>

                    {/* Progress Bar */}
                    <div style={{ height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.06)', marginTop: 8, overflow: 'hidden' }}>
                      <div style={{ height: '100%', width: `${Math.min(100, b.percentage)}%`, background: color }} />
                    </div>

                    <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', marginTop: 6 }}>
                      {b.count} transactions in cycle
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Bottom Split: Top 10 Merchants & Action Bar */}
          <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 16 }}>
            {/* Top Leak Merchants */}
            <div style={{ border: '1px solid var(--glass-border)', borderRadius: 'var(--radius-md)', padding: 14 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                <span style={{ fontSize: '0.85rem', fontWeight: 800, textTransform: 'uppercase', color: 'var(--text-secondary)' }}>
                  Top 5 Outflow Merchants
                </span>
                <span style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)' }}>Ranked by total spent</span>
              </div>

              {forensics.topMerchants.length === 0 ? (
                <div style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', padding: '12px 0' }}>
                  No merchant outflows found in this cycle.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {forensics.topMerchants.slice(0, 5).map((m, idx) => (
                    <div
                      key={m.merchant}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        fontSize: '0.82rem',
                        padding: '4px 0',
                        borderBottom: idx < 4 ? '1px solid rgba(255,255,255,0.04)' : 'none',
                      }}
                    >
                      <span style={{ color: '#f1f5f9', fontWeight: 600 }}>
                        <span style={{ color: 'var(--text-tertiary)', marginRight: 6 }}>#{idx + 1}</span>
                        {m.merchant}
                        <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', marginLeft: 6 }}>({m.count} scans)</span>
                      </span>
                      <span style={{ fontFamily: 'var(--font-outfit)', fontWeight: 700, color: 'var(--neon-rose)' }}>
                        {formatMoney(m.totalPaise, currency, masked)}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Habit Review & Mandate Callout */}
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                padding: 16,
                borderRadius: 'var(--radius-md)',
                background: 'linear-gradient(135deg, rgba(239, 68, 68, 0.08), rgba(0, 240, 255, 0.04))',
                border: '1px solid rgba(239, 68, 68, 0.2)',
              }}
            >
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--neon-rose)', fontWeight: 800, fontSize: '0.88rem' }}>
                  <ShieldAlert size={16} /> Joyless Leak Review (Video 12)
                </div>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: 6, lineHeight: 1.45 }}>
                  Small tea stall UPI scans and impulse quick commerce bleed your salary before mid-month. Review habit candidates and tag joyless leaks with one click.
                </p>
              </div>

              {onOpenJoylessReview && (
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={onOpenJoylessReview}
                  style={{
                    marginTop: 12,
                    background: 'var(--neon-rose)',
                    color: '#fff',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 6,
                    fontSize: '0.82rem',
                    fontWeight: 700,
                  }}
                >
                  Audit Joyless Leaks <ArrowRight size={14} />
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
