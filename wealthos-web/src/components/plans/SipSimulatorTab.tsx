import React, { useState, useMemo } from 'react';
import { sipMonthsToTarget, projectSipTrajectory } from '../../lib/sipProjection';
import { formatIndian } from '../../lib/money';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { TrendingUp, ShieldCheck, Target, Sparkles, AlertCircle, Info, Calendar } from 'lucide-react';

const MILESTONES = [
  { name: 'First Milestone (₹10 Lakhs)', amountPaise: 100000000, icon: '🌱', tip: 'The first ₹10 Lakhs is the hardest due to starting principal.' },
  { name: 'Half Crore (₹50 Lakhs)', amountPaise: 500000000, icon: '🌿', tip: 'Compounding accelerates as returns outgrow annual contributions.' },
  { name: 'One Crore Milestone (₹1 Crore)', amountPaise: 1000000000, icon: '🌳', tip: 'Financial Independence baseline corpus in India.' },
  { name: 'Wealth Fortress (₹1.5 Crores)', amountPaise: 1500000000, icon: '🏰', tip: 'Long-term freedom fund with passive cash-flow generation.' },
];

export function SipSimulatorTab() {
  const [sipStr, setSipStr] = useState<string>('10000');
  const [stepUpStr, setStepUpStr] = useState<string>('10');
  const [expectedReturnStr, setExpectedReturnStr] = useState<string>('12');
  const [safetyReturnStr, setSafetyReturnStr] = useState<string>('8');
  const [horizonYears, setHorizonYears] = useState<number>(25);

  const startSipPaise = useMemo(() => {
    const s = Math.max(100, parseInt(sipStr.replace(/[^0-9]/g, ''), 10) || 10000);
    return s * 100;
  }, [sipStr]);

  const stepUpPct = useMemo(() => Math.max(0, parseInt(stepUpStr, 10) || 0), [stepUpStr]);
  const expectedReturnPct = useMemo(() => Math.max(1, parseInt(expectedReturnStr, 10) || 12), [expectedReturnStr]);
  const safetyReturnPct = useMemo(() => Math.max(1, parseInt(safetyReturnStr, 10) || 8), [safetyReturnStr]);

  // Compute milestones conforming to §5.8 / Appendix A.3
  const milestoneResults = useMemo(() => {
    return MILESTONES.map(m => {
      const expMonths = sipMonthsToTarget(startSipPaise, stepUpPct, expectedReturnPct, m.amountPaise);
      const safeMonths = sipMonthsToTarget(startSipPaise, stepUpPct, safetyReturnPct, m.amountPaise);
      return {
        ...m,
        expectedMonths: expMonths,
        safetyMonths: safeMonths,
      };
    });
  }, [startSipPaise, stepUpPct, expectedReturnPct, safetyReturnPct]);

  // Project trajectory
  const trajectory = useMemo(() => {
    return projectSipTrajectory(startSipPaise, stepUpPct, horizonYears * 12, expectedReturnPct, safetyReturnPct);
  }, [startSipPaise, stepUpPct, horizonYears, expectedReturnPct, safetyReturnPct]);

  const finalPoint = trajectory[trajectory.length - 1];

  const formatMonthsToYears = (months: number | null) => {
    if (months === null) return '600+ months';
    const yrs = Math.floor(months / 12);
    const m = months % 12;
    if (yrs === 0) return `Month ${months}`;
    return `Month ${months} (${yrs}y ${m}m)`;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Title */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.45rem', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <TrendingUp size={22} style={{ color: 'var(--neon-emerald)' }} />
            SIP Compounding & Milestone Projector (§5.8)
          </h2>
          <p style={{ fontSize: '0.84rem', color: 'var(--text-tertiary)', margin: '4px 0 0 0' }}>
            Dual-curve projection with margin of safety and annual step-up. Exact integer paise math.
          </p>
        </div>

        <div style={{ padding: '6px 12px', borderRadius: 'var(--radius-pill)', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', fontSize: '0.78rem', color: 'var(--text-tertiary)' }}>
          Convention: contribution at month start, step-up every 12 months
        </div>
      </div>

      {/* Control Inputs */}
      <div className="bento-grid" style={{ gridTemplateColumns: 'repeat(12, 1fr)', gap: 16 }}>
        <div style={{ gridColumn: 'span 3' }}>
          <Tilt3DCard>
            <div className="cyber-card" style={{ padding: 18 }}>
              <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Monthly SIP (₹)</label>
              <input
                type="text"
                value={sipStr}
                onChange={e => setSipStr(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: '#fff', fontFamily: 'var(--font-mono)', fontSize: '1.1rem', marginTop: 6 }}
              />
              <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 4, display: 'block' }}>
                ₹{formatIndian(startSipPaise / 100)} / month
              </span>
            </div>
          </Tilt3DCard>
        </div>

        <div style={{ gridColumn: 'span 3' }}>
          <Tilt3DCard>
            <div className="cyber-card" style={{ padding: 18 }}>
              <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Annual Step-Up (%)</label>
              <input
                type="number"
                min="0"
                max="50"
                value={stepUpStr}
                onChange={e => setStepUpStr(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: '#fff', fontFamily: 'var(--font-mono)', fontSize: '1.1rem', marginTop: 6 }}
              />
              <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 4, display: 'block' }}>
                Increase SIP with salary hikes
              </span>
            </div>
          </Tilt3DCard>
        </div>

        <div style={{ gridColumn: 'span 3' }}>
          <Tilt3DCard>
            <div className="cyber-card" style={{ padding: 18 }}>
              <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Expected Return (%)</label>
              <input
                type="number"
                min="1"
                max="30"
                value={expectedReturnStr}
                onChange={e => setExpectedReturnStr(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: 'var(--neon-emerald)', fontFamily: 'var(--font-mono)', fontSize: '1.1rem', marginTop: 6 }}
              />
              <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 4, display: 'block' }}>
                Nifty 50 historical (~12% p.a.)
              </span>
            </div>
          </Tilt3DCard>
        </div>

        <div style={{ gridColumn: 'span 3' }}>
          <Tilt3DCard>
            <div className="cyber-card" style={{ padding: 18 }}>
              <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Margin of Safety (%)</label>
              <input
                type="number"
                min="1"
                max="30"
                value={safetyReturnStr}
                onChange={e => setSafetyReturnStr(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: 'var(--neon-cyan)', fontFamily: 'var(--font-mono)', fontSize: '1.1rem', marginTop: 6 }}
              />
              <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 4, display: 'block' }}>
                Conservative floor (~8% p.a.)
              </span>
            </div>
          </Tilt3DCard>
        </div>
      </div>

      {/* Golden Milestone Cards (§5.8 / Appendix A.3) */}
      <div>
        <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#fff', marginBottom: 12, display: 'flex', alignItems: 'center', gap: 8 }}>
          <Target size={18} style={{ color: 'var(--neon-emerald)' }} />
          Milestone Speedometer (Expected vs Margin of Safety)
        </h3>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 14 }}>
          {milestoneResults.map(m => (
            <div
              key={m.name}
              style={{
                padding: 18,
                borderRadius: 'var(--radius-md)',
                background: 'rgba(17,24,39,0.7)',
                border: '1px solid var(--glass-border)',
                display: 'flex',
                flexDirection: 'column',
                gap: 12,
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <div style={{ fontSize: '1.25rem' }}>{m.icon}</div>
                  <div style={{ fontSize: '0.88rem', fontWeight: 800, color: '#fff', marginTop: 4 }}>{m.name}</div>
                </div>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8rem', fontWeight: 800, color: 'var(--neon-cyan)', background: 'rgba(6,182,212,0.1)', padding: '2px 8px', borderRadius: 4 }}>
                  ₹{formatIndian(m.amountPaise / 100)}
                </span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, background: 'rgba(255,255,255,0.02)', padding: 10, borderRadius: 8 }}>
                <div>
                  <span style={{ fontSize: '0.72rem', color: 'var(--neon-emerald)', fontWeight: 700 }}>Expected ({expectedReturnPct}%):</span>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.9rem', fontWeight: 800, color: '#fff', marginTop: 2 }}>
                    {formatMonthsToYears(m.expectedMonths)}
                  </div>
                </div>
                <div>
                  <span style={{ fontSize: '0.72rem', color: 'var(--neon-cyan)', fontWeight: 700 }}>Safety ({safetyReturnPct}%):</span>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.9rem', fontWeight: 800, color: '#fff', marginTop: 2 }}>
                    {formatMonthsToYears(m.safetyMonths)}
                  </div>
                </div>
              </div>

              <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)' }}>{m.tip}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Trajectory Summary Banner */}
      {finalPoint && (
        <div style={{ padding: 20, borderRadius: 'var(--radius-lg)', background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.1) 0%, rgba(6, 182, 212, 0.05) 100%)', border: '1px solid rgba(16, 185, 129, 0.25)', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16 }}>
          <div>
            <span style={{ fontSize: '0.75rem', fontWeight: 800, color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>
              Cumulative Capital Invested ({horizonYears} Years)
            </span>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.6rem', fontWeight: 900, color: '#fff', marginTop: 4 }}>
              ₹{formatIndian(finalPoint.investedPaise / 100)}
            </div>
          </div>

          <div>
            <span style={{ fontSize: '0.75rem', fontWeight: 800, color: 'var(--neon-emerald)', textTransform: 'uppercase' }}>
              Expected Portfolio Value ({expectedReturnPct}%)
            </span>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.6rem', fontWeight: 900, color: 'var(--neon-emerald)', marginTop: 4 }}>
              ₹{formatIndian(finalPoint.expectedValuePaise / 100)}
            </div>
          </div>

          <div>
            <span style={{ fontSize: '0.75rem', fontWeight: 800, color: 'var(--neon-cyan)', textTransform: 'uppercase' }}>
              Margin of Safety Value ({safetyReturnPct}%)
            </span>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.6rem', fontWeight: 900, color: 'var(--neon-cyan)', marginTop: 4 }}>
              ₹{formatIndian(finalPoint.marginOfSafetyValuePaise / 100)}
            </div>
          </div>
        </div>
      )}

      {/* Legal & Educational Disclaimer (§8.10) */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '10px 14px', borderRadius: 8, background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.06)', color: 'var(--text-tertiary)', fontSize: '0.76rem' }}>
        <Info size={16} />
        Projections are illustrative and assume monthly compounding with discipline. Educational framework from reference videos, not individual investment advice.
      </div>
    </div>
  );
}
