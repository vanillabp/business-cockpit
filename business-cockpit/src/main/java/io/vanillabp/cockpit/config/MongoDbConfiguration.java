package io.vanillabp.cockpit.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.WriteResultChecking;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.MessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.mongodb.WriteConcern;
import com.mongodb.connection.SslSettings;
import com.mongodb.management.JMXConnectionPoolListener;

import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;

@Configuration
public class MongoDbConfiguration {

    @Autowired
    private ApplicationProperties properties;
    
    /**
     * Used to enable TLS in integration environments.
     */
    @Bean
    public MongoClientSettingsBuilderCustomizer settings() {
        
        return clientSettingsBuilder -> clientSettingsBuilder
                .applyToSslSettings(builder -> builder.applySettings(
                        SslSettings.builder()
                        .enabled(properties.getMongodb().isUseTls())
                        .build()))
                .applyToConnectionPoolSettings(builder -> builder.addConnectionPoolListener(
                        new JMXConnectionPoolListener()));
        
    }
    
    @Bean
    public MongoTransactionManager transactionManager(
            final MongoDatabaseFactory dbFactory) {
        
        return new MongoTransactionManager(dbFactory);
        
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
    
    /**
     * Initialize thread pool executor used to process change stream messages.
     */
    @Bean
    public MessageListenerContainer messageListenerContainer(
            final MongoTemplate template) {

        final var changeStreamExecutorConfig = properties
                .getMongodb()
                .getChangeStreamExecutor();
        final var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(changeStreamExecutorConfig.getCorePoolSize());
        executor.setMaxPoolSize(changeStreamExecutorConfig.getMaxPoolSize());
        executor.setQueueCapacity(changeStreamExecutorConfig.getQueueCapacity());
        executor.setThreadNamePrefix("ChangeStream-Executor-");
        executor.initialize();
        
        final var result = new DefaultMessageListenerContainer(template, executor);
        
        result.start();
        
        return result;
        
    }

    /**
     * Prepare MongoDb template
     */
    @Bean
    public MongoTemplate mongoTemplate(final MongoDatabaseFactory mongoDbFactory,
            final MongoConverter converter) {

        final var template = new MongoTemplate(mongoDbFactory, converter);
        // throw exceptions for write concerns - also required for optimistic locking
        template.setWriteResultChecking(WriteResultChecking.EXCEPTION);
        template.setWriteConcern(WriteConcern
                .MAJORITY
                .withJournal(Boolean.TRUE)
                .withWTimeout(
                        Duration.parse(properties
                                .getMongodb()
                                .getUseTimeout()).get(ChronoUnit.SECONDS),
                        TimeUnit.SECONDS));
        return template;
        
    }
    
}
