import express from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { randomInt } from "node:crypto";
import { config } from "./config.mjs";
import { pool } from "./db.mjs";
import { sendVerificationCode } from "./email.mjs";

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
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const verificationLifetimeMs = 10 * 60 * 1000;
const verificationCooldownMs = 60 * 1000;
const maxVerificationAttempts = 5;
const rateLimitBuckets = new Map();

function normalizeUsername(value) {
  return String(value || "").trim().toLowerCase();
}

function normalizeEmail(value) {
  return String(value || "").trim().toLowerCase();
}

function validatePassword(password) {
  if (typeof password !== "string" || password.length < 8 || password.length > 128) {
    return "密码长度必须为 8-128 位";
  }
  return null;
}

function validateCredentials(username, password) {
  if (!usernamePattern.test(username)) return "用户名仅支持 3-32 位字母、数字和下划线";
  return validatePassword(password);
}

function validateEmail(email) {
  return email.length <= 254 && emailPattern.test(email) ? null : "邮箱格式不正确";
}

function isRateLimited(key, maximum, windowMs) {
  const now = Date.now();
  const timestamps = (rateLimitBuckets.get(key) || []).filter((value) => value > now - windowMs);
  if (timestamps.length >= maximum) {
    rateLimitBuckets.set(key, timestamps);
    return true;
  }
  timestamps.push(now);
  rateLimitBuckets.set(key, timestamps);
  if (rateLimitBuckets.size > 10_000) {
    for (const [bucketKey, values] of rateLimitBuckets) {
      if (!values.some((value) => value > now - windowMs)) rateLimitBuckets.delete(bucketKey);
    }
  }
  return false;
}

function createVerificationCode() {
  return String(randomInt(100_000, 1_000_000));
}

