USE bbs_dev;

CREATE TABLE IF NOT EXISTS notification
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_user_id   INT          NOT NULL,
    actor_user_id    INT               DEFAULT NULL,
    type             VARCHAR(50)  NOT NULL,
    priority         VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    title            VARCHAR(255)      DEFAULT NULL,
    content          TEXT              DEFAULT NULL,
    redirect_url     VARCHAR(1024)     DEFAULT NULL,
    source_type      VARCHAR(100)      DEFAULT NULL,
    source_id        VARCHAR(100)      DEFAULT NULL,
    source_snippet   VARCHAR(512)      DEFAULT NULL,
    context          JSON              DEFAULT NULL,
    is_read          BOOLEAN      NOT NULL DEFAULT FALSE,
    delivered        BOOLEAN      NOT NULL DEFAULT TRUE,
    read_at          TIMESTAMP         DEFAULT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_target_user FOREIGN KEY (target_user_id) REFERENCES `user` (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_actor_user FOREIGN KEY (actor_user_id) REFERENCES `user` (id) ON DELETE SET NULL
);

CREATE INDEX idx_notification_user_created ON notification (target_user_id, created_at DESC);
CREATE INDEX idx_notification_user_read ON notification (target_user_id, is_read, created_at DESC);
