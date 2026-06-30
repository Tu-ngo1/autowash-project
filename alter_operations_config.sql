-- SQL Script to add 'is_active' column to DAILY_OPERATIONS_CONFIG if it does not exist
-- This ensures existing rows will have the default value '1' (true) and prevents null constraints violation.

IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('DAILY_OPERATIONS_CONFIG') AND name = 'is_active'
)
BEGIN
    ALTER TABLE DAILY_OPERATIONS_CONFIG ADD is_active BIT NOT NULL DEFAULT 1;
    PRINT 'Added column is_active to DAILY_OPERATIONS_CONFIG table.';
END
ELSE
BEGIN
    PRINT 'Column is_active already exists in DAILY_OPERATIONS_CONFIG table.';
END
