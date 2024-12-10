package io.vanillabp.cockpit.adapter.common.properties;

import io.vanillabp.cockpit.commons.kafka.KafkaProperties;
import io.vanillabp.cockpit.commons.rest.adapter.Client;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;

public class CockpitProperties {

    private Client rest;
    
    private KafkaProperties kafka = new KafkaProperties();

    private boolean userTasksEnabled = true;

    private boolean workflowListEnabled = true;

    private String templateLoaderPath;

    private JwtProperties jwt = new JwtProperties();

    public Client getRest() {
        return rest;
    }
    
    public void setRest(Client rest) {
        this.rest = rest;
    }

    public KafkaProperties getKafka() {
        return kafka;
    }

    public void setKafka(KafkaProperties kafka) {
        this.kafka = kafka;
    }

    public String getTemplateLoaderPath() {
        return templateLoaderPath;
    }

    public void setTemplateLoaderPath(String templateLoaderPath) {
        this.templateLoaderPath = templateLoaderPath;
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

    public JwtProperties getJwt() {
        return jwt;
    }

    public void setJwt(JwtProperties jwt) {
        this.jwt = jwt;
    }

}
