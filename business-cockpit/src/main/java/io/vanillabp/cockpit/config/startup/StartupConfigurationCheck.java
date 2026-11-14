package io.vanillabp.cockpit.config.startup;

import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.APPLICATION_URI;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.APPLICATION_VERSION;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.BPMS_API_PASSWORD;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.BPMS_API_REALM_NAME;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.BPMS_API_USERNAME;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.DEFAULT_LOCALE;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.JWT_EXPIRES_DURATION;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.JWT_KEY;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.JWT_MAX_LOGIN_DURATION;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.KAFKA_GROUP_ID_SUFFIX;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.KAFKA_TOPIC_USER_TASK;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.KAFKA_TOPIC_WORKFLOW;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.KAFKA_TOPIC_WORKFLOW_MODULE;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.MONGODB_HOST;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.MONGODB_URI;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.SMTP_ENABLED;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.SMTP_FROM;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.SPA_DEFAULT_FILE;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.TITLE_LONG;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.TITLE_SHORT;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.WORKER_ID;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.crypto.KeyGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import io.vanillabp.cockpit.bpms.BpmsApiAuthority;
import io.vanillabp.cockpit.support.security.jwt.JwtCookie;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsProvider;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import io.vanillabp.cockpit.users.directory.UserDirectoryConfiguration;

/**
 * Reports everything a cockpit application still has to configure, on every start and in as few
 * messages as possible.
 * <p>
 * It is a {@link BeanFactoryPostProcessor} because that is the last moment before the first bean is
 * built and therefore the earliest moment at which the whole configuration can be judged at once.
 * Every value checked here used to surface much later and much worse. It came as
 * {@code realmName must be specified} out of a security filter chain, as
 * {@code Cannot pass null or empty values to constructor} out of a user details manager, as a
 * connection timeout against a MongoDB nobody configured, or, for the signing key, only once
 * somebody tried to log in.
 * <p>
 * What is missing decides how it is reported:
 * <ul>
 * <li>Values without which no part of the cockpit works stop the start, collected into one
 * {@link CockpitIsNotConfiguredException} listing all of them.</li>
 * <li>Values which switch off one feature let the application boot. They are reported as a
 * warning naming that feature, so the developer can fix them one start at a time.</li>
 * <li>The JWT signing key is generated on the spot and the generated value is handed to the
 * developer to copy, because guessing a key is the one thing this check can do for them.</li>
 * <li>A missing security chain is a warning. The library brings none, and the application decides
 * how it is protected. A missing user details provider stops the start, because nothing works
 * without it. So does a missing user directory, a second one, or settings of the directory the
 * cockpit brings which cannot work.</li>
 * </ul>
 */
@Component
public class StartupConfigurationCheck implements BeanFactoryPostProcessor, EnvironmentAware, BeanClassLoaderAware {

  private static final Logger logger = LoggerFactory.getLogger(StartupConfigurationCheck.class);

  /**
   * The bean name of the chain which protects the GUI API, in the module {@code container} and in
   * the wiki.
   */
  static final String GUI_CHAIN = "guiHttpSecurity";

  /**
   * The bean name of the chain which protects the BPMS API, in the module {@code container} and
   * in the wiki.
   */
  static final String BPMS_API_CHAIN = "bpmsApiHttpSecurity";

  private static final String WIKI_SECURITY = "https://github.com/vanillabp/business-cockpit/wiki/Security";

  private static final String GENERATED_JWT_KEY_SOURCE = "business-cockpit-generated-jwt-key";

  /**
   * Holds the defaults of the two lifetimes of the login token, so they are written down once.
   */
  private static final JwtCookie DEFAULT_JWT_COOKIE = new JwtCookie();

  private ConfigurableEnvironment environment;

  private ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

  /**
   * Creates the check. Spring creates it as a bean, and Spring passes the environment in later.
   */
  public StartupConfigurationCheck() {
  }

  @Override
  public void setEnvironment(
      final Environment environment) {

    this.environment = (ConfigurableEnvironment) environment;

  }

  @Override
  public void setBeanClassLoader(
      final ClassLoader classLoader) {

    this.classLoader = classLoader;

  }

  @Override
  public void postProcessBeanFactory(
      final ConfigurableListableBeanFactory beanFactory) {

    checkConfigurationOf(environment);
    checkTheSecurityOf(environment, beanFactory);

  }

