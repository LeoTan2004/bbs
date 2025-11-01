USE bbs_dev;

CREATE TABLE IF NOT EXISTS users
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    nickname      VARCHAR(100)          DEFAULT NULL,
    bio           VARCHAR(1023)         DEFAULT NULL,
    profile_slug  VARCHAR(100)          DEFAULT NULL,
    avatar_url    VARCHAR(255)          DEFAULT NULL,
    verified_id   INT                   DEFAULT NULL,
    role          VARCHAR(255) NOT NULL DEFAULT 'user',
    status        VARCHAR(255) NOT NULL DEFAULT 'active',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Indexes for faster lookups
    UNIQUE INDEX idx_username (username),
    UNIQUE INDEX idx_email (email)
);

CREATE TABLE IF NOT EXISTS verified_info
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NOT NULL,
    full_name   VARCHAR(255) NOT NULL,
    sid         VARCHAR(100) NOT NULL,
    institution VARCHAR(255) NOT NULL,
    verified_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    visible     BOOLEAN      NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) comment "Table to store verified user information";

CREATE TABLE IF NOT EXISTS email_verification_request
(
    id           INT AUTO_INCREMENT PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    scope        VARCHAR(255) NOT NULL,
    token        VARCHAR(255) NOT NULL,
    credential   VARCHAR(255) NOT NULL,
    requested_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at   TIMESTAMP    NOT NULL,
    status       VARCHAR(50)  NOT NULL DEFAULT 'pending',
    UNIQUE INDEX idx_email_token (email, token)
) comment "Table to store user verification requests";