async function verifyCode({ purpose, emailNormalized, code }) {
  const [rows] = await pool.execute(
    `SELECT code_hash, expires_at, attempt_count, username, username_normalized, pending_password_hash
     FROM email_verifications WHERE purpose = ? AND email_normalized = ? LIMIT 1`,
    [purpose, emailNormalized],
  );
  const verification = rows[0];
  if (!verification || new Date(verification.expires_at).getTime() <= Date.now()) {
    await pool.execute("DELETE FROM email_verifications WHERE purpose = ? AND email_normalized = ?", [purpose, emailNormalized]);
    return { error: "验证码无效或已过期" };
  }
  if (verification.attempt_count >= maxVerificationAttempts) {
    await pool.execute("DELETE FROM email_verifications WHERE purpose = ? AND email_normalized = ?", [purpose, emailNormalized]);
    return { error: "验证码错误次数过多，请重新获取" };
  }
  if (typeof code !== "string" || !/^\d{6}$/.test(code) || !(await bcrypt.compare(code, verification.code_hash))) {
    await pool.execute(
      "UPDATE email_verifications SET attempt_count = attempt_count + 1 WHERE purpose = ? AND email_normalized = ?",
      [purpose, emailNormalized],
    );
    return { error: "验证码无效或已过期" };
  }
  return { verification };
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

app.post("/v1/auth/register/request-code", async (request, response, next) => {
  try {
    const username = String(request.body?.username || "").trim();
    const password = request.body?.password;
    const email = String(request.body?.email || "").trim();
    const emailNormalized = normalizeEmail(email);
    const validationError = validateCredentials(username, password);
    if (validationError) return response.status(400).json({ error: validationError });
    const emailValidationError = validateEmail(emailNormalized);
    if (emailValidationError) return response.status(400).json({ error: emailValidationError });
    if (isRateLimited(`register:${request.ip}`, 5, 15 * 60 * 1000)) {
      return response.status(429).json({ error: "请求过于频繁，请稍后再试" });
    }

    const usernameNormalized = normalizeUsername(username);
    const [existing] = await pool.execute(
      "SELECT username_normalized, email_normalized FROM users WHERE username_normalized = ? OR email_normalized = ? LIMIT 1",
      [usernameNormalized, emailNormalized],
    );
    if (existing.length) {
      const isUsername = existing[0].username_normalized === usernameNormalized;
      return response.status(409).json({ error: isUsername ? "用户名已存在" : "邮箱已被注册" });
    }
    const [previous] = await pool.execute(
      "SELECT sent_at FROM email_verifications WHERE purpose = 'register' AND email_normalized = ? LIMIT 1",
      [emailNormalized],
    );
    if (previous[0] && Date.now() - new Date(previous[0].sent_at).getTime() < verificationCooldownMs) {
      return response.status(429).json({ error: "验证码已发送，请 60 秒后再试" });
    }
    const code = createVerificationCode();
    const [passwordHash, codeHash] = await Promise.all([bcrypt.hash(password, 12), bcrypt.hash(code, 12)]);
    await sendVerificationCode({ email, code, purpose: "register" });
    await pool.execute(
      `INSERT INTO email_verifications
        (purpose, email, email_normalized, username, username_normalized, pending_password_hash, code_hash, expires_at, sent_at, attempt_count)
       VALUES ('register', ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(3), 0)
       ON DUPLICATE KEY UPDATE email = VALUES(email), username = VALUES(username), username_normalized = VALUES(username_normalized),
         pending_password_hash = VALUES(pending_password_hash), code_hash = VALUES(code_hash), expires_at = VALUES(expires_at),
         sent_at = CURRENT_TIMESTAMP(3), attempt_count = 0`,
      [email, emailNormalized, username, usernameNormalized, passwordHash, codeHash, new Date(Date.now() + verificationLifetimeMs)],
    );
    response.status(202).json({ message: "验证码已发送到邮箱" });
  } catch (error) {
    next(error);
  }
});

app.post("/v1/auth/register", async (request, response, next) => {
  try {
    const username = String(request.body?.username || "").trim();
    const password = request.body?.password;
    const email = String(request.body?.email || "").trim();
    const emailNormalized = normalizeEmail(email);
    const validationError = validateCredentials(username, password) || validateEmail(emailNormalized);
    if (validationError) return response.status(400).json({ error: validationError });
    const checked = await verifyCode({ purpose: "register", emailNormalized, code: request.body?.code });
    if (checked.error) return response.status(400).json({ error: checked.error });
    const verification = checked.verification;
    if (verification.username_normalized !== normalizeUsername(username) || !(await bcrypt.compare(password, verification.pending_password_hash))) {
      return response.status(400).json({ error: "注册信息与验证码不匹配，请重新获取验证码" });
    }
    const connection = await pool.getConnection();
    try {
      await connection.beginTransaction();
      const [result] = await connection.execute(
        "INSERT INTO users (username, username_normalized, email, email_normalized, password_hash, email_verified_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP(3))",
        [username, normalizeUsername(username), email, emailNormalized, verification.pending_password_hash],
      );
      await connection.execute("DELETE FROM email_verifications WHERE purpose = 'register' AND email_normalized = ?", [emailNormalized]);
      await connection.commit();
      const user = { id: result.insertId, username };
      response.status(201).json({ token: issueToken(user), user });
    } catch (error) {
      await connection.rollback();
      if (error?.code === "ER_DUP_ENTRY") return response.status(409).json({ error: "用户名或邮箱已被注册" });
      throw error;
    } finally {
      connection.release();
    }
  } catch (error) {
    next(error);
  }
});

app.post("/v1/auth/password-reset/request-code", async (request, response, next) => {
  try {
    const email = String(request.body?.email || "").trim();
    const emailNormalized = normalizeEmail(email);
    const emailValidationError = validateEmail(emailNormalized);
    if (emailValidationError) return response.status(400).json({ error: emailValidationError });
    if (isRateLimited(`reset:${request.ip}`, 5, 15 * 60 * 1000)) {
      return response.status(429).json({ error: "请求过于频繁，请稍后再试" });
    }
    const [users] = await pool.execute("SELECT id FROM users WHERE email_normalized = ? LIMIT 1", [emailNormalized]);
    if (!users.length) return response.status(202).json({ message: "若该邮箱已注册，验证码将发送到邮箱" });
    const [previous] = await pool.execute(
      "SELECT sent_at FROM email_verifications WHERE purpose = 'reset' AND email_normalized = ? LIMIT 1",
      [emailNormalized],
    );
    if (previous[0] && Date.now() - new Date(previous[0].sent_at).getTime() < verificationCooldownMs) {
      return response.status(429).json({ error: "验证码已发送，请 60 秒后再试" });
    }
    const code = createVerificationCode();
    await sendVerificationCode({ email, code, purpose: "reset" });
    await pool.execute(
      `INSERT INTO email_verifications (purpose, email, email_normalized, code_hash, expires_at, sent_at, attempt_count)
       VALUES ('reset', ?, ?, ?, ?, CURRENT_TIMESTAMP(3), 0)
       ON DUPLICATE KEY UPDATE email = VALUES(email), code_hash = VALUES(code_hash), expires_at = VALUES(expires_at),
         sent_at = CURRENT_TIMESTAMP(3), attempt_count = 0`,
      [email, emailNormalized, await bcrypt.hash(code, 12), new Date(Date.now() + verificationLifetimeMs)],
    );
    response.status(202).json({ message: "若该邮箱已注册，验证码将发送到邮箱" });
  } catch (error) {
    next(error);
  }
});

app.post("/v1/auth/password-reset/confirm", async (request, response, next) => {
  try {
    const emailNormalized = normalizeEmail(request.body?.email);
    const password = request.body?.newPassword;
    const validationError = validateEmail(emailNormalized) || validatePassword(password);
    if (validationError) return response.status(400).json({ error: validationError });
    const checked = await verifyCode({ purpose: "reset", emailNormalized, code: request.body?.code });
    if (checked.error) return response.status(400).json({ error: checked.error });
    const connection = await pool.getConnection();
    try {
      await connection.beginTransaction();
      const [result] = await connection.execute(
        "UPDATE users SET password_hash = ? WHERE email_normalized = ?",
        [await bcrypt.hash(password, 12), emailNormalized],
      );
      await connection.execute("DELETE FROM email_verifications WHERE purpose = 'reset' AND email_normalized = ?", [emailNormalized]);
      await connection.commit();
      if (!result.affectedRows) return response.status(400).json({ error: "验证码无效或已过期" });
      response.json({ message: "密码已重置，请使用新密码登录" });
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
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

app.get("/v1/profile/snapshot", requireUser, async (request, response, next) => {
  try {
    const [rows] = await pool.execute(
      "SELECT snapshot, updated_at AS updatedAt FROM user_profile_snapshots WHERE user_id = ? LIMIT 1",
      [Number(request.user.sub)],
    );
    const snapshot = rows[0];
    if (!snapshot) return response.json({ document: null });
    const document = typeof snapshot.snapshot === "string" ? JSON.parse(snapshot.snapshot) : snapshot.snapshot;
    response.json({ document, updatedAt: snapshot.updatedAt });
  } catch (error) {
    next(error);
  }
});

app.post("/v1/profile/snapshot", requireUser, async (request, response, next) => {
  try {
    const document = request.body?.document;
    if (!document || typeof document !== "object" || Array.isArray(document)) {
      return response.status(400).json({ error: "备份数据无效" });
    }
    if (Number(document.schemaVersion) !== 1 || !Array.isArray(document.links)) {
      return response.status(400).json({ error: "备份格式不支持" });
    }
    const serialized = JSON.stringify(document);
    if (Buffer.byteLength(serialized, "utf8") > 60 * 1024) {
      return response.status(400).json({ error: "备份数据不能超过 60KB" });
    }
    await pool.execute(
      `INSERT INTO user_profile_snapshots (user_id, snapshot)
       VALUES (?, ?)
       ON DUPLICATE KEY UPDATE snapshot = VALUES(snapshot), updated_at = CURRENT_TIMESTAMP(3)`,
      [Number(request.user.sub), serialized],
    );
    response.json({ ok: true });
  } catch (error) {
    next(error);
  }
});

app.post("/v1/traffic/sync", requireUser, async (request, response, next) => {
  try {
    const consumedBytes = parseBytes(request.body?.consumedBytes);
    const taskCount = Number(request.body?.taskCount);
    if (consumedBytes === null || !Number.isSafeInteger(taskCount) || taskCount < 0 || taskCount > 1_000_000) {
      return response.status(400).json({ error: "统计数据无效" });
    }
    const installationId = String(request.body?.installationId || "").trim();
    if (installationId && !/^[a-f0-9-]{36}$/i.test(installationId)) {
      return response.status(400).json({ error: "设备标识无效" });
    }
    if (!installationId) {
      // Older app versions send one account-wide absolute total.
      await pool.execute(
        `INSERT INTO traffic_daily
          (user_id, stat_date, stat_year, stat_month, stat_day, consumed_bytes, task_count)
         VALUES (?, CURRENT_DATE, YEAR(CURRENT_DATE), MONTH(CURRENT_DATE), DAY(CURRENT_DATE), ?, ?)
         ON DUPLICATE KEY UPDATE
           consumed_bytes = GREATEST(consumed_bytes, VALUES(consumed_bytes)),
           task_count = GREATEST(task_count, VALUES(task_count))`,
        [Number(request.user.sub), consumedBytes, taskCount],
      );
      return response.json({ ok: true });
    }

    const connection = await pool.getConnection();
    try {
      await connection.beginTransaction();
      const [existing] = await connection.execute(
        `SELECT consumed_bytes, task_count FROM traffic_device_daily
         WHERE user_id = ? AND installation_id = ? AND stat_date = CURRENT_DATE FOR UPDATE`,
        [Number(request.user.sub), installationId],
      );
      const previous = existing[0] || { consumed_bytes: 0, task_count: 0 };
      const nextBytes = Math.max(Number(previous.consumed_bytes), consumedBytes);
      const nextTasks = Math.max(Number(previous.task_count), taskCount);
      const addedBytes = nextBytes - Number(previous.consumed_bytes);
      const addedTasks = nextTasks - Number(previous.task_count);

      await connection.execute(
        `INSERT INTO traffic_device_daily (user_id, installation_id, stat_date, consumed_bytes, task_count)
         VALUES (?, ?, CURRENT_DATE, ?, ?)
         ON DUPLICATE KEY UPDATE consumed_bytes = VALUES(consumed_bytes), task_count = VALUES(task_count)`,
        [Number(request.user.sub), installationId, nextBytes, nextTasks],
      );
      if (addedBytes || addedTasks) {
        await connection.execute(
          `INSERT INTO traffic_daily
            (user_id, stat_date, stat_year, stat_month, stat_day, consumed_bytes, task_count)
           VALUES (?, CURRENT_DATE, YEAR(CURRENT_DATE), MONTH(CURRENT_DATE), DAY(CURRENT_DATE), ?, ?)
           ON DUPLICATE KEY UPDATE
             consumed_bytes = consumed_bytes + VALUES(consumed_bytes),
             task_count = task_count + VALUES(task_count)`,
          [Number(request.user.sub), addedBytes, addedTasks],
        );
      }
      await connection.commit();
      response.json({ ok: true, addedBytes, addedTasks });
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
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
