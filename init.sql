-- ── Finance Tracker — Database Schema ───────────────────────────────────────
-- This file is mounted to /docker-entrypoint-initdb.d/ in the MySQL container.
-- MySQL runs it ONLY once — on the very first startup when the data volume is empty.
-- Safe to re-run: all statements use IF NOT EXISTS.

CREATE DATABASE IF NOT EXISTS financeTracker
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE financeTracker;

CREATE TABLE IF NOT EXISTS `users` (
  `id`                bigint       NOT NULL AUTO_INCREMENT,
  `username`          varchar(50)  NOT NULL,
  `password`          varchar(255) NOT NULL,
  `email`             varchar(255) DEFAULT NULL,
  `profile_image_url` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `categories` (
  `id`      bigint               NOT NULL AUTO_INCREMENT,
  `name`    varchar(100)         NOT NULL,
  `type`    enum('INCOME','EXPENSE') NOT NULL,
  `user_id` bigint               NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_user_cat` (`user_id`,`name`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `transactions` (
  `id`          bigint               NOT NULL AUTO_INCREMENT,
  `text`        varchar(255)         NOT NULL,
  `amount`      decimal(10,2)        NOT NULL,
  `date`        date                 NOT NULL,
  `user_id`     bigint               DEFAULT NULL,
  `type`        enum('INCOME','EXPENSE') NOT NULL,
  `note`        text,
  `category_id` bigint               NOT NULL,
  `image_url`   varchar(500)         DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tra_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `budgets` (
  `id`           bigint        NOT NULL AUTO_INCREMENT,
  `user_id`      bigint        NOT NULL,
  `category_id`  bigint        NOT NULL,
  `limit_amount` decimal(12,2) NOT NULL,
  `month`        varchar(7)    NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_budget` (`user_id`,`category_id`,`month`),
  CONSTRAINT `budgets_ibfk_1` FOREIGN KEY (`user_id`)     REFERENCES `users` (`id`),
  CONSTRAINT `budgets_ibfk_2` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `savings_goals` (
  `id`             bigint        NOT NULL AUTO_INCREMENT,
  `user_id`        bigint        NOT NULL,
  `name`           varchar(255)  NOT NULL,
  `icon`           varchar(10)   NOT NULL DEFAULT '🎯',
  `target_amount`  decimal(15,2) NOT NULL,
  `current_amount` decimal(15,2) NOT NULL DEFAULT '0.00',
  `deadline`       date          DEFAULT NULL,
  `created_at`     date          NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `savings_goals_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `account` (
  `id`      int DEFAULT NULL,
  `balance` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `recurring_transactions` (
  `id`            bigint       NOT NULL AUTO_INCREMENT,
  `user_id`       bigint       NOT NULL,
  `text`          varchar(255) NOT NULL,
  `amount`        double       NOT NULL,
  `type`          varchar(10)  NOT NULL,
  `category_id`   bigint       DEFAULT 0,
  `frequency`     varchar(20)  NOT NULL,
  `next_due_date` date         NOT NULL,
  `is_active`     tinyint(1)   DEFAULT 1,
  `note`          varchar(500) DEFAULT NULL,
  `created_at`    timestamp    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `recurring_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
