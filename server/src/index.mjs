import express from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { config } from "./config.mjs";
import { pool } from "./db.mjs";

const app = express();
app.disable("x-powered-by");
app.use(express.json({ limit: "64kb" }));
app.use((request, response, next) => {
  response.setHeader("Access-Control-Allow-Origin", config.allowedOrigin);
  response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
  response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  if (request.method === "OPTIONS") return response.sendStatus(204);
  next();
});

const usernamePattern = /^[A-Za-z0-9_]{3,32}$/;

function normalizeUsername(value) {
  return String(value || "").trim().toLowerCase();
}

function validateCredentials(username, password) {
  if (!usernamePattern.test(username)) return "用户名仅支持 3-32 位字母、数字和下划线";
  if (typeof password !== "string" || password.length < 8 || password.length > 128) {
    return "密码长度必须为 8-128 位";
  }
  return null;
}

function issueToken(user) {
  return jwt.sign({ sub: String(user.id), username: user.username }, config.jwtSecret, {
    expiresIn: "30d",
    issuer: "network-panel-x",
    audience: "network-panel-x-app",
  });
}

function requireUser(request, response, next) {
  const token = request.header("Authorization")?.replace(/^Bearer\s+/i, "");
  if (!token) return response.status(401).json({ error: "未登录" });
  try {
    request.user = jwt.verify(token, config.jwtSecret, {
      issuer: "network-panel-x",
      audience: "network-panel-x-app",
    });
    next();
  } catch {
    response.status(401).json({ error: "登录已过期，请重新登录" });
  }
}

function parseBytes(value) {
  const bytes = Number(value);
  return Number.isSafeInteger(bytes) && bytes >= 0 && bytes <= Number.MAX_SAFE_INTEGER ? bytes : null;
}

function periodPredicate(period) {
  switch (period) {
    case "day":
      return { where: "stat_date = CURRENT_DATE", label: "日" };
    case "month":
      return { where: "stat_year = YEAR(CURRENT_DATE) AND stat_month = MONTH(CURRENT_DATE)", label: "月" };
    case "year":
      return { where: "stat_year = YEAR(CURRENT_DATE)", label: "年" };
    default:
      return null;
  }
}

app.get("/health", async (_request, response, next) => {
  try {
    await pool.query("SELECT 1");
    response.json({ ok: true });
  } catch (error) {
    next(error);
  }
});

app.post("/v1/auth/register", async (request, response, next) => {
  try {
    const username = String(request.body?.username || "").trim();
    const password = request.body?.password;
    const validationError = validateCredentials(username, password);
    if (validationError) return response.status(400).json({ error: validationError });

    const passwordHash = await bcrypt.hash(password, 12);
    try {
      const [result] = await pool.execute(
        "INSERT INTO users (username, username_normalized, password_hash) VALUES (?, ?, ?)",
        [username, normalizeUsername(username), passwordHash],
      );
      const user = { id: result.insertId, username };
      response.status(201).json({ token: issueToken(user), user });
    } catch (error) {
      if (error?.code === "ER_DUP_ENTRY") return response.status(409).json({ error: "用户名已存在" });
      throw error;
    }
  } catch (error) {
    next(error);
  }
});

app.post("/v1/auth/login", async (request, response, next) => {
  try {
    const username = String(request.body?.username || "").trim();
    const password = request.body?.password;
    const validationError = validateCredentials(username, password);
    if (validationError) return response.status(400).json({ error: "用户名或密码错误" });

    const [rows] = await pool.execute(
      "SELECT id, username, password_hash FROM users WHERE username_normalized = ? LIMIT 1",
      [normalizeUsername(username)],
    );
    const user = rows[0];
    if (!user || !(await bcrypt.compare(password, user.password_hash))) {
      return response.status(401).json({ error: "用户名或密码错误" });
    }
    response.json({ token: issueToken(user), user: { id: user.id, username: user.username } });
  } catch (error) {
    next(error);
  }
});

app.get("/v1/me", requireUser, async (request, response) => {
  response.json({ id: Number(request.user.sub), username: request.user.username });
});

app.post("/v1/traffic/sync", requireUser, async (request, response, next) => {
  try {
    const consumedBytes = parseBytes(request.body?.consumedBytes);
    const taskCount = Number(request.body?.taskCount);
    if (consumedBytes === null || !Number.isSafeInteger(taskCount) || taskCount < 0 || taskCount > 1_000_000) {
      return response.status(400).json({ error: "统计数据无效" });
    }
    await pool.execute(
      `INSERT INTO traffic_daily
        (user_id, stat_date, stat_year, stat_month, stat_day, consumed_bytes, task_count)
       VALUES (?, CURRENT_DATE, YEAR(CURRENT_DATE), MONTH(CURRENT_DATE), DAY(CURRENT_DATE), ?, ?)
       ON DUPLICATE KEY UPDATE
         consumed_bytes = GREATEST(consumed_bytes, VALUES(consumed_bytes)),
         task_count = GREATEST(task_count, VALUES(task_count))`,
      [Number(request.user.sub), consumedBytes, taskCount],
    );
    response.json({ ok: true });
  } catch (error) {
    next(error);
  }
});

app.get("/v1/stats/me", requireUser, async (request, response, next) => {
  try {
    const predicate = periodPredicate(request.query.period || "day");
    if (!predicate) return response.status(400).json({ error: "period 必须是 day、month 或 year" });
    const [rows] = await pool.query(
      `SELECT COALESCE(SUM(consumed_bytes), 0) AS consumedBytes,
              COALESCE(SUM(task_count), 0) AS taskCount
       FROM traffic_daily
       WHERE user_id = ? AND ${predicate.where}`,
      [Number(request.user.sub)],
    );
    response.json({ period: predicate.label, ...rows[0] });
  } catch (error) {
    next(error);
  }
});

app.get("/v1/leaderboard", requireUser, async (request, response, next) => {
  try {
    const predicate = periodPredicate(request.query.period || "day");
    if (!predicate) return response.status(400).json({ error: "period 必须是 day、month 或 year" });
    const [rows] = await pool.query(
      `SELECT u.username,
              SUM(t.consumed_bytes) AS consumedBytes,
              SUM(t.task_count) AS taskCount
       FROM traffic_daily t
       JOIN users u ON u.id = t.user_id
       WHERE ${predicate.where}
       GROUP BY u.id, u.username
       ORDER BY consumedBytes DESC, u.id ASC
       LIMIT 100`,
    );
    response.json({
      period: predicate.label,
      entries: rows.map((entry, index) => ({ rank: index + 1, ...entry })),
    });
  } catch (error) {
    next(error);
  }
});

app.use((error, _request, response, _next) => {
  console.error(error);
  response.status(500).json({ error: "服务器内部错误" });
});

app.listen(config.port, () => {
  console.log(`Network Panel X API listening on ${config.port}`);
});
