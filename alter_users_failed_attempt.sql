-- SQL script to add failed_attempt and lock_time columns to USERS table in PostgreSQL / SQL Server
-- Execute this script in PostgreSQL (Database: AutoWash)

ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_attempt INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS lock_time TIMESTAMP;
