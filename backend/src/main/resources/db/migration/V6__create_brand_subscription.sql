CREATE TABLE brand_subscription (
                                    id         BIGINT      NOT NULL AUTO_INCREMENT,
                                    user_id    BIGINT      NOT NULL,
                                    brand_id   BIGINT      NOT NULL,
                                    created_at DATETIME(6) NOT NULL,
                                    PRIMARY KEY (id),
                                    CONSTRAINT uk_brand_subscription_user_brand UNIQUE (user_id, brand_id),
                                    INDEX idx_brand_subscription_brand (brand_id),
                                    CONSTRAINT fk_brand_subscription_user  FOREIGN KEY (user_id)  REFERENCES users (id),
                                    CONSTRAINT fk_brand_subscription_brand FOREIGN KEY (brand_id) REFERENCES brand (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

ALTER TABLE event
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
