import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import postgres from 'postgres';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Database configuration
const DB_HOST = process.env.SUPABASE_DB_HOST || 'db.zahzagnindkkttdpmqii.supabase.co';
const DB_PORT = process.env.SUPABASE_DB_PORT || 5432;
const DB_NAME = process.env.SUPABASE_DB_NAME || 'postgres';
const DB_USER = process.env.SUPABASE_DB_USER || 'postgres';
const DB_PASSWORD = process.env.SUPABASE_DB_PASSWORD || process.env.FLYWAY_PASSWORD || process.env.DB_PASSWORD;

const connectionString = process.env.DATABASE_URL || (
  DB_PASSWORD
    ? `postgres://${encodeURIComponent(DB_USER)}:${encodeURIComponent(DB_PASSWORD)}@${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=require`
    : null
);

if (!connectionString) {
  console.error('\n❌ ERROR: Supabase Database password not provided!');
  console.error('Please set SUPABASE_DB_PASSWORD or DATABASE_URL:');
  console.error('  $env:SUPABASE_DB_PASSWORD="your-db-password"; npm run db:migrate');
  console.error('  or in .env: SUPABASE_DB_PASSWORD=your-db-password\n');
  process.exit(1);
}

const sql = postgres(connectionString, {
  ssl: 'require',
  max: 1,
});

async function runMigrations() {
  console.log('\n🚀 WealthOS Automated Database Migration (Flyway Compatible)');
  console.log(`📡 Connecting to Supabase Host: ${DB_HOST} ...`);

  try {
    // 1. Ensure flyway_schema_history table exists
    await sql`
      CREATE TABLE IF NOT EXISTS flyway_schema_history (
        installed_rank INT NOT NULL PRIMARY KEY,
        version VARCHAR(50),
        description VARCHAR(200) NOT NULL,
        type VARCHAR(20) NOT NULL,
        script VARCHAR(1000) NOT NULL,
        checksum INT,
        installed_by VARCHAR(100) NOT NULL,
        installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        execution_time INT NOT NULL,
        success BOOLEAN NOT NULL
      )
    `;

    // 2. Fetch already installed versions
    const installedRows = await sql`
      SELECT version FROM flyway_schema_history WHERE success = true
    `;
    const installedVersions = new Set(installedRows.map(r => r.version));

    // 3. Locate migration directory
    const migrationsDir = path.resolve(__dirname, '../../supabase/migrations');
    if (!fs.existsSync(migrationsDir)) {
      console.error(`❌ Migration directory not found at: ${migrationsDir}`);
      process.exit(1);
    }

    const files = fs.readdirSync(migrationsDir)
      .filter(f => f.endsWith('.sql'))
      .sort();

    console.log(`📂 Found ${files.length} migration file(s) in supabase/migrations\n`);

    let appliedCount = 0;
    let rank = (installedRows.length || 0) + 1;

    for (const file of files) {
      // Parse version and description from filename (e.g. 20261014000000_web_sync.sql or V1__init.sql)
      const cleanName = file.replace(/\.sql$/, '');
      let version = cleanName;
      let description = cleanName;

      if (cleanName.includes('__')) {
        const parts = cleanName.split('__');
        version = parts[0].replace(/^V/, '');
        description = parts[1].replace(/_/g, ' ');
      } else if (cleanName.includes('_')) {
        const firstUnderscore = cleanName.indexOf('_');
        version = cleanName.slice(0, firstUnderscore);
        description = cleanName.slice(firstUnderscore + 1).replace(/_/g, ' ');
      }

      if (installedVersions.has(version)) {
        console.log(`  ✓ Already applied: [${version}] ${file}`);
        continue;
      }

      console.log(`  ⚡ Executing migration: [${version}] ${file} ...`);
      const filePath = path.join(migrationsDir, file);
      const fileContent = fs.readFileSync(filePath, 'utf8');

      const startTime = Date.now();

      // Execute SQL content inside transaction
      await sql.begin(async (tx) => {
        await tx.unsafe(fileContent);
        const duration = Date.now() - startTime;
        await tx`
          INSERT INTO flyway_schema_history (
            installed_rank, version, description, type, script, checksum, installed_by, execution_time, success
          ) VALUES (
            ${rank++}, ${version}, ${description}, 'SQL', ${file}, null, current_user, ${duration}, true
          )
        `;
      });

      console.log(`  ✅ Successfully applied: ${file} (${Date.now() - startTime}ms)`);
      appliedCount++;
    }

    console.log(`\n🎉 Migration Complete! ${appliedCount} new migration(s) applied.`);
  } catch (err) {
    console.error('\n❌ Migration Failed with Error:');
    console.error(err);
    process.exit(1);
  } finally {
    await sql.end();
  }
}

runMigrations();
