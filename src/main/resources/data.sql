INSERT INTO users (slack_user_id, slack_username) VALUES
('U123456', 'jia'),
('U08NPGDGQ7P', 'tester');

INSERT INTO study_log (content, timestamp, user_slack_user_id) VALUES
('오늘 자바 공부함', '2025-05-01T10:00:00', 'U123456'),
('스프링 JPA 복습', '2025-05-02T09:30:00', 'U123456'),
('OpenAI 연동 테스트', '2025-05-03T15:00:00', 'U123456');