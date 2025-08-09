-- Tenant 1 database initialization
CREATE DATABASE IF NOT EXISTS tenant1_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE tenant1_db;

-- Create additional user for application
CREATE USER IF NOT EXISTS 'appuser'@'%' IDENTIFIED BY 'apppassword';
GRANT ALL PRIVILEGES ON tenant1_db.* TO 'appuser'@'%';

-- Create indexes for better performance
-- These will be created by Hibernate, but we can add custom indexes here if needed

FLUSH PRIVILEGES;