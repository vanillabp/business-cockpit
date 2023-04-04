package io.vanillabp.cockpit.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "business-cockpit", ignoreUnknownFields = false)
public class ApplicationProperties {

    private MongoDb mongodb = new MongoDb();
    
    private int guiSseUpdateInterval = 1000;
    
    public MongoDb getMongodb() {
        return mongodb;
    }
    
    public void setMongodb(MongoDb mongodb) {
        this.mongodb = mongodb;
    }
    
    public int getGuiSseUpdateInterval() {
        return guiSseUpdateInterval;
    }
    
    public void setGuiSseUpdateInterval(int guiSseUpdateInterval) {
        this.guiSseUpdateInterval = guiSseUpdateInterval;
    }
    
}
