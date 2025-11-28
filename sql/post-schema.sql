USE bbs_dev;

CREATE TABLE IF NOT EXISTS post
(
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    author_id          INT          NOT NULL,
    title              VARCHAR(255) NOT NULL,
    content            TEXT         NULL,
    media              JSON         DEFAULT NULL,

    status             VARCHAR(50)  NOT NULL,
    possibly_sensitive BOOLEAN      DEFAULT FALSE,

    category           VARCHAR(100) DEFAULT NULL,

    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (author_id) REFERENCES `user` (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public_matric
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    post_id    INT NOT NULL,

    comments INT DEFAULT 0,
    likes      INT       DEFAULT 0,
    favorites  INT       DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS post_like
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    post_id    INT NOT NULL,
    user_id    INT NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS post_favorite
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    post_id    INT NOT NULL,
    user_id    INT NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
);