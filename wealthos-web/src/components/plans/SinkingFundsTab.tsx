import React, { useState } from 'react';
import { useData } from '../../hooks/useData';
import { formatIndian } from '../../lib/money';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { Umbrella, Plus, Trash2, Calendar, Check, Sparkles, AlertCircle } from 'lucide-react';

const SINKING_TEMPLATES = [
  { name: 'Health Insurance Premium', targetRupees: 18000, icon: '🩺', desc: 'Family floater annual renewal' },
  { name: 'Two-Wheeler / Car Service', targetRupees: 10000, icon: '🚗', desc: 'Annual service, oil, insurance' },
  { name: 'Festivals & Family Functions (Moi)', targetRupees: 25000, icon: '✨', desc: 'Diwali, Pongal, weddings' },
  { name: 'Annual Software Subscriptions', targetRupees: 6000, icon: '💻', desc: 'Cloud, productivity, media' },
  { name: 'School Admission & Annual Fees', targetRupees: 35000, icon: '🎒', desc: 'June school fees & books' },
];

export function SinkingFundsTab() {
  const { sinkingFunds, addSinkingFund, deleteSinkingFund, updateSinkingFund } = useData();

  const [showAddForm, setShowAddForm] = useState(false);
  const [name, setName] = useState('');
  const [targetStr, setTargetStr] = useState('');
  const [targetDate, setTargetDate] = useState(() => {
    const d = new Date();
    d.setFullYear(d.getFullYear() + 1);
    return d.toISOString().split('T')[0];
  });

  const handleApplyTemplate = (tpl: typeof SINKING_TEMPLATES[0]) => {
    setName(tpl.name);
    setTargetStr(tpl.targetRupees.toString());
    setShowAddForm(true);
  };

  const handleCreateFund = async (e: React.FormEvent) => {
    e.preventDefault();
    const target = parseInt(targetStr.replace(/[^0-9]/g, ''), 10) || 0;
    if (name && target > 0) {
      const targetPaise = target * 100;
      const monthlyPaise = Math.floor(targetPaise / 12);
      await addSinkingFund({
        id: `sink_${Date.now()}`,
        name,
        target_paise: targetPaise,
        current_paise: 0,
        target_date: targetDate,
        monthly_allocation_paise: monthlyPaise,
        archived: false,
        created_at: Date.now(),
        updated_at: Date.now(),
      });
      setName('');
      setTargetStr('');
      setShowAddForm(false);
    }
  };

  const totalMonthlyCommitmentPaise = sinkingFunds.reduce((sum, f) => sum + f.monthly_allocation_paise, 0);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Title */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.45rem', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Umbrella size={22} style={{ color: 'var(--neon-cyan)' }} />
            Sinking Funds Envelope Planner (§5.11)
          </h2>
          <p style={{ fontSize: '0.84rem', color: 'var(--text-tertiary)', margin: '4px 0 0 0' }}>
            Smoothes large annual or irregular expenses into 12 equal monthly savings allocations.
          </p>
        </div>

        <button
          onClick={() => setShowAddForm(true)}
          className="btn btn-sm btn-primary"
          style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
        >
          <Plus size={15} /> Create Sinking Fund
        </button>
      </div>

      {/* Overview Banner */}
      <div
        style={{
          padding: 20,
          borderRadius: 'var(--radius-lg)',
          background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.12) 0%, rgba(17, 24, 39, 0.8) 100%)',
          border: '1px solid rgba(6, 182, 212, 0.3)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 16,
        }}
      >
        <div>
          <span style={{ fontSize: '0.78rem', fontWeight: 800, textTransform: 'uppercase', color: 'var(--neon-cyan)' }}>
            Total Monthly Sinking Commitment
          </span>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: '2rem', fontWeight: 900, color: '#fff', marginTop: 4 }}>
            ₹{formatIndian(totalMonthlyCommitmentPaise / 100)} <span style={{ fontSize: '1rem', color: 'var(--text-secondary)' }}>/ month</span>
          </div>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>
            Automatically planned from your Necessities or Long-Term savings envelopes
          </span>
        </div>

        <div style={{ padding: '8px 16px', borderRadius: 8, background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)' }}>
          <span style={{ fontSize: '0.74rem', color: 'var(--text-secondary)', display: 'block' }}>Active Envelopes</span>
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: '1.3rem', fontWeight: 900, color: '#fff' }}>
            {sinkingFunds.length} Funds
          </span>
        </div>
      </div>

      {/* Quick Preset Templates */}
      <div>
        <span style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--text-tertiary)', textTransform: 'uppercase', marginBottom: 8, display: 'block' }}>
          Suggested Indian Templates (Video 13):
        </span>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {SINKING_TEMPLATES.map(tpl => (
            <button
              key={tpl.name}
              type="button"
              onClick={() => handleApplyTemplate(tpl)}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                padding: '6px 12px',
                borderRadius: 'var(--radius-pill)',
                background: 'rgba(255,255,255,0.03)',
                border: '1px solid rgba(255,255,255,0.08)',
                color: '#fff',
                fontSize: '0.78rem',
                cursor: 'pointer',
              }}
            >
              <span>{tpl.icon}</span>
              <span style={{ fontWeight: 600 }}>{tpl.name}</span>
              <span style={{ color: 'var(--neon-cyan)', fontWeight: 800 }}>₹{formatIndian(tpl.targetRupees)}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Add Sinking Fund Form Modal */}
      {showAddForm && (
        <div style={{ padding: 20, borderRadius: 'var(--radius-lg)', background: 'rgba(17,24,39,0.9)', border: '1.5px solid var(--neon-cyan)' }}>
          <h3 style={{ fontSize: '1rem', fontWeight: 800, color: '#fff', margin: '0 0 14px 0' }}>New Sinking Fund Envelope</h3>
          <form onSubmit={handleCreateFund} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr)) 100px', gap: 12, alignItems: 'end' }}>
            <div>
              <label style={{ fontSize: '0.76rem', color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>Fund Name *</label>
              <input
                type="text"
                placeholder="e.g. Vehicle Insurance"
                value={name}
                onChange={e => setName(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 6, color: '#fff' }}
                required
              />
            </div>
            <div>
              <label style={{ fontSize: '0.76rem', color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>Annual Target Amount (₹) *</label>
              <input
                type="text"
                placeholder="e.g. 12000"
                value={targetStr}
                onChange={e => setTargetStr(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 6, color: '#fff' }}
                required
              />
            </div>
            <div>
              <label style={{ fontSize: '0.76rem', color: 'var(--text-secondary)', display: 'block', marginBottom: 4 }}>Due Date</label>
              <input
                type="date"
                value={targetDate}
                onChange={e => setTargetDate(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 6, color: '#fff' }}
              />
            </div>
            <div style={{ display: 'flex', gap: 6 }}>
              <button type="submit" className="btn btn-sm btn-primary" style={{ padding: '8px 16px' }}>Save</button>
              <button type="button" onClick={() => setShowAddForm(false)} className="btn btn-sm btn-ghost">Cancel</button>
            </div>
          </form>
        </div>
      )}

      {/* Sinking Funds List */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 16 }}>
        {sinkingFunds.length === 0 ? (
          <div style={{ padding: 32, textAlign: 'center', color: 'var(--text-tertiary)', borderRadius: 12, background: 'rgba(255,255,255,0.01)', border: '1px dashed rgba(255,255,255,0.08)', gridColumn: '1 / -1' }}>
            No sinking funds added yet. Click one of the templates above or create a custom fund to protect yourself against annual financial shocks.
          </div>
        ) : (
          sinkingFunds.map(f => {
            const pct = f.target_paise > 0 ? Math.min(100, Math.floor((f.current_paise * 100) / f.target_paise)) : 0;
            return (
              <Tilt3DCard key={f.id}>
                <div className="cyber-card" style={{ padding: 20 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div>
                      <span style={{ fontWeight: 800, fontSize: '0.95rem', color: '#fff' }}>{f.name}</span>
                      <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', display: 'block', marginTop: 2 }}>
                        Target due: {f.target_date}
                      </span>
                    </div>

                    <button
                      type="button"
                      onClick={() => deleteSinkingFund(f.id)}
                      style={{ background: 'transparent', border: 'none', color: 'var(--text-tertiary)', cursor: 'pointer', padding: 4 }}
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginTop: 14 }}>
                    <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.25rem', fontWeight: 900, color: 'var(--neon-cyan)' }}>
                      ₹{formatIndian(f.current_paise / 100)}{' '}
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
                        / ₹{formatIndian(f.target_paise / 100)}
                      </span>
                    </div>
                    <span style={{ fontSize: '0.75rem', fontWeight: 800, color: 'var(--neon-emerald)' }}>
                      {pct}%
                    </span>
                  </div>

                  {/* Progress Bar */}
                  <div style={{ width: '100%', height: 6, borderRadius: 3, background: 'rgba(255,255,255,0.06)', overflow: 'hidden', marginTop: 8 }}>
                    <div style={{ width: `${pct}%`, height: '100%', background: 'var(--neon-cyan)', borderRadius: 3, transition: 'width 0.3s ease' }} />
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 14, paddingTop: 10, borderTop: '1px solid rgba(255,255,255,0.05)', fontSize: '0.78rem' }}>
                    <span style={{ color: 'var(--text-secondary)' }}>Monthly Allocation:</span>
                    <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 800, color: '#fff' }}>
                      ₹{formatIndian(f.monthly_allocation_paise / 100)} / mo
                    </span>
                  </div>
                </div>
              </Tilt3DCard>
            );
          })
        )}
      </div>
    </div>
  );
}
