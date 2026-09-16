package io.vanillabp.cockpit.config;

import com.mongodb.connection.SslSettings;
import com.mongodb.management.JMXConnectionPoolListener;
import io.vanillabp.cockpit.commons.mongo.MongoDbProperties;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.BigDecimalWriteConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.commons.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.config.startup.WriteConcernCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.WriteResultChecking;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@Configuration
public class MongoDbConfiguration {

    @Autowired
    private MongoDbProperties properties;
    
    /**
     * Switches TLS on, which an integration environment needs.
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
     * Reads and writes an OffsetDateTime the way MongoDB stores a date.
     */
    @Bean
    public MongoCustomConversions customConversions() {
        
        final var converters = List.of(
                new OffsetDateTimeReadConverter(),
                new OffsetDateTimeWriteConverter(),
                new BigDecimalReadConverter(),
                new BigDecimalWriteConverter());
        
        return new MongoCustomConversions(converters);
        
    }

    /**
     * The template everything of the cockpit writes through, and the earliest moment at which the
     * write concern of those writes exists. So this is where it is checked: before the database
     * migration runs, before the first repository is built and before anything has been written.
     */
    @Bean
    public MongoTemplate mongoTemplate(
            final MongoDatabaseFactory mongoDbFactory,
            final MongoConverter converter) {
        
        final var template = new CockpitMongoTemplate(mongoDbFactory, converter);
        // throw an exception when a write concern is not met. Optimistic locking needs it too
        template.setWriteResultChecking(WriteResultChecking.EXCEPTION);
        final var configured = WriteConcernCheck.writeConcernConfigured(properties);
        configured.ifPresent(template::setWriteConcern);
        WriteConcernCheck.reportWhatTheCockpitWritesWith(
                configured.orElse(null),
                template.writeConcernApplied(),
                template.writeConcernOfTheConnection(),
                properties.getMode());
        return template;
        
    }
    
}
