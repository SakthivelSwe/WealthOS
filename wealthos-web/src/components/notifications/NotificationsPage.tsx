import { useState } from 'react';
import { useData } from '../../hooks/useData';
import { Bell, CheckCheck, Trash2, X, Sparkles } from 'lucide-react';
import { Tilt3DCard } from '../shared/Tilt3DCard';
import { format } from 'date-fns';

const CATEGORY_ICONS: Record<string, string> = {
  CHECK_IN: '📝',
  BILL: '🧾',
  BUDGET: '📊',
  GOAL: '🎯',
  BACKUP: '☁️',
  SECURITY: '🔒',
  SYSTEM: '⚙️',
};

export function NotificationsPage() {
  const { notifications, markNotificationRead, markAllNotificationsRead } = useData();
  const [filter, setFilter] = useState<'all' | 'unread'>('all');

  const filtered = filter === 'unread' ? notifications.filter(n => !n.read) : notifications;
  const unreadCount = notifications.filter(n => !n.read).length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div>
            <h1 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
              Notification Telemetry
            </h1>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
              Budget thresholds, cloud sync events, and intelligent check-ins
            </p>
          </div>
          {unreadCount > 0 && <span className="neon-dot-badge" style={{ position: 'static' }}>{unreadCount}</span>}
        </div>

        {unreadCount > 0 && (
          <button className="btn btn-secondary btn-sm" onClick={markAllNotificationsRead}>
            <CheckCheck size={14} /> Mark All Read
          </button>
        )}
      </div>

      {/* Filter Pills */}
      <div style={{ display: 'flex', gap: 6, background: 'rgba(255, 255, 255, 0.04)', padding: 4, borderRadius: 'var(--radius-pill)', border: '1px solid var(--glass-border)', width: 'fit-content' }}>
        <button
          onClick={() => setFilter('all')}
          className={`btn btn-sm ${filter === 'all' ? 'btn-primary' : 'btn-ghost'}`}
          style={{ borderRadius: 'var(--radius-pill)', fontSize: '0.8rem', padding: '5px 16px' }}
        >
          All Activity ({notifications.length})
        </button>
        <button
          onClick={() => setFilter('unread')}
          className={`btn btn-sm ${filter === 'unread' ? 'btn-primary' : 'btn-ghost'}`}
          style={{ borderRadius: 'var(--radius-pill)', fontSize: '0.8rem', padding: '5px 16px' }}
        >
          Unread Only ({unreadCount})
        </button>
      </div>

      {/* Notification Stream */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {filtered.map(notif => (
          <div
            key={notif.id}
            className="cyber-card"
            style={{
              padding: '16px 20px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              borderLeft: notif.read ? '1px solid var(--glass-border)' : '3px solid var(--neon-cyan)',
              background: notif.read ? 'rgba(13, 19, 34, 0.6)' : 'rgba(13, 19, 34, 0.9)',
              borderRadius: 'var(--radius-md)',
              transition: 'transform 0.15s ease',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
              <div
                style={{
                  width: 42,
                  height: 42,
                  borderRadius: 12,
                  background: 'rgba(255, 255, 255, 0.05)',
                  fontSize: '1.25rem',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                {CATEGORY_ICONS[notif.category] || '🔔'}
              </div>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ fontWeight: 700, fontSize: '0.95rem', color: '#fff' }}>{notif.title}</span>
                  {!notif.read && <span className="badge badge-cyan">NEW</span>}
                </div>
                <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginTop: 2 }}>{notif.message}</div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', marginTop: 4 }}>
                  {format(new Date(notif.created_at), 'MMM d, h:mm a')}
                </div>
              </div>
            </div>

            {!notif.read && (
              <button
                className="btn btn-ghost btn-sm"
                onClick={() => markNotificationRead(notif.id)}
                title="Mark as read"
              >
                Dismiss
              </button>
            )}
          </div>
        ))}

        {filtered.length === 0 && (
          <div className="cyber-card" style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-tertiary)' }}>
            <Bell size={32} style={{ margin: '0 auto 12px auto', opacity: 0.4 }} />
            <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.1rem', fontWeight: 700 }}>All Caught Up</div>
            <p style={{ fontSize: '0.82rem', marginTop: 4 }}>No notifications pending in your inbox.</p>
          </div>
        )}
      </div>
    </div>
  );
}
