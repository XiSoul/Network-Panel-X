CREATE TABLE IF NOT EXISTS user_profile_snapshots (
    user_id BIGINT UNSIGNED NOT NULL,
    snapshot JSON NOT NULL,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT user_profile_snapshots_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
