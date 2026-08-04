import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { pool } from "./db.mjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const sql = fs.readFileSync(path.join(root, "sql", "001_init.sql"), "utf8");

for (const statement of sql.split(/;\s*(?:\r?\n|$)/)) {
  if (statement.trim()) await pool.query(statement);
}

await pool.end();
console.log("Database migration completed.");
