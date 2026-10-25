import { useState, useEffect } from 'react';
import { useData } from '../../hooks/useData';
import { formatMoney } from '../../types';
import {
  Clock,
  Shield,
  Lightbulb,
  Plus,
  Trash2,
  CheckCircle2,
  Lock,
  ChevronRight,
  Sparkles,
} from 'lucide-react';

interface CooldownItem {
  id: string;
  name: string;
  amountMinor: number;
  addedAt: number; // timestamp
}

const WISDOM_CARDS = [
  {
    quote: 'Savings = Income minus Ego.',
    author: 'Morgan Housel (Psychology of Money / Video 5)',
    lesson: 'Building wealth has more to do with your savings rate than your investment returns.',
  },
  {
    quote: 'Rich is what you see. Wealth is what you don’t see.',
    author: 'Video 4 (Quiet Wealth)',
    lesson: 'Spending money to show people how much money you have is the fastest way to have less money.',
  },
  {
    quote: 'Always build room for error and a margin of safety.',
    author: 'Video 6',
    lesson: 'A plan is only useful if it can survive reality. Your emergency fund buys your independence.',
  },
  {
    quote: 'Define what is "Enough" for you.',
    author: 'Video 15 (Three Boxes & Peace of Mind)',
    lesson: 'Without a finish line, lifestyle creep will consume every salary hike you ever earn.',
  },
];

