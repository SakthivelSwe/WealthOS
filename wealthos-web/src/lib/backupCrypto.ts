/**
 * WealthOS Encrypted Backup & Restore Cryptographic Engine (.wosenc)
 *
 * Implements binary wire format parity with Android CloudSyncService.kt:
 * [4 bytes MAGIC "WOSB"]
 * [4 bytes VERSION = 1 (Big-Endian)]
 * [4 bytes METADATA_LEN (Big-Endian)]
 * [METADATA_BYTES (UTF-8 JSON)]
 * [16 bytes SALT]
 * [12 bytes IV]
 * [CIPHERTEXT + 16 byte AES-GCM Tag]
 *
 * Key Derivation: PBKDF2 with HMAC-SHA256, 100,000 iterations, 256-bit AES key.
 * AEAD Authentication: Associated Authenticated Data (AAD) bound to METADATA_BYTES.
 * Payload Checksum: SHA-256 integrity hash of raw JSON bytes.
 */

export interface BackupMetadata {
  version: number;
  appVersion: string;
  timestamp: number;
  accountCount: number;
  txnCount: number;
  goalCount: number;
  debtCount: number;
  chitFundCount: number;
  recurringCount: number;
  checksumSha256: string;
}

export interface BackupPayload {
  accounts: any[];
  categories: any[];
  txns: any[];
  journal_entries?: any[];
  goals: any[];
  debts: any[];
  chits: any[];
  recurring: any[];
  budgets?: any[];
  version?: string;
  exportedAt?: string;
  [key: string]: any;
}

const MAGIC = new Uint8Array([0x57, 0x4f, 0x53, 0x42]); // "WOSB"
const CURRENT_VERSION = 1;
const MIN_ENVELOPE_SIZE = 58;
const MAX_ENVELOPE_SIZE = 50 * 1024 * 1024; // 50 MB
const MAX_METADATA_SIZE = 64 * 1024; // 64 KB

/**
 * Calculates SHA-256 hex string using Web Crypto.
 */
async function sha256Hex(data: Uint8Array): Promise<string> {
  const hashBuffer = await crypto.subtle.digest('SHA-256', data as unknown as BufferSource);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
}

/**
 * Derives an AES-GCM-256 CryptoKey using PBKDF2WithHmacSHA256 (100,000 iterations).
 * Strict security requirement: Empty or whitespace passphrases are strictly rejected.
 */
async function deriveAesKey(passphrase: string, salt: Uint8Array): Promise<CryptoKey> {
  if (!passphrase || passphrase.trim().length === 0) {
    throw new Error('A secure passphrase is required. Blank passphrases are not permitted.');
  }

  const enc = new TextEncoder();
  const passBytes = enc.encode(passphrase.trim());
  const baseKey = await crypto.subtle.importKey(
    'raw',
    passBytes as unknown as BufferSource,
    { name: 'PBKDF2' },
    false,
    ['deriveKey']
  );

  return crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt: salt as unknown as BufferSource,
      iterations: 100_000,
      hash: 'SHA-256',
    },
    baseKey,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt', 'decrypt']
  );
}

/**
 * Normalizes accounts, transactions, journal entries, and horizons to canonical bilingual (camelCase + snake_case) wire format.
 */
