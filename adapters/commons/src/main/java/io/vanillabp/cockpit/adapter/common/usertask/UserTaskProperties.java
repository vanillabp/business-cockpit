package io.vanillabp.cockpit.adapter.common.usertask;

import java.util.Map;

public class UserTaskProperties {

    private String uiUriType;

    private String templatesPath;
    
    private Map<String, DetailsProperties> detailsProperties;

    private String bpmnDescriptionLanguage;

    public String getUiUriType() {
        return uiUriType;
    }

    public void setUiUriType(String uiUriType) {
        this.uiUriType = uiUriType;
    }

    public String getTemplatesPath() {
        return templatesPath;
    }

    public void setTemplatesPath(String templatesPath) {
        this.templatesPath = templatesPath;
    }
    
    public Map<String, DetailsProperties> getDetailsProperties() {
        return detailsProperties;
    }
    
    public void setDetailsProperties(Map<String, DetailsProperties> detailsProperties) {
        this.detailsProperties = detailsProperties;
    }

    public String getBpmnDescriptionLanguage() {
        return bpmnDescriptionLanguage;
    }
    
    public void setBpmnDescriptionLanguage(String bpmnDescriptionLanguage) {
        this.bpmnDescriptionLanguage = bpmnDescriptionLanguage;
    }
    
}
