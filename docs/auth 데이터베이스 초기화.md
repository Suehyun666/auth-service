# hts\_auth 데이터베이스 구축 및 테스트

## 0\. 관리자 설정 (User & DB 생성 및 권한 부여)

```bash
# postgres 계정으로 전환 및 접속
sudo -i -u postgres
psql

# 1. 사용자 생성 (비밀번호 설정)
CREATE USER hts WITH PASSWORD 'password123';

# 2. 데이터베이스 생성 (소유자는 postgres, 이후 권한 부여)
CREATE DATABASE hts_auth;

# 3. hts 사용자에게 모든 권한 부여
GRANT ALL PRIVILEGES ON DATABASE hts_auth TO hts;

# \q로 종료 (혹은 DB 전환)
\q
```

## 1\. 로그인 (hts 사용자)

```bash
# hts 사용자로 hts_auth 데이터베이스 접속
psql -U hts -d hts_auth -h localhost
```

## 2\. 테이블 생성 (Schema 초기화)

```sql
-- accounts 테이블 생성 예시
CREATE TABLE IF NOT EXISTS accounts (
    account_id BIGINT PRIMARY KEY,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL
);

-- 생성된 테이블 소유권 확인/변경 (필요시)
ALTER TABLE accounts OWNER TO hts;
```

## 3\. 더미 데이터 입력 (Bulk Insert)

```sql
DO $$
BEGIN
INSERT INTO accounts (account_id, password_hash, salt)
SELECT
    i,
    i::TEXT,
    'dummy_salt'
FROM
    GENERATE_SERIES(1, 3001) AS t(i);
END $$;
```

## 4\. 데이터 조회 및 확인

```sql
-- 데이터 개수 확인
SELECT count(*) FROM accounts;

-- 상위 10개 조회
SELECT * FROM accounts LIMIT 10;
```

## 5\. 데이터 초기화 (Truncate)

```sql
-- 데이터만 빠르게 삭제 (구조 유지)
TRUNCATE TABLE accounts;

-- (옵션) 시퀀스 재설정이 필요한 경우
-- TRUNCATE TABLE accounts RESTART IDENTITY;
```

## 6\. 스키마 삭제 (Drop)

```sql
-- 테이블 목록 확인
\dt

-- 테이블 삭제
DROP TABLE IF EXISTS accounts;
```