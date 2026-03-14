-- 기존 데이터 제거 (테스트 초기화용)
DELETE FROM study_log;
DELETE FROM users;

-- 테스트용 사용자 2명 삽입
INSERT INTO users (slack_user_id, slack_username)
VALUES ('U123456', 'jia');

INSERT INTO users (slack_user_id, slack_username)
VALUES ('U999999', 'other');

-- 'jia'라는 유저의 StudyLog 삽입
-- DAILY 테스트용: 2025-05-02 (1건)
-- WEEKLY 테스트용: 주간 범위 2025-04-28 ~ 2025-05-04 기준 (총 3건)
-- MONTHLY 테스트용: 2025년 5월 기준 (총 3건)
INSERT INTO study_log (content, timestamp, user_slack_user_id) VALUES
('오늘 자바 공부함', '2025-05-01T10:00:00', 'U123456'),
('스프링 JPA 복습', '2025-05-02T09:30:00', 'U123456'),
('OpenAI 연동 테스트', '2025-05-03T15:00:00', 'U123456');

-- 다른 유저의 StudyLog
INSERT INTO study_log (content, timestamp, user_slack_user_id) VALUES
('영어를 공부함', '2025-05-02T10:00:00', 'U999999');