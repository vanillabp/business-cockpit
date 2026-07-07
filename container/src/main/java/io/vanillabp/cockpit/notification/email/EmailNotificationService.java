package io.vanillabp.cockpit.notification.email;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.notification.AbstractTemplatingNotificationService;
import io.vanillabp.cockpit.notification.NotificationProperties;
import io.vanillabp.cockpit.notification.NotificationType;
import io.vanillabp.cockpit.notification.RecipientConfiguration;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.users.model.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;

/**
 * Reference {@link io.vanillabp.cockpit.notification.NotificationService} implementation sending
 * e-mails via the Spring Boot mail sender (AC tech 1.vi / 2). Registered by
 * {@link EmailNotificationConfiguration} when {@code business-cockpit.notification.smtp.enabled=true},
 * overridable by a derived cockpit application via {@code @ConditionalOnMissingBean}.
 * <p>
 * Threading: the {@link NotificationService} contract is synchronous, whereas persistence is
 * reactive - the repository calls are bridged with {@code block()}. Callers must therefore invoke
 * these methods off the Netty event-loop threads (the poller uses a bounded-elastic scheduler; the
 * reactive controllers wrap the calls in {@code Mono.fromCallable(...).subscribeOn(boundedElastic())}).
 */
public class EmailNotificationService extends AbstractTemplatingNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    public static final String TYPE = "email";

    /** {@link RecipientConfiguration#type()} of the e-mail address value. */
    public static final String FIELD_EMAIL_ADDRESS = "emailAddress";

    private static final String TEMPLATE_HEADER = "email-header";
    private static final String TEMPLATE_BODY = "email-body";

    private final UserRepository userRepository;

    private final JavaMailSender mailSender;

    private final ApplicationProperties applicationProperties;

    public EmailNotificationService(
            final NotificationProperties notificationProperties,
            final UserRepository userRepository,
            final JavaMailSender mailSender,
            final ApplicationProperties applicationProperties) {

        super(notificationProperties);
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.applicationProperties = applicationProperties;

    }

    @Override
    public String getType() {

        return TYPE;

    }

    @Override
    public Map<String, String> getName() {

        return Map.of("de", "E-Mail", "en", "Email");

    }

    @Override
    public List<RecipientConfiguration> getRecipientConfiguration(
            final String userId) {

        final var user = userRepository.findById(userId).block();

        String value = null;
        if (user != null
                && user.getRecipientConfigurations() != null
                && user.getRecipientConfigurations().get(TYPE) != null) {
            value = user.getRecipientConfigurations().get(TYPE).get(FIELD_EMAIL_ADDRESS);
        }
        if (!StringUtils.hasText(value) && user != null) {
            // suggest the address known from login; editable, applies to all tasks (AC func 6)
            value = user.getEmail();
        }

        return List.of(new RecipientConfiguration(
                FIELD_EMAIL_ADDRESS,
                Map.of("de", "E-Mail-Adresse", "en", "E-mail address"),
                Map.of(
                        "de", "Adresse für die Benachrichtigung aller Aufgaben",
                        "en", "Address used for notifications of all tasks"),
                value));

    }

    @Override
    public void saveRecipientConfiguration(
            final String userId,
            final Map<String, String> configurationValues) {

        final var user = userRepository
                .findById(userId)
                .blockOptional()
                .orElseGet(() -> {
                    final var created = new User();
                    created.setId(userId);
                    return created;
                });

        var recipientConfigurations = user.getRecipientConfigurations();
        if (recipientConfigurations == null) {
            recipientConfigurations = new HashMap<>();
            user.setRecipientConfigurations(recipientConfigurations);
        }
        recipientConfigurations.put(
                TYPE,
                configurationValues == null ? new HashMap<>() : new HashMap<>(configurationValues));

        userRepository.save(user).block();

    }

    @Override
    public void sendNotification(
            final List<String> userIds,
            final UserTask userTask) {

        final var notificationType = userTask.getNotificationType();
        final var forced = userTask.isForced();
        final var context = buildContext(userTask, notificationType);

        final var failures = new ArrayList<String>();
        for (final var userId : userIds) {
            try {
                sendToRecipient(userId, userTask, notificationType, forced, context);
            } catch (Exception e) {
                logger.error("Could not send notification e-mail to user '{}'", userId, e);
                failures.add(userId);
            }
        }

        if (!failures.isEmpty()) {
            // signal failure so the notification poller leaves the bulk pending and retries it
            throw new IllegalStateException(
                    "Could not deliver notification e-mail to users " + failures);
        }

    }

    private void sendToRecipient(
            final String userId,
            final UserTask userTask,
            final NotificationType notificationType,
            final boolean forced,
            final Map<String, Object> context) throws Exception {

        final var user = userRepository.findById(userId).block();
        final var address = resolveAddress(user);
        if (!StringUtils.hasText(address)) {
            logger.info("Skipping notification e-mail: user '{}' has no e-mail address configured", userId);
            return;
        }

        final var userDetails = toUserDetails(user);
        final var locale = resolveLocale(user);
        // subject is always plain text; the body may be HTML when an .ftlh template is used
        final var subject = render(TEMPLATE_HEADER, context, userDetails, notificationType, forced, locale);
        final var body = renderContent(TEMPLATE_BODY, context, userDetails, notificationType, forced, locale);
        if (subject == null || body == null) {
            throw new IllegalStateException("Missing e-mail template (header and/or body)");
        }

        final var message = mailSender.createMimeMessage();
        final var helper = new MimeMessageHelper(message, "UTF-8");
        final var from = notificationProperties.getSmtp().getFrom();
        if (StringUtils.hasText(from)) {
            helper.setFrom(from);
        }
        helper.setTo(address);
        helper.setSubject(subject.strip());
        helper.setText(body.content(), body.html());
        mailSender.send(message);

    }

    /**
     * The locale used to render the notification for a recipient: the user's preferred locale, or
     * the application default locale ({@code business-cockpit.default-locale}) when the user has
     * none (AC: templating honors the user locale with the default as fallback).
     */
    Locale resolveLocale(
            final User user) {

        if (user != null && user.getLocale() != null) {
            return user.getLocale();
        }
        return applicationProperties.getDefaultLocale();

    }

    private String resolveAddress(
            final User user) {

        if (user == null) {
            return null;
        }
        if (user.getRecipientConfigurations() != null
                && user.getRecipientConfigurations().get(TYPE) != null) {
            final var configured = user.getRecipientConfigurations().get(TYPE).get(FIELD_EMAIL_ADDRESS);
            if (StringUtils.hasText(configured)) {
                return configured;
            }
        }
        return user.getEmail();

    }

    private Map<String, Object> buildContext(
            final UserTask userTask,
            final NotificationType notificationType) {

        final var context = new HashMap<String, Object>();
        context.put("userTask", userTask);
        context.put("taskTitle", pickLanguage(userTask.getTitle()));
        context.put("workflowTitle", pickLanguage(userTask.getWorkflowTitle()));
        context.put("taskDefinitionTitle", pickLanguage(userTask.getTaskDefinitionTitle()));
        context.put("businessId", userTask.getBusinessId());
        context.put("notificationTypeName", notificationType == null ? null : notificationType.name());
        // only the application base URI is passed; the (localized) template builds the deep-link
        // and task-list URLs itself, because the URL path segments differ per language (AC func 3b/3c)
        context.put("baseUri", baseUri());
        return context;

    }

    private String baseUri() {

        final var uri = applicationProperties.getApplicationUri();
        if (uri == null) {
            return "";
        }
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;

    }

    private static String pickLanguage(
            final Map<String, String> translatable) {

        if (translatable == null || translatable.isEmpty()) {
            return null;
        }
        final var english = translatable.get("en");
        if (english != null) {
            return english;
        }
        return translatable.values().iterator().next();

    }

    private static UserDetails toUserDetails(
            final User user) {

        final var id = user == null ? null : user.getId();
        final var email = user == null ? null : user.getEmail();
        return new UserDetails() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getEmail() {
                return email;
            }

            @Override
            public String getDisplay() {
                return id;
            }

            @Override
            public String getDisplayShort() {
                return id;
            }

            @Override
            public List<String> getAuthorities() {
                return List.of();
            }
        };

    }

}
