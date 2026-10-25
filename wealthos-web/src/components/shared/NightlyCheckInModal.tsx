import { useState, useMemo } from 'react';
import { useData } from '../../hooks/useData';
import { formatMoney } from '../../types';
import {
  X,
  Moon,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  Flame,
} from 'lucide-react';
import { format } from 'date-fns';

interface NightlyCheckInModalProps {
  onClose: () => void;
}

export function NightlyCheckInModal({ onClose }: { onClose: () => void }) {
  const { transactions, dailyUpiMeter, settings, updateTransaction } = useData();
  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;

  const todayStr = useMemo(() => format(new Date(), 'yyyy-MM-dd'), []);
  const todayFormatted = useMemo(() => format(new Date(), 'EEEE, d MMMM yyyy'), []);

  // Today's transactions
  const todayTxns = useMemo(() =>
    transactions.filter(t => !t.deleted_at && t.local_date === todayStr),
    [transactions, todayStr]
  );

  const todaySpentPaise = useMemo(() =>
    todayTxns.filter(t => t.type === 'EXPENSE').reduce((s, t) => s + t.amount_minor, 0),
    [todayTxns]
  );

  const dailyAllowancePaise = dailyUpiMeter.totalPaise > 0 ? dailyUpiMeter.totalPaise : 115000; // default ₹1,150
  const isOverAllowance = todaySpentPaise > dailyAllowancePaise;
  const burnPct = dailyAllowancePaise > 0 ? Math.round((todaySpentPaise / dailyAllowancePaise) * 100) : 0;

  // Untagged transactions
  const untaggedTxns = useMemo(() =>
    todayTxns.filter(t => t.type === 'EXPENSE' && !(t as any).spend_emotion_tag),
    [todayTxns]
  );

  // Cooldown wishlist from localStorage
  const cooldownItems = useMemo(() => {
    try {
      const stored = localStorage.getItem('wealthos_cooldown_wishlist');
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  }, []);

  const [taggedMap, setTaggedMap] = useState<Record<string, string>>({});
  const [completed, setCompleted] = useState(false);

  const handleTagTransaction = async (txnId: string, tag: 'NEED' | 'PLANNED_WANT' | 'REGRET_LEAK' | 'UNPLANNED_TRANSFER') => {
    setTaggedMap(prev => ({ ...prev, [txnId]: tag }));
    await updateTransaction(txnId, {
      spend_emotion_tag: tag,
      need_want: tag === 'NEED' ? 'NEED' : 'WANT',
    } as any);
  };

  const handleFinishCheckin = () => {
    try {
      localStorage.setItem('wealthos_last_nightly_checkin', todayStr);
    } catch (e) {
      console.warn(e);
    }
    setCompleted(true);
    setTimeout(() => {
      onClose();
    }, 1200);
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-content"
        onClick={e => e.stopPropagation()}
        style={{
          maxWidth: 620,
          background: 'rgba(13, 19, 34, 0.95)',
          backdropFilter: 'blur(24px)',
          border: '1px solid rgba(168, 85, 247, 0.3)',
          boxShadow: '0 25px 60px -15px var(--neon-purple-glow)',
        }}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: '50%',
                background: 'linear-gradient(135deg, var(--neon-purple), var(--neon-indigo))',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
                boxShadow: '0 0 16px var(--neon-purple-glow)',
              }}
            >
              <Moon size={18} />
            </div>
            <div>
              <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, margin: 0, color: '#f8fafc' }}>
                Nightly Financial Check-in
              </h2>
              <div style={{ fontSize: '0.78rem', color: 'var(--neon-purple)', fontWeight: 600 }}>
                9:30 PM Protocol · {todayFormatted}
              </div>
            </div>
          </div>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        {completed ? (
          <div style={{ textAlign: 'center', padding: '40px 20px' }}>
            <div
              style={{
                width: 64,
                height: 64,
                borderRadius: '50%',
                background: 'var(--neon-emerald-glow)',
                border: '2px solid var(--neon-emerald)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                margin: '0 auto 16px auto',
                color: 'var(--neon-emerald)',
              }}
            >
              <CheckCircle2 size={36} />
            </div>
            <h3 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.4rem', fontWeight: 800, color: '#f8fafc', marginBottom: 8 }}>
              Nightly Check-in Complete!
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', margin: 0 }}>
              Your financial ledger and daily balance are clean. Sleep with total peace of mind.
            </p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            {/* 1. Daily Allowance Gauge */}
            <div
              style={{
                padding: '16px 18px',
                borderRadius: 'var(--radius-md)',
                background: isOverAllowance ? 'var(--neon-rose-glow)' : 'rgba(255, 255, 255, 0.03)',
                border: isOverAllowance ? '1px solid rgba(244, 63, 94, 0.4)' : '1px solid var(--glass-border)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <div>
                <div style={{ fontSize: '0.76rem', color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.06em', fontWeight: 700 }}>
                  Today's Spend vs Safe Allowance
                </div>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginTop: 4 }}>
                  <span style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.4rem', fontWeight: 900, color: isOverAllowance ? 'var(--neon-rose)' : 'var(--neon-emerald)' }}>
                    {formatMoney(todaySpentPaise, currency, masked)}
                  </span>
                  <span style={{ fontSize: '0.82rem', color: 'var(--text-tertiary)' }}>
                    / {formatMoney(dailyAllowancePaise, currency, masked)} allowance
                  </span>
                </div>
                <div style={{ fontSize: '0.74rem', color: isOverAllowance ? 'var(--neon-rose)' : 'var(--text-secondary)', marginTop: 2 }}>
                  {isOverAllowance
                    ? `⚠️ Over allowance by ${formatMoney(todaySpentPaise - dailyAllowancePaise, currency, masked)} (${burnPct}% burn)`
                    : `✓ Remaining buffer: ${formatMoney(dailyAllowancePaise - todaySpentPaise, currency, masked)} (${burnPct}% used)`}
                </div>
              </div>

              <div
                style={{
                  padding: '8px 14px',
                  borderRadius: 'var(--radius-pill)',
                  background: isOverAllowance ? 'rgba(244, 63, 94, 0.2)' : 'rgba(16, 185, 129, 0.2)',
                  color: isOverAllowance ? 'var(--neon-rose)' : 'var(--neon-emerald)',
                  fontWeight: 800,
                  fontSize: '0.84rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                }}
              >
                {isOverAllowance ? <Flame size={14} /> : <ShieldCheck size={14} />}
                {burnPct}% Pace
              </div>
            </div>

            {/* 2. Untagged Spends Review */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                <span style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  Today's Spends to Review ({untaggedTxns.length})
                </span>
                <span style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)' }}>
                  Assign Need / Planned Want / Regret
                </span>
              </div>

              {untaggedTxns.length === 0 ? (
                <div style={{ padding: '16px', background: 'rgba(255, 255, 255, 0.02)', borderRadius: 'var(--radius-md)', border: '1px dashed var(--glass-border)', textAlign: 'center', fontSize: '0.82rem', color: 'var(--text-tertiary)' }}>
                  ✓ All today's expenses are already tagged and classified.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 180, overflowY: 'auto' }}>
                  {untaggedTxns.map(t => {
                    const currentTag = taggedMap[t.id];
                    return (
                      <div
                        key={t.id}
                        style={{
                          padding: '10px 14px',
                          background: 'rgba(255, 255, 255, 0.03)',
                          border: '1px solid var(--glass-border)',
                          borderRadius: 'var(--radius-md)',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          gap: 12,
                        }}
                      >
                        <div style={{ overflow: 'hidden' }}>
                          <div style={{ fontWeight: 700, fontSize: '0.85rem', color: '#f8fafc', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                            {t.merchant || t.description || 'Spend Entry'}
                          </div>
                          <div style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                            {formatMoney(t.amount_minor, currency, masked)}
                          </div>
                        </div>

                        {/* Quick Tag Chips */}
                        <div style={{ display: 'flex', gap: 4, flexShrink: 0 }}>
                          {[
                            { id: 'NEED', label: 'Need', color: 'var(--neon-emerald)' },
                            { id: 'PLANNED_WANT', label: 'Want', color: 'var(--neon-cyan)' },
                            { id: 'REGRET_LEAK', label: 'Regret', color: 'var(--neon-rose)' },
                          ].map(chip => (
                            <button
                              key={chip.id}
                              type="button"
                              onClick={() => handleTagTransaction(t.id, chip.id as any)}
                              style={{
                                padding: '3px 8px',
                                borderRadius: 'var(--radius-pill)',
                                fontSize: '0.7rem',
                                fontWeight: 700,
                                background: currentTag === chip.id ? chip.color : 'rgba(255, 255, 255, 0.05)',
                                color: currentTag === chip.id ? '#000' : 'var(--text-secondary)',
                                border: `1px solid ${chip.color}55`,
                                cursor: 'pointer',
                              }}
                            >
                              {chip.label}
                            </button>
                          ))}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* 3. 48-Hour Wishlist Cooldowns Audit */}
            <div>
              <div style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
                <Clock size={13} style={{ color: 'var(--neon-cyan)' }} /> 48-Hour Impulse Wishlist Check
              </div>

              {cooldownItems.length === 0 ? (
                <div style={{ fontSize: '0.76rem', color: 'var(--text-tertiary)', padding: '10px 12px', background: 'rgba(255, 255, 255, 0.02)', borderRadius: 'var(--radius-sm)' }}>
                  No active impulse cooldowns. You are in full control!
                </div>
              ) : (
                <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 4 }}>
                  {cooldownItems.slice(0, 3).map((item: any) => {
                    const elapsedHours = (Date.now() - item.addedAt) / (1000 * 60 * 60);
                    const remainingHours = Math.max(0, Math.ceil(48 - elapsedHours));
                    const isReady = remainingHours === 0;

                    return (
                      <div
                        key={item.id}
                        style={{
                          padding: '8px 12px',
                          borderRadius: 'var(--radius-sm)',
                          background: isReady ? 'rgba(16, 185, 129, 0.1)' : 'rgba(255, 255, 255, 0.03)',
                          border: isReady ? '1px solid var(--neon-emerald)' : '1px solid var(--glass-border)',
                          minWidth: 160,
                        }}
                      >
                        <div style={{ fontWeight: 700, fontSize: '0.8rem', color: '#f8fafc', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                          {item.name}
                        </div>
                        <div style={{ fontSize: '0.72rem', color: isReady ? 'var(--neon-emerald)' : 'var(--neon-amber)', marginTop: 2 }}>
                          {isReady ? '✓ Ready to buy' : `⏳ ${remainingHours}h cooldown left`}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Submit Action */}
            <div style={{ display: 'flex', gap: 10, marginTop: 8 }}>
              <button type="button" className="btn btn-secondary" onClick={onClose} style={{ flex: 1 }}>
                Close
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleFinishCheckin}
                style={{ flex: 2, background: 'linear-gradient(135deg, var(--neon-purple), var(--neon-indigo))' }}
              >
                <CheckCircle2 size={16} /> Complete Nightly Check-in
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
