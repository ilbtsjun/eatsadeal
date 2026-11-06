ALTER TABLE notification_setting
    ADD COLUMN unsubscribe_token   VARCHAR(64) NULL,
    ADD COLUMN consent_notified_at DATETIME(6) NULL;

UPDATE notification_setting
SET unsubscribe_token = SHA2(CONCAT(id, '-', UUID(), '-', RAND()), 256)
WHERE unsubscribe_token IS NULL;

ALTER TABLE notification_setting
    MODIFY unsubscribe_token VARCHAR(64) NOT NULL,
    ADD CONSTRAINT uk_notification_setting_unsubscribe_token UNIQUE (unsubscribe_token);