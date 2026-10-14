import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { useData } from '../../hooks/useData';
import {
  LayoutDashboard, Wallet, ScrollText, BarChart2, ListChecks,
  Bell, Settings, LogOut, X, Eye, EyeOff
} from 'lucide-react';

const NAV_ITEMS = [
  { path: '/', label: 'Dashboard', icon: LayoutDashboard },
  { path: '/accounts', label: 'Accounts', icon: Wallet },
  { path: '/ledger', label: 'Ledger', icon: ScrollText },
  { path: '/insights', label: 'Insights', icon: BarChart2 },
  { path: '/plans', label: 'Plans & Goals', icon: ListChecks },
  { path: '/notifications', label: 'Notifications', icon: Bell, badge: true },
];

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

export function Sidebar({ open, onClose }: SidebarProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, signOut } = useAuth();
  const { settings, notifications, updateSettings } = useData();
  const unreadCount = notifications.filter(n => !n.read).length;
  const masked = settings?.mask_amounts || false;

  const handleNav = (path: string) => {
    navigate(path);
    onClose();
  };

  return (
    <>
      {open && (
        <div
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 98 }}
          onClick={onClose}
        />
      )}

      <nav className={`sidebar ${open ? 'open' : ''}`}>
        {/* Logo */}
        <div className="sidebar-logo">
          <div className="sidebar-logo-icon">W</div>
          <span className="sidebar-logo-text">WealthOS</span>
          <button className="btn btn-ghost btn-icon" onClick={onClose} style={{ marginLeft: 'auto', width: 28, height: 28 }}>
            <X size={14} />
          </button>
        </div>

        {/* Nav */}
        <nav className="sidebar-nav">
          <div className="sidebar-section-label">Menu</div>
          {NAV_ITEMS.map(item => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;
            return (
              <button
                key={item.path}
                className={`nav-item ${isActive ? 'active' : ''}`}
                onClick={() => handleNav(item.path)}
              >
                <Icon size={18} className="nav-item-icon" />
                {item.label}
                {item.badge && unreadCount > 0 && (
                  <span className="nav-item-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
                )}
              </button>
            );
          })}

          <div className="sidebar-section-label" style={{ marginTop: 8 }}>Tools</div>
          <button className="nav-item" onClick={() => handleNav('/settings')}>
            <Settings size={18} className="nav-item-icon" />
            Settings
          </button>
          <button
            className="nav-item"
            onClick={() => updateSettings({ mask_amounts: !masked })}
          >
            {masked ? <Eye size={18} className="nav-item-icon" /> : <EyeOff size={18} className="nav-item-icon" />}
            {masked ? 'Show Amounts' : 'Privacy Shield'}
          </button>
        </nav>

        {/* User Footer */}
        <div className="sidebar-footer">
          <div className="sidebar-user">
            <div className="sidebar-user-avatar">
              {(settings?.user_name || 'W').charAt(0).toUpperCase()}
            </div>
            <div className="sidebar-user-info">
              <div className="sidebar-user-name">{settings?.user_name || 'WealthOS User'}</div>
              <div className="sidebar-user-phone">{user?.phone || 'No phone'}</div>
            </div>
            <button
              className="btn btn-ghost btn-icon"
              onClick={() => { if (confirm('Sign out?')) signOut(); }}
              title="Sign out"
              style={{ flexShrink: 0 }}
            >
              <LogOut size={15} />
            </button>
          </div>
          <div style={{ fontSize: '0.65rem', color: 'var(--text-muted)', textAlign: 'center', marginTop: 8, padding: '0 8px' }}>
            🔄 Real-time sync active • Web + Android
          </div>
        </div>
      </nav>
    </>
  );
}
