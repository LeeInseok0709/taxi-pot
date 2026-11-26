-- 로컬 DB·계정 생성 (MySQL root 로 한 번 실행)
-- mysql -u root -p < db/create-database.sql
CREATE DATABASE IF NOT EXISTS taxipot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'taxipot'@'localhost' IDENTIFIED BY 'taxipot';
GRANT ALL PRIVILEGES ON taxipot.* TO 'taxipot'@'localhost';
FLUSH PRIVILEGES;
