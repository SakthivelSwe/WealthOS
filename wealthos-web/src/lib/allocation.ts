/**
 * WealthOS Salary Allocation Engine
 * Port of Appendix B reference implementation from WEALTHOS_FINAL_SPEC.md v3 §5.3.
 * Zero floating-point representation. Pure integer minor units (paise).
 */

export type FrameworkType = 'SIX_JAR' | 'THREE_BOX' | 'FIFTY_THIRTY_TWENTY' | 'CUSTOM';

export interface FrameworkDefinition {
  type: FrameworkType;
  order: string[];
  bps: Record<string, number>;
  emergencyJar: string;
  sinkJar: string;
  reduceOrder: string[];
  savingsJars: string[];
  spendJar: string | null;
}

export const FRAMEWORKS: Record<FrameworkType, FrameworkDefinition> = {
  SIX_JAR: {
    type: 'SIX_JAR',
    order: ['NEEDS', 'FREEDOM_SIP', 'EDUCATION', 'PLAY', 'LONG_TERM', 'GIVING'],
    bps: {
      NEEDS: 5500,
      FREEDOM_SIP: 1000,
      EDUCATION: 1000,
      PLAY: 1000,
      LONG_TERM: 1000,
      GIVING: 500,
    },
    emergencyJar: 'LONG_TERM',
    sinkJar: 'FREEDOM_SIP',
    reduceOrder: ['PLAY', 'EDUCATION', 'GIVING'],
    savingsJars: ['FREEDOM_SIP', 'LONG_TERM'],
    spendJar: 'PLAY',
  },
  THREE_BOX: {
    type: 'THREE_BOX',
    order: ['NEEDS', 'EMERGENCY', 'BUFFER', 'MEDIUM_TERM', 'LONG_TERM_SIP'],
    bps: {
      NEEDS: 5000,
      EMERGENCY: 1000,
      BUFFER: 1000,
      MEDIUM_TERM: 1000,
      LONG_TERM_SIP: 2000,
    },
    emergencyJar: 'EMERGENCY',
    sinkJar: 'LONG_TERM_SIP',
    reduceOrder: ['MEDIUM_TERM', 'BUFFER'],
    savingsJars: ['EMERGENCY', 'MEDIUM_TERM', 'LONG_TERM_SIP'],
    spendJar: null,
  },
  FIFTY_THIRTY_TWENTY: {
    type: 'FIFTY_THIRTY_TWENTY',
    order: ['NEEDS', 'WANTS', 'SAVINGS'],
    bps: {
      NEEDS: 5000,
      WANTS: 3000,
      SAVINGS: 2000,
    },
    emergencyJar: 'SAVINGS',
    sinkJar: 'SAVINGS',
    reduceOrder: ['WANTS'],
    savingsJars: ['SAVINGS'],
    spendJar: 'WANTS',
  },
  CUSTOM: {
    type: 'CUSTOM',
    order: ['NEEDS', 'FREEDOM_SIP', 'EDUCATION', 'PLAY', 'LONG_TERM', 'GIVING'],
    bps: {
      NEEDS: 5500,
      FREEDOM_SIP: 1000,
      EDUCATION: 1000,
      PLAY: 1000,
      LONG_TERM: 1000,
      GIVING: 500,
    },
    emergencyJar: 'LONG_TERM',
    sinkJar: 'FREEDOM_SIP',
    reduceOrder: ['PLAY', 'EDUCATION', 'GIVING'],
    savingsJars: ['FREEDOM_SIP', 'LONG_TERM'],
    spendJar: 'PLAY',
  },
};

export interface AllocationParams {
  salaryPaise: number;
  framework?: FrameworkDefinition;
  baselinePaise?: number;
  emiPaise?: number;
  emergencyBalancePaise?: number;
  emergencyTargetPaise?: number;
  creepLock?: boolean;
  bufferPaise?: number;
  shiftPlayToLongTerm?: boolean;
  daysInCycle?: number;
}

export interface AllocationResult {
  jars: Record<string, number>;
  flags: string[];
  dailySafePaise: number;
  weeklySafePaise: number;
}

/**
 * Largest-remainder allocation. Produces integer amounts summing exactly to total.
 * Ties broken by jar order.
 */
