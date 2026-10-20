import { useState, useMemo } from 'react';
import { getCategoryDisplayName, getCategoryEmoji, type Category, type TxnType } from '../../types';
import { Search, X, Check, ChevronDown, Sparkles } from 'lucide-react';

interface CategoryPickerProps {
  type: TxnType;
  value: string;
  onChange: (categoryId: string) => void;
  categories: Category[];
}

export function CategoryPicker({ type, value, onChange, categories }: CategoryPickerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');

  // Deduplicate and filter categories for current type
  const availableCategories = useMemo(() => {
    const list = categories.filter(c => c.kind === type && !c.archived);
    const seen = new Set<string>();
    return list.filter(c => {
      if (c.id === 'cat_salary' && list.some(o => o.id === 'cat_income_0')) return false;
      const label = getCategoryDisplayName(c.id, categories).trim();
      if (seen.has(label)) return false;
      seen.add(label);
      return true;
    });
  }, [categories, type]);

  // Curated quick-select popular categories for rapid 1-tap entry in India
  const quickPillIds = useMemo(() => {
    if (type === 'INCOME') {
      return ['cat_income_0', 'cat_income_1', 'cat_income_2', 'cat_income_3'];
    }
    return [
      'cat_bills_rent',
      'cat_food_groceries',
      'cat_transport_fuel',
      'cat_food_restaurant',
      'cat_bills_electricity',
      'cat_investment',
      'cat_bills_subscriptions',
      'cat_shopping',
      'cat_loan_emi',
      'cat_health',
    ];
  }, [type]);

  const quickPillCategories = useMemo(() => {
    return quickPillIds
      .map(id => availableCategories.find(c => c.id === id))
      .filter((c): c is Category => Boolean(c));
  }, [quickPillIds, availableCategories]);

  // Filtered list for the modal search
  const filteredModalCategories = useMemo(() => {
    if (!search.trim()) return availableCategories;
    const q = search.toLowerCase();
    return availableCategories.filter(c => {
      const name = getCategoryDisplayName(c.id, categories).toLowerCase();
      return name.includes(q) || c.id.toLowerCase().includes(q);
    });
  }, [availableCategories, search, categories]);

  const selectedLabel = value ? getCategoryDisplayName(value, categories) : '';
  const selectedEmoji = value ? getCategoryEmoji(value, categories) : '';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {/* 1-Tap Quick Select Pills */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 7, fontSize: '0.78rem', color: 'var(--text-tertiary)', fontWeight: 600 }}>
          <Sparkles size={13} style={{ color: 'var(--neon-cyan)' }} />
          <span>Quick 1-Tap Select:</span>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {quickPillCategories.map(cat => {
            const isSelected = value === cat.id;
            const label = getCategoryDisplayName(cat.id, categories);
            return (
              <button
                key={cat.id}
                type="button"
                onClick={() => onChange(cat.id)}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '5px 11px',
                  borderRadius: 'var(--radius-pill)',
                  fontSize: '0.82rem',
                  fontWeight: isSelected ? 700 : 500,
                  cursor: 'pointer',
                  border: isSelected
                    ? '1.5px solid var(--neon-cyan)'
                    : '1px solid rgba(255, 255, 255, 0.08)',
                  background: isSelected
                    ? 'rgba(0, 240, 255, 0.18)'
                    : 'rgba(255, 255, 255, 0.04)',
                  color: isSelected ? '#ffffff' : 'var(--text-secondary)',
                  boxShadow: isSelected ? '0 0 12px rgba(0, 240, 255, 0.35)' : 'none',
                  transition: 'all 0.18s ease',
                }}
              >
                <span>{label}</span>
                {isSelected && <Check size={13} style={{ color: 'var(--neon-cyan)' }} />}
              </button>
            );
          })}
        </div>
      </div>

      {/* Selected Box / Browse Button */}
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <button
          type="button"
          onClick={() => setIsOpen(true)}
          style={{
            flex: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '11px 14px',
            borderRadius: 'var(--radius-md)',
            border: value ? '1.5px solid var(--neon-cyan)' : '1px solid var(--glass-border)',
            background: value ? 'rgba(0, 240, 255, 0.06)' : 'rgba(255, 255, 255, 0.03)',
            color: value ? 'var(--text-primary)' : 'var(--text-tertiary)',
            cursor: 'pointer',
            textAlign: 'left',
            transition: 'border 0.2s, background 0.2s',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
            {value ? (
              <>
                <span style={{ fontSize: '1.25rem', lineHeight: 1 }}>{selectedEmoji}</span>
                <span style={{ fontWeight: 700, fontSize: '0.92rem', color: '#f8fafc', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {selectedLabel}
                </span>
                <span style={{ fontSize: '0.72rem', padding: '2px 8px', borderRadius: 999, background: 'rgba(0, 240, 255, 0.15)', color: 'var(--neon-cyan)', fontWeight: 600 }}>
                  Selected
                </span>
              </>
            ) : (
              <span style={{ fontSize: '0.88rem' }}>👉 Click to browse all categories with emojis...</span>
            )}
          </div>
          <ChevronDown size={16} style={{ color: 'var(--text-tertiary)', flexShrink: 0 }} />
        </button>

        {value && (
          <button
            type="button"
            onClick={() => onChange('')}
            title="Clear category"
            style={{
              padding: '10px 12px',
              borderRadius: 'var(--radius-md)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              background: 'rgba(255, 255, 255, 0.04)',
              color: 'var(--text-tertiary)',
              cursor: 'pointer',
            }}
          >
            <X size={16} />
          </button>
        )}
      </div>

      {/* Visual Category Grid Modal */}
      {isOpen && (
        <div
          className="modal-overlay"
          style={{ zIndex: 1200 }}
          onClick={() => setIsOpen(false)}
        >
          <div
            className="modal-content"
            style={{ maxWidth: 620, maxHeight: '85vh', display: 'flex', flexDirection: 'column', gap: 16 }}
            onClick={e => e.stopPropagation()}
          >
            {/* Modal Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <h3 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, margin: 0 }}>
                  Choose {type === 'INCOME' ? 'Income' : 'Expense'} Category
                </h3>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)', margin: '2px 0 0 0' }}>
                  Click any category to select it instantly
                </p>
              </div>
              <button
                type="button"
                onClick={() => setIsOpen(false)}
                className="btn-ghost"
                style={{ padding: 6, borderRadius: '50%' }}
              >
                <X size={18} />
              </button>
            </div>

            {/* Live Search Bar */}
            <div style={{ position: 'relative' }}>
              <Search
                size={16}
                style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-tertiary)' }}
              />
              <input
                type="text"
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder="Search category (e.g. food, fuel, rent, eb, salary)..."
                autoFocus
                style={{
                  width: '100%',
                  paddingLeft: 38,
                  paddingRight: 36,
                  borderRadius: 'var(--radius-md)',
                  background: 'rgba(255, 255, 255, 0.04)',
                  border: '1px solid var(--glass-border)',
                }}
              />
              {search && (
                <button
                  type="button"
                  onClick={() => setSearch('')}
                  style={{ position: 'absolute', right: 10, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', color: 'var(--text-tertiary)', cursor: 'pointer' }}
                >
                  <X size={14} />
                </button>
              )}
            </div>

            {/* Visual Grid */}
            <div style={{ overflowY: 'auto', maxHeight: '55vh', paddingRight: 4, display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(170px, 1fr))', gap: 10 }}>
              {filteredModalCategories.map(cat => {
                const isSelected = value === cat.id;
                const displayName = getCategoryDisplayName(cat.id, categories);
                const emoji = getCategoryEmoji(cat.id, categories);
                // Clean name without leading emoji for card title
                const cleanName = displayName.replace(/^\p{Extended_Pictographic}\s*/u, '');

                return (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => {
                      onChange(cat.id);
                      setIsOpen(false);
                    }}
                    style={{
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'flex-start',
                      padding: '12px 14px',
                      borderRadius: 'var(--radius-md)',
                      textAlign: 'left',
                      cursor: 'pointer',
                      border: isSelected ? '1.5px solid var(--neon-cyan)' : '1px solid var(--glass-border)',
                      background: isSelected ? 'rgba(0, 240, 255, 0.16)' : 'rgba(255, 255, 255, 0.03)',
                      boxShadow: isSelected ? '0 0 14px rgba(0, 240, 255, 0.25)' : 'none',
                      transition: 'all 0.16s ease',
                      position: 'relative',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center', marginBottom: 6 }}>
                      <span style={{ fontSize: '1.6rem', lineHeight: 1 }}>{emoji}</span>
                      {isSelected && (
                        <div style={{ width: 18, height: 18, borderRadius: '50%', background: 'var(--neon-cyan)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          <Check size={12} color="#000" strokeWidth={3} />
                        </div>
                      )}
                    </div>
                    <span style={{ fontSize: '0.86rem', fontWeight: 700, color: '#f8fafc', lineHeight: 1.25 }}>
                      {cleanName}
                    </span>
                    {cat.need_want && (
                      <span style={{ fontSize: '0.68rem', color: 'var(--text-tertiary)', marginTop: 4, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                        {cat.need_want}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>

            {filteredModalCategories.length === 0 && (
              <div style={{ textAlign: 'center', padding: '30px 10px', color: 'var(--text-tertiary)', fontSize: '0.88rem' }}>
                No categories matching "{search}"
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