  /**
   * Throws {@link CockpitIsNotConfiguredException} if the application cannot run at all, and
   * logs one warning per feature which stays switched off. A missing JWT signing key is
   * generated and added to the environment.
   *
   * @param environment The configuration of the application
   */
  public void checkConfigurationOf(
      final ConfigurableEnvironment environment) {

    failOnMissingMandatoryValues(environment);
    failOnARemovedProperty(environment);
    failOnAnUnusableMapKeyDotReplacement(environment);
    failOnAnUnusableJwtLifetime(environment);
    failOnAnUnusableSortIndexLimit(environment);
    failOnAnUnusableUserDirectory(environment);
    generateAJwtKeyIfNoneIsConfigured(environment);
    warnAboutALoginWhichIsNeverRenewed(environment);
    warnAboutKafkaIngestion(environment);
    warnAboutUserDirectorySettingsWhichAreNotRead(environment);
    warnAboutTheRemainingValues(environment);

  }

  private void failOnMissingMandatoryValues(
      final ConfigurableEnvironment environment) {

    final var missing = new ArrayList<MissingConfiguration>();

    // the driver falls back to an unauthenticated localhost:27017 when nothing is configured,
    // which ends in a connection timeout rather than in a hint about the missing URI
    if (CockpitConfiguration.isMissing(environment, MONGODB_URI) && CockpitConfiguration.isMissing(environment,
        MONGODB_HOST)) {
      missing.add(new MissingConfiguration(
          MONGODB_URI, "The MongoDB the cockpit stores user tasks, workflows and modules in. It has to "
              + "be a replica set: the user interface updates from its change stream.", "spring.mongodb.uri: mongodb://localhost:27017/business-cockpit?directConnection=true"));
    }

    if (CockpitConfiguration.isMissing(environment, TITLE_SHORT)) {
      missing.add(new MissingConfiguration(
          TITLE_SHORT, "The short application title. It names the application in the user interface and "
              + "is the HTTP basic realm of the login in the module 'container'.", "business-cockpit.title-short: MyCockpit"));
    }

    // Kafka ingestion is optional, but once topics are given the application is meant to consume
    // them, and it cannot without these two
    if (isKafkaIngestionFullyConfigured(environment)) {
      if (CockpitConfiguration.isWorkerIdMissing(environment)) {
        missing.add(new MissingConfiguration(
            WORKER_ID, "Tells the instances sharing a Kafka consumer group apart, which the BPMS "
                + "API's Kafka ingestion needs to consume every event once.", "start parameter '-DworkerId=local', or 'workerId: local' in the configuration"));
      }
      if (CockpitConfiguration.isMissing(environment, KAFKA_GROUP_ID_SUFFIX)) {
        missing.add(new MissingConfiguration(
            KAFKA_GROUP_ID_SUFFIX, "Identifies this application in the Kafka consumer group ids of the cockpit. "
                + "Do not mix it up with 'workerId', which identifies the instance.", "bpms-api.kafka.group-id-suffix: ${spring.application.name}"));
      }
    }

    if (!missing.isEmpty()) {
      throw new CockpitIsNotConfiguredException(List.copyOf(missing));
    }

  }

  /**
   * A removed key would stop the start anyway where its section binds strictly, but with a
   * message about a field which is missing. This one says that the key is gone and why.
   */
  private void failOnARemovedProperty(
      final Environment environment) {

    final var removedKeys = RemovedProperties.setIn(environment);
    if (!removedKeys.isEmpty()) {
      throw new RemovedPropertyIsSetException(removedKeys);
    }

  }

  /**
   * The replacement is read only when the first record is written, which can be hours after the
   * start. A value which cannot work is reported now instead.
   */
  private void failOnAnUnusableMapKeyDotReplacement(
      final Environment environment) {

    final var replacement = MapKeyDotReplacement.configuredIn(environment);
    if (replacement.isEmpty()) {
      return;
    }
    MapKeyDotReplacement
        .whatIsWrongWith(replacement.get())
        .ifPresent(reason -> {
          throw new MapKeyDotReplacementIsNotUsableException(replacement.get(), reason);
        });

  }

  /**
   * The limit is read only when a list is sorted by a new path, which can be days after the
   * start. A value which cannot work is reported now instead.
   */
  private void failOnAnUnusableSortIndexLimit(
      final Environment environment) {

    SortIndexLimit
        .whatIsWrongWith(environment)
        .ifPresent(reason -> {
          throw new SortIndexLimitIsNotUsableException(reason);
        });

  }

