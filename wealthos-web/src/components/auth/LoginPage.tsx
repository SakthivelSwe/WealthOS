import { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { Phone, Mail, Shield, ArrowRight, Loader2, Sparkles, Lock, KeyRound } from 'lucide-react';

export function LoginPage() {
  const { signInWithOtp, verifyOtp, loginAsDemo } = useAuth();
  const [step, setStep] = useState<'phone' | 'otp'>('phone');
  const [authMode, setAuthMode] = useState<'phone' | 'email'>('email');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [error, setError] = useState('');
  const [infoMsg, setInfoMsg] = useState('');
  const [loading, setLoading] = useState(false);
  const otpRefs = useRef<(HTMLInputElement | null)[]>([]);

  const handleSendOtp = async () => {
    setError('');
    setInfoMsg('');

    if (authMode === 'email') {
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
    } else {
      if (phone.length < 10) {
        setError('Enter a valid 10-digit mobile number');
        return;
      }
      setLoading(true);
      const fullPhone = phone.startsWith('+') ? phone : `+91${phone}`;
      const result = await signInWithOtp(fullPhone, false);
      setLoading(false);
      if (result.error) {
        setError(result.error.message);
        return;
      }
      if (result.isDemoFallback) {
        setInfoMsg('⚠️ SMS Provider (Twilio) not configured in your Supabase project. Use test code 123456, or switch to Email Login to receive a live code.');
      } else {
        setInfoMsg('✅ OTP sent to your phone via SMS. Check messages.');
      }
      setStep('otp');
    }
  };

  const handleVerifyOtp = async () => {
    const token = otp.join('');
    if (token.length !== 6) { setError('Enter the 6-digit OTP'); return; }
    setError('');
    setLoading(true);
    const target = authMode === 'email' ? email.trim() : (phone.startsWith('+') ? phone : `+91${phone}`);
    const { error } = await verifyOtp(target, token, authMode === 'email');
    setLoading(false);
    if (error) { setError(error.message); }
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

        {step === 'phone' ? (
          <>
            {/* Mode Switcher: Free Email OTP vs Phone SMS */}
            <div
              style={{
                display: 'flex',
                background: 'rgba(255, 255, 255, 0.04)',
                borderRadius: 'var(--radius-md)',
                padding: 4,
                marginBottom: 20,
                border: '1px solid var(--glass-border-bright)',
              }}
            >
              <button
                type="button"
                onClick={() => { setAuthMode('email'); setError(''); setInfoMsg(''); }}
                style={{
                  flex: 1,
                  padding: '9px 12px',
                  borderRadius: 'var(--radius-sm)',
                  background: authMode === 'email' ? 'rgba(6, 182, 212, 0.15)' : 'transparent',
                  border: authMode === 'email' ? '1px solid var(--neon-cyan)' : '1px solid transparent',
                  color: authMode === 'email' ? '#fff' : 'var(--text-tertiary)',
                  fontWeight: 700,
                  fontSize: '0.82rem',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 6,
                  transition: 'all 0.18s ease',
                }}
              >
                <Mail size={14} style={{ color: authMode === 'email' ? 'var(--neon-cyan)' : 'inherit' }} /> Email (100% Free)
              </button>
              <button
                type="button"
                onClick={() => { setAuthMode('phone'); setError(''); setInfoMsg(''); }}
                style={{
                  flex: 1,
                  padding: '9px 12px',
                  borderRadius: 'var(--radius-sm)',
                  background: authMode === 'phone' ? 'rgba(6, 182, 212, 0.15)' : 'transparent',
                  border: authMode === 'phone' ? '1px solid var(--neon-cyan)' : '1px solid transparent',
                  color: authMode === 'phone' ? '#fff' : 'var(--text-tertiary)',
                  fontWeight: 700,
                  fontSize: '0.82rem',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 6,
                  transition: 'all 0.18s ease',
                }}
              >
                <Phone size={14} style={{ color: authMode === 'phone' ? 'var(--neon-cyan)' : 'inherit' }} /> Mobile Phone
              </button>
            </div>

            <p style={{ fontSize: '0.83rem', color: 'var(--text-secondary)', textAlign: 'center', marginBottom: 20, lineHeight: 1.5 }}>
              {authMode === 'email'
                ? 'Enter your email address to receive a live 6-digit verification code directly in your inbox. No payments or phone carrier fees required.'
                : 'Enter your mobile number to authenticate. (Use test code 123456 or add your number to Supabase Test Phone Numbers for free).'}
            </p>

            {authMode === 'phone' ? (
              <div className="form-group" style={{ marginBottom: 20 }}>
                <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Phone size={14} style={{ color: 'var(--neon-cyan)' }} /> Mobile Phone Number
                </label>
                <div style={{ display: 'flex', gap: 10 }}>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      padding: '0 14px',
                      background: 'rgba(255, 255, 255, 0.04)',
                      border: '1px solid var(--glass-border-bright)',
                      borderRadius: 'var(--radius-md)',
                      color: 'var(--neon-cyan)',
                      fontSize: '0.95rem',
                      fontWeight: 700,
                      fontFamily: 'var(--font-mono)',
                      flexShrink: 0,
                    }}
                  >
                    +91
                  </div>
                  <input
                    type="tel"
                    placeholder="Enter 10-digit number"
                    value={phone}
                    onChange={e => setPhone(e.target.value.replace(/\D/g, '').slice(0, 10))}
                    onKeyDown={e => e.key === 'Enter' && handleSendOtp()}
                    autoFocus
                    style={{
                      flex: 1,
                      fontFamily: 'var(--font-mono)',
                      fontSize: '1.05rem',
                      letterSpacing: '0.05em',
                    }}
                  />
                </div>
              </div>
            ) : (
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
            )}

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
              {loading ? <Loader2 size={18} className="spinner" /> : <><Lock size={16} /> Request Secure OTP</>}
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
                End-to-end PBKDF2 encryption. Sessions stay active until explicit logout.
              </span>
            </div>
          </>
        ) : (
          <>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', textAlign: 'center', marginBottom: 20 }}>
              Enter the 6-digit verification code sent to <strong style={{ color: 'var(--neon-cyan)' }}>{authMode === 'email' ? email : `+91 ${phone}`}</strong>
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
                  marginBottom: 16,
                  textAlign: 'center',
                }}
              >
                ℹ️ {infoMsg}
              </div>
            )}

            {authMode === 'email' && (
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
                💡 <strong>Instant 1-Click Login</strong>: You can click the <strong>[Sign in]</strong> link inside your email, or type/paste the code below.
              </div>
            )}

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
              onClick={() => { setStep('phone'); setOtp(['','','','','','']); setError(''); setInfoMsg(''); }}
              style={{ width: '100%' }}
            >
              ← Edit {authMode === 'email' ? 'Email Address' : 'Phone Number'}
            </button>
          </>
        )}
      </div>
    </div>
  );
}
