package com.hutech.demo.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time migration: allow products.category_id to be NULL when product is assigned to parent category only.
 */
@Component
@Order(2)
public class ProductCategoryNullableMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ProductCategoryNullableMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE products MODIFY COLUMN category_id BIGINT NULL"
            );
        } catch (Exception e) {
            // Column may already be nullable or DB might use different syntax; ignore
        }
    }
}