  /**
   * The directory is asked only when a logged-in user sends a request. A setting which cannot work
   * is reported now instead.
   */
  private void failOnAnUnusableUserDirectory(
      final Environment environment) {

    final var unusable = UserDirectorySettings.whatIsWrongWith(environment, classLoader);
    if (!unusable.isEmpty()) {
      throw new UserDirectoryIsNotUsableException(unusable);
    }

  }

  private void warnAboutUserDirectorySettingsWhichAreNotRead(
      final Environment environment) {

    final var notRead = UserDirectorySettings.sectionsWhichAreNotRead(environment);
    if (notRead.isEmpty()) {
      return;
    }
    logger.warn("""
        The settings below '{}' are not read, because '{}' is '{}'. Set '{}' to the directory \
        they belong to, or remove them.""",
        String.join("' and '", notRead), UserDirectorySettings.TYPE,
        CockpitConfiguration.valueOf(environment, UserDirectorySettings.TYPE).orElse(""),
        UserDirectorySettings.TYPE);

  }

  /**
   * Both lifetimes are read only when somebody logs in, which can be long after the start. A value
   * which cannot be read is reported now instead.
   */
  private void failOnAnUnusableJwtLifetime(
      final Environment environment) {

    final var unusable = new ArrayList<MissingConfiguration>();
    if (expiresDurationOf(environment).isEmpty()) {
      unusable.add(new MissingConfiguration(
          JWT_EXPIRES_DURATION, "How long a login token lives after its last renewal. The value '"
              + CockpitConfiguration.valueOf(environment, JWT_EXPIRES_DURATION).orElse("")
              + "' is no duration greater than zero.", JWT_EXPIRES_DURATION
                  + ": "
                  + DEFAULT_JWT_COOKIE.getExpiresDuration()));
    }
    if (maxLoginDurationOf(environment).isEmpty()) {
      unusable.add(new MissingConfiguration(
          JWT_MAX_LOGIN_DURATION, "How long a login lasts at most, however often its token is renewed. The value '"
              + CockpitConfiguration.valueOf(environment, JWT_MAX_LOGIN_DURATION).orElse("")
              + "' is no duration greater than zero.", JWT_MAX_LOGIN_DURATION
                  + ": "
                  + DEFAULT_JWT_COOKIE.getMaxLoginDuration()));
    }

    if (!unusable.isEmpty()) {
      throw new JwtLifetimeIsNotUsableException(List.copyOf(unusable));
    }

  }

  /**
   * A maximum shorter than the lifetime of one token is allowed, it just switches the renewal off.
   * That is worth a word, because it does not look like it.
   */
  private void warnAboutALoginWhichIsNeverRenewed(
      final Environment environment) {

    final var expiresDuration = expiresDurationOf(environment).orElseThrow();
    final var maxLoginDuration = maxLoginDurationOf(environment).orElseThrow();
    if (maxLoginDuration.compareTo(expiresDuration) >= 0) {
      return;
    }

    logger.warn("""
        Logins are never renewed, because '{}' ({}) is shorter than '{}' ({}). Every login \
        ends {} after it was made, whether the user is working or not. Set '{}' to a longer \
        value to keep a working user logged in, for example:

          {}: {}""",
        JWT_MAX_LOGIN_DURATION, maxLoginDuration, JWT_EXPIRES_DURATION, expiresDuration,
        maxLoginDuration, JWT_MAX_LOGIN_DURATION,
        JWT_MAX_LOGIN_DURATION, DEFAULT_JWT_COOKIE.getMaxLoginDuration());

  }

  private static Optional<Duration> expiresDurationOf(
      final Environment environment) {

    return durationOf(environment, JWT_EXPIRES_DURATION, DEFAULT_JWT_COOKIE.getExpiresDuration());

  }

  private static Optional<Duration> maxLoginDurationOf(
      final Environment environment) {

    return durationOf(environment, JWT_MAX_LOGIN_DURATION, DEFAULT_JWT_COOKIE.getMaxLoginDuration());

  }

  /**
   * The configured duration, the default if none is configured, or empty if the configured value
   * is no duration greater than zero.
   */
  private static Optional<Duration> durationOf(
      final Environment environment,
      final String propertyName,
      final String defaultValue) {

    try {
      final var duration = Duration.parse(CockpitConfiguration
          .valueOf(environment, propertyName)
          .orElse(defaultValue));
      return duration.isPositive() ? Optional.of(duration) : Optional.empty();
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }

  }