export function normalizePayloadForExport(payload: BackupPayload) {
  const accounts = (payload.accounts || []).map((a: any) => ({
    id: a.id,
    name: a.name,
    type: a.type,
    institution: a.institution ?? null,
    lastFour: a.lastFour ?? a.last_four ?? null,
    last_four: a.lastFour ?? a.last_four ?? null,
    openingBalanceMinor: Number(a.openingBalanceMinor ?? a.opening_balance_minor ?? 0),
    opening_balance_minor: Number(a.openingBalanceMinor ?? a.opening_balance_minor ?? 0),
    currency: a.currency || 'INR',
    includeInNetWorth: Boolean(a.includeInNetWorth ?? a.include_in_net_worth ?? true),
    include_in_net_worth: Boolean(a.includeInNetWorth ?? a.include_in_net_worth ?? true),
    includeInStats: Boolean(a.includeInStats ?? a.include_in_stats ?? true),
    include_in_stats: Boolean(a.includeInStats ?? a.include_in_stats ?? true),
    archived: Boolean(a.archived ?? false),
    createdAt: Number(a.createdAt ?? a.created_at ?? Date.now()),
    created_at: Number(a.createdAt ?? a.created_at ?? Date.now()),
    updatedAt: Number(a.updatedAt ?? a.updated_at ?? Date.now()),
    updated_at: Number(a.updatedAt ?? a.updated_at ?? Date.now()),
  }));

  const categories = (payload.categories || []).map((c: any) => ({
    id: c.id,
    parentId: c.parentId ?? c.parent_id ?? null,
    parent_id: c.parentId ?? c.parent_id ?? null,
    name: c.name,
    kind: c.kind,
    needWant: c.needWant ?? c.need_want ?? null,
    need_want: c.needWant ?? c.need_want ?? null,
    icon: c.icon || 'default',
    color: Number(c.color || 0),
    archived: Boolean(c.archived ?? false),
    sortOrder: Number(c.sortOrder ?? c.sort_order ?? 0),
    sort_order: Number(c.sortOrder ?? c.sort_order ?? 0),
  }));

  const txns = (payload.txns || []).map((t: any) => ({
    id: t.id,
    type: t.type,
    amountMinor: Number(t.amountMinor ?? t.amount_minor ?? 0),
    amount_minor: Number(t.amountMinor ?? t.amount_minor ?? 0),
    currency: t.currency || 'INR',
    localDate: t.localDate ?? t.local_date ?? new Date().toISOString().slice(0, 10),
    local_date: t.localDate ?? t.local_date ?? new Date().toISOString().slice(0, 10),
    localTime: t.localTime ?? t.local_time ?? '00:00',
    local_time: t.localTime ?? t.local_time ?? '00:00',
    accountId: t.accountId ?? t.account_id,
    account_id: t.accountId ?? t.account_id,
    toAccountId: t.toAccountId ?? t.to_account_id ?? null,
    to_account_id: t.toAccountId ?? t.to_account_id ?? null,
    categoryId: t.categoryId ?? t.category_id ?? null,
    category_id: t.categoryId ?? t.category_id ?? null,
    merchant: t.merchant ?? null,
    description: t.description ?? null,
    notes: t.notes ?? null,
    paymentMethod: t.paymentMethod ?? t.payment_method ?? null,
    payment_method: t.paymentMethod ?? t.payment_method ?? null,
    needWant: t.needWant ?? t.need_want ?? null,
    need_want: t.needWant ?? t.need_want ?? null,
    tags: t.tags || [],
    imported: Boolean(t.imported ?? false),
    source: t.source || 'MANUAL',
    createdAt: Number(t.createdAt ?? t.created_at ?? Date.now()),
    created_at: Number(t.createdAt ?? t.created_at ?? Date.now()),
    updatedAt: Number(t.updatedAt ?? t.updated_at ?? Date.now()),
    updated_at: Number(t.updatedAt ?? t.updated_at ?? Date.now()),
  }));

  const journal_entries = (payload.journal_entries || []).map((j: any) => ({
    txnId: j.txnId ?? j.txn_id,
    txn_id: j.txnId ?? j.txn_id,
    accountId: j.accountId ?? j.account_id ?? null,
    account_id: j.accountId ?? j.account_id ?? null,
    categoryId: j.categoryId ?? j.category_id ?? null,
    category_id: j.categoryId ?? j.category_id ?? null,
    amountMinor: Number(j.amountMinor ?? j.amount_minor ?? 0),
    amount_minor: Number(j.amountMinor ?? j.amount_minor ?? 0),
  }));

  const goals = (payload.goals || []).map((g: any) => ({
    id: g.id,
    name: g.name,
    targetAmountMinor: Number(g.targetAmountMinor ?? g.target_amount_minor ?? 0),
    target_amount_minor: Number(g.targetAmountMinor ?? g.target_amount_minor ?? 0),
    currentAmountMinor: Number(g.currentAmountMinor ?? g.current_amount_minor ?? 0),
    current_amount_minor: Number(g.currentAmountMinor ?? g.current_amount_minor ?? 0),
    plannedMonthlyContributionMinor: Number(g.plannedMonthlyContributionMinor ?? g.planned_monthly_contribution_minor ?? 0),
    planned_monthly_contribution_minor: Number(g.plannedMonthlyContributionMinor ?? g.planned_monthly_contribution_minor ?? 0),
    currency: g.currency || 'INR',
    targetDate: g.targetDate ?? g.target_date ?? '',
    target_date: g.targetDate ?? g.target_date ?? '',
    archived: Boolean(g.archived ?? false),
    createdAt: Number(g.createdAt ?? g.created_at ?? Date.now()),
    created_at: Number(g.createdAt ?? g.created_at ?? Date.now()),
    updatedAt: Number(g.updatedAt ?? g.updated_at ?? Date.now()),
    updated_at: Number(g.updatedAt ?? g.updated_at ?? Date.now()),
  }));

  const debts = (payload.debts || []).map((d: any) => ({
    id: d.id,
    name: d.name,
    principalMinor: Number(d.principalMinor ?? d.principal_minor ?? 0),
    principal_minor: Number(d.principalMinor ?? d.principal_minor ?? 0),
    currentBalanceMinor: Number(d.currentBalanceMinor ?? d.current_balance_minor ?? 0),
    current_balance_minor: Number(d.currentBalanceMinor ?? d.current_balance_minor ?? 0),
    annualPercent: String(d.annualPercent ?? d.annual_percent ?? '0'),
    annual_percent: String(d.annualPercent ?? d.annual_percent ?? '0'),
    expectedMonthlyPaymentMinor: Number(d.expectedMonthlyPaymentMinor ?? d.expected_monthly_payment_minor ?? 0),
    expected_monthly_payment_minor: Number(d.expectedMonthlyPaymentMinor ?? d.expected_monthly_payment_minor ?? 0),
    currency: d.currency || 'INR',
    archived: Boolean(d.archived ?? false),
    createdAt: Number(d.createdAt ?? d.created_at ?? Date.now()),
    created_at: Number(d.createdAt ?? d.created_at ?? Date.now()),
    updatedAt: Number(d.updatedAt ?? d.updated_at ?? Date.now()),
    updated_at: Number(d.updatedAt ?? d.updated_at ?? Date.now()),
  }));

  const chits = (payload.chits || []).map((c: any) => ({
    id: c.id,
    name: c.name,
    totalMonths: Number(c.totalMonths ?? c.total_months ?? 20),
    total_months: Number(c.totalMonths ?? c.total_months ?? 20),
    startMonth: c.startMonth ?? c.start_month ?? '',
    start_month: c.startMonth ?? c.start_month ?? '',
    monthlyContributionMinor: Number(c.monthlyContributionMinor ?? c.monthly_contribution_minor ?? 0),
    monthly_contribution_minor: Number(c.monthlyContributionMinor ?? c.monthly_contribution_minor ?? 0),
    currency: c.currency || 'INR',
    active: Boolean(c.active ?? true),
    createdAt: Number(c.createdAt ?? c.created_at ?? Date.now()),
    created_at: Number(c.createdAt ?? c.created_at ?? Date.now()),
    updatedAt: Number(c.updatedAt ?? c.updated_at ?? Date.now()),
    updated_at: Number(c.updatedAt ?? c.updated_at ?? Date.now()),
  }));

  const recurring = (payload.recurring || []).map((r: any) => ({
    id: r.id,
    type: r.type,
    amountMinor: Number(r.amountMinor ?? r.amount_minor ?? 0),
    amount_minor: Number(r.amountMinor ?? r.amount_minor ?? 0),
    currency: r.currency || 'INR',
    accountId: r.accountId ?? r.account_id,
    account_id: r.accountId ?? r.account_id,
    categoryId: r.categoryId ?? r.category_id ?? null,
    category_id: r.categoryId ?? r.category_id ?? null,
    description: r.description ?? null,
    frequency: r.frequency,
    nextRunDate: r.nextRunDate ?? r.next_run_date ?? '',
    next_run_date: r.nextRunDate ?? r.next_run_date ?? '',
    active: Boolean(r.active ?? true),
    createdAt: Number(r.createdAt ?? r.created_at ?? Date.now()),
    created_at: Number(r.createdAt ?? r.created_at ?? Date.now()),
    updatedAt: Number(r.updatedAt ?? r.updated_at ?? Date.now()),
    updated_at: Number(r.updatedAt ?? r.updated_at ?? Date.now()),
  }));

  return {
    accounts,
    categories,
    txns,
    journal_entries,
    goals,
    debts,
    chits,
    recurring,
  };
}

