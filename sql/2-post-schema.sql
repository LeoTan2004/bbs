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

    comments   INT       DEFAULT 0,
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

CREATE TABLE IF NOT EXISTS post_comment
(
    id             INT AUTO_INCREMENT PRIMARY KEY,

    post_id        INT          NOT NULL,
    user_id        INT          NOT NULL,
    parent_post_id INT               DEFAULT NULL,
    media          JSON              DEFAULT NULL,
    content        TEXT         NOT NULL,
    status         VARCHAR(50)  NOT NULL DEFAULT 'PUBLISHED',

    comments       INT               DEFAULT 0,
    likes          INT               DEFAULT 0,

    created_at     TIMESTAMP         DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (post_id) REFERENCES post (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comment_like
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    comment_id INT NOT NULL,
    user_id    INT NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY unique_comment_user (comment_id, user_id),
    FOREIGN KEY (comment_id) REFERENCES post_comment (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
);