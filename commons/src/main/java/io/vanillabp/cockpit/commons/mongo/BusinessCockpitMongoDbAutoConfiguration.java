package io.vanillabp.cockpit.commons.mongo;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.mongodb.reactivestreams.client.MongoClient;

@Configuration
@AutoConfigureBefore(MongoReactiveRepositoriesAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, ReactiveMongoTemplate.class })
public class BusinessCockpitMongoDbAutoConfiguration {

    @Bean
    public MongoDbProperties businessCockpitMongoDbProperties() {
        
        return new MongoDbProperties();
        
    }
    
}
