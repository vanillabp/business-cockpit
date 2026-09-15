package io.vanillabp.cockpit.config.properties;

import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import io.vanillabp.cockpit.gui.api.v1.GuiSseProperties;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "business-cockpit", ignoreUnknownFields = true)
public class ApplicationProperties {

    private GuiSseProperties guiSse = new GuiSseProperties();

    private @NonNull String titleShort;
    
    private @NonNull String titleLong;
    
    private @NonNull String applicationVersion;

    private String buildTimestamp;
    
    private @NonNull String applicationUri;

    private JwtProperties jwt = new JwtProperties();

    /**
     * The default locale of the whole application. It is used where a user has no locale of their
     * own, when a notification template is rendered for example. It defaults to German, the
     * language every user interface is fixed to today.
     */
    private Locale defaultLocale = Locale.GERMAN;

    private Map<String, Object> additionalProperties;

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

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public String getBuildTimestamp() {
        return buildTimestamp;
    }

    public void setBuildTimestamp(String buildTimestamp) {
        this.buildTimestamp = buildTimestamp;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
	this.additionalProperties = additionalProperties;
    }

    public Map<String, Object> getAdditionalProperties() {
	return additionalProperties;
    }

}
