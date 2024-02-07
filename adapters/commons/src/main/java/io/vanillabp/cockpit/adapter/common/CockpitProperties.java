package io.vanillabp.cockpit.adapter.common;

import io.vanillabp.cockpit.commons.kafka.KafkaTopicProperties;
import io.vanillabp.cockpit.commons.rest.adapter.Client;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = CockpitProperties.PREFIX, ignoreUnknownFields = true)
public class CockpitProperties {
    
    public static final String PREFIX = "vanillabp.cockpit";

    private Client client;
    
    private List<String> i18nLanguages = List.of();
    
    private String uiUriType;
    
    private boolean userTasksEnabled = true;

    private boolean workflowListEnabled = true;
    
    private String templateLoaderPath;

    private JwtProperties jwt = new JwtProperties();

    private KafkaTopicProperties kafkaTopics = new KafkaTopicProperties();
    
    public Client getClient() {
        return client;
    }
    
    public void setClient(Client client) {
        this.client = client;
    }

    public String getUiUriType() {
        return uiUriType;
    }

    public void setUiUriType(String uiUriType) {
        this.uiUriType = uiUriType;
    }
    
    public List<String> getI18nLanguages() {
        return i18nLanguages;
    }
    
    public void setI18nLanguages(List<String> i18nLanguages) {
        this.i18nLanguages = i18nLanguages;
    }
    
    public boolean isUserTasksEnabled() {
        return userTasksEnabled;
    }
    
    public void setUserTasksEnabled(boolean userTasksEnabled) {
        this.userTasksEnabled = userTasksEnabled;
    }

    public boolean isWorkflowListEnabled() {
        return workflowListEnabled;
    }

    public void setWorkflowListEnabled(boolean workflowListEnabled) {
        this.workflowListEnabled = workflowListEnabled;
    }

    public String getTemplateLoaderPath() {
        return templateLoaderPath;
    }
    
    public void setTemplateLoaderPath(String templateLoaderPath) {
        this.templateLoaderPath = templateLoaderPath;
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

    public void setKafkaTopics(KafkaTopicProperties kafkaTopics) {
        this.kafkaTopics = kafkaTopics;
    }
}
