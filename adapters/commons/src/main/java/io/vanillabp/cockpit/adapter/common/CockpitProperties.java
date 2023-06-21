package io.vanillabp.cockpit.adapter.common;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.cockpit.commons.rest.adapter.Client;

@ConfigurationProperties(prefix = CockpitProperties.PREFIX, ignoreUnknownFields = true)
public class CockpitProperties {
    
    public static final String PREFIX = "vanillabp.cockpit";

    private Client client;
    
    private List<String> i18nLanguages = List.of();
    
    private String uiUriType;
    
    private boolean userTasksEnabled = true;
    
    private String templateLoaderPath;
    
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
    
    public String getTemplateLoaderPath() {
        return templateLoaderPath;
    }
    
    public void setTemplateLoaderPath(String templateLoaderPath) {
        this.templateLoaderPath = templateLoaderPath;
    }
    
}
