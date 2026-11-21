package com.aquadist.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Verifica e registra no log as pragmas efetivas do SQLite.
 * O modo WAL e configurado no JDBC URL para valer em todas as conexoes do pool.
 */
@Component
@ConditionalOnProperty(name = "spring.datasource.driver-class-name", havingValue = "org.sqlite.JDBC")
public class SqliteWalVerifier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SqliteWalVerifier.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public SqliteWalVerifier(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        String journalMode = jdbcTemplate.queryForObject(
                "PRAGMA journal_mode", (rs, rowNum) -> rs.getString(1));
        Integer synchronous = jdbcTemplate.queryForObject(
                "PRAGMA synchronous", (rs, rowNum) -> rs.getInt(1));
        Integer busyTimeout = jdbcTemplate.queryForObject(
                "PRAGMA busy_timeout", (rs, rowNum) -> rs.getInt(1));
        Integer foreignKeys = jdbcTemplate.queryForObject(
                "PRAGMA foreign_keys", (rs, rowNum) -> rs.getInt(1));

        int maxPool = dataSource instanceof HikariDataSource hikari
                ? hikari.getMaximumPoolSize()
                : -1;

        if ("wal".equalsIgnoreCase(journalMode)) {
            log.info("SQLite em modo WAL | journal_mode={} | synchronous={} (1=NORMAL) | "
                            + "busy_timeout={}ms | foreign_keys={} | pool_max={}",
                    journalMode, synchronous, busyTimeout, foreignKeys, maxPool);
        } else {
            log.warn("SQLite NAO esta em WAL | journal_mode={} | leitores podem ser bloqueados "
                    + "durante escritas. Verifique a URL JDBC.", journalMode);
        }
    }
}
