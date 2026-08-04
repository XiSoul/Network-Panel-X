import mysql from "mysql2/promise";
import { config } from "./config.mjs";

export const pool = mysql.createPool({
  ...config.database,
  waitForConnections: true,
  connectionLimit: 10,
  enableKeepAlive: true,
});
