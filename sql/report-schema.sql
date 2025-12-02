USE bbs_dev;

CREATE TABLE IF NOT EXISTS report
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id      INT           NOT NULL,
    target_type      VARCHAR(20)   NOT NULL,
    target_id        INT           NOT NULL,
    reason           VARCHAR(255)  NOT NULL,
    description      TEXT               DEFAULT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    result           TEXT               DEFAULT NULL,
    result_notified  BOOLEAN       NOT NULL DEFAULT FALSE,
    resolved_at      TIMESTAMP          DEFAULT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP          DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES `user` (id) ON DELETE CASCADE
);

CREATE INDEX idx_report_reporter_created ON report (reporter_id, created_at DESC);
CREATE INDEX idx_report_target ON report (target_type, target_id);
