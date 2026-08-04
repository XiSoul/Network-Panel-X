CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(32) NOT NULL,
    username_normalized VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY users_username_normalized_uq (username_normalized)
);

CREATE TABLE IF NOT EXISTS traffic_daily (
    user_id BIGINT UNSIGNED NOT NULL,
    stat_date DATE NOT NULL,
    stat_year SMALLINT UNSIGNED NOT NULL,
    stat_month TINYINT UNSIGNED NOT NULL,
    stat_day TINYINT UNSIGNED NOT NULL,
    consumed_bytes BIGINT UNSIGNED NOT NULL DEFAULT 0,
    task_count INT UNSIGNED NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, stat_date),
    KEY traffic_daily_date_rank_idx (stat_date, consumed_bytes),
    KEY traffic_daily_month_rank_idx (stat_year, stat_month, consumed_bytes),
    KEY traffic_daily_year_rank_idx (stat_year, consumed_bytes),
    CONSTRAINT traffic_daily_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
