# WealthOS "Quiet Wealth" Design System

**Specification Version**: 2.5.0  
**Design Philosophy**: "Quiet Wealth" — Restrained, Editorial, High-Integrity Personal Finance  
**Platforms**: Web (`wealthos-web/src/index.css`) & Android Jetpack Compose (`app.privatemoney.ui.theme`)

---

## 1. Visual Direction & Aesthetic Philosophy

Personal financial management software requires an atmosphere of **clarity, discretion, and quiet confidence**. Gratuitous visual noise—such as neon gradients, rainbow cards, holographic drop shadows, and oversized decorative icons—distracts from the user's primary goal: understanding and stewarding their wealth.

The **Quiet Wealth** design aesthetic enforces:
- **Calm, High-Legibility Canvas**: Deep graphite/obsidian surfaces in dark mode; warm paper alabaster surfaces in light mode.
- **Single Authoritative Accent**: A tailored, restrained forest-emerald (`#10b981` dark / `#059669` light) acting as the focal point for primary actions and active states.
- **Muted Semantic Accents**: Income green, expense crimson, and warning amber are desaturated and contextual, avoiding visual panic while maintaining accessibility standards (WCAG AAA for body text, AA for charts/meters).
- **Subtle Structural Hierarchy**: Fine 1px borders and soft ambient elevation shadows replace decorative neon glows and heavy frosted glass blur filters.
- **Strict Motion Restraint**: Micro-animations are fast (150ms-250ms), subtle, and immediately respect `prefers-reduced-motion: reduce`.

---

## 2. Core Semantic Design Tokens

### 2.1 Dark Mode Palette (Default Canvas)

```css
:root {
  /* Canvas Backgrounds */
  --bg-cosmos: #090c10;      /* Application viewport root */
  --bg-primary: #0d1117;     /* Primary screen canvas */
  --bg-secondary: #131922;   /* Secondary card / bento surface */
  --bg-tertiary: #18202c;    /* Deep inset or secondary well */
  --bg-elevated: #202b3b;    /* Popovers, modals, dropdowns */

  /* Structural Surfaces & Borders */
  --surface-base: #131922;
  --surface-elevated: #18202c;
  --surface-hover: #202b3b;
  --surface-active: #28364a;
  --border-subtle: rgba(255, 255, 255, 0.07);
  --border-default: rgba(255, 255, 255, 0.12);
  --border-strong: rgba(255, 255, 255, 0.20);

  /* Primary Editorial Accent — Forest Emerald */
  --accent-forest: #10b981;
  --accent-forest-hover: #059669;
  --accent-forest-fg: #ffffff;
  --accent-forest-dim: rgba(16, 185, 129, 0.12);

  /* Semantic UI Tokens */
  --semantic-income: #10b981;
  --semantic-income-bg: rgba(16, 185, 129, 0.12);
  --semantic-expense: #f43f5e;
  --semantic-expense-bg: rgba(244, 63, 94, 0.12);
  --semantic-warning: #f59e0b;
  --semantic-warning-bg: rgba(245, 158, 11, 0.12);
  --semantic-info: #0ea5e9;
  --semantic-info-bg: rgba(14, 165, 233, 0.12);

  /* Typography Colors */
  --text-primary: #f8fafc;   /* 95% White - Headlines and amounts */
  --text-secondary: #94a3b8; /* Slate 400 - Field labels, subheadings */
  --text-tertiary: #64748b;  /* Slate 500 - Timestamps, helper text */
  --text-muted: #475569;     /* Slate 600 - Inactive icons, subtle captions */

  /* Shadows */
  --shadow-3d-sm: 0 1px 3px 0 rgba(0, 0, 0, 0.35), 0 0 0 1px var(--border-subtle);
  --shadow-3d-md: 0 4px 12px -2px rgba(0, 0, 0, 0.45), 0 0 0 1px var(--border-subtle);
  --shadow-3d-lg: 0 12px 28px -4px rgba(0, 0, 0, 0.60), 0 0 0 1px var(--border-default);
}
```

