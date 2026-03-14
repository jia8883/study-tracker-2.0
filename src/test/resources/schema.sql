DROP TABLE IF EXISTS study_log;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  slack_user_id VARCHAR(255) PRIMARY KEY,
  slack_username VARCHAR(255) NOT NULL
);

CREATE TABLE study_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  content VARCHAR(255),
  timestamp TIMESTAMP,
  user_slack_user_id VARCHAR(255),
  CONSTRAINT fk_user FOREIGN KEY (user_slack_user_id) REFERENCES users(slack_user_id)
);
