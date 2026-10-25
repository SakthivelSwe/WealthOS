import React, { useState, useMemo } from 'react';
import { useData } from '../../hooks/useData';
import { allocate, FRAMEWORKS, type FrameworkType } from '../../lib/allocation';
import { evaluateEmi, evaluateSavingsRate, evaluateRunway, calculateEmergencyTiers } from '../../lib/healthRules';
import { formatIndian } from '../../lib/money';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { Sliders, Shield, Zap, Sparkles, AlertTriangle, CheckCircle, ArrowRight, Lock, Unlock, HelpCircle } from 'lucide-react';

const JAR_METADATA: Record<string, { label: string; icon: string; color: string; desc: string }> = {
  NEEDS: { label: 'Necessities (Fixed & Essentials)', icon: '🏠', color: 'var(--neon-emerald)', desc: 'Rent, food, transport, bills, medical' },
  FREEDOM_SIP: { label: 'Financial Freedom (SIP)', icon: '📈', color: 'var(--neon-cyan)', desc: 'Long-term equity & mutual fund wealth' },
  EDUCATION: { label: 'Skill & Education', icon: '📚', color: '#818cf8', desc: 'Courses, books, self-upgrades' },
  PLAY: { label: 'Guilt-Free Play', icon: '☕', color: '#f43f5e', desc: 'Dining out, cafes, leisure' },
  LONG_TERM: { label: 'Long-Term & Emergency', icon: '🛡️', color: 'var(--neon-amber)', desc: 'Emergency reserve & big future goals' },
  GIVING: { label: 'Giving & Family Care', icon: '🤝', color: '#c084fc', desc: 'Charity, parents, family support' },
  EMERGENCY: { label: 'Emergency Fund', icon: '🛡️', color: 'var(--neon-amber)', desc: '6 to 7 months survival buffer' },
  BUFFER: { label: 'Unplanned Buffer', icon: '🧯', color: '#fb923c', desc: 'Home repairs, minor contingencies' },
  MEDIUM_TERM: { label: 'Medium-Term Goals', icon: '🚗', color: '#38bdf8', desc: 'Gold, vehicle down payments, FDs' },
  LONG_TERM_SIP: { label: 'Long-Term SIP', icon: '🚀', color: 'var(--neon-cyan)', desc: 'Index funds, compounding engine' },
  WANTS: { label: 'Discretionary Wants', icon: '🛍️', color: '#f43f5e', desc: 'Shopping, entertainment, lifestyle' },
  SAVINGS: { label: 'Savings & Investments', icon: '💰', color: 'var(--neon-emerald)', desc: 'Investments and debt elimination' },
};

