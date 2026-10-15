import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './hooks/useAuth';
import { DataProvider } from './hooks/useData';
import { LoginPage } from './components/auth/LoginPage';
import { Dashboard } from './components/dashboard/Dashboard';
import { AccountsPage } from './components/accounts/AccountsPage';
import { LedgerPage } from './components/ledger/LedgerPage';
import { InsightsPage } from './components/insights/InsightsPage';
import { PlansPage } from './components/plans/PlansPage';
import { NotificationsPage } from './components/notifications/NotificationsPage';
import { SettingsPage } from './components/settings/SettingsPage';
import { FloatingNav } from './components/shared/FloatingNav';
import { Canvas3DBackground } from './components/shared/Canvas3DBackground';
import { NewTransactionModal } from './components/shared/NewTransactionModal';
import { Search } from 'lucide-react';

// ===== COMMAND PALETTE =====
function CommandPalette({ onClose }: { onClose: () => void }) {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(0);
  const commands = [
    { label: 'Executive Dashboard', path: '/', shortcut: 'G D' },
    { label: 'Vaults & Accounts', path: '/accounts', shortcut: 'G A' },
    { label: 'Live Financial Ledger', path: '/ledger', shortcut: 'G L' },
    { label: 'Neural AI Insights', path: '/insights', shortcut: 'G I' },
    { label: 'Wealth Horizons & Plans', path: '/plans', shortcut: 'G P' },
    { label: 'System Configuration', path: '/settings', shortcut: 'G S' },
    { label: 'Notification Center', path: '/notifications', shortcut: '' },
  ].filter(c => !query || c.label.toLowerCase().includes(query.toLowerCase()));

  return (
    <div className="command-palette-overlay" onClick={onClose}>
      <div className="command-palette" onClick={e => e.stopPropagation()}>
        <div style={{ position: 'relative' }}>
          <Search size={18} style={{ position: 'absolute', left: 20, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-tertiary)' }} />
          <input
            className="command-input"
            placeholder="Type a command or jump to workspace..."
            value={query}
            onChange={e => setQuery(e.target.value)}
            autoFocus
            style={{ paddingLeft: 48 }}
            onKeyDown={e => {
              if (e.key === 'ArrowDown') setSelected(s => Math.min(s + 1, commands.length - 1));
              if (e.key === 'ArrowUp') setSelected(s => Math.max(s - 1, 0));
              if (e.key === 'Enter' && commands[selected]) {
                navigate(commands[selected].path);
                onClose();
              }
              if (e.key === 'Escape') onClose();
            }}
          />
        </div>
        <div className="command-results">
          {commands.map((cmd, i) => (
            <div
              key={cmd.path + cmd.label}
              className={`command-item ${i === selected ? 'selected' : ''}`}
              onClick={() => { navigate(cmd.path); onClose(); }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <Search size={14} style={{ color: 'var(--neon-cyan)' }} />
                <span>{cmd.label}</span>
              </div>
              {cmd.shortcut && <span className="hud-kbd">{cmd.shortcut}</span>}
            </div>
          ))}
          {commands.length === 0 && (
            <div style={{ padding: '16px 20px', color: 'var(--text-tertiary)', fontSize: '0.88rem' }}>
              No matching destinations found
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ===== LOADING SCREEN =====
function LoadingScreen() {
  return (
    <div className="loading-screen">
      <div style={{
        width: 64,
        height: 64,
        background: 'linear-gradient(135deg, var(--neon-cyan) 0%, var(--neon-indigo) 100%)',
        borderRadius: 18,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '1.8rem',
        fontWeight: 900,
        color: 'white',
        marginBottom: 20,
        boxShadow: '0 0 30px var(--neon-cyan-glow)',
      }}>
        W
      </div>
      <div className="spinner" />
      <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: 14, fontFamily: 'var(--font-outfit)', letterSpacing: '0.04em' }}>
        INITIALIZING NEURAL VAULT...
      </p>
    </div>
  );
}

// ===== PROTECTED LAYOUT =====
function ProtectedLayout() {
  const { user, loading } = useAuth();
  const [showCommand, setShowCommand] = useState(false);
  const [showNewTxn, setShowNewTxn] = useState(false);

  // Apply saved theme
  useEffect(() => {
    const savedTheme = localStorage.getItem('wealthos_theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);
  }, []);

  // Global keyboard shortcuts
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setShowCommand(v => !v);
      }
      if (e.key === 'Escape') {
        setShowCommand(false);
        setShowNewTxn(false);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  if (loading) return <LoadingScreen />;
  if (!user) return <Navigate to="/login" replace />;

  return (
    <DataProvider>
      <div className="app-viewport">
        {/* Living 3D Aurora Canvas Background */}
        <Canvas3DBackground />

        {/* Dynamic Floating Glass Island Navigation */}
        <FloatingNav
          onOpenCommand={() => setShowCommand(true)}
          onOpenNewTxn={() => setShowNewTxn(true)}
        />

        {/* Main Spatial Application Canvas */}
        <main className="app-main-canvas">
          <Routes>
            <Route path="/" element={<Dashboard onOpenNewTxn={() => setShowNewTxn(true)} />} />
            <Route path="/accounts" element={<AccountsPage />} />
            <Route path="/vaults" element={<AccountsPage />} />
            <Route path="/ledger" element={<LedgerPage />} />
            <Route path="/insights" element={<InsightsPage />} />
            <Route path="/plans" element={<PlansPage />} />
            <Route path="/horizons" element={<PlansPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/config" element={<SettingsPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>

        {showCommand && <CommandPalette onClose={() => setShowCommand(false)} />}
        {showNewTxn && <NewTransactionModal onClose={() => setShowNewTxn(false)} />}
      </div>
    </DataProvider>
  );
}

// ===== PUBLIC LAYOUT =====
function PublicLayout() {
  const { user, loading } = useAuth();
  if (loading) return <LoadingScreen />;
  if (user) return <Navigate to="/" replace />;
  return (
    <>
      <Canvas3DBackground />
      <LoginPage />
    </>
  );
}

// ===== ROOT APP =====
function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<PublicLayout />} />
          <Route path="/*" element={<ProtectedLayout />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
