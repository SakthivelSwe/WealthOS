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
  version?: string;
  exportedAt?: string;
}

const MAGIC = new Uint8Array([0x57, 0x4f, 0x53, 0x42]); // "WOSB"
const CURRENT_VERSION = 1;

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
 */
async function deriveAesKey(passphrase: string, salt: Uint8Array): Promise<CryptoKey> {
  const enc = new TextEncoder();
  const passBytes = enc.encode(passphrase || 'wealthos_default_vault_key');
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
 * Creates an encrypted, tamper-evident .wosenc backup envelope compatible with Android.
 */
export async function createEncryptedBackupEnvelope(
  payload: BackupPayload,
  passphrase: string = ''
): Promise<Uint8Array> {
  const enc = new TextEncoder();

  // Normalize structure
  const rootJson = {
    accounts: payload.accounts || [],
    categories: payload.categories || [],
    txns: payload.txns || [],
    journal_entries: payload.journal_entries || [],
    goals: payload.goals || [],
    debts: payload.debts || [],
    chits: payload.chits || [],
    recurring: payload.recurring || [],
  };

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
