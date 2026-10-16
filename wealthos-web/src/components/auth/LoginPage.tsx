import { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { Mail, Shield, Loader2, Sparkles, KeyRound } from 'lucide-react';

export function LoginPage() {
  const { signInWithOtp, verifyOtp, loginAsDemo } = useAuth();
  const [step, setStep] = useState<'email' | 'otp'>('email');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [error, setError] = useState('');
  const [infoMsg, setInfoMsg] = useState('');
  const [loading, setLoading] = useState(false);
  const otpRefs = useRef<(HTMLInputElement | null)[]>([]);

  const handleSendOtp = async () => {
    setError('');
    setInfoMsg('');

    const cleanEmail = email.trim();
    if (!cleanEmail || !cleanEmail.includes('@')) {
      setError('Please enter a valid email address');
      return;
    }
    setLoading(true);
    const result = await signInWithOtp(cleanEmail, true);
    setLoading(false);
    if (result.error) {
      setError(result.error.message);
      return;
    }
    setInfoMsg(`✅ Verification code dispatched to ${cleanEmail}. Check your inbox or spam folder.`);
    setStep('otp');
  };

  const handleVerifyOtp = async () => {
    const token = otp.join('');
    if (token.length !== 6) {
      setError('Please enter the 6-digit code');
      return;
    }
    setError('');
    setLoading(true);
    const { error } = await verifyOtp(email.trim(), token, true);
    setLoading(false);
    if (error) {
      setError(error.message);
    }
  };

  const handleOtpChange = (index: number, value: string) => {
    const digits = value.replace(/\D/g, '');
    if (digits.length > 1) {
      const newOtp = [...otp];
      for (let i = 0; i < otp.length && i < digits.length; i++) {
        newOtp[i] = digits[i];
      }
      setOtp(newOtp);
      const nextFocus = Math.min(digits.length, otp.length - 1);
      otpRefs.current[nextFocus]?.focus();
      return;
    }
    if (value && !/^\d$/.test(value)) return;
    const newOtp = [...otp];
    newOtp[index] = value;
    setOtp(newOtp);
    if (value && index < otp.length - 1) otpRefs.current[index + 1]?.focus();
  };

  const handleOtpKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      otpRefs.current[index - 1]?.focus();
    }
    if (e.key === 'Enter' && otp.every(d => d)) handleVerifyOtp();
  };

  useEffect(() => {
    if (step === 'otp') otpRefs.current[0]?.focus();
  }, [step]);

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20,
        position: 'relative',
        zIndex: 10,
      }}
    >
      {/* 3D Glass Portal Card */}
      <div
        className="animate-pop"
        style={{
          width: '100%',
          maxWidth: 440,
          background: 'rgba(13, 19, 34, 0.88)',
          backdropFilter: 'blur(36px) saturate(200%)',
          WebkitBackdropFilter: 'blur(36px) saturate(200%)',
          border: '1px solid var(--glass-border-bright)',
          borderRadius: 'var(--radius-xl)',
          padding: '40px 32px',
          boxShadow: '0 30px 70px -15px rgba(0, 0, 0, 0.85), 0 0 35px var(--neon-indigo-glow)',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        {/* Ambient Top Light Beam */}
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: '20%',
            right: '20%',
            height: 2,
            background: 'linear-gradient(90deg, transparent, var(--neon-cyan), transparent)',
            boxShadow: '0 0 12px var(--neon-cyan)',
          }}
        />

        {/* Brand Orb & Heading */}
        <div style={{ textAlign: 'center', marginBottom: 28 }}>
          <div
            style={{
              width: 58,
              height: 58,
              borderRadius: 18,
              background: 'linear-gradient(135deg, var(--neon-cyan) 0%, var(--neon-indigo) 100%)',
              margin: '0 auto 16px auto',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 0 25px var(--neon-cyan-glow)',
              position: 'relative',
            }}
          >
            <span style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.8rem', fontWeight: 900, color: 'white' }}>
              W
            </span>
          </div>

          <h1
            style={{
              fontFamily: 'var(--font-outfit)',
              fontSize: '1.75rem',
              fontWeight: 800,
              letterSpacing: '-0.02em',
              background: 'linear-gradient(135deg, #ffffff 40%, #a5f3fc 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            WealthOS
          </h1>
          <p style={{ fontSize: '0.82rem', color: 'var(--text-tertiary)', marginTop: 4 }}>
            Private Money Manager • Realtime Cloud Sync
          </p>
        </div>

        {step === 'email' ? (
          <>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', textAlign: 'center', marginBottom: 22, lineHeight: 1.6 }}>
              Enter your email address to authenticate. A 100% free one-time verification code will be dispatched to your inbox.
            </p>

            <div className="form-group" style={{ marginBottom: 20 }}>
              <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <Mail size={14} style={{ color: 'var(--neon-cyan)' }} /> Email Address
              </label>
              <input
                type="email"
                placeholder="your.email@example.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleSendOtp()}
                autoFocus
                style={{
                  width: '100%',
                  fontFamily: 'var(--font-mono)',
                  fontSize: '1rem',
                }}
              />
            </div>

            {error && (
              <div style={{ color: 'var(--neon-rose)', fontSize: '0.82rem', marginBottom: 14, textAlign: 'center', background: 'var(--neon-rose-glow)', padding: '6px 10px', borderRadius: 8 }}>
                {error}
              </div>
            )}

            <button
              className="btn btn-primary btn-lg"
              onClick={handleSendOtp}
              disabled={loading}
              style={{ width: '100%', marginBottom: 16 }}
            >
              {loading ? <Loader2 size={18} className="spinner" /> : <><Mail size={16} /> Request Secure OTP</>}
            </button>

            <div style={{ position: 'relative', margin: '20px 0', textAlign: 'center' }}>
              <div style={{ position: 'absolute', top: '50%', left: 0, right: 0, height: 1, background: 'var(--glass-border)' }} />
              <span style={{ position: 'relative', background: '#0e1424', padding: '0 12px', fontSize: '0.72rem', color: 'var(--text-tertiary)', letterSpacing: '0.08em' }}>
                OR EXPLORE INSTANTLY
              </span>
            </div>

            <button
              className="btn btn-secondary btn-lg"
              onClick={loginAsDemo}
              style={{ width: '100%', marginBottom: 24, borderColor: 'var(--neon-purple)', color: '#f1f5f9' }}
            >
              <Sparkles size={16} style={{ color: 'var(--neon-purple)' }} /> Launch Instant Demo Mode
            </button>

            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '12px 14px',
                background: 'rgba(255, 255, 255, 0.02)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--glass-border)',
              }}
            >
              <Shield size={18} style={{ color: 'var(--neon-emerald)', flexShrink: 0 }} />
              <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', lineHeight: 1.4 }}>
                100% Free Supabase authentication. End-to-end encryption with zero telecom fees.
              </span>
            </div>
          </>
        ) : (
          <>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', textAlign: 'center', marginBottom: 16 }}>
              Enter the 6-digit verification code sent to <strong style={{ color: 'var(--neon-cyan)' }}>{email}</strong>
            </p>

            {infoMsg && (
              <div
                style={{
                  background: 'var(--neon-cyan-glow)',
                  border: '1px solid rgba(6, 182, 212, 0.3)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '8px 12px',
                  fontSize: '0.8rem',
                  color: 'var(--neon-cyan)',
                  marginBottom: 14,
                  textAlign: 'center',
                }}
              >
                ℹ️ {infoMsg}
              </div>
            )}

            <div
              style={{
                background: 'rgba(99, 102, 241, 0.1)',
                border: '1px solid rgba(99, 102, 241, 0.3)',
                borderRadius: 'var(--radius-md)',
                padding: '10px 14px',
                marginBottom: 18,
                fontSize: '0.8rem',
                lineHeight: 1.5,
                color: '#e2e8f0',
                textAlign: 'center',
              }}
            >
              💡 <strong>1-Click Instant Login</strong>: You can also click the <strong>[Sign in]</strong> link inside your email to log in automatically.
            </div>

            <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginBottom: 22 }}>
              {otp.map((digit, i) => (
                <input
                  key={i}
                  ref={el => { otpRefs.current[i] = el; }}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={digit}
                  onChange={e => handleOtpChange(i, e.target.value)}
                  onKeyDown={e => handleOtpKeyDown(i, e)}
                  style={{
                    width: 46,
                    height: 52,
                    textAlign: 'center',
                    fontSize: '1.4rem',
                    fontFamily: 'var(--font-outfit)',
                    fontWeight: 800,
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: digit ? '1px solid var(--neon-cyan)' : '1px solid var(--glass-border-bright)',
                    boxShadow: digit ? '0 0 12px var(--neon-cyan-glow)' : 'none',
                    borderRadius: 'var(--radius-md)',
                    color: '#fff',
                  }}
                />
              ))}
            </div>

            {error && (
              <div style={{ color: 'var(--neon-rose)', fontSize: '0.82rem', marginBottom: 14, textAlign: 'center' }}>
                {error}
              </div>
            )}

            <button
              className="btn btn-primary btn-lg"
              onClick={handleVerifyOtp}
              disabled={loading}
              style={{ width: '100%', marginBottom: 14 }}
            >
              {loading ? <Loader2 size={18} className="spinner" /> : <><KeyRound size={16} /> Verify & Access Vault</>}
            </button>

            <button
              className="btn btn-ghost btn-sm"
              onClick={() => { setStep('email'); setOtp(['','','','','','']); setError(''); setInfoMsg(''); }}
              style={{ width: '100%' }}
            >
              ← Change Email Address
            </button>
          </>
        )}
      </div>
    </div>
  );
}
