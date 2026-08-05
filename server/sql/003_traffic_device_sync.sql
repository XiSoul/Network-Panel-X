CREATE TABLE IF NOT EXISTS traffic_device_daily (
    user_id BIGINT UNSIGNED NOT NULL,
    installation_id CHAR(36) NOT NULL,
    stat_date DATE NOT NULL,
    consumed_bytes BIGINT UNSIGNED NOT NULL DEFAULT 0,
    task_count INT UNSIGNED NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, installation_id, stat_date),
    CONSTRAINT traffic_device_daily_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