export function largestRemainder(
  total: number,
  bps: Record<string, number>,
  order: string[]
): Record<string, number> {
  const base: Record<string, number> = {};
  const rem: Record<string, number> = {};
  let allocated = 0;

  for (const j of order) {
    const b = bps[j] || 0;
    base[j] = Math.floor((total * b) / 10000);
    rem[j] = (total * b) % 10000;
    allocated += base[j];
  }

  const left = total - allocated;

  const sortedJars = [...order].sort((a, b) => {
    const diff = (rem[b] || 0) - (rem[a] || 0);
    if (diff !== 0) return diff;
    return order.indexOf(a) - order.indexOf(b);
  });

  for (let i = 0; i < left; i++) {
    const j = sortedJars[i];
    base[j] = (base[j] || 0) + 1;
  }

  return base;
}

/**
 * Deterministic salary allocation conforming to WEALTHOS_FINAL_SPEC.md v3 §5.3 / Appendix B.
 */
export function allocate(params: AllocationParams): AllocationResult {
  const {
    salaryPaise: S,
    framework = FRAMEWORKS.SIX_JAR,
    baselinePaise = 0,
    emiPaise = 0,
    emergencyBalancePaise = 0,
    emergencyTargetPaise = baselinePaise * 7,
    creepLock = true,
    bufferPaise = 150000,
    shiftPlayToLongTerm = false,
    daysInCycle = 30,
  } = params;

  if (S < 0) throw new Error('Salary must be non-negative');
  if (daysInCycle < 1) throw new Error('Days in cycle must be at least 1');

  const order = framework.order;
  const bps = { ...framework.bps };
  const mandatory = baselinePaise + emiPaise;

  if (mandatory >= S) {
    const jars: Record<string, number> = {};
    for (const j of order) jars[j] = 0;
    jars['NEEDS'] = S;
    return { jars, flags: ['SHORTFALL'], dailySafePaise: 0, weeklySafePaise: 0 };
  }

  if (shiftPlayToLongTerm && framework.type === 'SIX_JAR') {
    bps['PLAY'] = (bps['PLAY'] || 1000) - 500;
    bps['LONG_TERM'] = (bps['LONG_TERM'] || 1000) + 500;
  }

  const shares = largestRemainder(S, bps, order);
  const needsShare = shares['NEEDS'] || 0;
  const needs = creepLock
    ? Math.max(mandatory, Math.min(needsShare, mandatory + bufferPaise))
    : Math.max(mandatory, needsShare);

  const jars: Record<string, number> = { ...shares };
  jars['NEEDS'] = needs;
  const flags: string[] = [];
  const full = emergencyBalancePaise >= emergencyTargetPaise;
  const diff = needsShare - needs;

  if (diff > 0) {
    const destJar = full ? framework.sinkJar : framework.emergencyJar;
    jars[destJar] = (jars[destJar] || 0) + diff;
  } else if (diff < 0) {
    let deficit = -diff;
    for (const j of framework.reduceOrder) {
      const cur = jars[j] || 0;
      const t = Math.min(cur, deficit);
      jars[j] = cur - t;
      deficit -= t;
    }
    if (deficit > 0) {
      flags.push('SAVINGS_REDUCED');
      const nonNeedsReversed = order.filter(x => x !== 'NEEDS').reverse();
      for (const j of nonNeedsReversed) {
        const cur = jars[j] || 0;
        const t = Math.min(cur, deficit);
        jars[j] = cur - t;
        deficit -= t;
      }
    }
  }

  if (full && framework.emergencyJar !== framework.sinkJar) {
    jars[framework.sinkJar] = (jars[framework.sinkJar] || 0) + (jars[framework.emergencyJar] || 0);
    jars[framework.emergencyJar] = 0;
  }

  const totalSavings = framework.savingsJars.reduce((sum, j) => sum + (jars[j] || 0), 0);
  if (totalSavings * 100 < 7 * S) {
    flags.push('BELOW_SAVINGS_FLOOR');
  }

  let daily = 0;
  let weekly = 0;
  const spendJar = framework.spendJar;
  if (spendJar) {
    const spendAmount = jars[spendJar] || 0;
    daily = Math.floor(spendAmount / daysInCycle);
    weekly = Math.floor((spendAmount * 7) / daysInCycle);
    if (spendAmount === 0) {
      flags.push('SPEND_JAR_EMPTY');
    }
  }

  // Invariant checks
  const sum = Object.values(jars).reduce((acc, v) => acc + v, 0);
  if (sum !== S) throw new Error(`Invariant violated: jars sum ${sum} != salary ${S}`);
  if (Object.values(jars).some(v => v < 0)) throw new Error('Invariant violated: negative jar value');
  if ((jars['NEEDS'] || 0) < mandatory) throw new Error(`Invariant violated: NEEDS ${jars['NEEDS']} < mandatory ${mandatory}`);

  return { jars, flags, dailySafePaise: daily, weeklySafePaise: weekly };
}