  /**
   * Throws {@link UserDetailsProviderIsMissingException} if the application has no user details
   * provider, and logs a warning for each security chain the application does not have. The
   * library brings no chain, so this is how a developer learns about them.
   * <p>
   * The chains are looked for by their bean names. That is the one thing the check can see before
   * the first bean is built, and the module {@code container} uses the same names.
   *
   * @param environment The configuration of the application
   * @param beanFactory The bean definitions of the application
   */
  public void checkTheSecurityOf(
      final Environment environment,
      final ConfigurableListableBeanFactory beanFactory) {

    failOnAMissingUserDetailsProvider(beanFactory);
    failOnAMissingOrASecondUserDirectory(environment, beanFactory);
    if (!beanFactory.containsBean(GUI_CHAIN)) {
      warnAboutTheMissingGuiChain();
    }
    if (!beanFactory.containsBean(BPMS_API_CHAIN)) {
      warnAboutTheMissingBpmsApiChain(environment);
    }

  }

  /**
   * Without the provider the start would fail anyway, but with a message about a parameter of a
   * bean the developer never wrote.
   */
  private void failOnAMissingUserDetailsProvider(
      final ConfigurableListableBeanFactory beanFactory) {

    if (beanFactory.getBeanNamesForType(UserDetailsProvider.class, true, false).length > 0) {
      return;
    }
    throw new UserDetailsProviderIsMissingException(
        "The Business Cockpit cannot start. The application has no bean of type '"
            + UserDetailsProvider.class.getName()
            + "'. The cockpit asks it for the user of every request, and the library brings none.", """
                Add a bean which reads the user from the login token:

                    @Bean
                    public UserDetailsProvider userDetailsProvider() {
                        return new JwtUserDetailsProvider();
                    }

                The login token names the user but carries no groups. The cockpit asks the \
                application's bean of type 'UserDirectoryService' for the groups of the user, \
                by the subject of the token, on every request. So that bean should cache the \
                users if it asks a remote system.

                The module 'container' of the Business Cockpit has this bean in its \
                GuiSecurityConfiguration. The wiki says more: """ + WIKI_SECURITY);

  }

  /**
   * Without a directory the start would fail anyway, but with a message about a parameter of a
   * bean the developer never wrote. With two of them it would fail the same way.
   */
  private void failOnAMissingOrASecondUserDirectory(
      final Environment environment,
      final ConfigurableListableBeanFactory beanFactory) {

    final var directories = List.of(beanFactory.getBeanNamesForType(UserDirectoryService.class, true, false));
    final var type = UserDirectorySettings.typeIn(environment);

    if (directories.isEmpty()) {
      throw new UserDirectoryIsNotUsableException(
          "The Business Cockpit cannot start. The application has no bean of type '"
              + UserDirectoryService.class.getName()
              + "'. The cockpit asks it for the groups of the user on every request, and for the "
              + "users it proposes when a task is assigned.", """
                  Let the cockpit bring one, for Keycloak or for LDAP:

                      %s: keycloak

                  The next start says which other values the directory needs. Or add a bean of \
                  type 'UserDirectoryService' of your own. The cockpit asks it on every request, \
                  so let it cache the users if it asks a remote system. The wiki says more: %s"""
                  .formatted(UserDirectorySettings.TYPE, WIKI_SECURITY));
    }

    if (type.isEmpty() || (directories.size() < 2)) {
      return;
    }
    final var ownDirectories = directories
        .stream()
        .filter(name -> !name.equals(UserDirectoryConfiguration.KEYCLOAK_BEAN))
        .filter(name -> !name.equals(UserDirectoryConfiguration.LDAP_BEAN))
        .toList();
    throw new UserDirectoryIsNotUsableException(
        "The Business Cockpit cannot start. '"
            + UserDirectorySettings.TYPE
            + "' asks the cockpit to bring the user directory for '"
            + UserDirectorySettings.nameOf(type.get())
            + "', and the application has a bean of type 'UserDirectoryService' of its own as "
            + "well: '"
            + String.join("', '", ownDirectories)
            + "'.", "Remove the property '"
                + UserDirectorySettings.TYPE
                + "' to keep your own directory, or remove your bean to use the one the cockpit brings.");

  }

