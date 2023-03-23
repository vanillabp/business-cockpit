package io.vanillabp.cockpit.config;

import io.vanillabp.cockpit.commons.mongo.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.commons.mongo.OffsetDateTimeWriteConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.ConverterBuilder;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.LinkedList;

@Configuration
public class MongoDbConfiguration {

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
    
}
