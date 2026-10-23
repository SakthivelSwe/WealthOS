import { useState, useRef, useEffect } from 'react';
import { useData } from '../../hooks/useData';
import { useAuth } from '../../hooks/useAuth';
import { formatMoney, parseMoney, getCurrencyInfo, CURRENCIES, type ThemeMode, type UserSettings } from '../../types';
import { User, Shield, Database, Download, Upload, LogOut, Eye, EyeOff, Phone, Save, Check, RefreshCw, CheckCircle2, Trash2, AlertTriangle, Lock, X, FileText, CheckCircle } from 'lucide-react';
import { SUPABASE_URL_VALUE } from '../../lib/supabase';
import { createEncryptedBackupEnvelope, decryptBackupEnvelope } from '../../lib/backupCrypto';

export function SettingsPage() {
  const { settings, updateSettings, accounts, categories, transactions, journalEntries, goals, debts, chitFunds, recurringTxns, budgets, importAllData, refresh } = useData();
  const { user, signOut, deleteAllUserData } = useAuth();
  const [activeSection, setActiveSection] = useState('profile');
  const [saved, setSaved] = useState(false);
  const [importStatus, setImportStatus] = useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Accessible Export Modal state
  const [showExportModal, setShowExportModal] = useState(false);
  const [exportPassphrase, setExportPassphrase] = useState('');
  const [exportPassphraseConfirm, setExportPassphraseConfirm] = useState('');
  const [showExportPassphrase, setShowExportPassphrase] = useState(false);
  const [exportError, setExportError] = useState('');
  const [isExporting, setIsExporting] = useState(false);

  // Accessible Import Modal state
  const [pendingImportFile, setPendingImportFile] = useState<File | null>(null);
  const [showImportPassphraseModal, setShowImportPassphraseModal] = useState(false);
  const [importPassphrase, setImportPassphrase] = useState('');
  const [showImportPassphrase, setShowImportPassphrase] = useState(false);
  const [importPassphraseError, setImportPassphraseError] = useState('');
  const [isDecrypting, setIsDecrypting] = useState(false);

  // Import Preview Modal state
  const [importPreviewData, setImportPreviewData] = useState<{
    metadata?: any;
    payload: any;
    filename: string;
    summary: { accounts: number; txns: number; entries: number; goals: number; debts: number; chits: number; recs: number; budgets: number };
  } | null>(null);
  const [isIngesting, setIsIngesting] = useState(false);

  // Delete account state
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteConfirmText, setDeleteConfirmText] = useState('');
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState('');

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

  // Hydrate settings asynchronously when loaded from cloud
  useEffect(() => {
    if (settings) {
      if (settings.user_name) setUserName(settings.user_name);
      if (settings.currency) setCurrency(settings.currency);
      if (settings.basis_income_minor !== undefined) setIncomeStr(String(settings.basis_income_minor / 100));
      if (settings.needs_pct !== undefined) setNeedsPct(settings.needs_pct);
      if (settings.wants_pct !== undefined) setWantsPct(settings.wants_pct);
      if (settings.savings_pct !== undefined) setSavingsPct(settings.savings_pct);
      if (settings.mask_amounts !== undefined) setMaskAmounts(settings.mask_amounts);
      if (settings.theme_mode) {
        setThemeMode(settings.theme_mode);
        document.documentElement.setAttribute('data-theme', settings.theme_mode.toLowerCase());
      }
    }
  }, [settings]);

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
    localStorage.setItem('wealthos_theme', themeMode.toLowerCase());
    document.documentElement.setAttribute('data-theme', themeMode.toLowerCase());
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  const executeExport = async () => {
    const trimmedPin = exportPassphrase.trim();
    if (!trimmedPin) {
      setExportError('Vault passphrase cannot be empty.');
      return;
    }
    if (trimmedPin.length < 4) {
      setExportError('Passphrase must be at least 4 characters long.');
      return;
    }
    if (trimmedPin !== exportPassphraseConfirm.trim()) {
      setExportError('Passphrases do not match. Please verify.');
      return;
    }

    setIsExporting(true);
    setExportError('');

    try {
      const payload = {
        accounts,
        categories: categories || [],
        txns: transactions,
        journal_entries: journalEntries || [],
        goals,
        debts,
        chits: chitFunds,
        recurring: recurringTxns,
        budgets: budgets || [],
        settings: settings || null,  // F-01 fix: include settings in encrypted backup
      };
      const envelope = await createEncryptedBackupEnvelope(payload, trimmedPin);
      const blob = new Blob([envelope.buffer as ArrayBuffer], { type: 'application/octet-stream' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `wealthos-backup-${new Date().toISOString().slice(0, 10)}.wosenc`;
      a.click();
      URL.revokeObjectURL(url);
      setShowExportModal(false);
      setExportPassphrase('');
      setExportPassphraseConfirm('');
      setImportStatus('Encrypted .wosenc vault exported successfully!');
      setTimeout(() => setImportStatus(null), 4000);
    } catch (err: any) {
      setExportError('Backup creation failed: ' + err.message);
    } finally {
      setIsExporting(false);
    }
  };

  const handleExportJSON = () => {
    const data = {
      accounts,
      transactions,
      txns: transactions,
      journal_entries: journalEntries || [],
      goals,
      debts,
      chits: chitFunds,
      chitFunds,
      recurring: recurringTxns,
      recurringTxns,
      budgets: budgets || [],
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

  const sanitizeCsvCell = (value: string | number | null | undefined): string => {
    if (value === null || value === undefined) return '""';
    let str = String(value);
    // Mitigate CSV / spreadsheet formula injection
    if (/^[=+\-@\t\r]/.test(str)) {
      str = `'${str}`;
    }
    // RFC 4180 double-quote escaping: " becomes ""
    return `"${str.replace(/"/g, '""')}"`;
  };

  const handleExportCSV = () => {
    const headers = ['Date', 'Type', 'Amount', 'Currency', 'Merchant', 'Description', 'Account', 'Category', 'PaymentMethod', 'NeedWant'];
    const rows = transactions.filter(t => !t.deleted_at).map(t => {
      const cur = t.currency || currency;
      const info = getCurrencyInfo(cur);
      const formattedAmount = (t.amount_minor / Math.pow(10, info.minorUnit)).toFixed(info.minorUnit);
      const acc = accounts.find(a => a.id === t.account_id);
      return [
        t.local_date,
        t.type,
        formattedAmount,
        cur,
        t.merchant || '',
        t.description || '',
        acc ? acc.name : t.account_id,
        t.category_id || '',
        t.payment_method || '',
        t.need_want || '',
      ];
    });
    const csv = [headers, ...rows]
      .map(row => row.map(cell => sanitizeCsvCell(cell)).join(','))
      .join('\r\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
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

    if (file.name.endsWith('.wosenc')) {
      setPendingImportFile(file);
      setImportPassphrase('');
      setImportPassphraseError('');
      setShowImportPassphraseModal(true);
    } else {
      try {
        const text = await file.text();
        const parsed = JSON.parse(text);
        const accts = parsed.accounts || [];
        const txs = parsed.txns || parsed.transactions || [];
        const entries = parsed.journal_entries || parsed.journalEntries || [];
        const gls = parsed.goals || [];
        const dts = parsed.debts || [];
        const chts = parsed.chits || parsed.chitFunds || [];
        const recs = parsed.recurring || parsed.recurringTxns || [];
        const bdgs = parsed.budgets || [];
        setImportPreviewData({
          payload: parsed,
          filename: file.name,
          summary: {
            accounts: accts.length,
            txns: txs.length,
            entries: entries.length,
            goals: gls.length,
            debts: dts.length,
            chits: chts.length,
            recs: recs.length,
            budgets: bdgs.length,
          },
        });
      } catch (err: any) {
        setImportStatus('JSON file parsing failed: ' + err.message);
        setTimeout(() => setImportStatus(null), 4000);
      }
    }
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const executeDecryptImport = async () => {
    if (!pendingImportFile) return;
    const pin = importPassphrase.trim();
    if (!pin) {
      setImportPassphraseError('Please enter the vault passphrase.');
      return;
    }

    setIsDecrypting(true);
    setImportPassphraseError('');

    try {
      const buffer = await pendingImportFile.arrayBuffer();
      const { metadata, payload } = await decryptBackupEnvelope(new Uint8Array(buffer), pin);
      setShowImportPassphraseModal(false);
      setImportPassphrase('');

      const p: any = payload;
      const accts = p.accounts || [];
      const txs = p.txns || p.transactions || [];
      const entries = p.journal_entries || p.journalEntries || [];
      const gls = p.goals || [];
      const dts = p.debts || [];
      const chts = p.chits || p.chitFunds || [];
      const recs = p.recurring || p.recurringTxns || [];
      const bdgs = p.budgets || [];

      setImportPreviewData({
        metadata,
        payload,
        filename: pendingImportFile.name,
        summary: {
          accounts: accts.length,
          txns: txs.length,
          entries: entries.length,
          goals: gls.length,
          debts: dts.length,
          chits: chts.length,
          recs: recs.length,
          budgets: bdgs.length,
        },
      });
    } catch (err: any) {
      setImportPassphraseError('Decryption failed: ' + (err.message || 'Incorrect passphrase or corrupted file.'));
    } finally {
      setIsDecrypting(false);
    }
  };

  const confirmIngest = async () => {
    if (!importPreviewData) return;
    setIsIngesting(true);
    try {
      const res = await importAllData(importPreviewData.payload);
      if (res.success) {
        setImportStatus(`Successfully imported ${res.localCount} records from ${importPreviewData.filename} — all synced to cloud.`);
      } else if (res.partialSuccess) {
        // F-03: Local import succeeded but cloud sync had failures. Be honest.
        setImportStatus(`Imported ${res.localCount} records locally. Cloud sync partial — ${res.cloudFailedCount} items pending. Check your connection and re-sync.`);
      } else {
        setImportStatus(`Import failed: ${res.errors.join('; ')}`);
      }
    } catch (err: any) {
      setImportStatus('Import failed: ' + err.message);
    } finally {
      setIsIngesting(false);
      setImportPreviewData(null);
      setPendingImportFile(null);
      setTimeout(() => setImportStatus(null), 8000);
    }
  };

  const handleManualSync = async () => {
    setIsRefreshing(true);
    await refresh();
    setTimeout(() => setIsRefreshing(false), 800);
  };

  const handleDeleteAccount = async () => {
    if (deleteConfirmText !== 'DELETE') {
      setDeleteError('Please type DELETE to confirm.');
      return;
    }
    setIsDeleting(true);
    setDeleteError('');
    const { error } = await deleteAllUserData();
    if (error) {
      setDeleteError(error.message);
      setIsDeleting(false);
    }
    // On success, useAuth clears session and app redirects to login
  };

  const sections = [
    { id: 'profile', label: 'Profile & Currency', icon: User },
    { id: 'budget', label: 'Monthly Budget Targets', icon: Database },
    { id: 'privacy', label: 'Appearance & Privacy', icon: Shield },
    { id: 'data', label: 'Cloud Sync & Backup', icon: Download },
    { id: 'account', label: 'Account & Logout', icon: Phone },
    { id: 'danger', label: 'Delete All Data', icon: Trash2 },
  ];

  const totalPct = needsPct + wantsPct + savingsPct;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
            Settings
          </h1>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
            Manage your profile, currency, budget targets, and backups
          </p>
        </div>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <button className="btn btn-secondary btn-sm" onClick={handleManualSync} disabled={isRefreshing}>
            <RefreshCw size={14} className={isRefreshing ? 'spinner' : ''} />
            {isRefreshing ? 'Syncing...' : 'Sync Cloud'}
          </button>
          <button className="btn btn-primary btn-sm" onClick={handleSave}>
            {saved ? <><Check size={14} /> Saved!</> : <><Save size={14} /> Save Changes</>}
          </button>
        </div>
      </div>

      <div className="settings-grid-layout">
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

                <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                  <button className="btn btn-primary" onClick={() => fileInputRef.current?.click()} style={{ flex: '1 1 200px' }}>
                    <Upload size={16} /> Import Backup (.wosenc / JSON)
                  </button>
                  <input ref={fileInputRef} type="file" accept=".wosenc,.json" onChange={handleFileChange} style={{ display: 'none' }} />

                  <button className="btn btn-secondary" onClick={() => { setShowExportModal(true); setExportPassphrase(''); setExportPassphraseConfirm(''); setExportError(''); }} style={{ flex: '1 1 200px' }}>
                    <Lock size={16} /> Export Encrypted Vault (.wosenc)
                  </button>

                  <button className="btn btn-secondary" onClick={handleExportJSON} style={{ flex: 1 }}>
                    <Download size={16} /> Export JSON
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
                  <div className="cyber-icon-badge" style={{ background: 'var(--semantic-income-bg)', color: 'var(--semantic-income)' }}>
                    <User size={18} />
                  </div>
                  <div>
                    <div className="cyber-title">Session Lifecycle</div>
                    <div className="cyber-subtitle">Your credentials and device disconnect</div>
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <div style={{ padding: 16, background: 'rgba(255,255,255,0.03)', borderRadius: 'var(--radius-md)', border: '1px solid var(--glass-border)' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.85rem' }}>Active Account Email</div>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: '0.95rem', color: 'var(--accent-forest)', marginTop: 4 }}>
                    {user?.email || user?.phone || 'Guest / Demo Active'}
                  </div>
                </div>

                <button
                  className="btn btn-danger"
                  onClick={() => signOut()}
                  style={{ width: '100%', marginTop: 10 }}
                >
                  <LogOut size={16} /> Disconnect & Sign Out
                </button>
              </div>
            </div>
          )}
          {/* Danger Zone */}
          {activeSection === 'danger' && (
            <div>
              <div className="cyber-header">
                <div className="cyber-title-group">
                  <div className="cyber-icon-badge" style={{ background: 'var(--neon-rose-glow)', color: 'var(--neon-rose)' }}>
                    <Trash2 size={18} />
                  </div>
                  <div>
                    <div className="cyber-title">Danger Zone</div>
                    <div className="cyber-subtitle">Permanent account and data deletion — irreversible</div>
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
                {/* Warning Banner */}
                <div style={{ padding: 16, background: 'var(--neon-rose-glow)', border: '1px solid rgba(244, 63, 94, 0.4)', borderRadius: 'var(--radius-lg)', display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                  <AlertTriangle size={20} style={{ color: 'var(--neon-rose)', flexShrink: 0, marginTop: 2 }} />
                  <div>
                    <div style={{ fontWeight: 700, color: 'var(--neon-rose)', marginBottom: 4 }}>This will permanently delete your account</div>
                    <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                      All your transactions, accounts, goals, debts, chit funds, settings, and profile data will be erased from Supabase cloud storage and all local caches. <strong style={{ color: '#fff' }}>This cannot be undone.</strong>
                    </div>
                  </div>
                </div>

                {/* Data Summary */}
                <div style={{ padding: 14, background: 'rgba(255,255,255,0.03)', borderRadius: 'var(--radius-md)', border: '1px solid var(--glass-border)', fontSize: '0.82rem' }}>
                  <div style={{ fontWeight: 700, marginBottom: 10 }}>Data to be permanently deleted:</div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                    {[
                      { label: 'Accounts', val: accounts.length },
                      { label: 'Transactions', val: transactions.filter(t => !t.deleted_at).length },
                      { label: 'Goals', val: goals.length },
                      { label: 'Debts', val: debts.length },
                    ].map(item => (
                      <div key={item.label} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 10px', background: 'rgba(255,255,255,0.02)', borderRadius: 6 }}>
                        <span style={{ color: 'var(--text-secondary)' }}>{item.label}</span>
                        <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--neon-rose)' }}>{item.val}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {!showDeleteConfirm ? (
                  <button
                    className="btn btn-danger"
                    onClick={() => setShowDeleteConfirm(true)}
                    style={{ width: '100%' }}
                  >
                    <Trash2 size={16} /> Request Permanent Account Deletion
                  </button>
                ) : (
                  <div style={{ padding: 18, background: 'rgba(244, 63, 94, 0.06)', border: '1px solid rgba(244, 63, 94, 0.3)', borderRadius: 'var(--radius-lg)', display: 'flex', flexDirection: 'column', gap: 14 }}>
                    <div style={{ fontWeight: 700, color: 'var(--neon-rose)', display: 'flex', alignItems: 'center', gap: 8 }}>
                      <AlertTriangle size={16} /> Final Confirmation Required
                    </div>
                    <p style={{ fontSize: '0.83rem', color: 'var(--text-secondary)', lineHeight: 1.6, margin: 0 }}>
                      Type <strong style={{ color: '#fff', fontFamily: 'var(--font-mono)' }}>DELETE</strong> below to confirm permanent deletion of your WealthOS account and all associated data.
                    </p>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <input
                        type="text"
                        placeholder="Type DELETE to confirm"
                        value={deleteConfirmText}
                        onChange={e => setDeleteConfirmText(e.target.value)}
                        style={{ borderColor: 'rgba(244, 63, 94, 0.5)', color: 'var(--neon-rose)' }}
                      />
                    </div>
                    {deleteError && (
                      <div style={{ fontSize: '0.82rem', color: 'var(--neon-rose)', background: 'var(--neon-rose-glow)', padding: '8px 12px', borderRadius: 8 }}>
                        {deleteError}
                      </div>
                    )}
                    <div style={{ display: 'flex', gap: 10 }}>
                      <button
                        className="btn btn-secondary"
                        onClick={() => { setShowDeleteConfirm(false); setDeleteConfirmText(''); setDeleteError(''); }}
                        style={{ flex: 1 }}
                        disabled={isDeleting}
                      >
                        Cancel
                      </button>
                      <button
                        className="btn btn-danger"
                        onClick={handleDeleteAccount}
                        disabled={isDeleting || deleteConfirmText !== 'DELETE'}
                        style={{ flex: 1 }}
                      >
                        {isDeleting ? 'Deleting...' : <><Trash2 size={16} /> Permanently Delete</>}
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Accessible Export Vault Modal */}
      {showExportModal && (
        <div className="modal-overlay" onClick={() => !isExporting && setShowExportModal(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: 480 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-cyan-glow)', color: 'var(--neon-cyan)', width: 36, height: 36 }}>
                  <Lock size={18} />
                </div>
                <div>
                  <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.2rem', fontWeight: 800, margin: 0 }}>
                    Export Sovereign Vault
                  </h2>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                    AES-GCM-256 Encrypted Portable File (.wosenc)
                  </div>
                </div>
              </div>
              <button
                type="button"
                onClick={() => !isExporting && setShowExportModal(false)}
                className="btn-ghost"
                style={{ padding: 6, borderRadius: '50%' }}
                disabled={isExporting}
              >
                <X size={18} />
              </button>
            </div>

            <div style={{ padding: 12, background: 'rgba(6, 182, 212, 0.08)', border: '1px solid rgba(6, 182, 212, 0.3)', borderRadius: 'var(--radius-md)', fontSize: '0.8rem', color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: 16 }}>
              <strong style={{ color: 'var(--neon-cyan)' }}>Zero-Knowledge Vault:</strong> Your data is client-side encrypted before export. Blank passphrases are strictly prohibited. Without your passphrase, this file cannot be recovered.
            </div>

            {exportError && (
              <div style={{ padding: 10, background: 'var(--neon-rose-glow)', color: 'var(--neon-rose)', borderRadius: 'var(--radius-md)', fontSize: '0.82rem', marginBottom: 14 }}>
                {exportError}
              </div>
            )}

            <div className="form-group">
              <label className="form-label" style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Vault Passphrase *</span>
                <button
                  type="button"
                  onClick={() => setShowExportPassphrase(!showExportPassphrase)}
                  style={{ background: 'none', border: 'none', color: 'var(--text-tertiary)', cursor: 'pointer', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: 4 }}
                >
                  {showExportPassphrase ? <EyeOff size={13} /> : <Eye size={13} />}
                  {showExportPassphrase ? 'Hide' : 'Show'}
                </button>
              </label>
              <input
                type={showExportPassphrase ? 'text' : 'password'}
                placeholder="Enter a strong passphrase (min 4 characters)"
                value={exportPassphrase}
                onChange={e => setExportPassphrase(e.target.value)}
                autoFocus
              />
            </div>

            <div className="form-group">
              <label className="form-label">Confirm Vault Passphrase *</label>
              <input
                type={showExportPassphrase ? 'text' : 'password'}
                placeholder="Re-enter your passphrase"
                value={exportPassphraseConfirm}
                onChange={e => setExportPassphraseConfirm(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setShowExportModal(false)}
                disabled={isExporting}
                style={{ flex: 1 }}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={executeExport}
                disabled={isExporting}
                style={{ flex: 2 }}
              >
                {isExporting ? 'Encrypting Vault...' : <><Download size={15} /> Encrypt & Export</>}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Accessible Decrypt Import Modal */}
      {showImportPassphraseModal && (
        <div className="modal-overlay" onClick={() => !isDecrypting && setShowImportPassphraseModal(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: 440 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-emerald-glow)', color: 'var(--neon-emerald)', width: 36, height: 36 }}>
                  <Lock size={18} />
                </div>
                <div>
                  <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.2rem', fontWeight: 800, margin: 0 }}>
                    Unlock Encrypted Vault
                  </h2>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                    {pendingImportFile?.name}
                  </div>
                </div>
              </div>
              <button
                type="button"
                onClick={() => !isDecrypting && setShowImportPassphraseModal(false)}
                className="btn-ghost"
                style={{ padding: 6, borderRadius: '50%' }}
                disabled={isDecrypting}
              >
                <X size={18} />
              </button>
            </div>

            {importPassphraseError && (
              <div style={{ padding: 10, background: 'var(--neon-rose-glow)', color: 'var(--neon-rose)', borderRadius: 'var(--radius-md)', fontSize: '0.82rem', marginBottom: 14 }}>
                {importPassphraseError}
              </div>
            )}

            <div className="form-group">
              <label className="form-label" style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span>Vault Passphrase *</span>
                <button
                  type="button"
                  onClick={() => setShowImportPassphrase(!showImportPassphrase)}
                  style={{ background: 'none', border: 'none', color: 'var(--text-tertiary)', cursor: 'pointer', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: 4 }}
                >
                  {showImportPassphrase ? <EyeOff size={13} /> : <Eye size={13} />}
                  {showImportPassphrase ? 'Hide' : 'Show'}
                </button>
              </label>
              <input
                type={showImportPassphrase ? 'text' : 'password'}
                placeholder="Enter the passphrase used for this backup"
                value={importPassphrase}
                onChange={e => setImportPassphrase(e.target.value)}
                autoFocus
                onKeyDown={e => { if (e.key === 'Enter') executeDecryptImport(); }}
              />
            </div>

            <div style={{ display: 'flex', gap: 10, marginTop: 20 }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => { setShowImportPassphraseModal(false); setPendingImportFile(null); }}
                disabled={isDecrypting}
                style={{ flex: 1 }}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={executeDecryptImport}
                disabled={isDecrypting}
                style={{ flex: 2 }}
              >
                {isDecrypting ? 'Decrypting Vault...' : <><CheckCircle size={15} /> Decrypt & Preview</>}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Accessible Import Preview Modal */}
      {importPreviewData && (
        <div className="modal-overlay" onClick={() => !isIngesting && setImportPreviewData(null)}>
          <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: 500 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div className="cyber-icon-badge" style={{ background: 'var(--neon-cyan-glow)', color: 'var(--neon-cyan)', width: 36, height: 36 }}>
                  <FileText size={18} />
                </div>
                <div>
                  <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.2rem', fontWeight: 800, margin: 0 }}>
                    Vault Ingestion Preview
                  </h2>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                    {importPreviewData.filename}
                  </div>
                </div>
              </div>
              <button
                type="button"
                onClick={() => !isIngesting && setImportPreviewData(null)}
                className="btn-ghost"
                style={{ padding: 6, borderRadius: '50%' }}
                disabled={isIngesting}
              >
                <X size={18} />
              </button>
            </div>

            <p style={{ fontSize: '0.84rem', color: 'var(--text-secondary)', lineHeight: 1.5, margin: '0 0 16px 0' }}>
              The file payload was successfully verified. Review the contents below before merging into your active ledger.
            </p>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 18 }}>
              {[
                { label: 'Accounts', count: importPreviewData.summary.accounts },
                { label: 'Transactions', count: importPreviewData.summary.txns },
                { label: 'Journal Legs', count: importPreviewData.summary.entries },
                { label: 'Budgets', count: importPreviewData.summary.budgets },
                { label: 'Financial Goals', count: importPreviewData.summary.goals },
                { label: 'Debt Accounts', count: importPreviewData.summary.debts },
                { label: 'Chit Funds', count: importPreviewData.summary.chits },
                { label: 'Recurring Rules', count: importPreviewData.summary.recs },
              ].map(item => (
                <div key={item.label} style={{ padding: '8px 12px', background: 'rgba(255,255,255,0.03)', borderRadius: 'var(--radius-md)', border: '1px solid var(--glass-border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>{item.label}</span>
                  <span style={{ fontFamily: 'var(--font-mono)', fontWeight: 700, color: 'var(--neon-cyan)', fontSize: '0.9rem' }}>{item.count}</span>
                </div>
              ))}
            </div>

            <div style={{ padding: 12, background: 'rgba(16, 185, 129, 0.08)', border: '1px solid rgba(16, 185, 129, 0.3)', borderRadius: 'var(--radius-md)', fontSize: '0.8rem', color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: 18 }}>
              <strong style={{ color: 'var(--semantic-income)' }}>Safe Non-Destructive Merge:</strong> Ingestion preserves existing records and deduplicates by entity identifier.
            </div>

            <div style={{ display: 'flex', gap: 10 }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setImportPreviewData(null)}
                disabled={isIngesting}
                style={{ flex: 1 }}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={confirmIngest}
                disabled={isIngesting}
                style={{ flex: 2 }}
              >
                {isIngesting ? 'Ingesting Records...' : <><CheckCircle2 size={16} /> Confirm & Ingest Vault</>}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
