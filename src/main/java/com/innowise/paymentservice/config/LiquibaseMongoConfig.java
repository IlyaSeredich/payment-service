package com.innowise.paymentservice.config;

import com.innowise.paymentservice.exception.LiquibaseInitException;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.ext.mongodb.database.MongoLiquibaseDatabase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LiquibaseMongoConfig {
    @Value("${spring.mongodb.uri}")
    private String url;

    @Value("${liquibase.change-log}")
    private String changelog;

    @Bean
    public ApplicationRunner liquibaseRunner() {
        return args -> {

            try (
                    MongoLiquibaseDatabase database =
                            (MongoLiquibaseDatabase) DatabaseFactory.getInstance()
                                    .openDatabase(
                                            url,
                                            null,
                                            null,
                                            null,
                                            new ClassLoaderResourceAccessor()
                                    );

                    Liquibase liquibase = new Liquibase(
                            changelog,
                            new ClassLoaderResourceAccessor(),
                            database)
            ) {

                List<ChangeSet> changeSetsList = liquibase.listUnrunChangeSets(null, null);

                if (!changeSetsList.isEmpty()) {
                    liquibase.update();
                }

            } catch (LiquibaseException e) {

            }
        };
    }
}