  private void warnAboutTheMissingGuiChain() {

    logger.warn("""
        The application has no security chain named '{}'. The Business Cockpit library brings no \
        login of its own, neither for the GUI API nor for the BPMS API. If the application has no \
        chain at all, Spring Boot's default chain asks for a login on every path, with a password \
        it generates. If the application has other chains, a request which none of them takes is \
        not checked at all.

        Write a bean '{}' of type SecurityFilterChain. The module 'container' of the Business \
        Cockpit is the reference, and the wiki says what every chain has to keep:
        {}

        If a chain with another name already protects the GUI API, name it '{}' and this \
        warning goes away.""", GUI_CHAIN, GUI_CHAIN, WIKI_SECURITY, GUI_CHAIN);

  }

  private void warnAboutTheMissingBpmsApiChain(
      final Environment environment) {

    final var missing = new ArrayList<MissingConfiguration>();
    if (CockpitConfiguration.isMissing(environment, BPMS_API_REALM_NAME)) {
      missing.add(new MissingConfiguration(
          BPMS_API_REALM_NAME, "The HTTP basic realm the BPMS API asks its clients to authenticate in.", "bpms-api.realm-name: BPMS-API"));
    }
    if (CockpitConfiguration.isMissing(environment, BPMS_API_USERNAME)) {
      missing.add(new MissingConfiguration(
          BPMS_API_USERNAME, "The user name BPMS adapters authenticate at the BPMS API with.", "bpms-api.username: bpms"));
    }
    if (CockpitConfiguration.isMissing(environment, BPMS_API_PASSWORD)) {
      missing.add(new MissingConfiguration(
          BPMS_API_PASSWORD, "The password of that user, prefixed by the encoding it is stored in.", "bpms-api.password: \"{bcrypt}$2a$10$...\", or \"{noop}secret\" for local development"));
    }

    final var howTheReferenceDoesIt = missing.isEmpty()
        ? "The values '%s', '%s' and '%s' are set. The library does not build a chain from them, the module 'container' does."
            .formatted(BPMS_API_REALM_NAME, BPMS_API_USERNAME, BPMS_API_PASSWORD)
        : "The module 'container' builds the chain once these values are set. Missing:\n\n"
            + MissingConfiguration.asMessageBlocks(missing);

    logger.warn("""
        The application has no security chain named '{}', so the BPMS API is not protected by a \
        login of its own. Its endpoints answer only a caller with the authority '{}'. As long as \
        nobody gets that authority, no workflow module and no BPMS adapter can report user tasks \
        or workflows to this cockpit.

        {}

        A custom cockpit writes a bean '{}' of type SecurityFilterChain, as the module \
        'container' of the Business Cockpit does. The wiki says what the chain has to keep:
        {}""", BPMS_API_CHAIN, BpmsApiAuthority.BPMS_API_AUTHORITY, howTheReferenceDoesIt,
        BPMS_API_CHAIN, WIKI_SECURITY);

  }

  /**
   * A cockpit without a signing key could not hand out a single login cookie, so a key is made up
   * rather than refusing to start. It is only good for this run, which is what the warning is
   * about.
   */
  private void generateAJwtKeyIfNoneIsConfigured(
      final ConfigurableEnvironment environment) {

    if (!CockpitConfiguration.isMissing(environment, JWT_KEY)) {
      return;
    }

    final var generatedKey = generateJwtKey();
    // ahead of every other source: the key counts as unconfigured when it is set to nothing,
    // and a source holding an empty value would otherwise keep winning
    environment
        .getPropertySources()
        .addFirst(new MapPropertySource(GENERATED_JWT_KEY_SOURCE, Map.of(JWT_KEY, generatedKey)));

    logger.warn("""
        No JWT signing key is configured, so one was generated for this start. Every restart \
        and every further instance of this application signs with a different key, which \
        logs all users out. Missing:

        {}""",
        new MissingConfiguration(
            JWT_KEY, "The Base64 encoded HMAC-SHA256 key the login cookie is signed with.", JWT_KEY
                + ": "
                + generatedKey)
            .asMessageBlock());

  }

  private static String generateJwtKey() {

    try {
      return Base64
          .getEncoder()
          .encodeToString(KeyGenerator
              .getInstance("HmacSha256")
              .generateKey()
              .getEncoded());
    } catch (Exception e) {
      throw new IllegalStateException(
          "No property '"
              + JWT_KEY
              + "' is set and generating a key to run with failed.", e);
    }

  }

  private boolean isKafkaIngestionFullyConfigured(
      final Environment environment) {

    return missingKafkaTopics(environment).isEmpty();

  }

