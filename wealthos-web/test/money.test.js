import test from 'node:test';
import assert from 'node:assert/strict';
import { parseMoney, formatIndian, roundHalfUp, calculatePercentage } from '../src/lib/money.js';

test('money.ts: parseMoney integer-only parsing', () => {
  assert.equal(parseMoney('₹ 1,23,970'), 12397000);
  assert.equal(parseMoney('₹ 1,23,970.50'), 12397050);
  assert.equal(parseMoney('INR 1,23,970.50'), 12397050);
  assert.equal(parseMoney('12,500'), 1250000);
  assert.equal(parseMoney('12,500 Dr'), -1250000);
  assert.equal(parseMoney('12,500 Cr'), 1250000);
  assert.equal(parseMoney('(500.00)'), -50000);
  assert.equal(parseMoney('-500.00'), -50000);
  assert.equal(parseMoney('-'), 0);
  assert.equal(parseMoney(''), 0);
  assert.equal(parseMoney(null), 0);
  assert.equal(parseMoney(undefined), 0);
  assert.equal(parseMoney(12345), 12345);
});

test('money.ts: formatIndian digit grouping', () => {
  assert.equal(formatIndian(12397000), '₹1,23,970');
  assert.equal(formatIndian(12397050, 2, true), '₹1,23,970.50');
  assert.equal(formatIndian(50000), '₹500');
  assert.equal(formatIndian(100000), '₹1,000');
  assert.equal(formatIndian(1234567800), '₹1,23,45,678');
  assert.equal(formatIndian(-50000), '-₹500');
});

test('money.ts: roundHalfUp and calculatePercentage', () => {
  assert.equal(roundHalfUp(5, 2), 3);
  assert.equal(roundHalfUp(4, 2), 2);
  assert.equal(roundHalfUp(1, 2), 1);
  assert.equal(roundHalfUp(0, 2), 0);

  assert.equal(calculatePercentage(20, 100), 20);
  assert.equal(calculatePercentage(7, 100), 7);
  assert.equal(calculatePercentage(0, 100), 0);
  assert.equal(calculatePercentage(10, 0), 0);
});
