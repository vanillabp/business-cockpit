package io.vanillabp.cockpit.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "business-cockpit", ignoreUnknownFields = false)
public class ApplicationProperties {

    private MongoDb mongodb = new MongoDb();
    
    public MongoDb getMongodb() {
        return mongodb;
    }
    
    public void setMongodb(MongoDb mongodb) {
        this.mongodb = mongodb;
    }
    
}
