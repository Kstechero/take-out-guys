CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scope` varchar(32) NOT NULL COMMENT 'user/admin',
  `owner_id` bigint NOT NULL COMMENT '用户ID或员工ID',
  `title` varchar(255) DEFAULT NULL,
  `last_message` varchar(2000) DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_session_owner` (`scope`,`owner_id`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话表';

CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `role` varchar(32) NOT NULL COMMENT 'system/user/assistant/tool',
  `content` text NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_message_session` (`session_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话消息表';

CREATE TABLE IF NOT EXISTS `dish_review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `dish_id` bigint NOT NULL,
  `rating` int NOT NULL,
  `content` varchar(500) NOT NULL,
  `images` text,
  `like_count` int NOT NULL DEFAULT '0',
  `status` int NOT NULL DEFAULT '1',
  `ai_generated` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_review_order_dish_user` (`order_id`,`dish_id`,`user_id`),
  KEY `idx_dish_review_dish` (`dish_id`,`create_time`),
  KEY `idx_dish_review_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品评价表';

CREATE TABLE IF NOT EXISTS `dish_review_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `review_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_like` (`review_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价点赞表';

CREATE TABLE IF NOT EXISTS `sensitive_word` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `word` varchar(100) NOT NULL,
  `level` int NOT NULL DEFAULT '1',
  `replacement` varchar(100) DEFAULT '***',
  `status` int NOT NULL DEFAULT '1',
  `hit_count` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sensitive_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

CREATE TABLE IF NOT EXISTS `customer_service_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `source` varchar(32) DEFAULT 'miniapp',
  `status` int NOT NULL DEFAULT '1' COMMENT '1进行中 2已结束',
  `last_message` varchar(500) DEFAULT NULL,
  `last_message_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `closed_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_customer_service_session_user` (`user_id`,`status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工客服会话表';

CREATE TABLE IF NOT EXISTS `customer_service_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `sender_type` varchar(32) NOT NULL COMMENT 'user/admin/system',
  `sender_id` bigint DEFAULT NULL,
  `message_type` varchar(32) NOT NULL DEFAULT 'text',
  `content` text NOT NULL,
  `flagged` int NOT NULL DEFAULT '0',
  `read_status` int NOT NULL DEFAULT '0' COMMENT '0未读 1已读',
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_customer_service_message_session` (`session_id`,`id`),
  KEY `idx_customer_service_message_read` (`session_id`,`sender_type`,`read_status`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工客服消息表';

ALTER TABLE `customer_service_message`
  ADD COLUMN IF NOT EXISTS `read_status` int NOT NULL DEFAULT '0' COMMENT '0未读 1已读' AFTER `flagged`;

ALTER TABLE `customer_service_message`
  ADD INDEX IF NOT EXISTS `idx_customer_service_message_read` (`session_id`,`sender_type`,`read_status`,`id`);
