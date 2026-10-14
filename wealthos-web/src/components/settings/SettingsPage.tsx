import { useState, useRef } from 'react';
import { useData } from '../../hooks/useData';
import { useAuth } from '../../hooks/useAuth';
import { formatMoney, parseMoney, CURRENCIES, type ThemeMode, type UserSettings } from '../../types';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { User, Shield, Palette, Database, Download, Upload, LogOut, Eye, EyeOff, Phone, Save, Check, RefreshCw, CheckCircle2, Sparkles } from 'lucide-react';
import { SUPABASE_URL_VALUE } from '../../lib/supabase';

export function SettingsPage() {
  const { settings, updateSettings, accounts, transactions, goals, debts, chitFunds, recurringTxns, importAllData, refresh } = useData();
  const { user, signOut } = useAuth();
  const [activeSection, setActiveSection] = useState('profile');
  const [saved, setSaved] = useState(false);
  const [importStatus, setImportStatus] = useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Profile state
  const [userName, setUserName] = useState(settings?.user_name || 'WealthOS User');
  const [currency, setCurrency] = useState(settings?.currency || 'INR');

  // Budget
  const [incomeStr, setIncomeStr] = useState(settings ? String(settings.basis_income_minor / 100) : '');
  const [needsPct, setNeedsPct] = useState(settings?.needs_pct || 50);
  const [wantsPct, setWantsPct] = useState(settings?.wants_pct || 30);
  const [savingsPct, setSavingsPct] = useState(settings?.savings_pct || 20);

  // Privacy
  const [maskAmounts, setMaskAmounts] = useState(settings?.mask_amounts || false);
  const [themeMode, setThemeMode] = useState<ThemeMode>(settings?.theme_mode || 'DARK');

  const handleSave = async () => {
    const updates: Partial<UserSettings> = {
      user_name: userName,
      currency,
      basis_income_minor: parseMoney(incomeStr || '0', currency),
      needs_pct: needsPct,
      wants_pct: wantsPct,
      savings_pct: savingsPct,
      mask_amounts: maskAmounts,
      theme_mode: themeMode,
    };
    await updateSettings(updates);
    document.documentElement.setAttribute('data-theme', themeMode.toLowerCase());
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const handleExportJSON = () => {
    const data = {
      accounts,
      transactions,
      txns: transactions,
      goals,
      debts,
      chits: chitFunds,
      chitFunds,
      recurring: recurringTxns,
      recurringTxns,
      exportedAt: new Date().toISOString(),
      version: '1.0',
    };
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `wealthos-backup-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleExportCSV = () => {
    const headers = ['Date', 'Type', 'Amount', 'Currency', 'Merchant', 'Description', 'Account', 'Category', 'PaymentMethod', 'NeedWant'];
    const rows = transactions.filter(t => !t.deleted_at).map(t => [
      t.local_date, t.type, (t.amount_minor / 100).toFixed(2), t.currency,
      t.merchant || '', t.description || '', t.account_id, t.category_id || '', t.payment_method || '', t.need_want || '',
    ]);
    const csv = [headers, ...rows].map(r => r.map(v => `"${v}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `wealthos-transactions-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const text = await file.text();
      const parsed = JSON.parse(text);
      const res = await importAllData(parsed);
      if (res.success) {
        setImportStatus(`Successfully ingested ${res.count} records from ${file.name}!`);
      } else {
        setImportStatus('Invalid JSON format. Please verify file.');
      }
    } catch (err: any) {
      setImportStatus('Import failed: ' + err.message);
    }
    if (fileInputRef.current) fileInputRef.current.value = '';
    setTimeout(() => setImportStatus(null), 5000);
  };

  const handleManualSync = async () => {
    setIsRefreshing(true);
    await refresh();
    setTimeout(() => setIsRefreshing(false), 800);
  };

  const sections = [
    { id: 'profile', label: 'Identity & Currency', icon: User },
    { id: 'budget', label: '50/30/20 Budgeting', icon: Database },
    { id: 'privacy', label: 'Appearance & Masking', icon: Shield },
    { id: 'data', label: 'Cloud Sync & Backups', icon: Download },
    { id: 'account', label: 'Session Management', icon: Phone },
  ];

  const totalPct = needsPct + wantsPct + savingsPct;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
            System Core & Sync
          </h1>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
            Manage neural preferences, Supabase cloud mirroring, and cryptographic backups
          </p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-secondary btn-sm" onClick={handleManualSync} disabled={isRefreshing}>
            <RefreshCw size={14} className={isRefreshing ? 'spinner' : ''} />
            {isRefreshing ? 'Syncing...' : 'Sync Cloud'}
          </button>
          <button className="btn btn-primary btn-sm" onClick={handleSave}>
            {saved ? <><Check size={14} /> Saved!</> : <><Save size={14} /> Save Changes</>}
          </button>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '240px 1fr', gap: 24 }}>
        {/* Nav Tabs */}
        <div>
          <div className="cyber-card" style={{ padding: 8 }}>
            {sections.map(s => {
              const Icon = s.icon;
              const isActive = activeSection === s.id;
              return (
                <button
                  key={s.id}
                  onClick={() => setActiveSection(s.id)}
                  style={{
                    width: '100%',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                    padding: '10px 14px',
                    borderRadius: 'var(--radius-md)',
                    background: isActive ? 'rgba(255, 255, 255, 0.1)' : 'transparent',
                    border: isActive ? '1px solid var(--neon-cyan)' : '1px solid transparent',
                    color: isActive ? '#fff' : 'var(--text-secondary)',
                    fontWeight: 700,
                    fontSize: '0.85rem',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                    marginBottom: 4,
                  }}
                >
                  <Icon size={16} style={{ color: isActive ? 'var(--neon-cyan)' : 'inherit' }} />
                  <span>{s.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Content Box */}
        <div className="cyber-card">
          {/* Profile */}
          {activeSection === 'profile' && (
            <div>
              <div className="cyber-header">
                <div className="cyber-title-group">
                  <div className="cyber-icon-badge" style={{ background: 'var(--neon-cyan-glow)', color: 'var(--neon-cyan)' }}>
                    <User size={18} />
                  </div>
                  <div>
                    <div className="cyber-title">Personal Identity & Currency</div>
                    <div className="cyber-subtitle">Your user profile and monetary base</div>
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 16, padding: 18, background: 'rgba(255,255,255,0.03)', borderRadius: 'var(--radius-lg)', border: '1px solid var(--glass-border)' }}>
                  <div style={{ width: 56, height: 56, borderRadius: '50%', background: 'linear-gradient(135deg, var(--neon-cyan), var(--neon-indigo))', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.5rem', fontWeight: 900, color: '#fff', boxShadow: '0 0 16px var(--neon-cyan-glow)' }}>
                    {userName.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <div style={{ fontFamily: 'var(--font-outfit)', fontWeight: 800, fontSize: '1.15rem' }}>{userName}</div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', marginTop: 2 }}>📱 {user?.phone || 'Guest Mode'}</div>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Display Name</label>
                  <input value={userName} onChange={e => setUserName(e.target.value)} />
                </div>

                <div className="form-group">
                  <label className="form-label">Base Currency</label>
                  <select value={currency} onChange={e => setCurrency(e.target.value)}>
                    {CURRENCIES.map(c => <option key={c.code} value={c.code}>{c.symbol} {c.code} — {c.name}</option>)}
                  </select>
                </div>
              </div>
            </div>
          )}

          {/* Budget */}
          {activeSection === 'budget' && (
            <div>
              <div className="cyber-header">
                <div className="cyber-title-group">
                  <div className="cyber-icon-badge" style={{ background: 'var(--neon-indigo-glow)', color: 'var(--neon-indigo)' }}>
                    <Database size={18} />
                  </div>
                  <div>
                    <div className="cyber-title">50/30/20 Budgeting Strategy</div>
                    <div className="cyber-subtitle">Segment your baseline monthly income into Needs, Wants, and Wealth Accumulation</div>
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
                <div className="form-group">
                  <label className="form-label">Baseline Monthly Income ({currency})</label>
                  <input type="number" value={incomeStr} onChange={e => setIncomeStr(e.target.value)} placeholder="e.g. 85000" />
                </div>

                <div style={{ padding: 18, background: 'rgba(255,255,255,0.03)', borderRadius: 'var(--radius-lg)', border: '1px solid var(--glass-border)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 14 }}>
                    <span style={{ fontWeight: 700 }}>Allocation Percentages</span>
                    <span style={{ color: totalPct === 100 ? 'var(--neon-emerald)' : 'var(--neon-rose)', fontWeight: 800, fontFamily: 'var(--font-mono)' }}>
                      Total: {totalPct}%
                    </span>
                  </div>

                  {[
                    { label: 'Essential Needs', val: needsPct, set: setNeedsPct, color: 'var(--neon-cyan)' },
                    { label: 'Lifestyle Wants', val: wantsPct, set: setWantsPct, color: 'var(--neon-purple)' },
                    { label: 'Wealth Savings / SIPs', val: savingsPct, set: setSavingsPct, color: 'var(--neon-emerald)' },
                  ].map(lane => (
                    <div key={lane.label} style={{ marginBottom: 14 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', marginBottom: 6 }}>
                        <span style={{ fontWeight: 600 }}>{lane.label}</span>
                        <span style={{ color: lane.color, fontWeight: 800, fontFamily: 'var(--font-mono)' }}>{lane.val}%</span>
                      </div>
                      <input
                        type="range"
                        min={0}
                        max={100}
                        value={lane.val}
                        onChange={e => lane.set(parseInt(e.target.value))}
                        style={{ width: '100%', accentColor: lane.color }}
                      />
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Privacy */}
          {activeSection === 'privacy' && (
            <div>
              <div className="cyber-header">
                <div className="cyber-title-group">
                  <div className="cyber-icon-badge" style={{ background: 'var(--neon-amber-glow)', color: 'var(--neon-amber)' }}>
                    <Shield size={18} />
                  </div>
                  <div>
                    <div className="cyber-title">Appearance & Privacy HUD</div>
                    <div className="cyber-subtitle">Configure theme luminance and balance privacy</div>
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: 16, background: 'rgba(255,255,255,0.03)', borderRadius: 'var(--radius-md)', border: '1px solid var(--glass-border)' }}>
                  <div>
                    <div style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 8 }}>
                      {maskAmounts ? <EyeOff size={16} style={{ color: 'var(--neon-amber)' }} /> : <Eye size={16} />}
                      <span>Mask All Balances (Privacy Mode)</span>
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                      Hides all sensitive numbers with •••••• when in public environments
                    </div>
                  </div>
                  <button
                    type="button"
                    className={`btn btn-sm ${maskAmounts ? 'btn-primary' : 'btn-secondary'}`}
                    onClick={() => setMaskAmounts(!maskAmounts)}
                  >
                    {maskAmounts ? 'Enabled' : 'Disabled'}
                  </button>
                </div>

                <div>
                  <label className="form-label">Theme Environment</label>
                  <div style={{ display: 'flex', gap: 10 }}>
                    {(['DARK', 'MIDNIGHT', 'LIGHT'] as ThemeMode[]).map(t => (
                      <button
                        key={t}
                        type="button"
                        className={`btn btn-sm ${themeMode === t ? 'btn-primary' : 'btn-secondary'}`}
                        onClick={() => {
                          setThemeMode(t);
                          document.documentElement.setAttribute('data-theme', t.toLowerCase());
                        }}
                        style={{ flex: 1 }}
                      >
                        {t === 'DARK' ? '🌑 Dark Obsidian' : t === 'MIDNIGHT' ? '🌚 Midnight OLED' : '☀️ Lunar Light'}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Data & Sync */}
          {activeSection === 'data' && (
            <div>
              <div className="cyber-header">
                <div className="cyber-title-group">
                  <div className="cyber-icon-badge" style={{ background: 'var(--neon-cyan-glow)', color: 'var(--neon-cyan)' }}>
                    <Download size={18} />
                  </div>
                  <div>
                    <div className="cyber-title">Cloud Mirror & Cryptographic Backups</div>
                    <div className="cyber-subtitle">Instant WebSocket sync with mobile app and portable JSON archives</div>
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                {importStatus && (
                  <div style={{ padding: 12, background: 'var(--neon-emerald-glow)', color: 'var(--neon-emerald)', borderRadius: 'var(--radius-md)', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 8 }}>
                    <CheckCircle2 size={16} /> {importStatus}
                  </div>
                )}

                <div style={{ padding: 16, background: 'rgba(255,255,255,0.03)', borderRadius: 'var(--radius-md)', border: '1px solid var(--glass-border)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.92rem' }}>Supabase Cloud Connection</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', fontFamily: 'var(--font-mono)', marginTop: 2 }}>{SUPABASE_URL_VALUE}</div>
                    </div>
                    <span className="badge badge-green">Connected • Live</span>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: 10 }}>
                  <button className="btn btn-primary" onClick={() => fileInputRef.current?.click()} style={{ flex: 1 }}>
                    <Upload size={16} /> Import Mobile / JSON Backup
                  </button>
                  <input ref={fileInputRef} type="file" accept=".json" onChange={handleFileChange} style={{ display: 'none' }} />

                  <button className="btn btn-secondary" onClick={handleExportJSON} style={{ flex: 1 }}>
                    <Download size={16} /> Export All (JSON)
                  </button>
                </div>

                <button className="btn btn-secondary" onClick={handleExportCSV}>
                  <Download size={16} /> Export Transactions to CSV (Excel / Sheets)
                </button>
              </div>
            </div>
          )}

          {/* Account */}
          {activeSection === 'account' && (
            <div>
              <div className="cyber-header">
                <div className="cyber-title-group">
                  <div className="cyber-icon-badge" style={{ background: 'var(--neon-rose-glow)', color: 'var(--neon-rose)' }}>
                    <Phone size={18} />
                  </div>
                  <div>
                    <div className="cyber-title">Session Lifecycle</div>
                    <div className="cyber-subtitle">Your credentials and device disconnect</div>
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <div style={{ padding: 16, background: 'rgba(255,255,255,0.03)', borderRadius: 'var(--radius-md)', border: '1px solid var(--glass-border)' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>Active Mobile Number</div>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.95rem', color: 'var(--neon-cyan)', marginTop: 4 }}>
                    {user?.phone || 'Guest / Demo Active'}
                  </div>
                </div>

                <button
                  className="btn btn-danger"
                  onClick={() => { if (confirm('Disconnect session? Your cloud data remains intact in Supabase.')) signOut(); }}
                  style={{ width: '100%', marginTop: 10 }}
                >
                  <LogOut size={16} /> Disconnect & Sign Out
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
