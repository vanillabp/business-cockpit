package io.vanillabp.cockpit.commons.mongo;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoReactiveRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.mongodb.reactivestreams.client.MongoClient;

// registered through the .imports file, so it has to be an @AutoConfiguration - only then are
// @AutoConfigureBefore/After honoured
@AutoConfiguration
@AutoConfigureBefore(DataMongoReactiveRepositoriesAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, ReactiveMongoTemplate.class })
public class BusinessCockpitMongoDbAutoConfiguration {

    @Bean
    public MongoDbProperties businessCockpitMongoDbProperties() {
        
        return new MongoDbProperties();
        
    }
    
}
