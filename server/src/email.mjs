import nodemailer from "nodemailer";
import { lookup } from "node:dns/promises";
import { config } from "./config.mjs";

const smtpAddress = config.smtp.family === 4
  ? (await lookup(config.smtp.host, { family: 4 })).address
  : config.smtp.host;

const transporter = nodemailer.createTransport({
  host: smtpAddress,
  port: config.smtp.port,
  secure: config.smtp.secure,
  tls: { servername: config.smtp.host },
  auth: {
    user: config.smtp.username,
    pass: config.smtp.password,
  },
});

export function verifyEmailTransport() {
  return transporter.verify();
}

export async function sendVerificationCode({ email, code, purpose }) {
  const isReset = purpose === "reset";
  await transporter.sendMail({
    from: config.smtp.from,
    to: email,
    subject: isReset ? "Network Panel X 密码重置验证码" : "Network Panel X 注册验证码",
    text: `${isReset ? "你正在重置密码" : "你正在注册账号"}。验证码：${code}\n\n验证码 10 分钟内有效。请勿将验证码提供给任何人。`,
  });
}
