import React, { useState, useMemo } from 'react';
import { useData } from '../../hooks/useData';
import { calculateEmi, evaluateEmiAffordability } from '../../lib/emiSimulator';
import { formatIndian } from '../../lib/money';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { Calculator, ShieldAlert, CheckCircle, Clock, AlertTriangle, ArrowRight, Info } from 'lucide-react';

export function EmiSimulatorTab() {
  const { financialProfile, activeCycle, accounts } = useData();

  const [priceStr, setPriceStr] = useState<string>('500000');
  const [downPaymentStr, setDownPaymentStr] = useState<string>('0');
  const [rateStr, setRateStr] = useState<string>('12');
  const [tenureMonths, setTenureMonths] = useState<number>(36);
  const [cooldownActivated, setCooldownActivated] = useState<boolean>(false);

  const pricePaise = useMemo(() => {
    const p = Math.max(0, parseInt(priceStr.replace(/[^0-9]/g, ''), 10) || 0);
    return p * 100;
  }, [priceStr]);

  const downPaymentPaise = useMemo(() => {
    const d = Math.max(0, parseInt(downPaymentStr.replace(/[^0-9]/g, ''), 10) || 0);
    return d * 100;
  }, [downPaymentStr]);

  const loanPrincipalPaise = Math.max(0, pricePaise - downPaymentPaise);
  const annualRate = Math.max(0, parseFloat(rateStr) || 0);

  // Run exact EMI calculation (§5.10 / Appendix A.5)
  const emiResult = useMemo(() => {
    if (loanPrincipalPaise <= 0 || tenureMonths <= 0) return null;
    return calculateEmi(loanPrincipalPaise, annualRate, tenureMonths);
  }, [loanPrincipalPaise, annualRate, tenureMonths]);

  // Run Affordability & Job-Loss Check
  const salaryPaise = activeCycle?.salary_paise || 3220000;
  const baselinePaise = financialProfile?.fixed_baseline_paise || 1771000;
  const liquidEmergencyPaise = accounts
    .filter(a => ['BANK', 'CASH', 'SAVINGS', 'UPI_WALLET'].includes(a.type))
    .reduce((s, a) => s + (a.current_balance_minor || 0), 0);

  const affordability = useMemo(() => {
    if (!emiResult) return null;
    return evaluateEmiAffordability(emiResult.emiPaise, salaryPaise, baselinePaise, liquidEmergencyPaise);
  }, [emiResult, salaryPaise, baselinePaise, liquidEmergencyPaise]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Title */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.45rem', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Calculator size={22} style={{ color: 'var(--neon-amber)' }} />
            EMI Affordability & Job-Loss Survival Simulator (§5.10)
          </h2>
          <p style={{ fontSize: '0.84rem', color: 'var(--text-tertiary)', margin: '4px 0 0 0' }}>
            Calculates exact reducing-balance EMI and tests your financial survival if monthly income stops.
          </p>
        </div>

        <div style={{ padding: '6px 14px', borderRadius: 'var(--radius-pill)', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
          Salary reference: ₹{formatIndian(salaryPaise / 100)} / mo
        </div>
      </div>

      {/* Control Bento */}
      <div className="bento-grid" style={{ gridTemplateColumns: 'repeat(12, 1fr)', gap: 16 }}>
        {/* Total Price */}
        <div style={{ gridColumn: 'span 3' }}>
          <Tilt3DCard>
            <div className="cyber-card" style={{ padding: 18 }}>
              <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Asset / Purchase Price (₹)</label>
              <input
                type="text"
                value={priceStr}
                onChange={e => setPriceStr(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: '#fff', fontFamily: 'var(--font-mono)', fontSize: '1.1rem', marginTop: 6 }}
              />
              <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 4, display: 'block' }}>
                ₹{formatIndian(pricePaise / 100)} total
              </span>
            </div>
          </Tilt3DCard>
        </div>

        {/* Down Payment */}
        <div style={{ gridColumn: 'span 3' }}>
          <Tilt3DCard>
            <div className="cyber-card" style={{ padding: 18 }}>
              <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Down Payment (₹)</label>
              <input
                type="text"
                value={downPaymentStr}
                onChange={e => setDownPaymentStr(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: '#fff', fontFamily: 'var(--font-mono)', fontSize: '1.1rem', marginTop: 6 }}
              />
              <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 4, display: 'block' }}>
                Loan: ₹{formatIndian(loanPrincipalPaise / 100)}
              </span>
            </div>
          </Tilt3DCard>
        </div>

        {/* Annual Interest Rate */}
        <div style={{ gridColumn: 'span 3' }}>
          <Tilt3DCard>
            <div className="cyber-card" style={{ padding: 18 }}>
              <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Interest Rate (% APR)</label>
              <input
                type="number"
                step="0.1"
                min="0"
                max="40"
                value={rateStr}
                onChange={e => setRateStr(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: 'var(--neon-amber)', fontFamily: 'var(--font-mono)', fontSize: '1.1rem', marginTop: 6 }}
              />
              <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 4, display: 'block' }}>
                Reducing balance rate
              </span>
            </div>
          </Tilt3DCard>
        </div>

        {/* Tenure Months */}
        <div style={{ gridColumn: 'span 3' }}>
          <Tilt3DCard>
            <div className="cyber-card" style={{ padding: 18 }}>
              <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Tenure: {tenureMonths} Months ({tenureMonths / 12} yrs)</label>
              <input
                type="range"
                min="6"
                max="84"
                step="6"
                value={tenureMonths}
                onChange={e => setTenureMonths(parseInt(e.target.value, 10))}
                style={{ width: '100%', accentColor: 'var(--neon-amber)', cursor: 'pointer', marginTop: 10 }}
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem', color: 'var(--text-tertiary)' }}>
                <span>6 Mo</span>
                <span>36 Mo (3y)</span>
                <span>84 Mo (7y)</span>
              </div>
            </div>
          </Tilt3DCard>
        </div>
      </div>

      {/* EMI Results Card */}
      {emiResult && (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
            gap: 16,
            padding: 22,
            borderRadius: 'var(--radius-lg)',
            background: 'linear-gradient(135deg, rgba(245, 158, 11, 0.12) 0%, rgba(17, 24, 39, 0.8) 100%)',
            border: '1px solid rgba(245, 158, 11, 0.3)',
          }}
        >
          <div>
            <span style={{ fontSize: '0.78rem', fontWeight: 800, textTransform: 'uppercase', color: 'var(--neon-amber)' }}>
              Monthly EMI Commitment
            </span>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: '2.2rem', fontWeight: 900, color: '#fff', marginTop: 4 }}>
              ₹{formatIndian(emiResult.emiPaise / 100)} <span style={{ fontSize: '1rem', color: 'var(--text-secondary)' }}>/ mo</span>
            </div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>
              Final payment absorbs rounding: ₹{formatIndian(emiResult.finalPaymentPaise / 100)}
            </span>
          </div>

          <div>
            <span style={{ fontSize: '0.78rem', fontWeight: 800, textTransform: 'uppercase', color: 'var(--text-tertiary)' }}>
              Total Interest Paid Over Loan
            </span>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.6rem', fontWeight: 800, color: 'var(--neon-rose)', marginTop: 4 }}>
              ₹{formatIndian(emiResult.totalInterestPaise / 100)}
            </div>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>
              Total repayment: ₹{formatIndian(emiResult.totalRepaymentPaise / 100)}
            </span>
          </div>

          {affordability && (
            <div>
              <span style={{ fontSize: '0.78rem', fontWeight: 800, textTransform: 'uppercase', color: 'var(--text-tertiary)' }}>
                Share of Monthly Salary
              </span>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.6rem', fontWeight: 800, color: affordability.passesOneSeventhRule ? 'var(--neon-emerald)' : 'var(--neon-rose)', marginTop: 4 }}>
                {affordability.emiToIncomePctOneDecimal}
              </div>
              <span style={{ fontSize: '0.75rem', color: affordability.passesOneSeventhRule ? 'var(--neon-emerald)' : 'var(--neon-rose)' }}>
                {affordability.passesOneSeventhRule ? '✓ Within safe 1/7 salary rule' : '⚠ Exceeds 1/7 salary safe limit'}
              </span>
            </div>
          )}
        </div>
      )}

      {/* Job-Loss Survival Check (§5.10) */}
      {affordability && (
        <div style={{ padding: 20, borderRadius: 'var(--radius-lg)', background: 'rgba(17,24,39,0.7)', border: '1px solid var(--glass-border)' }}>
          <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#fff', margin: '0 0 14px 0', display: 'flex', alignItems: 'center', gap: 8 }}>
            <ShieldAlert size={18} style={{ color: 'var(--neon-cyan)' }} />
            Job-Loss Survival Stress-Test (Video 11 / §5.10)
          </h3>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 14 }}>
            <div style={{ padding: 14, borderRadius: 8, background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.06)' }}>
              <span style={{ fontSize: '0.76rem', color: 'var(--text-secondary)' }}>Combined Monthly Burn (Baseline + New EMI):</span>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.2rem', fontWeight: 800, color: '#fff', marginTop: 4 }}>
                ₹{formatIndian((baselinePaise + (emiResult?.emiPaise || 0)) / 100)} / mo
              </div>
              <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)' }}>
                Baseline ₹{formatIndian(baselinePaise / 100)} + EMI ₹{formatIndian((emiResult?.emiPaise || 0) / 100)}
              </span>
            </div>

            <div style={{ padding: 14, borderRadius: 8, background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.06)' }}>
              <span style={{ fontSize: '0.76rem', color: 'var(--text-secondary)' }}>Liquid Emergency Buffer:</span>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.2rem', fontWeight: 800, color: 'var(--neon-cyan)', marginTop: 4 }}>
                ₹{formatIndian(liquidEmergencyPaise / 100)}
              </div>
              <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)' }}>
                Total liquid cash and savings
              </span>
            </div>

            <div style={{ padding: 14, borderRadius: 8, background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.06)' }}>
              <span style={{ fontSize: '0.76rem', color: 'var(--text-secondary)' }}>Survival Runway Without Salary:</span>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.2rem', fontWeight: 900, color: affordability.jobLossRunwayMonths >= 6 ? 'var(--neon-emerald)' : affordability.jobLossRunwayMonths >= 3 ? 'var(--neon-amber)' : 'var(--neon-rose)', marginTop: 4 }}>
                {affordability.jobLossRunwayMonths} Months Runway
              </div>
              <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)' }}>
                {affordability.jobLossRunwayMonths < 3 ? 'Danger: Less than 3 months runway' : 'Safe: Meets runway safety guideline'}
              </span>
            </div>
          </div>
        </div>
      )}

      {/* 48-Hour Cooldown Action (§5.14) */}
      <div style={{ padding: 18, borderRadius: 'var(--radius-lg)', background: 'rgba(255,255,255,0.02)', border: '1px solid rgba(255,255,255,0.08)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 14 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Clock size={24} style={{ color: 'var(--neon-cyan)' }} />
          <div>
            <div style={{ fontSize: '0.9rem', fontWeight: 800, color: '#fff' }}>
              48-Hour Emotional Purchase Cooldown Rule (Video 5)
            </div>
            <div style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)' }}>
              Never commit to a major EMI on flash sales or impulse. Wait 48 hours to evaluate ego versus utility.
            </div>
          </div>
        </div>

        <button
          onClick={() => setCooldownActivated(true)}
          className="btn btn-sm"
          style={{
            background: cooldownActivated ? 'rgba(16, 185, 129, 0.2)' : 'rgba(255,255,255,0.08)',
            border: cooldownActivated ? '1px solid var(--neon-emerald)' : '1px solid rgba(255,255,255,0.15)',
            color: cooldownActivated ? 'var(--neon-emerald)' : '#fff',
            fontWeight: 700,
            padding: '8px 14px',
            borderRadius: 8,
          }}
        >
          {cooldownActivated ? '✓ 48-Hour Cooldown Activated' : 'Start 48-Hour Cooldown Timer'}
        </button>
      </div>
    </div>
  );
}
