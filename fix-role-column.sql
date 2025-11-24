-- Fix role column length in users table
ALTER TABLE users MODIFY COLUMN role VARCHAR(20);