/**
 * Creates an encrypted, tamper-evident .wosenc backup envelope compatible with Android.
 */
export async function createEncryptedBackupEnvelope(
  payload: BackupPayload,
  passphrase: string
): Promise<Uint8Array> {
  if (!passphrase || passphrase.trim().length === 0) {
    throw new Error('A secure passphrase is required to create an encrypted backup. Operation cancelled.');
  }

  const enc = new TextEncoder();
  const rootJson = normalizePayloadForExport(payload);
  const rawJsonBytes = enc.encode(JSON.stringify(rootJson));
  const checksum = await sha256Hex(rawJsonBytes);

  const metadata: BackupMetadata = {
    version: CURRENT_VERSION,
    appVersion: '0.2.0-web',
    timestamp: Date.now(),
    accountCount: rootJson.accounts.length,
    txnCount: rootJson.txns.length,
    goalCount: rootJson.goals.length,
    debtCount: rootJson.debts.length,
    chitFundCount: rootJson.chits.length,
    recurringCount: rootJson.recurring.length,
    checksumSha256: checksum,
  };

  const metadataBytes = enc.encode(JSON.stringify(metadata));
  if (metadataBytes.length > MAX_METADATA_SIZE) {
    throw new Error(`Metadata exceeds maximum size limit of ${MAX_METADATA_SIZE} bytes.`);
  }

  // Generate random 16-byte salt and 12-byte IV
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const iv = crypto.getRandomValues(new Uint8Array(12));

  // Derive key & encrypt with AEAD AAD binding metadataBytes
  const aesKey = await deriveAesKey(passphrase, salt);
  const ciphertextBuffer = await crypto.subtle.encrypt(
    {
      name: 'AES-GCM',
      iv,
      additionalData: metadataBytes as unknown as BufferSource,
      tagLength: 128,
    },
    aesKey,
    rawJsonBytes as unknown as BufferSource
  );

  const ciphertext = new Uint8Array(ciphertextBuffer);

  // Total buffer allocation:
  // 4 (MAGIC) + 4 (VERSION) + 4 (METADATA_LEN) + metadataBytes.length + 16 (SALT) + 12 (IV) + ciphertext.length
  const totalLength = 4 + 4 + 4 + metadataBytes.length + 16 + 12 + ciphertext.length;
  if (totalLength > MAX_ENVELOPE_SIZE) {
    throw new Error(`Encrypted backup exceeds maximum allowable size (${MAX_ENVELOPE_SIZE} bytes).`);
  }

  const result = new Uint8Array(totalLength);
  const view = new DataView(result.buffer);

  let offset = 0;
  result.set(MAGIC, offset);
  offset += 4;

  view.setInt32(offset, CURRENT_VERSION, false); // Big-Endian
  offset += 4;

  view.setInt32(offset, metadataBytes.length, false); // Big-Endian
  offset += 4;

  result.set(metadataBytes, offset);
  offset += metadataBytes.length;

  result.set(salt, offset);
  offset += 16;

  result.set(iv, offset);
  offset += 12;

  result.set(ciphertext, offset);

  return result;
}

