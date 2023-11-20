package io.vanillabp.cockpit.config;

import com.mongodb.WriteConcern;
import com.mongodb.connection.SslSettings;
import com.mongodb.management.JMXConnectionPoolListener;
import io.vanillabp.cockpit.commons.mongo.MongoDbProperties;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeWriteConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.WriteResultChecking;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

@Configuration
public class MongoDbConfiguration {

    @Autowired
    private MongoDbProperties properties;
    
    /**
     * Used to enable TLS in integration environments.
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer settings() {
        
        return clientSettingsBuilder -> clientSettingsBuilder
                .applyToSslSettings(builder -> builder.applySettings(
                        SslSettings.builder()
                        .enabled(properties.isUseTls())
                        .build()))
                .applyToConnectionPoolSettings(builder -> builder.addConnectionPoolListener(
                        new JMXConnectionPoolListener()));
        
    }
    
    /**
     * Used to serialize and deserialize
     * <ul>
     * <li>OffsetDateTime
     * </ul>
     * properly.
     */
    @Bean
    public MongoCustomConversions customConversions() {
        
        final var converters = new LinkedList<>();
        
        converters.add(new OffsetDateTimeReadConverter());
        converters.add(new OffsetDateTimeWriteConverter());
        
        return new MongoCustomConversions(converters);
        
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate(
            final ReactiveMongoDatabaseFactory mongoDbFactory,
            final MongoConverter converter) {
        
        final var template = new ReactiveMongoTemplate(mongoDbFactory, converter);
        // throw exceptions for write concerns - also required for optimistic locking
        template.setWriteResultChecking(WriteResultChecking.EXCEPTION);
        template.setWriteConcern(WriteConcern
                .MAJORITY
                .withJournal(Boolean.TRUE)
                .withWTimeout(
                        Duration.parse(properties
                                .getUseTimeout()).get(ChronoUnit.SECONDS),
                        TimeUnit.SECONDS));
        return template;
        
    }
    
}
