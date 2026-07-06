-- Add is_blocked column to users table for admin block/unblock functionality
ALTER TABLE users ADD COLUMN is_blocked BOOLEAN NOT NULL DEFAULT FALSE;
