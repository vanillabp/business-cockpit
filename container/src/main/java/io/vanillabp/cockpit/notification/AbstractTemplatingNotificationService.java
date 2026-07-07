package io.vanillabp.cockpit.notification;

import freemarker.core.HTMLOutputFormat;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import no.api.freemarker.java8.Java8ObjectWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link NotificationService} implementations that render their message from
 * Freemarker templates (AC tech 2). Medium-agnostic: an SMS/Teams/... medium can reuse it.
 * <p>
 * Templates are looked up on the classpath under the directory resolved from
 * {@code business-cockpit.notification.<type>.templates} (default
 * {@code templates/notification/<type>/}). The rendering approach mirrors
 * {@code io.vanillabp.cockpit.adapter.common.wiring.TemplatingHandlerBase#renderText} (which lives
 * in the {@code adapters/commons} module the {@code container} does not depend on, hence it is
 * replicated here): a Freemarker {@link Configuration} with a {@link Java8ObjectWrapper} exposed at
 * {@code EXPOSE_SAFE} (required so Java record component accessors become template properties).
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
     * A rendered template plus whether it is HTML (derived from the template's Freemarker output
     * format, i.e. a {@code .ftlh} template or an explicit {@code <#ftl output_format="HTML">}).
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
     * Renders a template for an explicit locale. Freemarker's localized lookup resolves the most
     * specific template available (e.g. {@code email-body_de.ftl}) and falls back to the
     * locale-less template ({@code email-body.ftl}). Callers that know the recipient's preferred
     * locale (e.g. the e-mail medium, which reads it from the user record and falls back to the
     * application default) use this overload; the public 5-arg {@link #render} keeps the contract
     * signature and derives the locale via {@link #localeOf(UserDetails)}.
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
     * The locale used by the public 5-arg {@link #render} for template lookup. Defaults to
     * {@link Locale#ENGLISH}; the e-mail medium instead passes the recipient's resolved locale
     * (user preference or application default) to the 6-arg overload.
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

    private Configuration buildConfiguration() {

        final var config = new Configuration(FREEMARKER_VERSION);
        config.setLocalizedLookup(true);
        config.setRecognizeStandardFileExtensions(true);
        config.setDefaultEncoding("UTF-8");

        var basePath = notificationProperties.templatesPath(getType());
        // ClassLoader-based lookup wants a package-style base path without a leading slash
        if (basePath.startsWith("/")) {
            basePath = basePath.substring(1);
        }
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        config.setClassLoaderForTemplateLoading(getClass().getClassLoader(), basePath);

        final var objectWrapper = new Java8ObjectWrapper(FREEMARKER_VERSION);
        // EXPOSE_SAFE promotes Java record accessors to template properties (Freemarker 2.3.33+).
        objectWrapper.setExposureLevel(BeansWrapper.EXPOSE_SAFE);
        config.setObjectWrapper(objectWrapper);

        return config;

    }

}
