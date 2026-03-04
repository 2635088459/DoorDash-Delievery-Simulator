package com.shydelivery.doordashsimulator.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    @Order(0)
    public ApplicationRunner updateUserRoleConstraint() {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
                jdbcTemplate.execute(
                    "ALTER TABLE users ADD CONSTRAINT users_role_check " +
                        "CHECK (role IN ('CUSTOMER','RESTAURANT_OWNER','DRIVER','ADMIN'))"
                );
                log.info("Ensured users_role_check includes ADMIN role");
            } catch (Exception ex) {
                log.warn("Failed to update users_role_check constraint: {}", ex.getMessage());
            }
        };
    }

    @Bean
    @Order(1)
    public ApplicationRunner ensureTicketActionLogsTable() {
        return args -> {
            try {
                jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS ticket_action_logs (" +
                        "id BIGSERIAL PRIMARY KEY," +
                        "ticket_id BIGINT NOT NULL," +
                        "action_type VARCHAR(100) NOT NULL," +
                        "status VARCHAR(30) NOT NULL," +
                        "operator VARCHAR(200)," +
                        "note TEXT," +
                        "result_message TEXT," +
                        "created_at TIMESTAMP NOT NULL DEFAULT NOW()," +
                        "updated_at TIMESTAMP NOT NULL DEFAULT NOW()" +
                    ")"
                );
                jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_ticket_action_ticket ON ticket_action_logs(ticket_id)"
                );
                jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_ticket_action_status ON ticket_action_logs(status)"
                );
                jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_ticket_action_created ON ticket_action_logs(created_at)"
                );
                log.info("Ensured ticket_action_logs table exists");
            } catch (Exception ex) {
                log.warn("Failed to ensure ticket_action_logs table: {}", ex.getMessage());
            }
        };
    }

    @Bean
    @Order(2)
    public ApplicationRunner updateTicketCategoryConstraint() {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE tickets DROP CONSTRAINT IF EXISTS tickets_category_check");
                jdbcTemplate.execute(
                    "ALTER TABLE tickets ADD CONSTRAINT tickets_category_check " +
                        "CHECK (category IN (" +
                        "'RESTAURANT_CANCEL_SPIKE'," +
                        "'DELIVERY_DELAY_SPIKE'," +
                        "'DELIVERY_TIMEOUT_SPIKE'," +
                        "'PAYMENT_REFUND_SPIKE'," +
                        "'PAYMENT_ISSUE'," +
                        "'DRIVER_ISSUE'," +
                        "'OTHER'))"
                );
                log.info("Ensured tickets_category_check includes payment/driver categories");
            } catch (Exception ex) {
                log.warn("Failed to update tickets_category_check constraint: {}", ex.getMessage());
            }
        };
    }
}