/**
 * Reads and verifies header magic, version, and metadata from an encrypted envelope without decrypting payload.
 */
export function parseBackupMetadata(envelope: Uint8Array): {
  metadata: BackupMetadata;
  saltOffset: number;
} {
  if (envelope.length < 4 + 4 + 4 + 16 + 12 + 16) {
    throw new Error('Envelope is too small to be a valid WealthOS backup.');
  }

  // Verify magic "WOSB"
  for (let i = 0; i < 4; i++) {
    if (envelope[i] !== MAGIC[i]) {
      throw new Error('Invalid file format: Magic header mismatch. Expected WealthOS backup.');
    }
  }

  const view = new DataView(envelope.buffer, envelope.byteOffset, envelope.byteLength);
  const version = view.getInt32(4, false);
  if (version !== CURRENT_VERSION) {
    throw new Error(`Unsupported backup format version: ${version}. Expected version ${CURRENT_VERSION}.`);
  }

  const metadataLen = view.getInt32(8, false);
  if (metadataLen <= 0 || metadataLen > 100_000) {
    throw new Error(`Corrupted metadata length: ${metadataLen}`);
  }

  const metadataBytes = envelope.subarray(12, 12 + metadataLen);
  const dec = new TextDecoder('utf-8');
  const metadata: BackupMetadata = JSON.parse(dec.decode(metadataBytes));

  return {
    metadata,
    saltOffset: 12 + metadataLen,
  };
}

