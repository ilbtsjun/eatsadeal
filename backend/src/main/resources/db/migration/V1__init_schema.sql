CREATE TABLE category (
                          id   BIGINT       NOT NULL AUTO_INCREMENT,
                          name VARCHAR(255) NOT NULL,
                          img  VARCHAR(255) NOT NULL,
                          PRIMARY KEY (id),
                          CONSTRAINT uk_category_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE users (
                       id                BIGINT       NOT NULL AUTO_INCREMENT,
                       name              VARCHAR(255),
                       email             VARCHAR(255) NOT NULL,
                       password          VARCHAR(255) NOT NULL,
                       nickname          VARCHAR(255) NOT NULL,
                       phone_number      VARCHAR(255),
                       role              ENUM ('USER', 'ADMIN')                          NOT NULL,
                       gender            ENUM ('MALE', 'FEMALE', 'OTHER', 'UNSPECIFIED'),
                       birth             DATE,
                       user_status       ENUM ('ACTIVE', 'SUSPEND', 'WITHDRAWN')         NOT NULL,
                       created_at        DATETIME(6)  NOT NULL,
                       updated_at        DATETIME(6),
                       last_login_at     DATETIME(6),
                       suspended_at      DATETIME(6),
                       suspended_until   DATETIME(6),
                       suspending_reason VARCHAR(255),
                       PRIMARY KEY (id),
                       CONSTRAINT uk_users_email    UNIQUE (email),
                       CONSTRAINT uk_users_nickname UNIQUE (nickname)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE brand (
                       id        BIGINT       NOT NULL AUTO_INCREMENT,
                       name      VARCHAR(255) NOT NULL,
                       url       VARCHAR(255) NOT NULL,
                       img       VARCHAR(255) NOT NULL,
                       is_active BIT          NOT NULL,
                       PRIMARY KEY (id),
                       CONSTRAINT uk_brand_name UNIQUE (name),
                       CONSTRAINT uk_brand_url  UNIQUE (url)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE brand_category (
                                id          BIGINT NOT NULL AUTO_INCREMENT,
                                brand_id    BIGINT NOT NULL,
                                category_id BIGINT NOT NULL,
                                PRIMARY KEY (id),
                                CONSTRAINT uk_brand_category UNIQUE (brand_id, category_id),
                                CONSTRAINT fk_brand_category_brand    FOREIGN KEY (brand_id)    REFERENCES brand (id),
                                CONSTRAINT fk_brand_category_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE event (
                       id          BIGINT       NOT NULL AUTO_INCREMENT,
                       title       VARCHAR(500) NOT NULL,
                       description VARCHAR(255),
                       url         VARCHAR(700) NOT NULL,
                       img         VARCHAR(1000) NOT NULL,
                       start_date  DATETIME(6)  NOT NULL,
                       end_date    DATETIME(6),
                       view_count  BIGINT       NOT NULL,
                       is_active   BIT          NOT NULL,
                       brand       BIGINT       NOT NULL,   -- 엔티티의 @JoinColumn(name = "brand")
                       PRIMARY KEY (id),
                       CONSTRAINT uk_event_url UNIQUE (url),
                       CONSTRAINT fk_event_brand FOREIGN KEY (brand) REFERENCES brand (id),
                       INDEX idx_event_active_end (is_active, end_date),
                       INDEX idx_event_start_date (start_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE event_event_codes (
                                   event_id   BIGINT NOT NULL,
                                   event_code ENUM ('DISCOUNT_PRICE', 'DISCOUNT_RATE', 'BUY_ONE_GET_ONE', 'BUY_N_GET_N',
                     'TAKE_OUT', 'DELIVERY_FREE', 'GIFT_PROMO', 'PAYMENT_PROMO',
                     'MEMBERSHIP', 'TIME_SALE') NOT NULL,
                                   PRIMARY KEY (event_id, event_code),
                                   CONSTRAINT fk_event_event_codes_event FOREIGN KEY (event_id) REFERENCES event (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE favorite (
                          id       BIGINT NOT NULL AUTO_INCREMENT,
                          user_id  BIGINT NOT NULL,
                          event_id BIGINT NOT NULL,
                          PRIMARY KEY (id),
                          CONSTRAINT uk_user_event UNIQUE (user_id, event_id),
                          CONSTRAINT fk_favorite_user  FOREIGN KEY (user_id)  REFERENCES users (id),
                          CONSTRAINT fk_favorite_event FOREIGN KEY (event_id) REFERENCES event (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE comment (
                         comment_id     BIGINT       NOT NULL AUTO_INCREMENT,
                         content        VARCHAR(500) NOT NULL,
                         created_at     DATETIME(6)  NOT NULL,
                         updated_at     DATETIME(6),
                         deleted_at     DATETIME(6),
                         comment_status ENUM ('ACTIVE', 'MODIFIED', 'HIDDEN', 'DELETED') NOT NULL,
                         user_id        BIGINT       NOT NULL,
                         event_id       BIGINT       NOT NULL,
                         PRIMARY KEY (comment_id),
                         CONSTRAINT fk_comment_user  FOREIGN KEY (user_id)  REFERENCES users (id),
                         CONSTRAINT fk_comment_event FOREIGN KEY (event_id) REFERENCES event (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;