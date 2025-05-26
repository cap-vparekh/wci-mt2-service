/*
 * Copyright 2023 SNOMED International - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of SNOMED International
 * The intellectual and technical concepts contained herein are proprietary to
 * SNOMED International and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */

package org.ihtsdo.refsetservice.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.flywaydb.core.Flyway;
import org.ihtsdo.refsetservice.util.PropertyUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Set up the migration strategy.
 */
@Configuration
@DependsOn("propertyUtility")
public class PersistenceConfiguration {

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(PersistenceConfiguration.class);

    /** The config properties. */
    private final Properties properties = PropertyUtility.getProperties();

    /**
     * Instantiates an empty {@link PersistenceConfiguration}.
     */
    public PersistenceConfiguration() {

        LOG.debug("Creating instance of class FlywayConfiguration");
    }

    /**
     * flyway migration strategy.
     *
     * @return the migration strategy
     */
    @Bean
    public FlywayMigrationStrategy customMigrationStrategy() {

        final FlywayMigrationStrategy strategy = new FlywayMigrationStrategy() {

            @Override
            public void migrate(final Flyway flyway) {

                LOG.debug("customMigrationStrategy SHOULD BE MIGRATING");
                flyway.migrate();
            }
        };

        return strategy;
    }

    /**
     * custom flyway class.
     * 
     * @return the custom flyway class
     * @throws Exception the exception
     */
    @Bean
    public Flyway customFlyway() throws Exception {

        final String dbName = properties.getProperty("flyway.database.name");
        final String jdbcUrl = properties.getProperty("flyway.url");
        final String user = properties.getProperty("spring.jpa.properties.hibernate.connection.username");
        final String pwd = properties.getProperty("spring.jpa.properties.hibernate.connection.password");
        final String location = properties.getProperty("flyway.locations");
        final Map<String, String> placeholders = new HashMap<>();

        if (jdbcUrl.toLowerCase().startsWith("jdbc:mysql")) {

            placeholders.put("pre_if_exists", "if exists");
            placeholders.put("post_if_exists", "");
            placeholders.put("create_mapping_events_seq", "CREATE SEQUENCE mapping_events_seq;");
            placeholders.put("auto_increment", "set default nextval('mapping_events_seq')");
            placeholders.put("jsonb", "jsonb");

        } else if (jdbcUrl.toLowerCase().startsWith("jdbc:h2")) {

            placeholders.put("pre_if_exists", "");
            placeholders.put("post_if_exists", "if exists");
            placeholders.put("create_mapping_events_seq", "");
            placeholders.put("auto_increment", "BIGINT auto_increment");
            placeholders.put("jsonb", "text");
        } else {
            throw new RuntimeException("Unhandled database url: " + jdbcUrl);
        }

        return Flyway.configure().createSchemas(true).schemas(dbName).dataSource(jdbcUrl, user, pwd).locations(location).placeholders(placeholders).load();
    }

    /**
     * flyway migration strategy.
     *
     * @param flyway the query
     * @param migrationStrategy the fielded clauses
     * @return the flyway initializer
     */
    @Bean
    @DependsOn("customMigrationStrategy")
    public FlywayMigrationInitializer flywayInitializer(final Flyway flyway, final ObjectProvider<FlywayMigrationStrategy> migrationStrategy) {

        return new FlywayMigrationInitializer(flyway, migrationStrategy.getIfAvailable());
    }
}
