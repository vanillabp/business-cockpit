package io.vanillabp.cockpit.config.properties;

import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

@ConfigurationProperties(prefix = "business-cockpit", ignoreUnknownFields = false)
public class ApplicationProperties {

    private int guiSseUpdateInterval = 500;
    
    @NonNull
    private String titleShort;
    
    @NonNull
    private String titleLong;
    
    @NonNull
    private String applicationVersion;
    
    @NonNull
    private String applicationUri;
    
    private JwtProperties jwt = new JwtProperties();
    
    public int getGuiSseUpdateInterval() {
        return guiSseUpdateInterval;
    }
    
    public void setGuiSseUpdateInterval(int guiSseUpdateInterval) {
        this.guiSseUpdateInterval = guiSseUpdateInterval;
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
    
}
