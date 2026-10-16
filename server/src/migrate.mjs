import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { pool } from "./db.mjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const sqlDirectory = path.join(root, "sql");
const migrations = fs.readdirSync(sqlDirectory)
  .filter((file) => /^\d+_.+\.sql$/.test(file))
  .sort();

await pool.query(`CREATE TABLE IF NOT EXISTS schema_migrations (
  name VARCHAR(255) NOT NULL PRIMARY KEY,
  applied_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
)`);

// 将 TiDB 专属的 idempotent 语法翻译为 MySQL/MariaDB 兼容写法（TiDB 本身也兼容右列写法）。
// 目标：同一份 SQL 在 TiDB Cloud 与 MySQL 8/MariaDB 上都能跑。
async function columnExists(connection, table, column) {
  const [rows] = await connection.query(
    "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ? LIMIT 1",
    [table, column],
  );
  return rows.length > 0;
}

async function indexExists(connection, table, indexName) {
  const [rows] = await connection.query(
    "SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ? LIMIT 1",
    [table, indexName],
  );
  return rows.length > 0;
}

async function runStatement(connection, statement) {
  // 模式1: ALTER TABLE <t> ADD COLUMN IF NOT EXISTS <col> <def...>
  let match = statement.match(/^ALTER\s+TABLE\s+(\S+)\s+ADD\s+COLUMN\s+IF\s+NOT\s+EXISTS\s+(\S+)\s+(.*)$/i);
  if (match) {
    const [, table, column, definition] = match;
    if (await columnExists(connection, table, column)) return { skipped: `column ${table}.${column} exists` };
    await connection.query(`ALTER TABLE ${table} ADD COLUMN ${column} ${definition}`);
    return { applied: `column ${table}.${column}` };
  }
  // 模式2: CREATE [UNIQUE] INDEX IF NOT EXISTS <name> ON <t> (...)
  match = statement.match(/^CREATE\s+(UNIQUE\s+)?INDEX\s+IF\s+NOT\s+EXISTS\s+(\S+)\s+ON\s+(\S+)\s+(.*)$/i);
  if (match) {
    const [, unique, indexName, table, definition] = match;
    if (await indexExists(connection, table, indexName)) return { skipped: `index ${indexName} exists` };
    await connection.query(`CREATE ${unique || ""}INDEX ${indexName} ON ${table} ${definition}`);
    return { applied: `index ${indexName}` };
  }
  await connection.query(statement);
  return { applied: statement.slice(0, 40) };
}

for (const migration of migrations) {
  const [applied] = await pool.execute("SELECT 1 FROM schema_migrations WHERE name = ? LIMIT 1", [migration]);
  if (applied.length) continue;

  const sql = fs.readFileSync(path.join(sqlDirectory, migration), "utf8");
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();
    for (const rawStatement of sql.split(/;\s*(?:\r?\n|$)/)) {
      const statement = rawStatement.trim();
      if (!statement) continue;
      const result = await runStatement(connection, statement);
      if (result.skipped) console.warn(`  [skip] ${migration}: ${result.skipped}`);
    }
    await connection.execute("INSERT INTO schema_migrations (name) VALUES (?)", [migration]);
    await connection.commit();
    console.log(`Applied migration ${migration}`);
  } catch (error) {
    await connection.rollback();
    throw error;
  } finally {
    connection.release();
  }
}

await pool.end();
console.log("Database migration completed.");