### 2.2 Light Mode Palette (Warm Paper / Alabaster)

```css
[data-theme="light"] {
  /* Canvas Backgrounds */
  --bg-cosmos: #f8fafc;
  --bg-primary: #ffffff;
  --bg-secondary: #f1f5f9;
  --bg-tertiary: #e2e8f0;
  --bg-elevated: #cbd5e1;

  /* Surfaces & Structural Borders */
  --surface-base: #ffffff;
  --surface-elevated: #f8fafc;
  --surface-hover: #f1f5f9;
  --surface-active: #e2e8f0;
  --border-subtle: rgba(0, 0, 0, 0.06);
  --border-default: rgba(0, 0, 0, 0.10);
  --border-strong: rgba(0, 0, 0, 0.16);

  /* Primary Editorial Accent — Restrained Forest Green */
  --accent-forest: #059669;
  --accent-forest-hover: #047857;
  --accent-forest-fg: #ffffff;
  --accent-forest-dim: rgba(5, 150, 105, 0.10);

  /* Semantic UI Tokens */
  --semantic-income: #059669;
  --semantic-income-bg: rgba(5, 150, 105, 0.10);
  --semantic-expense: #e11d48;
  --semantic-expense-bg: rgba(225, 29, 72, 0.10);
  --semantic-warning: #d97706;
  --semantic-warning-bg: rgba(217, 119, 6, 0.10);

  /* Typography Colors */
  --text-primary: #0f172a;
  --text-secondary: #475569;
  --text-tertiary: #64748b;
  --text-muted: #94a3b8;

  /* Shadows */
  --shadow-3d-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05), 0 0 0 1px var(--border-subtle);
  --shadow-3d-md: 0 4px 6px -1px rgba(0, 0, 0, 0.08), 0 0 0 1px var(--border-subtle);
  --shadow-3d-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.10), 0 0 0 1px var(--border-default);
}
```

---

## 3. Typography & Spacing Scale

### 3.1 Type Hierarchy
- **Brand & Numerical Hero Display**: `Outfit` (`font-weight: 700`, `letter-spacing: -0.02em`)
  - Hero Numbers: `2.25rem` (36px) / `line-height: 1.1`
  - Section Headlines: `1.25rem` (20px) / `line-height: 1.3`
  - Subheadings: `1.00rem` (16px) / `line-height: 1.4`
- **Body & Financial Metadata**: `Plus Jakarta Sans` (`font-weight: 400`, `500`, `600`)
  - Body Base: `0.9375rem` (15px) / `line-height: 1.5`
  - Small / Table Cells: `0.8125rem` (13px) / `line-height: 1.4`
  - Micro / Captions: `0.75rem` (12px) / `line-height: 1.3`
- **Tabular Data & Transaction Codes**: `JetBrains Mono` (`font-weight: 500`)
  - Used for raw account numbers, timestamps, and balance reconciliation diffs.

### 3.2 Spacing & Radii Scale
- **Corner Radii**:
  - Tags / Chips: `var(--radius-xs)` = `4px`
  - Inputs / Buttons: `var(--radius-sm)` = `8px`
  - Standard Cards / Bento Tiles: `var(--radius-md)` = `12px`
  - Modal Sheets / Dialogs: `var(--radius-lg)` = `16px`
  - Pills / HUD Nav: `var(--radius-pill)` = `9999px`
- **Grid Layout**: 12-column responsive bento grid with `16px` gutters on desktop, collapsing smoothly to `12px` gutters on mobile.

---

## 4. Component Standards

### 4.1 Primary Action Buttons (`.btn-primary` / `.btn-futuristic-add`)
- Background: `var(--accent-forest)`.
- Border: `1px solid rgba(255, 255, 255, 0.15)`.
- Shadow: `0 2px 8px var(--accent-forest-dim)`.
- Hover State: Darkens to `var(--accent-forest-hover)` with `translateY(-1px)`. No rainbow outer-glows.

