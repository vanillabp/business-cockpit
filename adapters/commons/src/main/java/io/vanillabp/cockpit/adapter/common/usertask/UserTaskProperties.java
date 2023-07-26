package io.vanillabp.cockpit.adapter.common.usertask;

public class UserTaskProperties {

    private String uiUriType;

    private String templatesPath;

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

    public String getBpmnDescriptionLanguage() {
        return bpmnDescriptionLanguage;
    }
    
    public void setBpmnDescriptionLanguage(String bpmnDescriptionLanguage) {
        this.bpmnDescriptionLanguage = bpmnDescriptionLanguage;
    }
    
}
