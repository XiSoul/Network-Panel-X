ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(254) NULL AFTER username_normalized;

ALTER TABLE users ADD COLUMN IF NOT EXISTS email_normalized VARCHAR(254) NULL AFTER email;

ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP(3) NULL AFTER password_hash;

CREATE UNIQUE INDEX IF NOT EXISTS users_email_normalized_uq ON users (email_normalized);

CREATE TABLE IF NOT EXISTS email_verifications (
    purpose VARCHAR(16) NOT NULL,
    email VARCHAR(254) NOT NULL,
    email_normalized VARCHAR(254) NOT NULL,
    username VARCHAR(32) NULL,
    username_normalized VARCHAR(32) NULL,
    pending_password_hash VARCHAR(100) NULL,
    code_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    sent_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    attempt_count TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (purpose, email_normalized),
    KEY email_verifications_expires_idx (expires_at)
);
