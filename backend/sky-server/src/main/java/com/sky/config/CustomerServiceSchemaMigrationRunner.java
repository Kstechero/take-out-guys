package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomerServiceSchemaMigrationRunner implements ApplicationRunner {

    private static final String MESSAGE_TABLE = "customer_service_message";
    private static final String READ_STATUS_COLUMN = "read_status";
    private static final String READ_STATUS_INDEX = "idx_customer_service_message_read";

    private final JdbcTemplate jdbcTemplate;

    public CustomerServiceSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureReadStatusColumn();
        ensureReadStatusIndex();
    }

    private void ensureReadStatusColumn() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from information_schema.columns " +
                        "where table_schema = database() and table_name = ? and column_name = ?",
                Integer.class,
                MESSAGE_TABLE,
                READ_STATUS_COLUMN
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("alter table " + MESSAGE_TABLE
                + " add column " + READ_STATUS_COLUMN
                + " int not null default 0 comment '0未读 1已读' after flagged");
        log.info("Added {}.{} column for customer service read receipts", MESSAGE_TABLE, READ_STATUS_COLUMN);
    }

    private void ensureReadStatusIndex() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from information_schema.statistics " +
                        "where table_schema = database() and table_name = ? and index_name = ?",
                Integer.class,
                MESSAGE_TABLE,
                READ_STATUS_INDEX
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("alter table " + MESSAGE_TABLE
                + " add index " + READ_STATUS_INDEX + " (session_id, sender_type, read_status, id)");
        log.info("Added {} index for customer service unread aggregation", READ_STATUS_INDEX);
    }
}
