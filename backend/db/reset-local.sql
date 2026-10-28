-- 로컬 DB 초기화 (모든 데이터 삭제 후 schema.sql 다시 실행)
-- mysql -u root -p -e "source db/reset-local.sql" && mysql -u root -p taxipot < db/schema.sql
DROP DATABASE IF EXISTS taxipot;
CREATE DATABASE taxipot DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