/**
 * Returns only the unencrypted BackupMetadata header from an envelope.
 */
export function readEnvelopeMetadata(envelope: Uint8Array): BackupMetadata {
  return parseBackupMetadata(envelope).metadata;
}

/**
 * Decrypts, verifies AEAD tag, and checks SHA-256 payload integrity.
 * If passphrase or ciphertext is invalid, throws descriptive error and leaves existing state untouched.
 */
export async function decryptBackupEnvelope(
  envelope: Uint8Array,
  passphrase: string = ''
): Promise<{ metadata: BackupMetadata; payload: BackupPayload }> {
  const { metadata, saltOffset } = parseBackupMetadata(envelope);

  const dec = new TextDecoder('utf-8');
  const metadataLen = saltOffset - 12;
  const metadataBytes = envelope.subarray(12, 12 + metadataLen);

  const salt = envelope.subarray(saltOffset, saltOffset + 16);
  const iv = envelope.subarray(saltOffset + 16, saltOffset + 16 + 12);
  const ciphertext = envelope.subarray(saltOffset + 16 + 12);

  if (ciphertext.length < 16) {
    throw new Error('Ciphertext truncated or missing GCM authentication tag.');
  }

  const aesKey = await deriveAesKey(passphrase, salt);

  let decryptedBuffer: ArrayBuffer;
  try {
    decryptedBuffer = await crypto.subtle.decrypt(
      {
        name: 'AES-GCM',
        iv: iv as unknown as BufferSource,
        additionalData: metadataBytes as unknown as BufferSource,
        tagLength: 128,
      },
      aesKey,
      ciphertext as unknown as BufferSource
    );
  } catch (err: any) {
    throw new Error('Decryption failed: Incorrect passphrase or tampered backup data.');
  }

  const rawDecryptedBytes = new Uint8Array(decryptedBuffer);

  // Validate integrity checksum
  const actualChecksum = await sha256Hex(rawDecryptedBytes);
  if (actualChecksum !== metadata.checksumSha256) {
    throw new Error(
      `Payload integrity verification failed. Expected checksum ${metadata.checksumSha256}, got ${actualChecksum}.`
    );
  }

  const payload: BackupPayload = JSON.parse(dec.decode(rawDecryptedBytes));
  return { metadata, payload };
}
