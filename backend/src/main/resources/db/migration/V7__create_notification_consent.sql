CREATE TABLE notification_setting (
                                      id                  BIGINT      NOT NULL AUTO_INCREMENT,
                                      user_id             BIGINT      NOT NULL,
                                      marketing_agreed    BIT         NOT NULL DEFAULT 0,
                                      marketing_agreed_at DATETIME(6) NULL,
                                      email_agreed        BIT         NOT NULL DEFAULT 0,
                                      email_agreed_at     DATETIME(6) NULL,
                                      updated_at          DATETIME(6) NOT NULL,
                                      PRIMARY KEY (id),
                                      CONSTRAINT uk_notification_setting_user UNIQUE (user_id),
                                      CONSTRAINT fk_notification_setting_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE consent_history (
                                 id           BIGINT      NOT NULL AUTO_INCREMENT,
                                 user_id      BIGINT      NOT NULL,
                                 consent_type VARCHAR(30) NOT NULL,
                                 agreed       BIT         NOT NULL,
                                 source       VARCHAR(30) NOT NULL,
                                 created_at   DATETIME(6) NOT NULL,
                                 PRIMARY KEY (id),
                                 INDEX idx_consent_history_user (user_id),
                                 CONSTRAINT fk_consent_history_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;