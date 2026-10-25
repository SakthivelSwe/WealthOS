import { useState, useMemo } from 'react';
import { useData } from '../../hooks/useData';
import { allocate, FRAMEWORKS, type FrameworkType } from '../../lib/allocation';
import { formatMoney } from '../../types';
import {
  X,
  Sparkles,
  CheckCircle2,
  ArrowRight,
  ShieldAlert,
  Wallet,
  Calendar,
  Lock,
  ExternalLink,
} from 'lucide-react';

interface PaydayWizardModalProps {
  onClose: () => void;
  detectedSalaryMinor?: number;
  detectedPaydayDate?: string;
}

export function PaydayWizardModal({
  onClose,
  detectedSalaryMinor,
  detectedPaydayDate,
}: PaydayWizardModalProps) {
  const { financialProfile, accounts, addSalaryCycle, settings } = useData();
  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;

  const [salaryRupees, setSalaryRupees] = useState<number>(
    detectedSalaryMinor ? Math.floor(detectedSalaryMinor / 100) : 32200
  );
  const [cycleStartDate, setCycleStartDate] = useState<string>(
    detectedPaydayDate || new Date().toISOString().split('T')[0]
  );
  const [framework, setFramework] = useState<FrameworkType>('SIX_JAR');
  const [creepLock, setCreepLock] = useState<boolean>(
    financialProfile?.lifestyle_creep_lock ?? true
  );

  // First 48-Hour Checklist State (§8.8, Video 3)
  const [checklist, setChecklist] = useState<Record<string, boolean>>({
    move_emergency_sip: false,
    load_upi_lite: false,
    pay_fixed_bills: false,
    audit_mandates: false,
  });

  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [saving, setSaving] = useState(false);

  const baselinePaise = financialProfile?.fixed_baseline_paise || 1771000;
  const emiPaise = (financialProfile as any)?.current_total_emi_paise || (financialProfile as any)?.monthly_emi_paise || 0;
  const bufferPaise = financialProfile?.needs_buffer_paise || 150000;

  // Run allocation
  const allocation = useMemo(() => {
    const salaryPaise = salaryRupees * 100;
    return allocate({
      salaryPaise,
      framework: FRAMEWORKS[framework],
      baselinePaise,
      emiPaise,
      emergencyBalancePaise: 0,
      emergencyTargetPaise: baselinePaise * 7,
      creepLock: creepLock,
      bufferPaise: bufferPaise,
      shiftPlayToLongTerm: false,
      daysInCycle: 30,
    });
  }, [salaryRupees, framework, baselinePaise, emiPaise, creepLock, bufferPaise]);

  const toggleChecklistItem = (key: string) => {
    setChecklist(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const completedChecklistCount = Object.values(checklist).filter(Boolean).length;

  const handleActivateCycle = async () => {
    setSaving(true);
    try {
      await addSalaryCycle({
        id: `cycle_${Date.now()}`,
        cycle_start_date: cycleStartDate,
        salary_paise: salaryRupees * 100,
        baseline_paise: baselinePaise,
        emi_paise: emiPaise,
        framework,
        jar_amounts: allocation.jars,
        days_in_cycle: 30,
        daily_safe_upi_paise: allocation.dailySafePaise,
        net_worth_snapshot_paise: null,
        checklist_state: checklist,
        created_at: Date.now(),
      });
      onClose();
    } catch (e) {
      console.error('Failed to activate cycle:', e);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 1300 }}>
      <div
        className="modal-content"
        onClick={e => e.stopPropagation()}
        style={{ maxWidth: 740, maxHeight: '90vh', overflowY: 'auto' }}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div
              style={{
                width: 38,
                height: 38,
                borderRadius: 10,
                background: 'rgba(16, 185, 129, 0.15)',
                color: 'var(--neon-emerald)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Sparkles size={20} />
            </div>
            <div>
              <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, margin: 0 }}>
                Payday Wizard (First 48 Hours)
              </h2>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', margin: '2px 0 0 0' }}>
                Videos 3 &amp; 4: The first 48 hours decide whether your salary lasts 30 days or leaks in 17 days
              </p>
            </div>
          </div>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        {/* Wizard Steps Navigation */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8, marginBottom: 20 }}>
          {[
            { num: 1, label: '1. Confirm Salary' },
            { num: 2, label: '2. Jar Allocation' },
            { num: 3, label: '3. 48h Action Plan' },
          ].map(s => (
            <button
              key={s.num}
              type="button"
              onClick={() => setStep(s.num as any)}
              style={{
                padding: '8px 12px',
                borderRadius: 'var(--radius-md)',
                background: step === s.num ? 'rgba(16, 185, 129, 0.15)' : 'rgba(255, 255, 255, 0.03)',
                border: step === s.num ? '1px solid var(--neon-emerald)' : '1px solid var(--border-subtle)',
                color: step === s.num ? 'var(--neon-emerald)' : 'var(--text-secondary)',
                fontSize: '0.78rem',
                fontWeight: 700,
                cursor: 'pointer',
              }}
            >
              {s.label}
            </button>
          ))}
        </div>

        {/* Step 1: Confirm Salary & Framework */}
        {step === 1 && (
          <div>
            <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 14, marginBottom: 16 }}>
              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">Salary Credited Amount ({currency}) *</label>
                <input
                  type="number"
                  value={salaryRupees}
                  onChange={e => setSalaryRupees(Math.max(1000, Number(e.target.value)))}
                  style={{ fontSize: '1.25rem', fontWeight: 800, fontFamily: 'var(--font-outfit)' }}
                />
              </div>

              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">Cycle Start Date *</label>
                <input
                  type="date"
                  value={cycleStartDate}
                  onChange={e => setCycleStartDate(e.target.value)}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Allocation Framework</label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 8 }}>
                {[
                  { key: 'SIX_JAR', title: '6-Jar System', desc: 'Video 2 (Balanced)' },
                  { key: 'THREE_BOX', title: '3-Box Peace', desc: 'Videos 3, 15 (Simple)' },
                  { key: 'FIFTY_THIRTY_TWENTY', title: '50/30/20 Rule', desc: 'Video 13 (Classic)' },
                ].map(f => (
                  <button
                    key={f.key}
                    type="button"
                    onClick={() => setFramework(f.key as FrameworkType)}
                    style={{
                      padding: '10px 8px',
                      borderRadius: 'var(--radius-md)',
                      background: framework === f.key ? 'rgba(0, 240, 255, 0.12)' : 'rgba(255, 255, 255, 0.03)',
                      border: framework === f.key ? '1px solid var(--neon-cyan)' : '1px solid var(--border-subtle)',
                      color: framework === f.key ? 'var(--neon-cyan)' : 'var(--text-secondary)',
                      textAlign: 'left',
                      cursor: 'pointer',
                    }}
                  >
                    <div style={{ fontWeight: 800, fontSize: '0.82rem' }}>{f.title}</div>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)' }}>{f.desc}</div>
                  </button>
                ))}
              </div>
            </div>

            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '12px 14px',
                borderRadius: 'var(--radius-md)',
                background: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid var(--glass-border)',
                marginBottom: 20,
              }}
            >
              <div>
                <div style={{ fontWeight: 700, fontSize: '0.85rem', color: '#f1f5f9' }}>
                  Lifestyle Creep Lock (Video 4)
                </div>
                <div style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)' }}>
                  Keeps Needs capped at baseline (₹{Math.floor(baselinePaise / 100).toLocaleString('en-IN')}) and directs extra to Wealth / Emergency.
                </div>
              </div>
              <input
                type="checkbox"
                checked={creepLock}
                onChange={e => setCreepLock(e.target.checked)}
                style={{ width: 18, height: 18, cursor: 'pointer' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button
                type="button"
                className="btn btn-primary btn-md"
                onClick={() => setStep(2)}
                style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
              >
                Next: View Jars <ArrowRight size={16} />
              </button>
            </div>
          </div>
        )}

        {/* Step 2: Jar Allocation Preview */}
        {step === 2 && (
          <div>
            <div style={{ marginBottom: 14 }}>
              <div style={{ fontSize: '0.82rem', color: 'var(--text-tertiary)' }}>
                Total Salary: <strong>₹{salaryRupees.toLocaleString('en-IN')}</strong> · Daily Safe UPI Spend:{' '}
                <strong style={{ color: 'var(--neon-cyan)' }}>
                  {formatMoney(allocation.dailySafePaise, currency, masked)} / day
                </strong>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 10, marginBottom: 20 }}>
              {Object.entries(allocation.jars).map(([jar, paise]) => (
                <div
                  key={jar}
                  style={{
                    padding: 12,
                    borderRadius: 'var(--radius-md)',
                    background: 'rgba(255, 255, 255, 0.03)',
                    border: '1px solid var(--glass-border)',
                  }}
                >
                  <div style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)', textTransform: 'uppercase' }}>
                    {jar.replace(/_/g, ' ')}
                  </div>
                  <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.2rem', fontWeight: 800, color: '#f1f5f9', marginTop: 4 }}>
                    {formatMoney(paise, currency, masked)}
                  </div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--neon-cyan)', marginTop: 2 }}>
                    {salaryRupees > 0 ? ((paise / (salaryRupees * 100)) * 100).toFixed(1) : '0'}% of salary
                  </div>
                </div>
              ))}
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <button type="button" className="btn btn-ghost" onClick={() => setStep(1)}>
                Back
              </button>
              <button
                type="button"
                className="btn btn-primary btn-md"
                onClick={() => setStep(3)}
                style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
              >
                Next: 48-Hour Plan <ArrowRight size={16} />
              </button>
            </div>
          </div>
        )}

        {/* Step 3: First 48-Hour Action Plan (§8.8, Video 3) */}
        {step === 3 && (
          <div>
            <div
              style={{
                padding: 14,
                borderRadius: 'var(--radius-md)',
                background: 'rgba(16, 185, 129, 0.08)',
                border: '1px solid rgba(16, 185, 129, 0.25)',
                marginBottom: 16,
              }}
            >
              <div style={{ fontWeight: 800, fontSize: '0.88rem', color: 'var(--neon-emerald)' }}>
                🛡️ Salary Day First 48-Hour Execution Protocol (Video 3)
              </div>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginTop: 4, lineHeight: 1.45, margin: 0 }}>
                Never leave unallocated savings sitting in your primary UPI-linked account. Perform these 4 transfers within 48 hours to secure your month:
              </p>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 20 }}>
              {[
                {
                  key: 'move_emergency_sip',
                  title: '1. Move Emergency & Wealth Money Out of Daily Account',
                  desc: `Transfer ₹${Math.floor((allocation.jars.LONG_TERM || allocation.jars.EMERGENCY || 0) / 100).toLocaleString('en-IN')} to Emergency vault and trigger ₹${Math.floor((allocation.jars.FREEDOM_SIP || allocation.jars.SAVINGS || 0) / 100).toLocaleString('en-IN')} SIP.`,
                },
                {
                  key: 'load_upi_lite',
                  title: '2. Load Weekly Discretionary UPI Lite Allowance',
                  desc: `Load ₹${Math.floor((allocation.dailySafePaise * 7) / 100).toLocaleString('en-IN')} into UPI Lite wallet. Disconnect main bank from small QR scans.`,
                },
                {
                  key: 'pay_fixed_bills',
                  title: '3. Pay Rent, EMI, and Utility Bills Today',
                  desc: `Clear your ₹${Math.floor(baselinePaise / 100).toLocaleString('en-IN')} essential commitments before spending any discretionary money.`,
                },
                {
                  key: 'audit_mandates',
                  title: '4. Audit Active Subscriptions & AutoPay Mandates',
                  desc: 'Review any active recurring mandates in GPay / PhonePe / NetBanking to stop zombie subscriptions.',
                },
              ].map(item => (
                <div
                  key={item.key}
                  onClick={() => toggleChecklistItem(item.key)}
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: 12,
                    padding: '12px 14px',
                    borderRadius: 'var(--radius-md)',
                    background: checklist[item.key] ? 'rgba(16, 185, 129, 0.12)' : 'rgba(255, 255, 255, 0.03)',
                    border: checklist[item.key] ? '1px solid var(--neon-emerald)' : '1px solid var(--glass-border)',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                >
                  <input
                    type="checkbox"
                    checked={checklist[item.key] || false}
                    onChange={() => {}}
                    style={{ marginTop: 2, cursor: 'pointer' }}
                  />
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.85rem', color: checklist[item.key] ? 'var(--neon-emerald)' : '#f1f5f9' }}>
                      {item.title}
                    </div>
                    <div style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                      {item.desc}
                    </div>
                  </div>
                </div>
              ))}
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <button type="button" className="btn btn-ghost" onClick={() => setStep(2)}>
                Back
              </button>

              <button
                type="button"
                className="btn btn-primary btn-md"
                onClick={handleActivateCycle}
                disabled={saving}
                style={{
                  background: 'var(--neon-emerald)',
                  color: '#000',
                  fontWeight: 800,
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                }}
              >
                <CheckCircle2 size={16} />
                {saving ? 'Activating Cycle...' : `Activate Cycle (${completedChecklistCount}/4 Done)`}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
