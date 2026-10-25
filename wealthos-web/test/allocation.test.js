import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { allocate, FRAMEWORKS } from '../src/lib/allocation.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

test('Appendix A.1 & A.2: Allocation Golden Values', async (t) => {
  const vectorPath = path.resolve(__dirname, '../../shared-test-vectors/allocation.json');
  const raw = fs.readFileSync(vectorPath, 'utf8');
  const vectors = JSON.parse(raw);
  const defaults = vectors.defaults;

  for (const c of vectors.cases) {
    await t.test(c.name, () => {
      const fwKey = c.framework || defaults.framework;
      const fw = FRAMEWORKS[fwKey];
      assert.ok(fw, `Framework ${fwKey} must exist`);

      const res = allocate({
        salaryPaise: c.salaryPaise,
        framework: fw,
        baselinePaise: c.baselinePaise ?? defaults.baselinePaise,
        emiPaise: c.emiPaise ?? defaults.emiPaise,
        emergencyBalancePaise: c.emergencyBalancePaise ?? defaults.emergencyBalancePaise,
        emergencyTargetPaise: (c.baselinePaise ?? defaults.baselinePaise) * defaults.emergencyTargetMonths,
        creepLock: c.creepLock ?? defaults.creepLock,
        bufferPaise: c.bufferPaise ?? defaults.bufferPaise,
        shiftPlayToLongTerm: c.shiftPlayToLongTerm ?? defaults.shiftPlayToLongTerm,
        daysInCycle: c.daysInCycle ?? defaults.daysInCycle,
      });

      // Assert jars
      for (const [jarName, expectedPaise] of Object.entries(c.expectedJars)) {
        assert.equal(
          res.jars[jarName],
          expectedPaise,
          `Case [${c.id}] jar ${jarName}: expected ${expectedPaise}, got ${res.jars[jarName]}`
        );
      }

      // Assert daily & weekly
      assert.equal(res.dailySafePaise, c.expectedDailyPaise, `Case [${c.id}] dailySafePaise mismatch`);
      assert.equal(res.weeklySafePaise, c.expectedWeeklyPaise, `Case [${c.id}] weeklySafePaise mismatch`);

      // Assert flags
      assert.deepEqual(res.flags, c.expectedFlags, `Case [${c.id}] flags mismatch`);
    });
  }
});

test('Allocation Invariants Property Test over 10,000 randomized cases', () => {
  const frameworkList = [FRAMEWORKS.SIX_JAR, FRAMEWORKS.THREE_BOX, FRAMEWORKS.FIFTY_THIRTY_TWENTY];

  for (let i = 0; i < 10000; i++) {
    const fw = frameworkList[i % frameworkList.length];
    const salary = Math.floor(Math.random() * 20000000) + 100000; // ₹1,000 to ₹2,00,000
    const baseline = Math.floor(Math.random() * ((salary * 13) / 10));
    const emi = Math.floor(Math.random() * (salary / 2));
    const emgBal = Math.floor(Math.random() * (salary * 10));
    const emgTarget = Math.floor(Math.random() * (salary * 10));
    const creep = Math.random() > 0.5;
    const buffer = Math.floor(Math.random() * 500000);
    const shift = Math.random() > 0.5;
    const days = Math.floor(Math.random() * 34) + 1;

    const res = allocate({
      salaryPaise: salary,
      framework: fw,
      baselinePaise: baseline,
      emiPaise: emi,
      emergencyBalancePaise: emgBal,
      emergencyTargetPaise: emgTarget,
      creepLock: creep,
      bufferPaise: buffer,
      shiftPlayToLongTerm: shift,
      daysInCycle: days,
    });

    const sum = Object.values(res.jars).reduce((acc, v) => acc + v, 0);
    assert.equal(sum, salary, `Sum must equal salary on case ${i}`);
    assert.ok(Object.values(res.jars).every(v => v >= 0), `All jars must be >= 0 on case ${i}`);
    const mandatory = baseline + emi;
    if (mandatory >= salary) {
      assert.ok(res.flags.includes('SHORTFALL'));
      assert.equal(res.jars['NEEDS'], salary);
    } else {
      assert.ok(res.jars['NEEDS'] >= mandatory, `NEEDS must be >= mandatory on case ${i}`);
    }
  }
});
