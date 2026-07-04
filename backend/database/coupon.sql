-- 已有数据库单独执行此增量脚本。
CREATE TABLE IF NOT EXISTS `coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '优惠券名称',
  `type` int NOT NULL DEFAULT 1 COMMENT '1满减券 2折扣券 3新人券',
  `discount_amount` decimal(10,2) NOT NULL,
  `minimum_amount` decimal(10,2) NOT NULL DEFAULT 0,
  `total_count` int NOT NULL,
  `remaining_count` int NOT NULL,
  `per_user_limit` int NOT NULL DEFAULT 1,
  `valid_from` datetime NOT NULL,
  `valid_until` datetime NOT NULL,
  `status` int NOT NULL DEFAULT 0 COMMENT '0停用 1启用',
  `description` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_coupon_available` (`status`,`valid_from`,`valid_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券';

CREATE TABLE IF NOT EXISTS `user_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `coupon_id` bigint NOT NULL,
  `status` int NOT NULL DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
  `receive_time` datetime NOT NULL,
  `use_time` datetime DEFAULT NULL,
  `order_id` bigint DEFAULT NULL,
  `expire_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_coupon_user_status` (`user_id`,`status`,`expire_time`),
  KEY `idx_user_coupon_coupon` (`coupon_id`),
  CONSTRAINT `fk_user_coupon_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';