export function AllocationSimulatorTab() {
  const { financialProfile, accounts, activeCycle, addSalaryCycle, updateFinancialProfile } = useData();

  // Prefill inputs from financial profile or active cycle or defaults
  const initialSalary = activeCycle ? activeCycle.salary_paise / 100 : 32200;
  const initialBaseline = financialProfile?.fixed_baseline_paise ? financialProfile.fixed_baseline_paise / 100 : 17710;

  const [salaryStr, setSalaryStr] = useState<string>(initialSalary.toString());
  const [baselineStr, setBaselineStr] = useState<string>(initialBaseline.toString());
  const [emiStr, setEmiStr] = useState<string>('0');
  const [selectedFramework, setSelectedFramework] = useState<FrameworkType>(financialProfile?.allocation_framework || 'SIX_JAR');
  const [creepLock, setCreepLock] = useState<boolean>(financialProfile?.lifestyle_creep_lock ?? true);
  const [shiftPlay, setShiftPlay] = useState<boolean>(financialProfile?.play_to_long_term_shift ?? false);
  const [emergencyFundComplete, setEmergencyFundComplete] = useState<boolean>(false);
  const [isSavedNotice, setIsSavedNotice] = useState<boolean>(false);

  // Convert rupees to paise safely
  const salaryPaise = useMemo(() => {
    const s = Math.max(0, parseInt(salaryStr.replace(/[^0-9]/g, ''), 10) || 0);
    return s * 100;
  }, [salaryStr]);

  const baselinePaise = useMemo(() => {
    const b = Math.max(0, parseInt(baselineStr.replace(/[^0-9]/g, ''), 10) || 0);
    return b * 100;
  }, [baselineStr]);

  const emiPaise = useMemo(() => {
    const e = Math.max(0, parseInt(emiStr.replace(/[^0-9]/g, ''), 10) || 0);
    return e * 100;
  }, [emiStr]);

  const frameworkDef = FRAMEWORKS[selectedFramework] || FRAMEWORKS.SIX_JAR;
  const emergencyTargetPaise = baselinePaise * 7;
  const emergencyBalancePaise = emergencyFundComplete ? emergencyTargetPaise : 0;

  // Run pure deterministic allocation engine (§5.3 / Appendix B)
  const alloc = useMemo(() => {
    return allocate({
      salaryPaise,
      framework: frameworkDef,
      baselinePaise,
      emiPaise,
      emergencyBalancePaise,
      emergencyTargetPaise,
      creepLock,
      bufferPaise: 150000,
      shiftPlayToLongTerm: shiftPlay,
      daysInCycle: 30,
    });
  }, [salaryPaise, frameworkDef, baselinePaise, emiPaise, emergencyBalancePaise, emergencyTargetPaise, creepLock, shiftPlay]);

  // Health checks (§5.6)
  const emiHealth = evaluateEmi(emiPaise, salaryPaise);
  const savingsJars = frameworkDef.savingsJars || [];
  const totalSavingsPaise = savingsJars.reduce((sum, j) => sum + (alloc.jars[j] || 0), 0);
  const savingsRateHealth = evaluateSavingsRate(totalSavingsPaise, salaryPaise);
  const emergencyTiers = calculateEmergencyTiers(emergencyTargetPaise);

  const handleStartCycle = async () => {
    const today = new Date().toISOString().split('T')[0];
    await addSalaryCycle({
      id: `cycle_${Date.now()}`,
      cycle_start_date: today,
      salary_paise: salaryPaise,
      baseline_paise: baselinePaise,
      emi_paise: emiPaise,
      framework: selectedFramework,
      jar_amounts: alloc.jars,
      days_in_cycle: 30,
      daily_safe_upi_paise: alloc.dailySafePaise,
      net_worth_snapshot_paise: accounts.reduce((s, a) => s + (a.current_balance_minor || 0), 0),
      checklist_state: {},
      created_at: Date.now(),
    });

    await updateFinancialProfile({
      fixed_baseline_paise: baselinePaise,
      allocation_framework: selectedFramework,
      lifestyle_creep_lock: creepLock,
      play_to_long_term_shift: shiftPlay,
    });

    setIsSavedNotice(true);
    setTimeout(() => setIsSavedNotice(false), 4000);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Title & Explainer */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.45rem', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Sliders size={22} style={{ color: 'var(--neon-cyan)' }} />
            Universal Salary Allocation & Jars Simulator
          </h2>
          <p style={{ fontSize: '0.84rem', color: 'var(--text-tertiary)', margin: '4px 0 0 0' }}>
            Interactive mathematical allocator conforming to §5.3 and 16 reference videos. Zero floating-point drift.
          </p>
        </div>

        <button
          onClick={handleStartCycle}
          className="btn btn-sm"
          style={{
            background: 'var(--neon-emerald)',
            color: '#000',
            fontWeight: 800,
            display: 'inline-flex',
            alignItems: 'center',
            gap: 6,
            padding: '8px 16px',
            borderRadius: 'var(--radius-pill)',
          }}
        >
          <Sparkles size={16} /> Activate Plan as Current Cycle
        </button>
      </div>

      {isSavedNotice && (
        <div style={{ padding: '12px 16px', borderRadius: 'var(--radius-md)', background: 'rgba(16, 185, 129, 0.15)', border: '1px solid var(--neon-emerald)', color: '#fff', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 8 }}>
          <CheckCircle size={18} style={{ color: 'var(--neon-emerald)' }} />
          Salary plan successfully activated and saved to your financial profile!
        </div>
      )}

      {/* Simulator Control Bento */}
      <div className="bento-grid" style={{ gridTemplateColumns: 'repeat(12, 1fr)', gap: 16 }}>
        {/* Salary Slider & Input */}
        <div className="col-span-12 md:col-span-6" style={{ gridColumn: 'span 6' }}>
          <Tilt3DCard>
            <div className="cyber-card" style={{ padding: 20 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Net Credited Salary (₹)</span>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: '1.25rem', fontWeight: 900, color: 'var(--neon-emerald)' }}>
                  ₹{formatIndian(salaryPaise / 100)}
                </span>
              </div>
              <input
                type="range"
                min="15000"
                max="150000"
                step="500"
                value={salaryStr}
                onChange={e => setSalaryStr(e.target.value)}
                style={{ width: '100%', accentColor: 'var(--neon-emerald)', cursor: 'pointer' }}
              />
              <div style={{ display: 'flex', gap: 6, marginTop: 10, flexWrap: 'wrap' }}>
                {['25000', '32200', '35000', '40000', '45000', '60000'].map(val => (
                  <button
                    key={val}
                    type="button"
                    onClick={() => setSalaryStr(val)}
                    style={{
                      padding: '3px 9px',
                      fontSize: '0.74rem',
                      borderRadius: 'var(--radius-pill)',
                      border: salaryStr === val ? '1.5px solid var(--neon-emerald)' : '1px solid rgba(255,255,255,0.08)',
                      background: salaryStr === val ? 'rgba(16,185,129,0.2)' : 'rgba(255,255,255,0.03)',
                      color: salaryStr === val ? '#fff' : 'var(--text-secondary)',
                      cursor: 'pointer',
                    }}
                  >
                    ₹{formatIndian(parseInt(val, 10))}
                  </button>
                ))}
              </div>
            </div>
          </Tilt3DCard>
        </div>

        {/* Baseline & Fixed Commitments */}
        <div className="col-span-12 md:col-span-6" style={{ gridColumn: 'span 6' }}>
          <Tilt3DCard>
            <div className="cyber-card" style={{ padding: 20 }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
                <div>
                  <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                    Fixed Baseline (₹)
                  </label>
                  <input
                    type="text"
                    value={baselineStr}
                    onChange={e => setBaselineStr(e.target.value)}
                    className="form-control"
                    style={{ width: '100%', padding: '8px 12px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: '#fff', fontFamily: 'var(--font-mono)' }}
                  />
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 4, display: 'block' }}>
                    Rent, mess, bills, transport
                  </span>
                </div>

                <div>
                  <label style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                    Monthly Loans / EMI (₹)
                  </label>
                  <input
                    type="text"
                    value={emiStr}
                    onChange={e => setEmiStr(e.target.value)}
                    className="form-control"
                    style={{ width: '100%', padding: '8px 12px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, color: '#fff', fontFamily: 'var(--font-mono)' }}
                  />
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 4, display: 'block' }}>
                    1/7 salary ceiling rule
                  </span>
                </div>
              </div>
            </div>
          </Tilt3DCard>
        </div>
      </div>

      {/* Framework Selector & Guardrail Toggles */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12, padding: '16px 20px', borderRadius: 'var(--radius-lg)', background: 'rgba(17,24,39,0.7)', border: '1px solid var(--glass-border)' }}>
        {/* Framework Switcher */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          <span style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>Framework:</span>
          {(['SIX_JAR', 'THREE_BOX', 'FIFTY_THIRTY_TWENTY'] as FrameworkType[]).map(fw => (
            <button
              key={fw}
              type="button"
              onClick={() => setSelectedFramework(fw)}
              style={{
                padding: '6px 14px',
                fontSize: '0.8rem',
                fontWeight: 700,
                borderRadius: 'var(--radius-pill)',
                border: selectedFramework === fw ? '1.5px solid var(--neon-cyan)' : '1px solid rgba(255,255,255,0.08)',
                background: selectedFramework === fw ? 'rgba(6, 182, 212, 0.2)' : 'rgba(255,255,255,0.04)',
                color: selectedFramework === fw ? '#fff' : 'var(--text-secondary)',
                cursor: 'pointer',
                transition: 'all 0.15s ease',
              }}
            >
              {fw === 'SIX_JAR' ? '🍯 6-Jar (Harv Eker)' : fw === 'THREE_BOX' ? '📦 3-Box (Safety/Growth)' : '⚖️ 50 / 30 / 20'}
            </button>
          ))}
        </div>

        {/* Behavioral Toggles (§5.14) */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.8rem', fontWeight: 600, color: '#fff', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={creepLock}
              onChange={e => setCreepLock(e.target.checked)}
              style={{ accentColor: 'var(--neon-emerald)' }}
            />
            {creepLock ? <Lock size={14} style={{ color: 'var(--neon-emerald)' }} /> : <Unlock size={14} style={{ color: 'var(--text-tertiary)' }} />}
            Lifestyle Creep Lock (§5.3)
          </label>

          {selectedFramework === 'SIX_JAR' && (
            <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.8rem', fontWeight: 600, color: '#fff', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={shiftPlay}
                onChange={e => setShiftPlay(e.target.checked)}
                style={{ accentColor: 'var(--neon-amber)' }}
              />
              Play → Long-Term Shift (+5%)
            </label>
          )}

          <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.8rem', fontWeight: 600, color: '#fff', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={emergencyFundComplete}
              onChange={e => setEmergencyFundComplete(e.target.checked)}
              style={{ accentColor: 'var(--neon-cyan)' }}
            />
            Emergency Target Full (Route to Sink Jar)
          </label>
        </div>
      </div>

      {/* Invariant & Flags Banners */}
      {alloc.flags.length > 0 && (
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {alloc.flags.map(f => (
            <div
              key={f}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                padding: '6px 12px',
                borderRadius: 'var(--radius-pill)',
                fontSize: '0.78rem',
                fontWeight: 700,
                background: f === 'SHORTFALL' ? 'rgba(239, 68, 68, 0.2)' : 'rgba(245, 158, 11, 0.2)',
                border: `1px solid ${f === 'SHORTFALL' ? 'var(--neon-rose)' : 'var(--neon-amber)'}`,
                color: f === 'SHORTFALL' ? 'var(--neon-rose)' : 'var(--neon-amber)',
              }}
            >
              <AlertTriangle size={14} />
              {f === 'SHORTFALL' && 'SHORTFALL: Income is insufficient to cover mandatory baseline + EMI'}
              {f === 'SPEND_JAR_EMPTY' && 'SPEND JAR EMPTY: Baseline commitments leaves ₹0 for guilt-free play'}
              {f === 'SAVINGS_REDUCED' && 'SAVINGS REDUCED: High necessities forced reduction in savings jars'}
              {f === 'BELOW_SAVINGS_FLOOR' && 'BELOW SAVINGS FLOOR: Total savings is below the recommended 7% safety floor'}
            </div>
          ))}
        </div>
      )}

      {/* Daily Safe UPI Meter Banner */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: 16,
          padding: 20,
          borderRadius: 'var(--radius-lg)',
          background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.12) 0%, rgba(16, 185, 129, 0.08) 100%)',
          border: '1px solid rgba(6, 182, 212, 0.3)',
        }}
      >
        <div>
          <span style={{ fontSize: '0.76rem', fontWeight: 800, textTransform: 'uppercase', color: 'var(--neon-cyan)', letterSpacing: '0.05em' }}>
            Daily Safe UPI Meter (30-Day Cycle)
          </span>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: '2.1rem', fontWeight: 900, color: '#fff', marginTop: 4 }}>
            ₹{formatIndian(alloc.dailySafePaise / 100)} <span style={{ fontSize: '1rem', color: 'var(--text-secondary)' }}>/ day</span>
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>
            Weekly UPI Lite budget: ₹{formatIndian(alloc.weeklySafePaise / 100)}
          </span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 6, borderLeft: '1px solid rgba(255,255,255,0.08)', paddingLeft: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Guilt-Free Spend Jar:</span>
            <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--neon-rose)' }}>
              ₹{formatIndian(alloc.dailySafePaise / 100)}
            </span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Needs Allowance via UPI:</span>
            <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--neon-emerald)' }}>
              ₹{formatIndian(Math.floor((financialProfile?.upi_needs_allowance_paise || 0) / 3000))}
            </span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.84rem', fontWeight: 800, paddingTop: 4, borderTop: '1px dashed rgba(255,255,255,0.1)' }}>
            <span style={{ color: '#fff' }}>Total Daily Cap:</span>
            <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--neon-cyan)' }}>
              ₹{formatIndian(alloc.dailySafePaise / 100 + Math.floor((financialProfile?.upi_needs_allowance_paise || 0) / 3000))}
            </span>
          </div>
        </div>
      </div>

      {/* Jars Grid */}
      <div>
        <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#fff', marginBottom: 12 }}>
          Allocated Jars ({frameworkDef.order.length} Envelopes) — Sum: ₹{formatIndian(salaryPaise / 100)}
        </h3>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 14 }}>
          {frameworkDef.order.map(jarKey => {
            const amount = alloc.jars[jarKey] || 0;
            const pct = salaryPaise > 0 ? ((amount * 100) / salaryPaise).toFixed(1) : '0';
            const meta = JAR_METADATA[jarKey] || { label: jarKey, icon: '📦', color: 'var(--neon-cyan)', desc: '' };
            const isEmergencyJar = frameworkDef.emergencyJar === jarKey;
            const isSinkJar = frameworkDef.sinkJar === jarKey;
            const isSpendJar = frameworkDef.spendJar === jarKey;

            return (
              <div
                key={jarKey}
                style={{
                  padding: 16,
                  borderRadius: 'var(--radius-md)',
                  background: 'rgba(17,24,39,0.7)',
                  border: `1px solid ${amount > 0 ? 'rgba(255,255,255,0.08)' : 'rgba(239,68,68,0.2)'}`,
                  position: 'relative',
                  overflow: 'hidden',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: '1.4rem' }}>{meta.icon}</span>
                    <div>
                      <div style={{ fontSize: '0.85rem', fontWeight: 800, color: '#fff' }}>{meta.label}</div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)' }}>{meta.desc}</div>
                    </div>
                  </div>
                  <span style={{ fontSize: '0.75rem', fontWeight: 800, color: meta.color, background: 'rgba(255,255,255,0.04)', padding: '2px 6px', borderRadius: 4 }}>
                    {pct}%
                  </span>
                </div>

                <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.4rem', fontWeight: 900, color: amount > 0 ? '#fff' : 'var(--neon-rose)', marginTop: 12 }}>
                  ₹{formatIndian(amount / 100)}
                </div>

                {/* Role tags */}
                <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
                  {isSpendJar && (
                    <span style={{ fontSize: '0.68rem', padding: '1px 6px', borderRadius: 4, background: 'rgba(244,63,94,0.15)', color: '#f43f5e', fontWeight: 700 }}>
                      Daily Spend Source
                    </span>
                  )}
                  {isEmergencyJar && (
                    <span style={{ fontSize: '0.68rem', padding: '1px 6px', borderRadius: 4, background: 'rgba(245,158,11,0.15)', color: 'var(--neon-amber)', fontWeight: 700 }}>
                      Emergency Destination
                    </span>
                  )}
                  {isSinkJar && (
                    <span style={{ fontSize: '0.68rem', padding: '1px 6px', borderRadius: 4, background: 'rgba(6,182,212,0.15)', color: 'var(--neon-cyan)', fontWeight: 700 }}>
                      Surplus Sink Jar
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 7-7-7 Health Rules Strip (§5.6) */}
      <div style={{ padding: 20, borderRadius: 'var(--radius-lg)', background: 'rgba(17,24,39,0.7)', border: '1px solid var(--glass-border)' }}>
        <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#fff', margin: '0 0 14px 0', display: 'flex', alignItems: 'center', gap: 8 }}>
          <Shield size={18} style={{ color: 'var(--neon-cyan)' }} />
          7-7-7 Health Rules & Vital Signs Evaluation (§5.6)
        </h3>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 14 }}>
          {/* EMI Rule */}
          <div style={{ padding: 14, borderRadius: 8, background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>1/7 EMI Affordability</span>
              <span style={{ fontSize: '0.72rem', fontWeight: 800, color: emiHealth === 'GREEN' ? 'var(--neon-emerald)' : emiHealth === 'AMBER' ? 'var(--neon-amber)' : 'var(--neon-rose)' }}>
                ● {emiHealth}
              </span>
            </div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.15rem', fontWeight: 800, color: '#fff', marginTop: 4 }}>
              ₹{formatIndian(emiPaise / 100)} / month
            </div>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', marginTop: 4, display: 'block' }}>
              Max safe: ₹{formatIndian(Math.floor(salaryPaise / 700))} (ceiling 30%)
            </span>
          </div>

          {/* Savings Rate Rule */}
          <div style={{ padding: 14, borderRadius: 8, background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>Savings Rate (≥20%)</span>
              <span style={{ fontSize: '0.72rem', fontWeight: 800, color: savingsRateHealth === 'GREEN' ? 'var(--neon-emerald)' : savingsRateHealth === 'AMBER' ? 'var(--neon-amber)' : 'var(--neon-rose)' }}>
                ● {savingsRateHealth}
              </span>
            </div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.15rem', fontWeight: 800, color: '#fff', marginTop: 4 }}>
              {salaryPaise > 0 ? ((totalSavingsPaise * 100) / salaryPaise).toFixed(1) : '0'}%
            </div>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', marginTop: 4, display: 'block' }}>
              ₹{formatIndian(totalSavingsPaise / 100)} saved monthly
            </span>
          </div>

          {/* Emergency Fund Target */}
          <div style={{ padding: 14, borderRadius: 8, background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>Emergency Target (7 Mo)</span>
              <span style={{ fontSize: '0.72rem', fontWeight: 800, color: 'var(--neon-cyan)' }}>
                ● 7 MONTHS
              </span>
            </div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.15rem', fontWeight: 800, color: '#fff', marginTop: 4 }}>
              ₹{formatIndian(emergencyTargetPaise / 100)}
            </div>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', marginTop: 4, display: 'block' }}>
              Tiers: 20% Cash / 50% FD / 30% Gold
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
