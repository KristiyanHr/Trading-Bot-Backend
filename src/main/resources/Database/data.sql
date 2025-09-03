INSERT INTO users (username, password_hash) VALUES ('testUser', 'dummy_Hash');

INSERT INTO accounts (user_id, balance, currency, updated_at)
VALUES (
    (SELECT id FROM users WHERE username = 'testUser'),
    10000.00,
    'USD',
    CURRENT_TIMESTAMP
);
