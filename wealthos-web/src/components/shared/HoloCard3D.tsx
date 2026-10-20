import { useState, useRef, useEffect } from 'react';
import { formatMoney } from '../../types';
import { Wifi, Eye, EyeOff, RotateCw, ShieldCheck } from 'lucide-react';

interface HoloCard3DProps {
  netWorth: number;
  liquidBalance: number;
  userName: string;
  currency: string;
  masked: boolean;
  onToggleMask: () => void;
}

export function HoloCard3D({
  netWorth,
  liquidBalance,
  userName,
  currency,
  masked,
  onToggleMask,
}: HoloCard3DProps) {
  const [isFlipped, setIsFlipped] = useState(false);
  const cardRef = useRef<HTMLDivElement>(null);
  const [tilt, setTilt] = useState({ rx: 0, ry: 0, glareX: 50, glareY: 50, glareOpacity: 0 });

  const [reduceMotion, setReduceMotion] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches || window.innerWidth < 768;
  });

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const media = window.matchMedia('(prefers-reduced-motion: reduce)');
    const handleMedia = () => setReduceMotion(media.matches || window.innerWidth < 768);
    media.addEventListener('change', handleMedia);
    window.addEventListener('resize', handleMedia);
    return () => {
      media.removeEventListener('change', handleMedia);
      window.removeEventListener('resize', handleMedia);
    };
  }, []);

  const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
    if (reduceMotion || !cardRef.current) return;
    const rect = cardRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const cx = rect.width / 2;
    const cy = rect.height / 2;

    const rx = ((y - cy) / cy) * -8;
    const ry = ((x - cx) / cx) * 10;

    setTilt({
      rx,
      ry,
      glareX: (x / rect.width) * 100,
      glareY: (y / rect.height) * 100,
      glareOpacity: 0.25,
    });
  };

  const handleMouseLeave = () => {
    setTilt({ rx: 0, ry: 0, glareX: 50, glareY: 50, glareOpacity: 0 });
  };

  return (
    <div
      style={{
        perspective: reduceMotion ? 'none' : '1200px',
        width: '100%',
        maxWidth: 440,
        margin: '0 auto',
      }}
    >
      <div
        ref={cardRef}
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
        onClick={() => setIsFlipped(f => !f)}
        style={{
          width: '100%',
          height: 250,
          position: 'relative',
          transformStyle: 'preserve-3d',
          transform: reduceMotion
            ? `rotateY(${isFlipped ? 180 : 0}deg)`
            : `rotateX(${tilt.rx}deg) rotateY(${tilt.ry + (isFlipped ? 180 : 0)}deg) scale3d(1.01, 1.01, 1.01)`,
          transition: reduceMotion ? 'transform 0.3s ease' : 'transform 0.45s cubic-bezier(0.175, 0.885, 0.32, 1.275), box-shadow 0.3s ease',
          cursor: 'pointer',
        }}
      >
        {/* Dynamic ambient card glow - Quiet Wealth Emerald */}
        <div
          style={{
            position: 'absolute',
            inset: -8,
            borderRadius: 24,
            background: 'radial-gradient(circle, rgba(16, 185, 129, 0.18) 0%, rgba(13, 17, 23, 0.1) 50%, transparent 75%)',
            filter: 'blur(16px)',
            opacity: 0.7,
            zIndex: 0,
            transform: 'translateZ(-10px)',
          }}
        />

        {/* FRONT FACE */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            borderRadius: 20,
            background: 'linear-gradient(135deg, #131922 0%, #0d1117 60%, #18202c 100%)',
            border: '1px solid var(--border-default, rgba(255, 255, 255, 0.12))',
            boxShadow: '0 20px 40px -10px rgba(0, 0, 0, 0.7), inset 0 1px 1px rgba(255, 255, 255, 0.12)',
            padding: 24,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            backfaceVisibility: 'hidden',
            overflow: 'hidden',
            zIndex: 2,
          }}
        >
          {/* Subtle Ambient Shimmer Overlay */}
          {!reduceMotion && (
            <div
              style={{
                position: 'absolute',
                inset: 0,
                background: `radial-gradient(circle at ${tilt.glareX}% ${tilt.glareY}%, rgba(255,255,255,${tilt.glareOpacity * 0.7}) 0%, rgba(16, 185, 129, ${tilt.glareOpacity * 0.4}) 30%, transparent 65%)`,
                mixBlendMode: 'overlay',
                pointerEvents: 'none',
                transition: 'opacity 0.15s ease',
              }}
            />
          )}

          {/* Holographic foil diagonal wave */}
          <div
            style={{
              position: 'absolute',
              top: '-50%',
              left: '-50%',
              width: '200%',
              height: '200%',
              background: 'linear-gradient(45deg, transparent 40%, rgba(255, 255, 255, 0.05) 50%, transparent 60%)',
              pointerEvents: 'none',
            }}
          />

          {/* Top row: Brand & Chips */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', zIndex: 5 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: 8,
                  background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  fontWeight: 900,
                  fontSize: '1rem',
                  boxShadow: '0 2px 8px rgba(16, 185, 129, 0.3)',
                }}
              >
                W
              </div>
              <div>
                <div style={{ fontSize: '0.88rem', fontWeight: 800, letterSpacing: '0.08em', color: '#f8fafc', textTransform: 'uppercase' }}>
                  WealthOS <span style={{ color: '#10b981', fontSize: '0.72rem', fontWeight: 700 }}>VAULT</span>
                </div>
                <div style={{ fontSize: '0.68rem', color: '#94a3b8', letterSpacing: '0.05em' }}>PRIVATE VAULT</div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <Wifi size={20} style={{ color: 'rgba(255,255,255,0.7)', transform: 'rotate(90deg)' }} />
              {/* EMV Gold Chip */}
              <div
                style={{
                  width: 36,
                  height: 28,
                  borderRadius: 6,
                  background: 'linear-gradient(135deg, #e0aa3e 0%, #f7d070 50%, #b8860b 100%)',
                  border: '1px solid rgba(255,255,255,0.4)',
                  boxShadow: '0 2px 4px rgba(0,0,0,0.3)',
                  position: 'relative',
                  overflow: 'hidden',
                }}
              >
                <div style={{ position: 'absolute', top: 9, left: 0, right: 0, height: 1, background: 'rgba(0,0,0,0.3)' }} />
                <div style={{ position: 'absolute', top: 18, left: 0, right: 0, height: 1, background: 'rgba(0,0,0,0.3)' }} />
                <div style={{ position: 'absolute', top: 0, bottom: 0, left: 18, width: 1, background: 'rgba(0,0,0,0.3)' }} />
              </div>
            </div>
          </div>

          {/* Middle: Net Worth Figure */}
          <div style={{ zIndex: 5 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
              <span style={{ fontSize: '0.72rem', letterSpacing: '0.1em', color: 'rgba(255,255,255,0.6)', textTransform: 'uppercase' }}>
                TOTAL NET WORTH
              </span>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  onToggleMask();
                }}
                style={{
                  background: 'rgba(255,255,255,0.1)',
                  border: 'none',
                  borderRadius: 6,
                  padding: '3px 8px',
                  color: 'white',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 4,
                  fontSize: '0.7rem',
                }}
              >
                {masked ? <EyeOff size={12} /> : <Eye size={12} />}
                {masked ? 'Reveal' : 'Mask'}
              </button>
            </div>
            <div
              style={{
                fontSize: '1.9rem',
                fontWeight: 900,
                fontFamily: 'var(--font-outfit), sans-serif',
                letterSpacing: '-0.02em',
                background: 'linear-gradient(135deg, #ffffff 40%, #e2e8f0 100%)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
              }}
            >
              {formatMoney(netWorth, currency, masked)}
            </div>
          </div>

          {/* Bottom row: Cardholder & Liquid Cash */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', zIndex: 5 }}>
            <div>
              <div style={{ fontSize: '0.65rem', color: 'rgba(255,255,255,0.5)', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                CARDHOLDER
              </div>
              <div style={{ fontSize: '0.85rem', fontWeight: 700, color: '#f1f5f9', letterSpacing: '0.04em' }}>
                {userName.toUpperCase()}
              </div>
            </div>

            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '0.65rem', color: 'rgba(255,255,255,0.5)', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
                CASH & BANK BALANCE
              </div>
              <div style={{ fontSize: '0.9rem', fontWeight: 800, color: '#10b981', fontFamily: 'var(--font-mono)' }}>
                {formatMoney(liquidBalance, currency, masked)}
              </div>
            </div>
          </div>

          {/* Flip Hint */}
          <div
            style={{
              position: 'absolute',
              bottom: 8,
              left: '50%',
              transform: 'translateX(-50%)',
              fontSize: '0.62rem',
              color: 'rgba(255,255,255,0.4)',
              display: 'flex',
              alignItems: 'center',
              gap: 4,
              pointerEvents: 'none',
            }}
          >
            <RotateCw size={10} /> Click to flip security back
          </div>
        </div>

        {/* BACK FACE */}
        <div
          style={{
            position: 'absolute',
            inset: 0,
            borderRadius: 20,
            background: 'linear-gradient(135deg, #0a0d16 0%, #151928 100%)',
            border: '1px solid rgba(255, 255, 255, 0.12)',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.75)',
            transform: 'rotateY(180deg)',
            backfaceVisibility: 'hidden',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            padding: 20,
            overflow: 'hidden',
            zIndex: 2,
          }}
        >
          {/* Black Magnetic Stripe */}
          <div
            style={{
              margin: '-20px -20px 0 -20px',
              height: 44,
              background: '#04060a',
              borderBottom: '1px solid rgba(255,255,255,0.05)',
            }}
          />

          {/* Signature and CVV bar */}
          <div style={{ marginTop: 12 }}>
            <div style={{ fontSize: '0.65rem', color: '#94a3b8', marginBottom: 4, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Authorized Security Token
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div
                style={{
                  flex: 1,
                  height: 32,
                  background: 'repeating-linear-gradient(45deg, #ffffff, #ffffff 10px, #f1f5f9 10px, #f1f5f9 20px)',
                  borderRadius: 4,
                  display: 'flex',
                  alignItems: 'center',
                  paddingLeft: 12,
                  color: '#334155',
                  fontStyle: 'italic',
                  fontFamily: 'serif',
                  fontWeight: 700,
                  fontSize: '0.85rem',
                }}
              >
                {userName}
              </div>
              <div
                style={{
                  width: 50,
                  height: 32,
                  background: '#1e293b',
                  borderRadius: 4,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#f8fafc',
                  fontFamily: 'var(--font-mono)',
                  fontWeight: 800,
                  letterSpacing: '0.1em',
                  border: '1px solid rgba(255,255,255,0.1)',
                }}
              >
                {masked ? '•••' : '998'}
              </div>
            </div>
          </div>

          {/* Security details */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginTop: 10 }}>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#10b981', fontSize: '0.72rem', fontWeight: 600 }}>
                <ShieldCheck size={14} /> 100% Private & Encrypted
              </div>
              <div style={{ fontSize: '0.65rem', color: '#64748b', marginTop: 2 }}>
                Only you can access your financial data
              </div>
            </div>
            <div style={{ color: '#64748b', fontSize: '0.68rem', fontWeight: 600 }}>
              Personal & Safe
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
