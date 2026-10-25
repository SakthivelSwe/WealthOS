/**
 * WealthOS Payday Cycle & Schedule Engine
 * Conforms to WEALTHOS_FINAL_SPEC.md v3 §2.3, §2.4, and §5.4.
 */

export type IncomeScheduleType = 'VARIABLE_WINDOW' | 'FIXED_DATE' | 'IRREGULAR';

export interface CycleDefinition {
  scheduleType: IncomeScheduleType;
  paydayWindowStartDay?: number;
  paydayWindowEndDay?: number;
  fixedPaydayOfMonth?: number;
  irregularHorizonDays?: number;
}

export function isWithinPaydayWindow(date: Date, definition: CycleDefinition): boolean {
  const day = date.getDate();
  switch (definition.scheduleType) {
    case 'VARIABLE_WINDOW': {
      const start = definition.paydayWindowStartDay ?? 2;
      const end = definition.paydayWindowEndDay ?? 5;
      return day >= start && day <= end;
    }
    case 'FIXED_DATE': {
      return day === (definition.fixedPaydayOfMonth ?? 1);
    }
    case 'IRREGULAR':
      return false;
  }
}

/**
 * Calculates days in cycle between cycle start and next expected payday.
 * Falls back to 30 if unknown.
 */
export function calculateDaysInCycle(
  cycleStartDate: Date,
  definition: CycleDefinition,
  explicitNextPayday?: Date
): number {
  if (explicitNextPayday) {
    const diffTime = explicitNextPayday.getTime() - cycleStartDate.getTime();
    const days = Math.round(diffTime / (1000 * 60 * 60 * 24));
    return days >= 1 ? days : 30;
  }

  switch (definition.scheduleType) {
    case 'VARIABLE_WINDOW': {
      const start = definition.paydayWindowStartDay ?? 2;
      const nextMonth = new Date(cycleStartDate.getFullYear(), cycleStartDate.getMonth() + 1, start);
      const diffTime = nextMonth.getTime() - cycleStartDate.getTime();
      const days = Math.round(diffTime / (1000 * 60 * 60 * 24));
      return days >= 1 ? days : 30;
    }
    case 'FIXED_DATE': {
      const day = definition.fixedPaydayOfMonth ?? 1;
      const nextMonth = new Date(cycleStartDate.getFullYear(), cycleStartDate.getMonth() + 1, day);
      const diffTime = nextMonth.getTime() - cycleStartDate.getTime();
      const days = Math.round(diffTime / (1000 * 60 * 60 * 24));
      return days >= 1 ? days : 30;
    }
    case 'IRREGULAR':
      return definition.irregularHorizonDays ?? 30;
  }
}
