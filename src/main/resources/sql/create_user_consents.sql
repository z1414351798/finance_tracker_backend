CREATE TABLE IF NOT EXISTS user_consents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    policy_version VARCHAR(20) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    accepted_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_consents_user_id (user_id)
);
