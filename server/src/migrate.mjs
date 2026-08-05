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

for (const migration of migrations) {
  const [applied] = await pool.execute("SELECT 1 FROM schema_migrations WHERE name = ? LIMIT 1", [migration]);
  if (applied.length) continue;

  const sql = fs.readFileSync(path.join(sqlDirectory, migration), "utf8");
  const connection = await pool.getConnection();
  try {
    await connection.beginTransaction();
    for (const statement of sql.split(/;\s*(?:\r?\n|$)/)) {
      if (statement.trim()) await connection.query(statement);
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