### 4.2 Bento & Financial Metric Cards (`.cyber-card`)
- Background: `var(--glass-surface)` (`rgba(19, 25, 34, 0.88)`).
- Border: `1px solid var(--border-subtle)`.
- Ambient Glow: Explicitly disabled (`.cyber-card-glow { display: none; }`).
- Hover: Soft border intensification to `var(--border-default)`.

### 4.3 Floating Navigation Bar (`.floating-nav-island`)
- Capsule-style floating navigation island with 44px touch targets.
- Responsive collapse: On tablet/mobile viewports (`max-width: 1100px`), brand text and keyboard shortcut chips automatically hide, preserving pure icon navigation with zero horizontal overflow.

---

## 5. Accessibility & Motion Compliance

1. **Reduced Motion**:
   All CSS keyframe animations, 3D translations, and smooth transitions are suppressed when the user prefers reduced motion:
   ```css
   @media (prefers-reduced-motion: reduce) {
     *, *::before, *::after {
       animation-duration: 0.01ms !important;
       animation-iteration-count: 1 !important;
       transition-duration: 0.01ms !important;
       scroll-behavior: auto !important;
     }
   }
   ```
2. **Contrast Invariants**:
   - Primary text (`#f8fafc`) on dark canvas (`#090c10`) achieves an **18.2:1 contrast ratio**, well exceeding the 7:1 WCAG AAA requirement.
   - Primary text (`#0f172a`) on light canvas (`#f8fafc`) achieves a **15.4:1 contrast ratio**.
   - Semantic badges use tinted background pills (`12% opacity`) with high-contrast foreground text to ensure accessibility without overwhelming the eye.

---

## 6. Cross-Platform Alignment & Verified Audits

### 6.1 Android Jetpack Compose (`app.privatemoney.ui.theme.Theme.kt`)
- **Primary Accent Alignment**: Updated Android `primary` from neon cyber green (`0xFF00E599`) to Quiet Wealth Forest Emerald (`0xFF10B981`), with subdued surface glows (`0x3310B981`).
- **Surface Tone**: Material 3 dark color scheme utilizes Obsidian/Graphite surfaces (`0xFF0D1117`, `0xFF131922`, `0xFF18202C`) matching web `--bg-primary` and `--bg-secondary`.
- **Typography**: Numeric weights use tabular numbers with aligned decimal points.

### 6.2 Motion Restraint in Web (`Canvas3DBackground.tsx`, `Tilt3DCard.tsx`)
- **Canvas3DBackground**:
  - Respects `window.matchMedia('(prefers-reduced-motion: reduce)')` and mobile viewports (`window.innerWidth < 768`).
  - When active, the continuous 60fps Three.js animation loop is cancelled; a static, calm ambient canvas is rendered with zero CPU/GPU overhead.
- **Tilt3DCard**:
  - Evaluates `prefers-reduced-motion` and touch devices. 3D transform perspectives and holographic gleams are disabled to ensure calm, stable rendering.

### 6.3 Accessible Modal Dialogs vs Browser Prompts (`SettingsPage.tsx`)
- Completely eliminated raw browser `window.prompt()` for backup export and vault unlock.
- **Export Vault Modal**: Accessible dialog with explicit passphrase input, confirmation field, strength validation, and clear zero-knowledge notice.
- **Unlock Vault Modal**: Secure in-memory passphrase entry with show/hide password toggle.
- **Ingestion Preview Modal**: Pre-import staging preview displaying verified counts of accounts, transactions, journal legs, budgets, goals, debts, and recurring items before applying changes.
- **WCAG Compliance**: All modals trap focus, provide explicit ARIA roles (`role="dialog"`, `aria-modal="true"`), support `Escape` key dismissal, and offer high-contrast touch targets (min 44x44px).