export function BehaviouralGuardrailsCard() {
  const { financialProfile, settings } = useData();
  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;

  const [wisdomIndex, setWisdomIndex] = useState(0);

  // 48-Hour Cooldown Wishlist state (persisted locally)
  const [cooldownItems, setCooldownItems] = useState<CooldownItem[]>(() => {
    try {
      const stored = localStorage.getItem('wealthos_cooldown_wishlist');
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  });

  const [newItemName, setNewItemName] = useState('');
  const [newItemAmount, setNewItemAmount] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);

  useEffect(() => {
    try {
      localStorage.setItem('wealthos_cooldown_wishlist', JSON.stringify(cooldownItems));
    } catch (e) {
      console.error(e);
    }
  }, [cooldownItems]);

  const handleAddItem = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newItemName.trim() || !newItemAmount) return;
    const amountMinor = Math.round(parseFloat(newItemAmount) * 100);
    const item: CooldownItem = {
      id: `cd_${Date.now()}`,
      name: newItemName.trim(),
      amountMinor,
      addedAt: Date.now(),
    };
    setCooldownItems(prev => [item, ...prev]);
    setNewItemName('');
    setNewItemAmount('');
    setShowAddForm(false);
  };

  const handleRemoveItem = (id: string) => {
    setCooldownItems(prev => prev.filter(i => i.id !== id));
  };

  const rotateWisdom = () => {
    setWisdomIndex(prev => (prev + 1) % WISDOM_CARDS.length);
  };

  const currentWisdom = WISDOM_CARDS[wisdomIndex];
  const freezeMonths = (financialProfile as any)?.lifestyle_freeze_months || 24;

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 16 }}>
      {/* 48-Hour Cooldown Wishlist (§5.14, Video 5) */}
      <div className="cyber-card" style={{ padding: 18 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Clock size={18} style={{ color: 'var(--neon-cyan)' }} />
            <h3 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1rem', fontWeight: 800, margin: 0 }}>
              48-Hour Cooldown Wishlist
            </h3>
          </div>
          <button
            type="button"
            className="btn btn-ghost btn-xs"
            onClick={() => setShowAddForm(!showAddForm)}
            style={{ color: 'var(--neon-cyan)', border: '1px solid rgba(0, 240, 255, 0.25)' }}
          >
            <Plus size={12} /> {showAddForm ? 'Cancel' : 'Add Item'}
          </button>
        </div>

        <p style={{ fontSize: '0.76rem', color: 'var(--text-tertiary)', margin: '0 0 12px 0' }}>
          Avoid flash sales and impulse purchases. Wait 48 hours before buying any non-essential item.
        </p>

        {showAddForm && (
          <form onSubmit={handleAddItem} style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
            <input
              placeholder="Item name (e.g. Shoes, Gadget)"
              value={newItemName}
              onChange={e => setNewItemName(e.target.value)}
              style={{ flex: 1, fontSize: '0.78rem', padding: '6px 10px' }}
              required
            />
            <input
              type="number"
              placeholder="₹ Amount"
              value={newItemAmount}
              onChange={e => setNewItemAmount(e.target.value)}
              style={{ width: 100, fontSize: '0.78rem', padding: '6px 10px' }}
              required
            />
            <button type="submit" className="btn btn-primary btn-sm" style={{ padding: '4px 10px' }}>
              Add
            </button>
          </form>
        )}

        {cooldownItems.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '18px 10px', color: 'var(--text-tertiary)', fontSize: '0.78rem' }}>
            No active impulse cooldowns. Use this before checking out on Amazon or Myntra!
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: 180, overflowY: 'auto' }}>
            {cooldownItems.map(item => {
              const elapsedHours = (Date.now() - item.addedAt) / (1000 * 60 * 60);
              const remainingHours = Math.max(0, Math.ceil(48 - elapsedHours));
              const isReady = remainingHours === 0;

              return (
                <div
                  key={item.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '8px 12px',
                    borderRadius: 'var(--radius-md)',
                    background: isReady ? 'rgba(16, 185, 129, 0.08)' : 'rgba(255, 255, 255, 0.03)',
                    border: isReady ? '1px solid var(--neon-emerald)' : '1px solid var(--glass-border)',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.82rem', color: '#f1f5f9' }}>
                      {item.name}
                    </div>
                    <div style={{ fontSize: '0.7rem', color: isReady ? 'var(--neon-emerald)' : 'var(--neon-amber)' }}>
                      {isReady ? '✓ 48 Hours Passed — Intentional Choice' : `⏳ ${remainingHours}h cooldown left`}
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span style={{ fontFamily: 'var(--font-outfit)', fontWeight: 800, fontSize: '0.88rem' }}>
                      {formatMoney(item.amountMinor, currency, masked)}
                    </span>
                    <button
                      type="button"
                      className="btn-ghost"
                      onClick={() => handleRemoveItem(item.id)}
                      style={{ padding: 4, color: 'var(--text-tertiary)' }}
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Rotating Wisdom Card (§5.14, Videos 4, 5, 15) */}
      <div
        className="cyber-card"
        style={{
          padding: 18,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          background: 'linear-gradient(135deg, rgba(6, 182, 212, 0.06), rgba(99, 102, 241, 0.06))',
          border: '1px solid var(--glass-border)',
        }}
      >
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--neon-cyan)', fontSize: '0.78rem', fontWeight: 700 }}>
              <Lightbulb size={16} /> Quiet Wealth Wisdom
            </div>
            <button
              type="button"
              className="btn-ghost"
              onClick={rotateWisdom}
              style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', padding: '2px 6px' }}
            >
              Next Wisdom <ChevronRight size={12} style={{ display: 'inline' }} />
            </button>
          </div>

          <blockquote style={{ margin: '8px 0', fontSize: '0.92rem', fontWeight: 800, color: '#f1f5f9', fontStyle: 'italic', lineHeight: 1.4 }}>
            "{currentWisdom.quote}"
          </blockquote>
          <div style={{ fontSize: '0.74rem', color: 'var(--neon-cyan)', fontWeight: 600 }}>
            — {currentWisdom.author}
          </div>
          <p style={{ fontSize: '0.76rem', color: 'var(--text-secondary)', marginTop: 8, lineHeight: 1.4 }}>
            {currentWisdom.lesson}
          </p>
        </div>

        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: '8px 10px',
            borderRadius: 'var(--radius-sm)',
            background: 'rgba(255, 255, 255, 0.03)',
            marginTop: 12,
            fontSize: '0.72rem',
            color: 'var(--text-tertiary)',
          }}
        >
          <Lock size={12} style={{ color: 'var(--neon-emerald)' }} />
          <span>Lifestyle Freeze Target: <strong>{freezeMonths} Months</strong> active</span>
        </div>
      </div>
    </div>
  );
}
