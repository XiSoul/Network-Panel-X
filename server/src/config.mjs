import "dotenv/config";
import fs from "node:fs";

const required = [
  "DB_HOST",
  "DB_USERNAME",
  "DB_PASSWORD",
  "DB_DATABASE",
  "JWT_SECRET",
];

for (const name of required) {
  if (!process.env[name]) throw new Error(`Missing required environment variable: ${name}`);
}

export const config = {
  port: Number(process.env.PORT || 8787),
  allowedOrigin: process.env.ALLOWED_ORIGIN || "*",
  jwtSecret: process.env.JWT_SECRET,
  database: {
    host: process.env.DB_HOST,
    port: Number(process.env.DB_PORT || 4000),
    user: process.env.DB_USERNAME,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_DATABASE,
    ssl: process.env.DATABASE_CA_PATH
      ? { ca: fs.readFileSync(process.env.DATABASE_CA_PATH), rejectUnauthorized: true }
      : { rejectUnauthorized: true },
  },
};
