package com.campus.material.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Profile("screenshot")
public class ScreenshotDatabaseInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final Resource schemaScript;
    private final Resource dataScript;

    public ScreenshotDatabaseInitializer(JdbcTemplate jdbcTemplate,
                                         DataSource dataSource,
                                         @Value("classpath:schema-screenshot.sql") Resource schemaScript,
                                         @Value("classpath:data-screenshot.sql") Resource dataScript) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.schemaScript = schemaScript;
        this.dataScript = dataScript;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (hasCoreSchema()) {
            return;
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(false, false, "UTF-8", schemaScript, dataScript);
        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    private boolean hasCoreSchema() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'SYS_USER'",
                Integer.class
        );
        return tableCount != null && tableCount > 0;
    }
}
