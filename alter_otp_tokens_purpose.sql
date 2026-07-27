-- SQL script to update CHECK constraint for purpose column in otp_tokens table
-- Execute this script in PostgreSQL (Database: AutoWash) to allow FORGOT_PASSWORD

ALTER TABLE otp_tokens DROP CONSTRAINT IF EXISTS otp_tokens_purpose_check;

ALTER TABLE otp_tokens ADD CONSTRAINT otp_tokens_purpose_check 
    CHECK (purpose IN ('REGISTER', 'FORGOT_PASSWORD'));
