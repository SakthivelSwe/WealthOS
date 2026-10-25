import { useState, useMemo } from 'react';
import { useData } from '../../hooks/useData';
import { formatMoney, type Transaction } from '../../types';
import { X, ShieldAlert, HeartCrack, Sparkles, Check, Coffee, ShoppingBag, ArrowRight } from 'lucide-react';

interface JoylessLeakModalProps {
  onClose: () => void;
}

export function JoylessLeakModal({ onClose }: JoylessLeakModalProps) {
  const { transactions, updateTransaction, salaryCycles, settings } = useData();
  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;

  const activeCycle = salaryCycles[0] || null;
  const cycleStartDate = activeCycle?.cycle_start_date || '2026-10-01';

  // Identify habit candidates in this cycle:
  // 1. Small scans <= ₹250
  // 2. Food delivery / dining / snacks
  // 3. E-commerce / quick commerce
  // 4. Any item already tagged as REGRET_LEAK
  const habitCandidates = useMemo(() => {
    return transactions.filter(t => {
      if (t.type !== 'EXPENSE') return false;
      if (t.local_date < cycleStartDate) return false;

      const tag = (t as any).spend_emotion_tag || '';
      if (tag === 'REGRET_LEAK') return true;

      const desc = `${t.merchant || ''} ${t.description || ''}`.toLowerCase();
      const isSmallScan = t.amount_minor <= 25000; // <= ₹250
      const isFoodQuickComm = /\b(swiggy|zomato|chai|tea|coffee|bakery|cafe|snacks|blinkit|zepto|instamart|starbucks)\b/i.test(desc);
      const isImpulseShop = /\b(amazon|flipkart|myntra|meesho|nykaa|zara)\b/i.test(desc);

      return isSmallScan || isFoodQuickComm || isImpulseShop;
    });
  }, [transactions, cycleStartDate]);

  // Tagging handler
  const handleTagTransaction = async (txn: Transaction, newTag: 'REGRET_LEAK' | 'PLANNED_WANT' | 'NEED') => {
    const updated = {
      ...txn,
      spend_emotion_tag: newTag,
      need_want: newTag === 'NEED' ? 'NEED' : 'WANT',
      user_overridden: true,
      updated_at: Date.now(),
    };
    await updateTransaction(txn.id, updated as any);
  };

  // Metrics
  const totalRegretPaise = useMemo(() => {
    return habitCandidates
      .filter(t => (t as any).spend_emotion_tag === 'REGRET_LEAK')
      .reduce((sum, t) => sum + t.amount_minor, 0);
  }, [habitCandidates]);

  const totalCandidatesPaise = useMemo(() => {
    return habitCandidates.reduce((sum, t) => sum + t.amount_minor, 0);
  }, [habitCandidates]);

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 1250 }}>
      <div
        className="modal-content"
        onClick={e => e.stopPropagation()}
        style={{ maxWidth: 760, maxHeight: '90vh', overflowY: 'auto' }}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: 8,
                background: 'rgba(239, 68, 68, 0.15)',
                color: 'var(--neon-rose)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <HeartCrack size={20} />
            </div>
            <div>
              <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, margin: 0 }}>
                Joyless Leak Review
              </h2>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', margin: '2px 0 0 0' }}>
                Psychology of Money (Video 12): Only you can judge regret. The app never auto-tags waste.
              </p>
            </div>
          </div>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        {/* Tally Card */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: 12,
            padding: 16,
            borderRadius: 'var(--radius-md)',
            background: 'linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(0, 0, 0, 0.3))',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            marginBottom: 20,
          }}
        >
          <div>
            <div style={{ fontSize: '0.74rem', color: 'var(--neon-rose)', fontWeight: 700, textTransform: 'uppercase' }}>
              Identified Joyless Leaks
            </div>
            <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.5rem', fontWeight: 800, color: 'var(--neon-rose)', marginTop: 4 }}>
              {formatMoney(totalRegretPaise, currency, masked)}
            </div>
            <div style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
              Money spent that brought no real joy or lasting utility
            </div>
          </div>

          <div>
            <div style={{ fontSize: '0.74rem', color: 'var(--neon-cyan)', fontWeight: 700, textTransform: 'uppercase' }}>
              Habit Review Pool
            </div>
            <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.5rem', fontWeight: 800, color: '#f1f5f9', marginTop: 4 }}>
              {habitCandidates.length} Items
            </div>
            <div style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
              Totalling {formatMoney(totalCandidatesPaise, currency, masked)} in candidate habit spend
            </div>
          </div>
        </div>

        {/* Candidate List */}
        <div style={{ marginBottom: 20 }}>
          <div style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: 10 }}>
            Review Candidates (Tap to classify):
          </div>

          {habitCandidates.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '30px 16px', color: 'var(--text-tertiary)', fontSize: '0.85rem' }}>
              🎉 No habit leak candidates found in this cycle! Your discretionary pacing is clean.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 380, overflowY: 'auto' }}>
              {habitCandidates.map(txn => {
                const tag = (txn as any).spend_emotion_tag || '';
                const isRegret = tag === 'REGRET_LEAK';
                const isWant = tag === 'PLANNED_WANT';
                const isNeed = tag === 'NEED';

                return (
                  <div
                    key={txn.id}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '10px 14px',
                      borderRadius: 'var(--radius-md)',
                      background: isRegret
                        ? 'rgba(239, 68, 68, 0.12)'
                        : 'rgba(255, 255, 255, 0.03)',
                      border: isRegret
                        ? '1px solid rgba(239, 68, 68, 0.4)'
                        : '1px solid var(--glass-border)',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.88rem', color: '#f1f5f9' }}>
                        {txn.merchant || txn.description || 'Spend'}
                      </div>
                      <div style={{ fontSize: '0.74rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                        {txn.local_date} · {txn.payment_method || 'UPI'}
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                      <div style={{ fontFamily: 'var(--font-outfit)', fontWeight: 800, fontSize: '1rem', color: isRegret ? 'var(--neon-rose)' : '#f1f5f9' }}>
                        -{formatMoney(txn.amount_minor, currency, masked)}
                      </div>

                      {/* Tag Actions */}
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button
                          type="button"
                          className="btn btn-xs"
                          onClick={() => handleTagTransaction(txn, 'REGRET_LEAK')}
                          style={{
                            background: isRegret ? 'var(--neon-rose)' : 'rgba(255, 255, 255, 0.05)',
                            color: isRegret ? '#fff' : 'var(--text-secondary)',
                            border: isRegret ? 'none' : '1px solid var(--border-subtle)',
                            fontWeight: 700,
                            padding: '4px 8px',
                          }}
                        >
                          💔 Joyless
                        </button>
                        <button
                          type="button"
                          className="btn btn-xs"
                          onClick={() => handleTagTransaction(txn, 'PLANNED_WANT')}
                          style={{
                            background: isWant ? 'var(--neon-cyan)' : 'rgba(255, 255, 255, 0.05)',
                            color: isWant ? '#000' : 'var(--text-secondary)',
                            border: isWant ? 'none' : '1px solid var(--border-subtle)',
                            fontWeight: 700,
                            padding: '4px 8px',
                          }}
                        >
                          ✨ Worth It
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Footer */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, borderTop: '1px solid var(--glass-border)', paddingTop: 14 }}>
          <button type="button" className="btn btn-primary btn-md" onClick={onClose} style={{ background: 'var(--neon-emerald)', color: '#000' }}>
            Done Reviewing
          </button>
        </div>
      </div>
    </div>
  );
}
