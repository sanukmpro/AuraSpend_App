CREATE DATABASE IF NOT EXISTS auraspend CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auraspend;
-- Spring Boot/JPA creates and updates app_users and expenses on first run.
-- For production, use a versioned migration tool and set ddl-auto=validate.
