package io.vanillabp.cockpit.config.properties;

import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import io.vanillabp.cockpit.gui.api.v1.GuiSseProperties;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

@ConfigurationProperties(prefix = "business-cockpit", ignoreUnknownFields = true)
public class ApplicationProperties {

    private GuiSseProperties guiSse = new GuiSseProperties();

    @NonNull
    private String titleShort;
    
    @NonNull
    private String titleLong;
    
    @NonNull
    private String applicationVersion;

    private String buildTimestamp;
    
    @NonNull
    private String applicationUri;

    private JwtProperties jwt = new JwtProperties();

    /**
     * The application-wide default locale, used as the fallback when a user has no preferred locale
     * (e.g. for rendering notification templates). Defaults to German, matching the currently
     * hard-coded UI language.
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
