import React, { useState, useMemo } from 'react';
import { useData } from '../../hooks/useData';
import { compareStrategies, type DebtLoan } from '../../lib/debtPayoff';
import { formatIndian } from '../../lib/money';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { CreditCard, Flame, Zap, Award, ArrowRight, ShieldCheck, AlertTriangle, Plus, Trash2 } from 'lucide-react';

export function DebtSimulatorTab() {
  const { debts } = useData();

  // Load from real user debts or provide default Appendix A.4 loans for interactive demonstration
  const [customLoans, setCustomLoans] = useState<DebtLoan[]>([
    { name: 'Credit Card (36% APR)', balancePaise: 6000000, rateBps: 3600, minimumPaymentPaise: 300000 },
    { name: 'Personal Loan (18% APR)', balancePaise: 2000000, rateBps: 1800, minimumPaymentPaise: 150000 },
    { name: 'Vehicle Loan (14% APR)', balancePaise: 12000000, rateBps: 1400, minimumPaymentPaise: 400000 },
  ]);

  const [extraPaymentStr, setExtraPaymentStr] = useState<string>('2000');

  const extraMonthlyPaymentPaise = useMemo(() => {
    const p = Math.max(0, parseInt(extraPaymentStr.replace(/[^0-9]/g, ''), 10) || 0);
    return p * 100;
  }, [extraPaymentStr]);

  // Run pure debt payoff comparison (§5.9 / Appendix A.4)
  const comparison = useMemo(() => {
    return compareStrategies(customLoans, extraMonthlyPaymentPaise);
  }, [customLoans, extraMonthlyPaymentPaise]);

  const totalDebtPaise = customLoans.reduce((sum, l) => sum + l.balancePaise, 0);
  const totalMinPaymentPaise = customLoans.reduce((sum, l) => sum + l.minimumPaymentPaise, 0);

  const handleRemoveLoan = (index: number) => {
    setCustomLoans(prev => prev.filter((_, i) => i !== index));
  };

  const [newLoanName, setNewLoanName] = useState('');
  const [newLoanBalance, setNewLoanBalance] = useState('');
  const [newLoanApr, setNewLoanApr] = useState('');
  const [newLoanMin, setNewLoanMin] = useState('');

  const handleAddLoan = (e: React.FormEvent) => {
    e.preventDefault();
    const bal = parseInt(newLoanBalance.replace(/[^0-9]/g, ''), 10) || 0;
    const apr = parseFloat(newLoanApr) || 0;
    const min = parseInt(newLoanMin.replace(/[^0-9]/g, ''), 10) || 0;
    if (newLoanName && bal > 0) {
      setCustomLoans(prev => [
        ...prev,
        {
          name: newLoanName,
          balancePaise: bal * 100,
          rateBps: Math.round(apr * 100),
          minimumPaymentPaise: min * 100,
        },
      ]);
      setNewLoanName('');
      setNewLoanBalance('');
      setNewLoanApr('');
      setNewLoanMin('');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Title */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.45rem', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
            <CreditCard size={22} style={{ color: 'var(--neon-rose)' }} />
            Debt Elimination Strategy Simulator (§5.9)
          </h2>
          <p style={{ fontSize: '0.84rem', color: 'var(--text-tertiary)', margin: '4px 0 0 0' }}>
            Mathematical optimization comparing Snowball vs Avalanche with constant monthly budget roll-forward.
          </p>
        </div>

        <div style={{ padding: '6px 14px', borderRadius: 'var(--radius-pill)', background: 'rgba(244, 63, 94, 0.1)', border: '1px solid rgba(244, 63, 94, 0.25)', fontSize: '0.82rem', fontWeight: 700, color: '#fff' }}>
          Total Debt: ₹{formatIndian(totalDebtPaise / 100)}
        </div>
      </div>

      {/* Comparison Verdict Banner */}
      <div
        style={{
          padding: 20,
          borderRadius: 'var(--radius-lg)',
          background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(6, 182, 212, 0.08) 100%)',
          border: '1px solid var(--neon-emerald)',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 16,
        }}
      >
        <div>
          <div style={{ fontSize: '0.8rem', fontWeight: 800, textTransform: 'uppercase', color: 'var(--neon-emerald)', display: 'flex', alignItems: 'center', gap: 6 }}>
            <Award size={18} />
            Optimization Verdict (Appendix A.4 Reference)
          </div>
          <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 900, color: '#fff', marginTop: 4 }}>
            Avalanche saves ₹{formatIndian(comparison.interestSavedPaise / 100)} in interest
            {comparison.monthsSaved > 0 && ` and frees you ${comparison.monthsSaved} month(s) earlier`}!
          </div>
          <span style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)', marginTop: 2, display: 'block' }}>
            Constant budget of ₹{formatIndian((totalMinPaymentPaise + extraMonthlyPaymentPaise) / 100)}/mo automatically rolls forward as each loan is cleared.
          </span>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <label style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-secondary)' }}>Extra Prepayment / Month:</label>
          <div style={{ display: 'flex', alignItems: 'center', background: 'rgba(0,0,0,0.3)', border: '1px solid rgba(255,255,255,0.15)', borderRadius: 8, padding: '4px 10px' }}>
            <span style={{ color: 'var(--neon-emerald)', fontWeight: 800, marginRight: 4 }}>+₹</span>
            <input
              type="text"
              value={extraPaymentStr}
              onChange={e => setExtraPaymentStr(e.target.value)}
              style={{ width: 80, background: 'transparent', border: 'none', color: '#fff', fontFamily: 'var(--font-mono)', fontWeight: 800, fontSize: '1rem', outline: 'none' }}
            />
          </div>
        </div>
      </div>

      {/* Side-by-Side Strategy Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: 18 }}>
        {/* Snowball Card */}
        <Tilt3DCard>
          <div className="cyber-card" style={{ padding: 22, height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '1rem', fontWeight: 800, color: '#fff', display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Flame size={18} style={{ color: '#fb923c' }} />
                  Debt Snowball
                </span>
                <span style={{ fontSize: '0.72rem', fontWeight: 700, padding: '2px 8px', borderRadius: 4, background: 'rgba(251, 146, 60, 0.15)', color: '#fb923c' }}>
                  Behavioral Momentum
                </span>
              </div>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)', marginTop: 6 }}>
                Target smallest balance first to build quick psychological wins, then roll budget into larger debts.
              </p>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 16, background: 'rgba(255,255,255,0.02)', padding: 14, borderRadius: 8 }}>
                <div>
                  <span style={{ fontSize: '0.74rem', color: 'var(--text-secondary)' }}>Debt-Free In:</span>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.4rem', fontWeight: 900, color: '#fff', marginTop: 2 }}>
                    {comparison.snowball.months} Months
                  </div>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)' }}>
                    ({(comparison.snowball.months / 12).toFixed(1)} years)
                  </span>
                </div>
                <div>
                  <span style={{ fontSize: '0.74rem', color: 'var(--text-secondary)' }}>Total Interest Paid:</span>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.4rem', fontWeight: 900, color: '#fb923c', marginTop: 2 }}>
                    ₹{formatIndian(comparison.snowball.totalInterestPaise / 100)}
                  </div>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)' }}>
                    Accrued interest
                  </span>
                </div>
              </div>
            </div>

            <div style={{ marginTop: 16, fontSize: '0.74rem', color: 'var(--text-tertiary)' }}>
              Best if you need quick visual progress to stay committed.
            </div>
          </div>
        </Tilt3DCard>

        {/* Avalanche Card */}
        <Tilt3DCard>
          <div className="cyber-card" style={{ padding: 22, height: '100%', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', border: '1px solid rgba(16, 185, 129, 0.3)' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '1rem', fontWeight: 800, color: '#fff', display: 'flex', alignItems: 'center', gap: 8 }}>
                  <Zap size={18} style={{ color: 'var(--neon-emerald)' }} />
                  Debt Avalanche
                </span>
                <span style={{ fontSize: '0.72rem', fontWeight: 700, padding: '2px 8px', borderRadius: 4, background: 'rgba(16, 185, 129, 0.15)', color: 'var(--neon-emerald)' }}>
                  Mathematically Optimal
                </span>
              </div>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)', marginTop: 6 }}>
                Target highest APR interest rate first to minimize total wealth leak, then eliminate lower-rate loans.
              </p>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 16, background: 'rgba(255,255,255,0.02)', padding: 14, borderRadius: 8 }}>
                <div>
                  <span style={{ fontSize: '0.74rem', color: 'var(--text-secondary)' }}>Debt-Free In:</span>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.4rem', fontWeight: 900, color: '#fff', marginTop: 2 }}>
                    {comparison.avalanche.months} Months
                  </div>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)' }}>
                    ({(comparison.avalanche.months / 12).toFixed(1)} years)
                  </span>
                </div>
                <div>
                  <span style={{ fontSize: '0.74rem', color: 'var(--text-secondary)' }}>Total Interest Paid:</span>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.4rem', fontWeight: 900, color: 'var(--neon-emerald)', marginTop: 2 }}>
                    ₹{formatIndian(comparison.avalanche.totalInterestPaise / 100)}
                  </div>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)' }}>
                    Saves ₹{formatIndian(comparison.interestSavedPaise / 100)}
                  </span>
                </div>
              </div>
            </div>

            <div style={{ marginTop: 16, fontSize: '0.74rem', color: 'var(--neon-emerald)', fontWeight: 600 }}>
              ★ Recommended: Saves the most money over the life of your loans.
            </div>
          </div>
        </Tilt3DCard>
      </div>

      {/* Loan Management Table */}
      <div style={{ padding: 20, borderRadius: 'var(--radius-lg)', background: 'rgba(17,24,39,0.7)', border: '1px solid var(--glass-border)' }}>
        <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: '#fff', margin: '0 0 14px 0' }}>
          Active Debts in Simulation ({customLoans.length})
        </h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {customLoans.map((l, idx) => (
            <div
              key={idx}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '12px 16px',
                borderRadius: 8,
                background: 'rgba(255,255,255,0.02)',
                border: '1px solid rgba(255,255,255,0.05)',
                flexWrap: 'wrap',
                gap: 8,
              }}
            >
              <div>
                <span style={{ fontWeight: 800, color: '#fff', fontSize: '0.9rem' }}>{l.name}</span>
                <span style={{ marginLeft: 8, fontSize: '0.75rem', fontWeight: 700, color: 'var(--neon-rose)', background: 'rgba(244,63,94,0.1)', padding: '2px 6px', borderRadius: 4 }}>
                  {(l.rateBps / 100).toFixed(1)}% APR
                </span>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                <div>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)' }}>Balance: </span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 800, color: '#fff' }}>₹{formatIndian(l.balancePaise / 100)}</span>
                </div>
                <div>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)' }}>Min Payment: </span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 800, color: 'var(--neon-cyan)' }}>₹{formatIndian(l.minimumPaymentPaise / 100)}</span>
                </div>
                <button
                  type="button"
                  onClick={() => handleRemoveLoan(idx)}
                  style={{ background: 'transparent', border: 'none', color: 'var(--text-tertiary)', cursor: 'pointer', padding: 4 }}
                >
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Add custom loan form */}
        <form onSubmit={handleAddLoan} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr)) 80px', gap: 8, marginTop: 14 }}>
          <input
            type="text"
            placeholder="Loan Name (e.g. Card)"
            value={newLoanName}
            onChange={e => setNewLoanName(e.target.value)}
            style={{ padding: '8px 10px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 6, color: '#fff', fontSize: '0.82rem' }}
          />
          <input
            type="text"
            placeholder="Balance (₹)"
            value={newLoanBalance}
            onChange={e => setNewLoanBalance(e.target.value)}
            style={{ padding: '8px 10px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 6, color: '#fff', fontSize: '0.82rem' }}
          />
          <input
            type="number"
            step="0.1"
            placeholder="APR %"
            value={newLoanApr}
            onChange={e => setNewLoanApr(e.target.value)}
            style={{ padding: '8px 10px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 6, color: '#fff', fontSize: '0.82rem' }}
          />
          <input
            type="text"
            placeholder="Min Payment (₹)"
            value={newLoanMin}
            onChange={e => setNewLoanMin(e.target.value)}
            style={{ padding: '8px 10px', background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 6, color: '#fff', fontSize: '0.82rem' }}
          />
          <button
            type="submit"
            className="btn btn-sm btn-primary"
            style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 4 }}
          >
            <Plus size={14} /> Add
          </button>
        </form>
      </div>
    </div>
  );
}
