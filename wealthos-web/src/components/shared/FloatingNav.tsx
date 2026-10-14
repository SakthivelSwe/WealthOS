import { useState, useRef, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useData } from '../../hooks/useData';
import {
  LayoutDashboard,
  Wallet,
  ScrollText,
  BarChart2,
  Target,
  Settings,
  Search,
  Plus,
  Eye,
  EyeOff,
  LogOut,
} from 'lucide-react';

interface FloatingNavProps {
  onOpenCommand: () => void;
  onOpenNewTxn?: () => void;
}

export function FloatingNav({ onOpenCommand, onOpenNewTxn }: FloatingNavProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, signOut } = useAuth();
  const { settings, updateSettings } = useData();
  const [profileOpen, setProfileOpen] = useState(false);
  const profileRef = useRef<HTMLDivElement>(null);

  const isMasked = settings?.mask_amounts || false;

  const navItems = [
    { path: '/', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/vaults', label: 'Vaults', icon: Wallet },
    { path: '/ledger', label: 'Ledger', icon: ScrollText },
    { path: '/insights', label: 'Insights', icon: BarChart2 },
    { path: '/horizons', label: 'Horizons', icon: Target },
    { path: '/config', label: 'Config', icon: Settings },
  ];

  // Click outside to close profile dropdown
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (profileRef.current && !profileRef.current.contains(e.target as Node)) {
        setProfileOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleToggleMask = () => {
    updateSettings({ mask_amounts: !isMasked });
  };

  const isItemActive = (path: string) => {
    if (path === '/' && location.pathname === '/') return true;
    if (path === '/vaults' && (location.pathname === '/vaults' || location.pathname === '/accounts')) return true;
    if (path === '/ledger' && location.pathname === '/ledger') return true;
    if (path === '/insights' && location.pathname === '/insights') return true;
    if (path === '/horizons' && (location.pathname === '/horizons' || location.pathname === '/plans')) return true;
    if (path === '/config' && (location.pathname === '/config' || location.pathname === '/settings')) return true;
    return false;
  };

  return (
    <>
      {/* Floating Dynamic Island Top Navigation Header */}
      <header className="floating-nav-header">
        <div className="floating-nav-island">
          {/* Brand Logo with 3D Holographic Aura */}
          <div className="nav-brand" onClick={() => navigate('/')}>
            <div className="nav-logo-orb">
              <span className="logo-letter">W</span>
              <div className="orb-glow" />
            </div>
            <div className="brand-text-container">
              <span className="brand-title">WealthOS</span>
            </div>
          </div>

          <div className="nav-divider" />

          {/* Centered Navigation Pills */}
          <nav className="nav-links-track">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = isItemActive(item.path);
              return (
                <button
                  key={item.path}
                  onClick={() => navigate(item.path)}
                  className={`nav-link-pill ${isActive ? 'active' : ''}`}
                >
                  <Icon size={16} className="nav-icon" />
                  <span className="nav-label">{item.label}</span>
                  {isActive && <div className="active-glow-pill" />}
                </button>
              );
            })}
          </nav>

          <div className="nav-divider" />

          {/* Right Action Utilities */}
          <div className="nav-actions-cluster">
            {/* Quick Command Palette Button */}
            <button
              type="button"
              className="hud-tool-btn"
              onClick={onOpenCommand}
              title="Quick Search (Ctrl+K)"
            >
              <Search size={15} />
              <kbd className="hud-kbd">Ctrl+K</kbd>
            </button>

            {/* Quick Privacy Mask Toggle */}
            <button
              type="button"
              className={`hud-tool-btn ${isMasked ? 'active' : ''}`}
              onClick={handleToggleMask}
              title={isMasked ? 'Reveal Balances' : 'Mask Balances'}
            >
              {isMasked ? <EyeOff size={15} style={{ color: 'var(--neon-amber)' }} /> : <Eye size={15} />}
            </button>

            {/* Action Button: New Transaction */}
            {onOpenNewTxn && (
              <button
                type="button"
                className="btn-futuristic-add"
                onClick={onOpenNewTxn}
              >
                <Plus size={15} />
                <span>Entry</span>
              </button>
            )}

            {/* User Profile Emblem */}
            <div className="profile-menu-wrapper" ref={profileRef}>
              <button
                type="button"
                className="user-avatar-btn"
                onClick={() => setProfileOpen(!profileOpen)}
              >
                <div className="user-initials">
                  {(settings?.user_name || 'W').charAt(0).toUpperCase()}
                </div>
                <div className="avatar-status-ring" />
              </button>

              {profileOpen && (
                <div className="profile-dropdown-glass animate-pop">
                  <div className="dropdown-user-info">
                    <div className="user-name">{settings?.user_name || 'WealthOS User'}</div>
                    <div className="user-phone">{user?.phone || 'Connected Session'}</div>
                  </div>
                  <div className="dropdown-divider" />
                  <button
                    className="dropdown-item"
                    onClick={() => {
                      setProfileOpen(false);
                      navigate('/config');
                    }}
                  >
                    <Settings size={14} />
                    <span>System Settings</span>
                  </button>
                  <button
                    className="dropdown-item danger"
                    onClick={() => {
                      setProfileOpen(false);
                      if (confirm('Sign out of WealthOS?')) signOut();
                    }}
                  >
                    <LogOut size={14} />
                    <span>Disconnect Session</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* Floating Bottom Navigation Bar for Mobile Viewports */}
      <nav className="mobile-floating-hud">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = isItemActive(item.path);
          return (
            <button
              key={item.path}
              onClick={() => navigate(item.path)}
              className={`mobile-hud-btn ${isActive ? 'active' : ''}`}
            >
              <Icon size={18} />
              <span>{item.label}</span>
              {isActive && <div className="mobile-active-dot" />}
            </button>
          );
        })}
      </nav>
    </>
  );
}
