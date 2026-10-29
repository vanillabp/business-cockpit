package io.vanillabp.cockpit.config.properties;

import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import io.vanillabp.cockpit.gui.api.v1.GuiSseProperties;

/**
 * The general settings of the cockpit application, read from {@code business-cockpit.*}.
 */
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

  /**
   * Creates the properties with their defaults. Spring binds the configured values afterwards.
   */
  public ApplicationProperties() {
  }

  /**
   * The settings of the update stream, the server-sent events which tell the browser about changes.
   *
   * @return The settings of the update stream
   */
  public GuiSseProperties getGuiSse() {
    return guiSse;
  }

  /**
   * Sets the settings of the update stream.
   *
   * @param guiSse The settings of the update stream
   */
  public void setGuiSse(
      GuiSseProperties guiSse) {
    this.guiSse = guiSse;
  }

  /**
   * The short title of the application. The browser shows it in the header, and the BPMS API
   * uses it as the realm name of its basic authentication.
   *
   * @return The short title
   */
  public String getTitleShort() {
    return titleShort;
  }

  /**
   * Sets the short title of the application.
   *
   * @param titleShort The short title
   */
  public void setTitleShort(
      String titleShort) {
    this.titleShort = titleShort;
  }

  /**
   * The long title of the application, which the browser shows where there is room for it.
   *
   * @return The long title
   */
  public String getTitleLong() {
    return titleLong;
  }

  /**
   * Sets the long title of the application.
   *
   * @param titleLong The long title
   */
  public void setTitleLong(
      String titleLong) {
    this.titleLong = titleLong;
  }

  /**
   * The version of the application, which the browser shows to the user.
   *
   * @return The version
   */
  public String getApplicationVersion() {
    return applicationVersion;
  }

  /**
   * Sets the version of the application.
   *
   * @param applicationVersion The version
   */
  public void setApplicationVersion(
      String applicationVersion) {
    this.applicationVersion = applicationVersion;
  }

  /**
   * The address under which users reach the cockpit. Notifications build their links on it.
   *
   * @return The base address of the cockpit
   */
  public String getApplicationUri() {
    return applicationUri;
  }

  /**
   * Sets the address under which users reach the cockpit.
   *
   * @param applicationUri The base address of the cockpit
   */
  public void setApplicationUri(
      String applicationUri) {
    this.applicationUri = applicationUri;
  }

  /**
   * The settings of the login token (JWT, JSON Web Token) and its cookie.
   *
   * @return The settings of the login token
   */
  public JwtProperties getJwt() {
    return jwt;
  }

  /**
   * Sets the settings of the login token.
   *
   * @param jwt The settings of the login token
   */
  public void setJwt(
      JwtProperties jwt) {
    this.jwt = jwt;
  }

  /**
   * The default locale of the whole application. It is used where a user has no locale of their
   * own, when a notification template is rendered for example. It defaults to German.
   *
   * @return The default locale
   */
  public Locale getDefaultLocale() {
    return defaultLocale;
  }

  /**
   * Sets the default locale of the whole application.
   *
   * @param defaultLocale The default locale
   */
  public void setDefaultLocale(
      Locale defaultLocale) {
    this.defaultLocale = defaultLocale;
  }

  /**
   * When the application was built, as text. The browser shows it next to the version.
   *
   * @return The time of the build, or {@code null} if it is not configured
   */
  public String getBuildTimestamp() {
    return buildTimestamp;
  }

  /**
   * Sets when the application was built.
   *
   * @param buildTimestamp The time of the build
   */
  public void setBuildTimestamp(
      String buildTimestamp) {
    this.buildTimestamp = buildTimestamp;
  }

  /**
   * Sets the additional values which are passed on to the browser.
   *
   * @param additionalProperties The additional values
   */
  public void setAdditionalProperties(
      Map<String, Object> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  /**
   * Any additional values the browser gets together with the title and the version. A user
   * interface can use them for its own settings.
   *
   * @return The additional values, or {@code null} if none are configured
   */
  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }

}
