-- 택시팟 DB 스키마 — DB 설계서 13차 DDL 시트의 CREATE 문만 추출 (아래쪽 ALTER 문은 기존 DB 이전용이라 제외)
-- 새 DB 에서 한 번 실행: mysql -u root -p taxipot < db/schema.sql
-- 이 파일은 서버가 자동 실행하지 않는다 (resources 밖에 둠). 스키마 변경은 설계서 → 이 파일 → Entity 순서로.

SET NAMES utf8mb4;

-- 가입 가능 대학교 테이블 (7차 신규: 학교 고정 목록 + 이메일 도메인)
CREATE TABLE UNIVERSITY (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '대학교 고유번호',
  name VARCHAR(50) NOT NULL COMMENT '대학교명',
  email_domain VARCHAR(100) NOT NULL COMMENT '학교 이메일 도메인(@ 뒤, 소문자)',
  is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT '가입 허용 여부',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '등록일시',
  PRIMARY KEY (id),
  UNIQUE KEY uk_university_name (name),
  UNIQUE KEY uk_university_email_domain (email_domain),
  CONSTRAINT chk_university_email_domain CHECK (LOCATE('@', email_domain) = 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='가입 가능 대학교 테이블';

-- 가입 가능 학교 목록 (예시 1건: 팀에서 실제 학교·도메인으로 교체)
INSERT INTO UNIVERSITY (name, email_domain) VALUES ('한국대학교', 'university.ac.kr');

-- 회원 테이블
CREATE TABLE USERS (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '회원 고유번호',
  email VARCHAR(100) NULL COMMENT '대학교 이메일',
  pw_hash VARCHAR(255) NULL COMMENT '비밀번호 해시',
  name VARCHAR(50) NULL COMMENT '이름',
  nickname VARCHAR(30) NOT NULL COMMENT '닉네임',
  phone VARCHAR(255) NULL COMMENT '휴대폰번호(AES-256-GCM 암호문)',
  gender CHAR(1) NULL COMMENT '성별',
  university VARCHAR(50) NOT NULL COMMENT '소속 대학교',
  student_no VARCHAR(20) NULL COMMENT '학번',
  is_verified TINYINT(1) NOT NULL DEFAULT 0 COMMENT '학생 인증 완료 여부',
  rating_avg DECIMAL(4,2) NOT NULL DEFAULT 5.00 COMMENT '평점',
  no_show_count INT NOT NULL DEFAULT 0 COMMENT '노쇼 누적 횟수',
  report_count INT NOT NULL DEFAULT 0 COMMENT '신고 누적 횟수',
  clean_streak TINYINT NOT NULL DEFAULT 0 COMMENT '무신고 연속 이용 횟수',
  role VARCHAR(10) NOT NULL DEFAULT 'USER' COMMENT '권한',
  status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' COMMENT '계정 상태',
  suspended_until DATETIME NULL COMMENT '정지 만료일시',
  withdrawn_at DATETIME NULL COMMENT '탈퇴일시',
  token_version INT NOT NULL DEFAULT 0 COMMENT '토큰 버전',
  bank_code VARCHAR(10) NULL COMMENT '출금 은행 코드',
  account_no VARCHAR(255) NULL COMMENT '출금 계좌번호(AES-256-GCM 암호문)',
  account_holder VARCHAR(50) NULL COMMENT '예금주',
  fcm_token VARCHAR(255) NULL COMMENT '푸시 토큰',
  agreed_at DATETIME NOT NULL COMMENT '필수 약관 동의일시',
  agree_marketing TINYINT(1) NOT NULL DEFAULT 0 COMMENT '마케팅 수신 동의',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입일시',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_nickname (nickname),
  UNIQUE KEY uk_users_student_no (university, student_no),
  CONSTRAINT fk_users_university FOREIGN KEY (university) REFERENCES UNIVERSITY(name) ON UPDATE CASCADE,
  CONSTRAINT chk_users_gender CHECK (gender IN ('M', 'F')),
  CONSTRAINT chk_users_rating_avg_rng CHECK (rating_avg BETWEEN 1.00 AND 10.00),
  CONSTRAINT chk_users_clean_streak_rng CHECK (clean_streak >= 0),
  CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN')),
  CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'WITHDRAWN')),
  CONSTRAINT chk_users_token_version_rng CHECK (token_version >= 0),
  CONSTRAINT chk_users_account_all_or_none CHECK ((bank_code IS NULL AND account_no IS NULL AND account_holder IS NULL) OR (bank_code IS NOT NULL AND account_no IS NOT NULL AND account_holder IS NOT NULL)),
  CONSTRAINT chk_users_nickname_reserved CHECK (status = 'WITHDRAWN' OR LEFT(nickname, 5) <> '탈퇴회원_'),
  CONSTRAINT chk_users_pii_required CHECK (status = 'WITHDRAWN' OR (email IS NOT NULL AND pw_hash IS NOT NULL AND name IS NOT NULL AND phone IS NOT NULL AND gender IS NOT NULL AND student_no IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원 테이블';

-- 학생 인증 테이블
CREATE TABLE STUDENT_VERIFICATION (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '인증 고유번호',
  member_id BIGINT NOT NULL COMMENT '인증 대상 회원번호',
  student_no VARCHAR(20) NOT NULL COMMENT '제출 학번',
  email_code VARCHAR(10) NOT NULL COMMENT '이메일 인증코드',
  email_code_expires_at DATETIME NOT NULL COMMENT '인증코드 만료일시',
  email_code_attempts TINYINT NOT NULL DEFAULT 0 COMMENT '인증코드 입력 실패 횟수',
  email_status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT '이메일 인증 상태',
  capture_img_path VARCHAR(255) NULL COMMENT '학생정보 캡처 파일 경로(서버 로컬)',
  review_status VARCHAR(15) NOT NULL DEFAULT 'NOT_SUBMITTED' COMMENT '캡처 심사 상태',
  reject_reason VARCHAR(255) NULL COMMENT '반려 사유',
  reviewed_at DATETIME NULL COMMENT '심사일시',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '인증 요청일시',
  PRIMARY KEY (id),
  KEY idx_sv_member (member_id, created_at),
  KEY idx_sv_review (review_status, created_at),
  CONSTRAINT fk_student_verification_member_id FOREIGN KEY (member_id) REFERENCES USERS(id) ON DELETE CASCADE,
  CONSTRAINT chk_student_verification_email_status CHECK (email_status IN ('PENDING', 'VERIFIED')),
  CONSTRAINT chk_student_verification_review_status CHECK (review_status IN ('NOT_SUBMITTED', 'PENDING', 'APPROVED', 'REJECTED')),
  CONSTRAINT chk_student_verification_attempts_rng CHECK (email_code_attempts BETWEEN 0 AND 5),
  CONSTRAINT chk_student_verification_capture_state CHECK ((review_status = 'NOT_SUBMITTED' AND capture_img_path IS NULL) OR (review_status <> 'NOT_SUBMITTED' AND capture_img_path IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='학생 인증 테이블';

-- 택시팟 방 테이블
CREATE TABLE TAXIPOT (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '택시팟 고유번호',
  host_id BIGINT NOT NULL COMMENT '방장 회원번호',
  title VARCHAR(100) NOT NULL COMMENT '방 제목',
  departure_name VARCHAR(100) NOT NULL COMMENT '출발지 명칭',
  departure_lat DECIMAL(10,7) NOT NULL COMMENT '출발지 위도',
  departure_lng DECIMAL(11,7) NOT NULL COMMENT '출발지 경도',
  destination_name VARCHAR(100) NOT NULL COMMENT '도착지 명칭',
  destination_lat DECIMAL(10,7) NOT NULL COMMENT '도착지 위도',
  destination_lng DECIMAL(11,7) NOT NULL COMMENT '도착지 경도',
  departure_time DATETIME NOT NULL COMMENT '출발 예정 시각',
  arrival_deadline DATETIME NOT NULL COMMENT '집결 기한(= 참여 마감)',
  max_members TINYINT NOT NULL DEFAULT 4 COMMENT '모집 인원',
  current_members TINYINT NOT NULL DEFAULT 1 COMMENT '현재 참여 인원',
  gender_option VARCHAR(10) NOT NULL DEFAULT 'ALL' COMMENT '성별 제한 옵션',
  expected_fare INT NOT NULL COMMENT '예상 총 택시 요금',
  actual_fare INT NULL COMMENT '실제 택시 요금',
  receipt_img_path VARCHAR(255) NULL COMMENT '결제내역(영수증) 사진 파일 경로(서버 로컬)',
  started_at DATETIME NULL COMMENT '탑승 시작 일시',
  start_reminded_at DATETIME NULL COMMENT '출발 확인 알림 일시',
  fare_claim_status VARCHAR(10) NOT NULL DEFAULT 'NONE' COMMENT '실요금 청구 상태',
  fare_claimed_at DATETIME NULL COMMENT '실요금 청구 일시',
  fare_reviewed_at DATETIME NULL COMMENT '청구 확인 일시',
  fare_reject_reason VARCHAR(255) NULL COMMENT '청구 반려 사유',
  status VARCHAR(10) NOT NULL DEFAULT 'RECRUITING' COMMENT '방 상태',
  has_canceled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '취소 발생 여부',
  open_chat_url VARCHAR(500) NULL COMMENT '오픈채팅 링크',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '방 개설일시',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (id),
  KEY idx_pot_list (status, has_canceled, departure_time),
  KEY idx_pot_fare_claim (fare_claim_status, fare_claimed_at),
  KEY idx_pot_departure (departure_name, departure_time),
  CONSTRAINT fk_taxipot_host_id FOREIGN KEY (host_id) REFERENCES USERS(id),
  CONSTRAINT chk_taxipot_max_members_rng CHECK (max_members IN (3, 4)),
  CONSTRAINT chk_taxipot_current_members_rng CHECK (current_members BETWEEN 1 AND max_members),
  CONSTRAINT chk_taxipot_gender_option CHECK (gender_option IN ('ALL', 'MALE', 'FEMALE')),
  CONSTRAINT chk_taxipot_expected_fare_rng CHECK (expected_fare >= 0),
  CONSTRAINT chk_taxipot_actual_fare_rng CHECK (actual_fare >= 0),
  CONSTRAINT chk_taxipot_deadline_order CHECK (arrival_deadline < departure_time),
  CONSTRAINT chk_taxipot_completed_fare CHECK (status <> 'COMPLETED' OR actual_fare IS NOT NULL),
  CONSTRAINT chk_taxipot_fare_claim_status CHECK (fare_claim_status IN ('NONE', 'PENDING', 'APPROVED', 'REJECTED', 'OVERDUE')),
  CONSTRAINT chk_taxipot_completed_claim CHECK (status <> 'COMPLETED' OR fare_claim_status = 'APPROVED'),
  CONSTRAINT chk_taxipot_claim_fare CHECK (fare_claim_status NOT IN ('PENDING', 'APPROVED') OR actual_fare IS NOT NULL),
  CONSTRAINT chk_taxipot_claim_reject CHECK (fare_claim_status <> 'REJECTED' OR fare_reject_reason IS NOT NULL),
  CONSTRAINT chk_taxipot_status CHECK (status IN ('RECRUITING', 'CLOSED', 'RUNNING', 'COMPLETED', 'CANCELED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='택시팟 방 테이블';

-- 택시팟 참여자 테이블
CREATE TABLE TAXIPOT_MEMBER (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '참여 고유번호',
  pot_id BIGINT NOT NULL COMMENT '택시팟 번호',
  member_id BIGINT NOT NULL COMMENT '회원 번호',
  role VARCHAR(10) NOT NULL DEFAULT 'GUEST' COMMENT '역할',
  preferred_seat VARCHAR(12) NOT NULL DEFAULT 'NONE' COMMENT '희망 좌석',
  status VARCHAR(10) NOT NULL DEFAULT 'JOINED' COMMENT '참여 상태',
  arrived_at DATETIME NULL COMMENT '도착 확인 일시',
  joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '참여 신청 일시',
  canceled_at DATETIME NULL COMMENT '취소·퇴장 일시',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tpm_pot_member (pot_id, member_id),
  KEY idx_tpm_member_status (member_id, status),
  CONSTRAINT fk_taxipot_member_pot_id FOREIGN KEY (pot_id) REFERENCES TAXIPOT(id),
  CONSTRAINT fk_taxipot_member_member_id FOREIGN KEY (member_id) REFERENCES USERS(id),
  CONSTRAINT chk_taxipot_member_role CHECK (role IN ('HOST', 'GUEST')),
  CONSTRAINT chk_taxipot_member_preferred_seat CHECK (preferred_seat IN ('FRONT', 'BACK_LEFT', 'BACK_MID', 'BACK_RIGHT', 'NONE')),
  CONSTRAINT chk_taxipot_member_status CHECK (status IN ('JOINED', 'CANCELED', 'BOARDED', 'NO_SHOW', 'KICKED', 'REQUESTED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='택시팟 참여자 테이블';

-- 가상 지갑 테이블
CREATE TABLE WALLET (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '지갑 고유번호',
  member_id BIGINT NOT NULL COMMENT '소유 회원번호',
  balance INT NOT NULL DEFAULT 0 COMMENT '사용 가능 잔액',
  held_balance INT NOT NULL DEFAULT 0 COMMENT '예치 금액',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wallet_member_id (member_id),
  CONSTRAINT fk_wallet_member_id FOREIGN KEY (member_id) REFERENCES USERS(id),
  CONSTRAINT chk_wallet_balance_rng CHECK (balance >= 0),
  CONSTRAINT chk_wallet_held_balance_rng CHECK (held_balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='가상 지갑 테이블';

-- 충전 결제 테이블
CREATE TABLE PAYMENT (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '결제 고유번호',
  member_id BIGINT NOT NULL COMMENT '결제 회원번호',
  order_id VARCHAR(64) NOT NULL COMMENT '주문번호',
  pg_provider VARCHAR(20) NOT NULL DEFAULT 'PORTONE' COMMENT 'PG사',
  pg_tid VARCHAR(100) NULL COMMENT 'PG 거래번호',
  amount INT NOT NULL COMMENT '충전 결제 금액',
  status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT '결제 상태',
  paid_at DATETIME NULL COMMENT '결제 승인 시각',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '결제 요청일시',
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_order_id (order_id),
  UNIQUE KEY uk_payment_pg_tid (pg_tid),
  KEY idx_payment_member (member_id, created_at),
  CONSTRAINT fk_payment_member_id FOREIGN KEY (member_id) REFERENCES USERS(id),
  CONSTRAINT chk_payment_pg_provider CHECK (pg_provider IN ('PORTONE', 'DUMMY')),
  CONSTRAINT chk_payment_amount_rng CHECK (amount > 0),
  CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='충전 결제 테이블';

-- 출금 테이블 (9차 신규: 등록 계좌로 잔액 출금, 시연은 더미 송금)
CREATE TABLE WITHDRAWAL (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '출금 고유번호',
  member_id BIGINT NOT NULL COMMENT '출금 회원번호',
  amount INT NOT NULL COMMENT '출금 금액',
  bank_code VARCHAR(10) NULL COMMENT '은행 코드(신청 시점)',
  account_no VARCHAR(255) NULL COMMENT '계좌번호(AES-256-GCM 암호문, 신청 시점)',
  account_holder VARCHAR(50) NULL COMMENT '예금주(신청 시점)',
  transfer_provider VARCHAR(20) NOT NULL DEFAULT 'DUMMY' COMMENT '송금 처리 방식',
  transfer_tid VARCHAR(100) NULL COMMENT '송금 거래번호',
  status VARCHAR(10) NOT NULL DEFAULT 'REQUESTED' COMMENT '출금 상태',
  fail_reason VARCHAR(255) NULL COMMENT '실패 사유',
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '출금 신청 일시',
  completed_at DATETIME NULL COMMENT '처리 완료 일시',
  PRIMARY KEY (id),
  UNIQUE KEY uk_withdrawal_tid (transfer_tid),
  KEY idx_withdrawal_member (member_id, requested_at),
  CONSTRAINT fk_withdrawal_member_id FOREIGN KEY (member_id) REFERENCES USERS(id),
  CONSTRAINT chk_withdrawal_amount_rng CHECK (amount >= 1000),
  CONSTRAINT chk_withdrawal_provider CHECK (transfer_provider IN ('DUMMY')),
  CONSTRAINT chk_withdrawal_status CHECK (status IN ('REQUESTED', 'COMPLETED', 'FAILED')),
  CONSTRAINT chk_withdrawal_done CHECK ((status = 'REQUESTED' AND completed_at IS NULL) OR (status = 'COMPLETED' AND completed_at IS NOT NULL AND transfer_tid IS NOT NULL) OR (status = 'FAILED' AND completed_at IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='출금 테이블';

-- 포인트 원장 테이블
CREATE TABLE POINT_HISTORY (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '원장 고유번호',
  member_id BIGINT NOT NULL COMMENT '거래 회원번호',
  pot_id BIGINT NULL COMMENT '택시팟 번호',
  payment_id BIGINT NULL COMMENT '충전 결제 번호',
  withdrawal_id BIGINT NULL COMMENT '출금 번호',
  type VARCHAR(15) NOT NULL COMMENT '거래 유형',
  amount INT NOT NULL COMMENT '증감 금액',
  balance_after INT NOT NULL COMMENT '거래 후 잔액',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '거래 일시',
  PRIMARY KEY (id),
  KEY idx_ph_member (member_id, created_at),
  KEY idx_ph_pot (pot_id),
  CONSTRAINT fk_point_history_member_id FOREIGN KEY (member_id) REFERENCES USERS(id),
  CONSTRAINT fk_point_history_pot_id FOREIGN KEY (pot_id) REFERENCES TAXIPOT(id),
  CONSTRAINT fk_point_history_payment_id FOREIGN KEY (payment_id) REFERENCES PAYMENT(id),
  CONSTRAINT fk_point_history_withdrawal_id FOREIGN KEY (withdrawal_id) REFERENCES WITHDRAWAL(id),
  CONSTRAINT chk_point_history_type CHECK (type IN ('CHARGE', 'CHARGE_REFUND', 'ADMIN_ADJUST', 'FARE_HOLD', 'FARE_REFUND', 'FARE_ADJUST', 'FARE_PAY', 'FEE', 'HOST_PAYOUT', 'DEBT_PAY', 'BALANCE_EXPIRE', 'WITHDRAW', 'WITHDRAW_REFUND')),
  CONSTRAINT chk_point_history_payment_link CHECK ((type IN ('CHARGE', 'CHARGE_REFUND') AND payment_id IS NOT NULL) OR (type NOT IN ('CHARGE', 'CHARGE_REFUND') AND payment_id IS NULL)),
  CONSTRAINT chk_point_history_balance_after_rng CHECK (balance_after >= 0),
  CONSTRAINT chk_point_history_withdrawal_link CHECK ((type IN ('WITHDRAW', 'WITHDRAW_REFUND') AND withdrawal_id IS NOT NULL) OR (type NOT IN ('WITHDRAW', 'WITHDRAW_REFUND') AND withdrawal_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='포인트 원장 테이블';

-- 정산 테이블
CREATE TABLE SETTLEMENT (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '정산 고유번호',
  pot_id BIGINT NOT NULL COMMENT '택시팟 번호',
  member_id BIGINT NOT NULL COMMENT '회원 번호',
  held_amount INT NOT NULL COMMENT '선결제 예치액',
  fare_share INT NULL COMMENT '최종 택시비 분담금',
  fee_amount INT NULL COMMENT '서비스 수수료(정산 시점 기록)',
  diff_amount INT NULL COMMENT '정산 차액',
  uncovered_amount INT NOT NULL DEFAULT 0 COMMENT '미수 부족분',
  debt_status VARCHAR(10) NOT NULL DEFAULT 'NONE' COMMENT '미수금 상태',
  debt_amount INT NOT NULL DEFAULT 0 COMMENT '남은 미수 납부액',
  debt_due_at DATETIME NULL COMMENT '미수금 납부 기한',
  debt_paid_at DATETIME NULL COMMENT '미수금 납부 완료 시각',
  status VARCHAR(10) NOT NULL DEFAULT 'HELD' COMMENT '정산 상태',
  settled_at DATETIME NULL COMMENT '정산 완료 시각',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '예치 일시',
  PRIMARY KEY (id),
  UNIQUE KEY uk_settle_pot_member (pot_id, member_id),
  KEY idx_settle_member (member_id, status),
  KEY idx_settle_debt (debt_status, debt_due_at),
  CONSTRAINT fk_settlement_pot_id FOREIGN KEY (pot_id) REFERENCES TAXIPOT(id),
  CONSTRAINT fk_settlement_member_id FOREIGN KEY (member_id) REFERENCES USERS(id),
  CONSTRAINT chk_settlement_held_amount_rng CHECK (held_amount >= 0),
  CONSTRAINT chk_settlement_fee_amount_rng CHECK (fee_amount >= 0),
  CONSTRAINT chk_settlement_uncovered_rng CHECK (uncovered_amount >= 0),
  CONSTRAINT chk_settlement_debt_amount_rng CHECK (debt_amount >= 0),
  CONSTRAINT chk_settlement_status CHECK (status IN ('HELD', 'SETTLED', 'REFUNDED')),
  CONSTRAINT chk_settlement_debt_status CHECK (debt_status IN ('NONE', 'UNPAID', 'OVERDUE', 'PAID')),
  CONSTRAINT chk_settlement_settled_values CHECK ((status = 'SETTLED' AND fare_share IS NOT NULL AND fee_amount IS NOT NULL AND diff_amount IS NOT NULL) OR (status <> 'SETTLED' AND fare_share IS NULL AND fee_amount IS NULL AND diff_amount IS NULL AND uncovered_amount = 0 AND debt_status = 'NONE')),
  CONSTRAINT chk_settlement_debt_state CHECK ((debt_status = 'NONE' AND debt_amount = 0 AND debt_due_at IS NULL) OR (debt_status IN ('UNPAID', 'OVERDUE') AND debt_amount > 0 AND debt_due_at IS NOT NULL) OR (debt_status = 'PAID' AND debt_amount = 0 AND debt_paid_at IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='정산 테이블';

-- 동승자 평가 테이블
CREATE TABLE RATING (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '평가 고유번호',
  pot_id BIGINT NOT NULL COMMENT '택시팟 번호',
  reviewer_id BIGINT NOT NULL COMMENT '평가자 회원번호',
  reviewee_id BIGINT NOT NULL COMMENT '피평가자 회원번호',
  noshow_score TINYINT NOT NULL DEFAULT 3 COMMENT '시간 약속 별점(1~5)',
  manner_score TINYINT NOT NULL DEFAULT 3 COMMENT '언행·태도 별점(1~5)',
  communication_score TINYINT NOT NULL DEFAULT 3 COMMENT '연락 별점(1~5)',
  payment_score TINYINT NOT NULL DEFAULT 3 COMMENT '정산 별점(1~5)',
  avg_score DECIMAL(3,2) AS ((noshow_score + manner_score + communication_score + payment_score) / 4) STORED COMMENT '4항목 평균 별점',
  bad_reason VARCHAR(255) NULL COMMENT '별로였어요 사유',
  is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '자동 기본 평가 여부',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '평가 일시',
  PRIMARY KEY (id),
  UNIQUE KEY uk_rating_pair (pot_id, reviewer_id, reviewee_id),
  KEY idx_rating_reviewee (reviewee_id),
  CONSTRAINT fk_rating_pot_id FOREIGN KEY (pot_id) REFERENCES TAXIPOT(id),
  CONSTRAINT fk_rating_reviewer_id FOREIGN KEY (reviewer_id) REFERENCES USERS(id),
  CONSTRAINT fk_rating_reviewee_id FOREIGN KEY (reviewee_id) REFERENCES USERS(id),
  CONSTRAINT chk_rating_noshow_score_rng CHECK (noshow_score BETWEEN 1 AND 5),
  CONSTRAINT chk_rating_manner_score_rng CHECK (manner_score BETWEEN 1 AND 5),
  CONSTRAINT chk_rating_communication_score_rng CHECK (communication_score BETWEEN 1 AND 5),
  CONSTRAINT chk_rating_payment_score_rng CHECK (payment_score BETWEEN 1 AND 5),
  CONSTRAINT chk_rating_default_values CHECK (is_default = 0 OR (noshow_score = 3 AND manner_score = 3 AND communication_score = 3 AND payment_score = 3 AND bad_reason IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='동승자 평가 테이블';

-- 신고 테이블
CREATE TABLE REPORT (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '신고 고유번호',
  reporter_id BIGINT NOT NULL COMMENT '신고자 회원번호',
  reported_id BIGINT NOT NULL COMMENT '피신고자 회원번호',
  pot_id BIGINT NOT NULL COMMENT '택시팟 번호',
  type VARCHAR(10) NOT NULL COMMENT '신고 유형',
  reason VARCHAR(255) NOT NULL COMMENT '신고 사유',
  status VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT '처리 상태',
  processed_at DATETIME NULL COMMENT '처리 일시',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '신고 일시',
  PRIMARY KEY (id),
  UNIQUE KEY uk_report_dup (pot_id, reporter_id, reported_id),
  KEY idx_report_reported (reported_id, status),
  KEY idx_report_status (status, created_at),
  CONSTRAINT fk_report_reporter_id FOREIGN KEY (reporter_id) REFERENCES USERS(id),
  CONSTRAINT fk_report_reported_id FOREIGN KEY (reported_id) REFERENCES USERS(id),
  CONSTRAINT fk_report_pot_id FOREIGN KEY (pot_id) REFERENCES TAXIPOT(id),
  CONSTRAINT chk_report_type CHECK (type IN ('NO_SHOW', 'ABUSIVE', 'ETC')),
  CONSTRAINT chk_report_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='신고 테이블';

-- 채팅 메시지 테이블
CREATE TABLE CHAT_MESSAGE (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '메시지 고유번호',
  pot_id BIGINT NOT NULL COMMENT '택시팟 번호',
  sender_id BIGINT NOT NULL COMMENT '발신 회원번호',
  msg_type VARCHAR(10) NOT NULL DEFAULT 'TEXT' COMMENT '메시지 유형',
  content VARCHAR(1000) NOT NULL COMMENT '메시지 내용',
  sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '전송 일시',
  PRIMARY KEY (id),
  KEY idx_chat_pot_sent (pot_id, sent_at),
  CONSTRAINT fk_chat_message_pot_id FOREIGN KEY (pot_id) REFERENCES TAXIPOT(id),
  CONSTRAINT fk_chat_message_sender_id FOREIGN KEY (sender_id) REFERENCES USERS(id),
  CONSTRAINT chk_chat_message_msg_type CHECK (msg_type IN ('TEXT', 'QUICK'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='채팅 메시지 테이블';

-- 알림 테이블
CREATE TABLE NOTIFICATION (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '알림 고유번호',
  member_id BIGINT NOT NULL COMMENT '수신 회원번호',
  pot_id BIGINT NULL COMMENT '택시팟 번호',
  type VARCHAR(20) NOT NULL COMMENT '알림 유형',
  title VARCHAR(100) NOT NULL COMMENT '알림 제목',
  content VARCHAR(300) NOT NULL COMMENT '알림 내용',
  is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '읽음 여부',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발송 일시',
  PRIMARY KEY (id),
  KEY idx_noti_member (member_id, is_read, created_at),
  CONSTRAINT fk_notification_member_id FOREIGN KEY (member_id) REFERENCES USERS(id),
  CONSTRAINT fk_notification_pot_id FOREIGN KEY (pot_id) REFERENCES TAXIPOT(id),
  CONSTRAINT chk_notification_type CHECK (type IN ('POT', 'PAYMENT', 'LATE_WARNING', 'KICKED', 'RATING_REQUEST', 'REPORT_RESULT', 'SYSTEM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림 테이블';
