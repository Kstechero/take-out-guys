-- 优惠券下单核销增量脚本；已有数据库执行一次。
ALTER TABLE `orders`
  ADD COLUMN `coupon_id` bigint DEFAULT NULL COMMENT '优惠券模板ID' AFTER `amount`,
  ADD COLUMN `original_amount` decimal(10,2) DEFAULT NULL COMMENT '优惠前金额' AFTER `coupon_id`,
  ADD COLUMN `discount_amount` decimal(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额' AFTER `original_amount`;

CREATE INDEX `idx_orders_coupon_id` ON `orders` (`coupon_id`);
