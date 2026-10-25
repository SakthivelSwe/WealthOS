import { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { Mail, Lock, Eye, EyeOff, Shield, Loader2, Sparkles, KeyRound, CheckCircle2, ArrowLeft, RefreshCw, Send, ChevronDown, ChevronUp, UserCheck, UserPlus, Key } from 'lucide-react';

export function LoginPage() {
  const { signInWithOtp, verifyOtp, signInWithPassword, signUpWithPassword, loginAsDemo, isDemoFallback } = useAuth();
  const [authMode, setAuthMode] = useState<'password' | 'magiclink'>('password');
  const [step, setStep] = useState<'email' | 'sent'>('email');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSignUp, setIsSignUp] = useState(false);
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [showCodeInput, setShowCodeInput] = useState(false);
  const [error, setError] = useState('');
  const [infoMsg, setInfoMsg] = useState('');
  const [loading, setLoading] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);
  const otpRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    let timer: any;
    if (resendCooldown > 0) {
      timer = setTimeout(() => setResendCooldown(resendCooldown - 1), 1000);
    }
    return () => clearTimeout(timer);
  }, [resendCooldown]);

  const handlePasswordAuth = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setError('');
    setInfoMsg('');

    const cleanEmail = email.trim();
    if (!cleanEmail || !cleanEmail.includes('@')) {
      setError('Please enter a valid email address');
      return;
    }
    if (!password) {
      setError('Please enter your password');
      return;
    }
    if (isSignUp && password.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }

    setLoading(true);
    if (isSignUp) {
      const result = await signUpWithPassword(cleanEmail, password);
      setLoading(false);
      if (result.error) {
        setError(result.error.message);
      } else {
        setInfoMsg('Account created successfully! Welcome to WealthOS.');
      }
    } else {
      const result = await signInWithPassword(cleanEmail, password);
      setLoading(false);
      if (result.error) {
        setError(result.error.message);
      }
    }
  };

  const handleSendLink = async () => {
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
    setInfoMsg(`Sign-in link sent to ${cleanEmail}`);
    setStep('sent');
    setResendCooldown(45);
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
    if (step === 'sent' && showCodeInput) {
      otpRefs.current[0]?.focus();
    }
  }, [step, showCodeInput]);

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
          maxWidth: 460,
          background: 'var(--bg-secondary, #131922)',
          backdropFilter: 'blur(36px) saturate(180%)',
          WebkitBackdropFilter: 'blur(36px) saturate(180%)',
          border: '1px solid var(--border-default, rgba(255, 255, 255, 0.12))',
          borderRadius: 'var(--radius-xl)',
          padding: '40px 32px',
          boxShadow: '0 24px 48px -12px rgba(0, 0, 0, 0.8), 0 0 0 1px var(--border-subtle)',
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
            background: 'linear-gradient(90deg, transparent, var(--accent-forest, #10b981), transparent)',
            boxShadow: '0 0 8px rgba(16, 185, 129, 0.3)',
          }}
        />

        {/* Brand Orb & Heading */}
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div
            style={{
              width: 58,
              height: 58,
              borderRadius: 18,
              background: 'linear-gradient(135deg, var(--accent-forest, #10b981) 0%, #059669 100%)',
              margin: '0 auto 16px auto',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 4px 16px rgba(16, 185, 129, 0.25)',
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
              background: 'linear-gradient(135deg, #ffffff 40%, #e2e8f0 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
            }}
          >
            WealthOS
          </h1>
          <p style={{ fontSize: '0.82rem', color: 'var(--text-tertiary)', marginTop: 4 }}>
            Private Money Manager • Cloud Ledger & Android Sync
          </p>
        </div>

        {/* Auth Mode Tabs */}
        <div
          style={{
            display: 'flex',
            background: 'rgba(255, 255, 255, 0.04)',
            padding: 4,
            borderRadius: 12,
            marginBottom: 22,
            border: '1px solid var(--border-subtle)',
          }}
        >
          <button
            type="button"
            onClick={() => { setAuthMode('password'); setError(''); setInfoMsg(''); }}
            style={{
              flex: 1,
              padding: '9px 12px',
              borderRadius: 8,
              border: 'none',
              background: authMode === 'password' ? 'var(--accent-forest, #10b981)' : 'transparent',
              color: authMode === 'password' ? '#ffffff' : 'var(--text-secondary)',
              fontWeight: authMode === 'password' ? 700 : 500,
              fontSize: '0.82rem',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 6,
              transition: 'all 0.2s ease',
            }}
          >
            <Key size={14} /> Password Sign-In
          </button>
          <button
            type="button"
            onClick={() => { setAuthMode('magiclink'); setError(''); setInfoMsg(''); }}
            style={{
              flex: 1,
              padding: '9px 12px',
              borderRadius: 8,
              border: 'none',
              background: authMode === 'magiclink' ? 'var(--accent-forest, #10b981)' : 'transparent',
              color: authMode === 'magiclink' ? '#ffffff' : 'var(--text-secondary)',
              fontWeight: authMode === 'magiclink' ? 700 : 500,
              fontSize: '0.82rem',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 6,
              transition: 'all 0.2s ease',
            }}
          >
            <Mail size={14} /> Magic Link / OTP
          </button>
        </div>

        {authMode === 'password' ? (
          <form onSubmit={handlePasswordAuth}>
            <div className="form-group" style={{ marginBottom: 16 }}>
              <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <Mail size={14} style={{ color: 'var(--accent-forest, #10b981)' }} /> Email Address
              </label>
              <input
                type="email"
                placeholder="your.email@example.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                autoFocus
                style={{
                  width: '100%',
                  fontFamily: 'var(--font-mono)',
                  fontSize: '0.95rem',
                }}
              />
            </div>

            <div className="form-group" style={{ marginBottom: 20 }}>
              <label className="form-label" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <Lock size={14} style={{ color: 'var(--accent-forest, #10b981)' }} /> Password
                </span>
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-tertiary)',
                    cursor: 'pointer',
                    fontSize: '0.75rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 4,
                  }}
                >
                  {showPassword ? <><EyeOff size={12} /> Hide</> : <><Eye size={12} /> Show</>}
                </button>
              </label>
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder={isSignUp ? 'Choose a password (min 6 chars)' : 'Enter your password'}
                value={password}
                onChange={e => setPassword(e.target.value)}
                style={{
                  width: '100%',
                  fontFamily: 'var(--font-mono)',
                  fontSize: '0.95rem',
                }}
              />
            </div>

            {error && (
              <div style={{ color: 'var(--semantic-expense, #f43f5e)', fontSize: '0.82rem', marginBottom: 14, textAlign: 'center', background: 'rgba(244, 63, 94, 0.1)', padding: '8px 12px', borderRadius: 8, border: '1px solid rgba(244, 63, 94, 0.3)' }}>
                {error}
              </div>
            )}

            {infoMsg && (
              <div style={{ color: 'var(--semantic-income, #10b981)', fontSize: '0.82rem', marginBottom: 14, textAlign: 'center', background: 'rgba(16, 185, 129, 0.1)', padding: '8px 12px', borderRadius: 8, border: '1px solid rgba(16, 185, 129, 0.3)' }}>
                {infoMsg}
              </div>
            )}

            <button
              type="submit"
              className="btn btn-primary btn-lg"
              disabled={loading}
              style={{ width: '100%', marginBottom: 14 }}
            >
              {loading ? (
                <Loader2 size={18} className="spinner" />
              ) : isSignUp ? (
                <><UserPlus size={16} /> Create Account & Sign In</>
              ) : (
                <><KeyRound size={16} /> Sign In with Password</>
              )}
            </button>

            <button
              type="button"
              onClick={() => { setIsSignUp(!isSignUp); setError(''); setInfoMsg(''); }}
              style={{
                width: '100%',
                background: 'none',
                border: 'none',
                color: 'var(--accent-forest, #10b981)',
                fontSize: '0.82rem',
                cursor: 'pointer',
                textAlign: 'center',
                marginBottom: 20,
              }}
            >
              {isSignUp ? 'Already have an account? Sign in here' : "Don't have an account yet? Create one here"}
            </button>

            {/* Seamless Android Hint */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                padding: '10px 12px',
                background: 'rgba(16, 185, 129, 0.06)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid rgba(16, 185, 129, 0.2)',
                marginBottom: 20,
              }}
            >
              <CheckCircle2 size={16} style={{ color: 'var(--accent-forest, #10b981)', flexShrink: 0 }} />
              <span style={{ fontSize: '0.72rem', color: 'var(--text-secondary)', lineHeight: 1.4 }}>
                <strong>Perfect Android Sync</strong>: Use the exact same email & password in the WealthOS Android app to sync immediately!
              </span>
            </div>

            <div style={{ position: 'relative', margin: '16px 0', textAlign: 'center' }}>
              <div style={{ position: 'absolute', top: '50%', left: 0, right: 0, height: 1, background: 'var(--border-subtle)' }} />
              <span style={{ position: 'relative', background: 'var(--bg-secondary, #131922)', padding: '0 12px', fontSize: '0.72rem', color: 'var(--text-tertiary)', letterSpacing: '0.08em' }}>
                OR EXPLORE INSTANTLY
              </span>
            </div>

            <button
              type="button"
              className="btn btn-secondary btn-lg"
              onClick={loginAsDemo}
              style={{ width: '100%', marginBottom: 16, borderColor: 'var(--border-default)', color: '#f1f5f9' }}
            >
              <Sparkles size={16} style={{ color: 'var(--accent-forest, #10b981)' }} /> Launch Instant Demo Mode
            </button>
          </form>
        ) : step === 'email' ? (
          <>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', textAlign: 'center', marginBottom: 22, lineHeight: 1.6 }}>
              Enter your email to receive a secure 1-click magic sign-in link. No password typing needed.
            </p>

            <div className="form-group" style={{ marginBottom: 20 }}>
              <label className="form-label" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <Mail size={14} style={{ color: 'var(--accent-forest, #10b981)' }} /> Email Address
              </label>
              <input
                type="email"
                placeholder="your.email@example.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleSendLink()}
                autoFocus
                style={{
                  width: '100%',
                  fontFamily: 'var(--font-mono)',
                  fontSize: '1rem',
                }}
              />
            </div>

            {error && (
              <div style={{ color: 'var(--semantic-expense, #f43f5e)', fontSize: '0.82rem', marginBottom: 14, textAlign: 'center', background: 'rgba(244, 63, 94, 0.1)', padding: '8px 12px', borderRadius: 8, border: '1px solid rgba(244, 63, 94, 0.3)' }}>
                {error}
              </div>
            )}

            <button
              className="btn btn-primary btn-lg"
              onClick={handleSendLink}
              disabled={loading}
              style={{ width: '100%', marginBottom: 16 }}
            >
              {loading ? <Loader2 size={18} className="spinner" /> : <><Send size={16} /> Send Magic Sign-in Link</>}
            </button>

            <div style={{ position: 'relative', margin: '20px 0', textAlign: 'center' }}>
              <div style={{ position: 'absolute', top: '50%', left: 0, right: 0, height: 1, background: 'var(--border-subtle)' }} />
              <span style={{ position: 'relative', background: 'var(--bg-secondary, #131922)', padding: '0 12px', fontSize: '0.72rem', color: 'var(--text-tertiary)', letterSpacing: '0.08em' }}>
                OR EXPLORE INSTANTLY
              </span>
            </div>

            <button
              className="btn btn-secondary btn-lg"
              onClick={loginAsDemo}
              style={{ width: '100%', marginBottom: 24, borderColor: 'var(--border-default)', color: '#f1f5f9' }}
            >
              <Sparkles size={16} style={{ color: 'var(--accent-forest, #10b981)' }} /> Launch Instant Demo Mode
            </button>

            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                padding: '12px 14px',
                background: 'rgba(255, 255, 255, 0.02)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <Shield size={18} style={{ color: 'var(--semantic-income, #10b981)', flexShrink: 0 }} />
              <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', lineHeight: 1.4 }}>
                100% Free Supabase authentication. Zero-knowledge encrypted ledger starting clean today.
              </span>
            </div>
          </>
        ) : (
          <>
            <div style={{ textAlign: 'center', marginBottom: 18 }}>
              <div
                style={{
                  width: 52,
                  height: 52,
                  borderRadius: 16,
                  background: 'rgba(16, 185, 129, 0.12)',
                  border: '1px solid rgba(16, 185, 129, 0.3)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  margin: '0 auto 12px auto',
                  color: 'var(--accent-forest, #10b981)',
                }}
              >
                <CheckCircle2 size={26} />
              </div>
              <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, color: '#fff', marginBottom: 4 }}>
                Check Your Inbox
              </h2>
              <p style={{ fontSize: '0.82rem', color: 'var(--text-tertiary)' }}>
                We emailed a 1-click sign-in link to:
              </p>
              <div
                style={{
                  display: 'inline-block',
                  marginTop: 6,
                  padding: '4px 14px',
                  borderRadius: 'var(--radius-pill)',
                  background: 'rgba(16, 185, 129, 0.08)',
                  border: '1px solid rgba(16, 185, 129, 0.25)',
                  color: 'var(--accent-forest, #10b981)',
                  fontWeight: 700,
                  fontSize: '0.85rem',
                  fontFamily: 'var(--font-mono)',
                }}
              >
                {email}
              </div>
            </div>

            {/* Instruction Card explaining the email received */}
            <div
              style={{
                background: 'rgba(255, 255, 255, 0.02)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-md)',
                padding: '16px',
                marginBottom: 16,
              }}
            >
              <div style={{ fontSize: '0.78rem', fontWeight: 700, color: 'var(--accent-forest, #10b981)', marginBottom: 8, letterSpacing: '0.04em' }}>
                HOW TO SIGN IN:
              </div>
              <ol style={{ fontSize: '0.8rem', color: '#cbd5e1', margin: 0, paddingLeft: 18, lineHeight: 1.6 }}>
                <li>Open your email inbox (subject: <em>"Your sign-in link"</em>).</li>
                <li>Tap the <strong>[Sign in]</strong> button inside the email.</li>
                <li>You will be logged into your clean vault immediately.</li>
              </ol>

              <div
                style={{
                  marginTop: 12,
                  padding: '8px 12px',
                  background: 'rgba(16, 185, 129, 0.1)',
                  border: '1px solid rgba(16, 185, 129, 0.25)',
                  borderRadius: 8,
                  fontSize: '0.75rem',
                  color: 'var(--semantic-income, #10b981)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                }}
              >
                <span
                  style={{
                    width: 8,
                    height: 8,
                    borderRadius: '50%',
                    background: 'var(--semantic-income, #10b981)',
                    boxShadow: '0 0 8px rgba(16, 185, 129, 0.4)',
                    display: 'inline-block',
                    flexShrink: 0,
                  }}
                />
                <span>Listening for email confirmation... Tap the link in your email to continue.</span>
              </div>

              {isDemoFallback && (
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={() => verifyOtp(email.trim(), '123456', true)}
                  style={{ width: '100%', marginTop: 12, background: 'var(--accent-forest, #10b981)', fontSize: '0.8rem' }}
                >
                  ⚡ Localhost Dev: Click to Simulate Email Link Sign-in
                </button>
              )}
            </div>

            {/* Expandable 6-digit code entry for users who receive OTP token */}
            <div style={{ marginBottom: 18 }}>
              <button
                type="button"
                onClick={() => setShowCodeInput(!showCodeInput)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--text-tertiary)',
                  fontSize: '0.78rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  cursor: 'pointer',
                  margin: '0 auto',
                  padding: '4px 8px',
                }}
              >
                <KeyRound size={13} style={{ color: 'var(--accent-forest, #10b981)' }} />
                <span>Received a 6-digit verification code instead?</span>
                {showCodeInput ? <ChevronUp size={13} /> : <ChevronDown size={13} />}
              </button>

              {showCodeInput && (
                <div className="animate-pop" style={{ marginTop: 14 }}>
                  <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginBottom: 14 }}>
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
                          width: 44,
                          height: 48,
                          textAlign: 'center',
                          fontSize: '1.3rem',
                          fontFamily: 'var(--font-outfit)',
                          fontWeight: 800,
                          background: 'rgba(255, 255, 255, 0.05)',
                          border: digit ? '1px solid var(--accent-forest, #10b981)' : '1px solid var(--border-default)',
                          boxShadow: digit ? '0 0 8px rgba(16, 185, 129, 0.2)' : 'none',
                          borderRadius: 'var(--radius-md)',
                          color: '#fff',
                        }}
                      />
                    ))}
                  </div>

                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={handleVerifyOtp}
                    disabled={loading || otp.some(d => !d)}
                    style={{ width: '100%', borderColor: 'var(--accent-forest, #10b981)', color: 'var(--accent-forest, #10b981)' }}
                  >
                    {loading ? <Loader2 size={16} className="spinner" /> : <><KeyRound size={14} /> Verify 6-Digit Code</>}
                  </button>
                </div>
              )}
            </div>

            {error && (
              <div style={{ color: 'var(--semantic-expense, #f43f5e)', fontSize: '0.82rem', marginBottom: 14, textAlign: 'center', background: 'rgba(244, 63, 94, 0.1)', padding: '8px 12px', borderRadius: 8, border: '1px solid rgba(244, 63, 94, 0.3)' }}>
                {error}
              </div>
            )}

            <div style={{ display: 'flex', gap: 10, marginBottom: 10 }}>
              <button
                className="btn btn-secondary btn-md"
                onClick={handleSendLink}
                disabled={loading || resendCooldown > 0}
                style={{ flex: 1, fontSize: '0.8rem' }}
              >
                {resendCooldown > 0 ? (
                  `Resend in ${resendCooldown}s`
                ) : (
                  <><RefreshCw size={14} /> Resend Link</>
                )}
              </button>

              <button
                className="btn btn-ghost btn-md"
                onClick={() => { setStep('email'); setOtp(['','','','','','']); setError(''); setInfoMsg(''); }}
                style={{ flex: 1, fontSize: '0.8rem' }}
              >
                <ArrowLeft size={14} /> Change Email
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
