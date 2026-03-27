CREATE TABLE IF NOT EXISTS app_users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    username        VARCHAR(64)     NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    totp_secret     VARCHAR(128),
    mfa_enabled     TINYINT(1)      NOT NULL DEFAULT 0,
    role            VARCHAR(16)     NOT NULL DEFAULT 'OPERATOR',
    enabled         TINYINT(1)      NOT NULL DEFAULT 1,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at   DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    username            VARCHAR(64)     NOT NULL,
    client_ip           VARCHAR(64),
    target_database     VARCHAR(128),
    sql_text            TEXT            NOT NULL,
    execution_time_ms   BIGINT          NOT NULL DEFAULT 0,
    success             TINYINT(1)      NOT NULL DEFAULT 1,
    error_message       TEXT,
    executed_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_username (username),
    KEY idx_executed_at (executed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
