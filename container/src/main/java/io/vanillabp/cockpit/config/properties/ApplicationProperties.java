package io.vanillabp.cockpit.config.properties;

import io.vanillabp.cockpit.commons.kafka.KafkaTopicProperties;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import io.vanillabp.cockpit.gui.api.v1.GuiSseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

@ConfigurationProperties(prefix = "business-cockpit", ignoreUnknownFields = false)
public class ApplicationProperties {

    private GuiSseProperties guiSse = new GuiSseProperties();

    @NonNull
    private String titleShort;
    
    @NonNull
    private String titleLong;
    
    @NonNull
    private String applicationVersion;
    
    @NonNull
    private String applicationUri;
    
    private JwtProperties jwt = new JwtProperties();

    private KafkaTopicProperties kafkaTopics = new KafkaTopicProperties();

    public GuiSseProperties getGuiSse() {
        return guiSse;
    }

    public void setGuiSse(GuiSseProperties guiSse) {
        this.guiSse = guiSse;
    }

    public String getTitleShort() {
        return titleShort;
    }

    public void setTitleShort(String titleShort) {
        this.titleShort = titleShort;
    }

    public String getTitleLong() {
        return titleLong;
    }

    public void setTitleLong(String titleLong) {
        this.titleLong = titleLong;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }
    
    public String getApplicationUri() {
        return applicationUri;
    }
    
    public void setApplicationUri(String applicationUri) {
        this.applicationUri = applicationUri;
    }

    public JwtProperties getJwt() {
        return jwt;
    }
    
    public void setJwt(JwtProperties jwt) {
        this.jwt = jwt;
    }

    public KafkaTopicProperties getKafkaTopics() {
        return kafkaTopics;
    }
    public void setKafkaTopics(KafkaTopicProperties kafkaTopicProperties){
        kafkaTopics = kafkaTopicProperties;
    }
}
