package io.vanillabp.cockpit.notification;

import freemarker.core.HTMLOutputFormat;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import no.api.freemarker.java8.Java8ObjectWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base class of every {@link NotificationService} which renders its message from Freemarker
 * templates. It knows no medium, so a medium sending an SMS or a chat message can reuse it.
 * <p>
 * Templates are looked up under the directory resolved from
 * {@code business-cockpit.notification.templates.<type>} (default
 * {@code classpath:templates/notification/<type>/}). The value says where the directory is:
 * {@code classpath:} for one built into the application, {@code file:} for one of the file system,
 * which is what lets an operator change the wording of a mail without a new build. A value which
 * says neither ends the boot of that medium, so nothing is guessed.
 * <p>
 * A template read from the file system is re-read while the cockpit runs. Freemarker checks the file
 * again once its own update delay has passed, which is five seconds by default, and there is no
 * setting of the cockpit's for it.
 * <p>
 * The rendering follows the way the version 1 adapters rendered text. That code has left this
 * repository, so the way is written out here: a Freemarker {@link Configuration} with a
 * {@link Java8ObjectWrapper} exposed at {@code EXPOSE_SAFE}. The exposure level is what makes the
 * accessors of a Java record into template properties.
 */
public abstract class AbstractTemplatingNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTemplatingNotificationService.class);

    private static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_34;

    /**
     * Candidate template file extensions, most specific first: {@code .ftlh} produces an HTML mail
     * (Freemarker's HTML output format, auto-escaping), {@code .ftl} a plain-text mail.
     */
    private static final String[] TEMPLATE_SUFFIXES = { ".ftlh", ".ftl" };

    /**
     * A rendered template, and whether it is HTML. That comes from the output format Freemarker
     * read, which is a {@code .ftlh} template or an explicit
     * {@code <#ftl output_format="HTML">}.
     */
    public record Rendered(String content, boolean html) {
    }

    protected final NotificationProperties notificationProperties;

    private volatile Configuration configuration;

    protected AbstractTemplatingNotificationService(
            final NotificationProperties notificationProperties) {

        this.notificationProperties = notificationProperties;

    }

    /**
     * Checks the template directory of this medium while the cockpit starts, rather than when the
     * first notification is due. It runs for an enabled medium only, because an instance of this
     * class is what an enabled medium is.
     *
     * @throws IllegalStateException if the configured directory does not say where it is, or if it
     *                               is a directory of the file system which cannot be read
     */
    @PostConstruct
    void checkTheTemplateDirectory() {

        final var directory = notificationProperties.templatesPath(getType());
        if (!saysWhereItIs(directory)) {
            throw new IllegalStateException("""
                    '%s' is set to '%s', and that says nothing about where the directory is. Write \
                    the place in front of it: '%s%s' for a directory of the classpath, or '%s%s' \
                    for one of the file system."""
                    .formatted(
                            NotificationProperties.templatesKey(getType()), directory,
                            NotificationProperties.CLASSPATH_PREFIX, directory,
                            NotificationProperties.FILE_PREFIX, directory));
        }
        if (!directory.startsWith(NotificationProperties.FILE_PREFIX)) {
            return;
        }
        final var path = Path.of(directory.substring(NotificationProperties.FILE_PREFIX.length()));
        if (!Files.isDirectory(path) || !Files.isReadable(path)) {
            throw new IllegalStateException("""
                    '%s' points at '%s', which is no directory this cockpit can read. Point it at \
                    the directory holding the %s templates, or write '%s' in front of the path to \
                    read them from the classpath instead."""
                    .formatted(
                            NotificationProperties.templatesKey(getType()), path, getType(),
                            NotificationProperties.CLASSPATH_PREFIX));
        }

    }

    /**
     * @param directory a configured template directory
     * @return whether it says where it is. {@code classpath*:} is the spelling a workflow module may
     *         write for the templates of its titles, and it is accepted here so that one value means
     *         one thing on both sides of the cockpit
     */
    private static boolean saysWhereItIs(
            final String directory) {

        return directory.startsWith(NotificationProperties.CLASSPATH_PREFIX)
                || directory.startsWith(NotificationProperties.EVERY_CLASSPATH_PREFIX)
                || directory.startsWith(NotificationProperties.FILE_PREFIX);

    }

    /**
     * Renders a template into a string.
     *
     * @param templateName     the base template name (e.g. {@code email-header}); {@code .ftl} and
     *                         locale suffixes are resolved by Freemarker
     * @param templateContext  the data model
     * @param userDetails      the recipient (exposed to the template as {@code userDetails})
     * @param notificationType the kind of change (exposed as {@code notificationType})
     * @param forced           whether the workflow module forced the notification (exposed as
     *                         {@code forced}; templates render a hint when true)
     * @return the rendered text, or {@code null} if no template was found
     */
    public String render(
            final String templateName,
            final Map<String, Object> templateContext,
            final UserDetails userDetails,
            final NotificationType notificationType,
            final boolean forced) {

        return render(templateName, templateContext, userDetails, notificationType, forced,
                localeOf(userDetails));

    }

    /**
     * Renders a template for one named locale. Freemarker looks for the most specific template
     * it has, {@code email-body_de.ftl} for example, and falls back to the template without a
     * locale, {@code email-body.ftl}. A caller which knows the locale the recipient prefers uses
     * this overload. The e-mail medium is such a caller: it reads the locale off the user record
     * and falls back to the application's default. The public {@link #render} with five arguments
     * keeps the signature of the contract and asks {@link #localeOf(UserDetails)} for the
     * locale.
     */
    protected String render(
            final String templateName,
            final Map<String, Object> templateContext,
            final UserDetails userDetails,
            final NotificationType notificationType,
            final boolean forced,
            final Locale locale) {

        final var rendered = renderContent(templateName, templateContext, userDetails, notificationType, forced, locale);
        return rendered == null ? null : rendered.content();

    }

    /**
     * Renders a template and additionally reports whether it is HTML (a {@code .ftlh} template), so
     * a medium can send the message with the correct content type. A {@code .ftlh} template is
     * preferred over a {@code .ftl} one of the same name.
     *
     * @return the rendered content and its HTML flag, or {@code null} if no template was found
     */
    protected Rendered renderContent(
            final String templateName,
            final Map<String, Object> templateContext,
            final UserDetails userDetails,
            final NotificationType notificationType,
            final boolean forced,
            final Locale locale) {

        final var model = new HashMap<String, Object>();
        if (templateContext != null) {
            model.putAll(templateContext);
        }
        model.put("userDetails", userDetails);
        model.put("notificationType", notificationType);
        model.put("forced", forced);

        final var template = resolveTemplate(templateName, locale);
        if (template == null) {
            logger.error("No notification template '{}' (.ftlh/.ftl) found below '{}'",
                    templateName, notificationProperties.templatesPath(getType()));
            return null;
        }
        try {
            final var writer = new StringWriter();
            template.process(model, writer);
            final var html = template.getOutputFormat() instanceof HTMLOutputFormat;
            return new Rendered(writer.toString(), html);
        } catch (Exception e) {
            logger.error("Could not render notification template '{}'", templateName, e);
            return null;
        }

    }

    private Template resolveTemplate(
            final String templateName,
            final Locale locale) {

        for (final var suffix : TEMPLATE_SUFFIXES) {
            try {
                return configuration().getTemplate(templateName + suffix, locale);
            } catch (TemplateNotFoundException e) {
                // try the next extension
            } catch (Exception e) {
                logger.error("Could not load notification template '{}{}'", templateName, suffix, e);
                return null;
            }
        }
        return null;

    }

    /**
     * The locale the public {@link #render} with five arguments looks a template up with. It is
     * {@link Locale#ENGLISH} by default. The e-mail medium takes the other way and hands the
     * recipient's own locale, or the application's default, to the overload with six
     * arguments.
     */
    protected Locale localeOf(final UserDetails userDetails) {

        return Locale.ENGLISH;

    }

    private Configuration configuration() {

        var result = configuration;
        if (result == null) {
            synchronized (this) {
                result = configuration;
                if (result == null) {
                    result = buildConfiguration();
                    configuration = result;
                }
            }
        }
        return result;

    }

    /**
     * @param directory a classpath template directory as it was configured
     * @return the package-style base path the class loader wants: without the prefix, and without
     *         the leading and trailing slashes Freemarker adds itself
     */
    private static String classpathBase(
            final String directory) {

        final var base = directory.substring(directory.indexOf(':') + 1);
        return base.replaceAll("^/+", "").replaceAll("/+$", "");

    }

    private Configuration buildConfiguration() {

        final var config = new Configuration(FREEMARKER_VERSION);
        config.setLocalizedLookup(true);
        config.setRecognizeStandardFileExtensions(true);
        config.setDefaultEncoding("UTF-8");

        final var directory = notificationProperties.templatesPath(getType());
        if (directory.startsWith(NotificationProperties.FILE_PREFIX)) {
            final var path = directory.substring(NotificationProperties.FILE_PREFIX.length());
            try {
                config.setDirectoryForTemplateLoading(new File(path));
            } catch (final IOException e) {
                // the boot already refused an unreadable directory, so this is one which stopped
                // being readable while the cockpit ran
                throw new IllegalStateException(
                        "The %s notification templates cannot be read from '%s' any more"
                                .formatted(getType(), path), e);
            }
        } else {
            config.setClassLoaderForTemplateLoading(getClass().getClassLoader(), classpathBase(directory));
        }

        final var objectWrapper = new Java8ObjectWrapper(FREEMARKER_VERSION);
        // EXPOSE_SAFE promotes Java record accessors to template properties (Freemarker 2.3.33+).
        objectWrapper.setExposureLevel(BeansWrapper.EXPOSE_SAFE);
        config.setObjectWrapper(objectWrapper);

        return config;

    }

}