  private List<MissingConfiguration> missingKafkaTopics(
      final Environment environment) {

    final var missing = new ArrayList<MissingConfiguration>();
    if (CockpitConfiguration.isMissing(environment, KAFKA_TOPIC_USER_TASK)) {
      missing.add(new MissingConfiguration(
          KAFKA_TOPIC_USER_TASK, "The topic user task events are consumed from.", "bpms-api.kafka.topics.user-task: business-cockpit-user-tasks"));
    }
    if (CockpitConfiguration.isMissing(environment, KAFKA_TOPIC_WORKFLOW)) {
      missing.add(new MissingConfiguration(
          KAFKA_TOPIC_WORKFLOW, "The topic workflow events are consumed from.", "bpms-api.kafka.topics.workflow: business-cockpit-workflows"));
    }
    if (CockpitConfiguration.isMissing(environment, KAFKA_TOPIC_WORKFLOW_MODULE)) {
      missing.add(new MissingConfiguration(
          KAFKA_TOPIC_WORKFLOW_MODULE, "The topic workflow module registrations are consumed from.", "bpms-api.kafka.topics.workflow-module: business-cockpit-workflow-modules"));
    }
    return missing;

  }

  /**
   * Kafka ingestion is switched on by the three topics together. Configuring only some of them
   * leaves it off, which is worth a word because it looks configured.
   */
  private void warnAboutKafkaIngestion(
      final Environment environment) {

    final var missingTopics = missingKafkaTopics(environment);
    if (missingTopics.isEmpty() || missingTopics.size() == 3) {
      return;
    }

    logger.warn("""
        The BPMS API's Kafka ingestion is deactivated because it is configured only in part. \
        It consumes all three topics or none. Missing:

        {}""",
        MissingConfiguration.asMessageBlocks(missingTopics));

  }

  private void warnAboutTheRemainingValues(
      final Environment environment) {

    final var missing = new ArrayList<MissingConfiguration>();

    if (CockpitConfiguration.isMissing(environment, TITLE_LONG)) {
      missing.add(new MissingConfiguration(
          TITLE_LONG, "The full application title, shown on the login screen and in the browser tab. "
              + "Without it the user interface shows no title at all.", "business-cockpit.title-long: My Business Cockpit"));
    }
    if (CockpitConfiguration.isMissing(environment, APPLICATION_VERSION)) {
      missing.add(new MissingConfiguration(
          APPLICATION_VERSION, "The version the user interface displays, usually filled by the build.", "business-cockpit.application-version: \"@project.version@\""));
    }

    if (CockpitConfiguration.isMissing(environment, SPA_DEFAULT_FILE)) {
      missing.add(new MissingConfiguration(
          SPA_DEFAULT_FILE, "The shell of the user interface, which a deep link like '/tasklist/4711' is "
              + "answered with. Without it every path no API knows gets 404. An application without a "
              + "user interface of its own can leave this warning as it is.", "application.spa-default-file: classpath:/static/index.html"));
    }

    // these only matter once notification e-mails are actually sent
    if (isSmtpNotificationEnabled(environment)) {
      if (CockpitConfiguration.isMissing(environment, DEFAULT_LOCALE)) {
        missing.add(new MissingConfiguration(
            DEFAULT_LOCALE, "The language of a notification e-mail for a user who has none of their own. "
                + "Without it such a user gets the template without a language in its name.", "business-cockpit.default-locale: en"));
      }
      if (CockpitConfiguration.isMissing(environment, APPLICATION_URI)) {
        missing.add(new MissingConfiguration(
            APPLICATION_URI, "The URI this cockpit is reachable at. Notification e-mails link to their "
                + "user task with it, and the links stay broken without it.", "business-cockpit.application-uri: https://cockpit.example.com"));
      }
      if (CockpitConfiguration.isMissing(environment, SMTP_FROM)) {
        missing.add(new MissingConfiguration(
            SMTP_FROM, "The From address of notification e-mails. Many SMTP servers reject messages "
                + "without one.", "business-cockpit.notification.smtp.from: cockpit@example.com"));
      }
    }

    if (missing.isEmpty()) {
      return;
    }

    logger.warn("""
        The Business Cockpit is running with an incomplete configuration:

        {}""",
        MissingConfiguration.asMessageBlocks(missing));

  }

  private boolean isSmtpNotificationEnabled(
      final Environment environment) {

    return CockpitConfiguration
        .valueOf(environment, SMTP_ENABLED)
        .map(Boolean::parseBoolean)
        .orElse(Boolean.FALSE);

  }

}
