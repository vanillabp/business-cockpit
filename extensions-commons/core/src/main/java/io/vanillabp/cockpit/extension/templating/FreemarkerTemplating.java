package io.vanillabp.cockpit.extension.templating;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.TemplateLookupStrategy;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;

/**
 * {@link Templating} on Freemarker, configured the way the Business Cockpit needs it.
 * <p>
 * The one setting worth explaining is the exposure level of the object wrapper. Freemarker
 * promotes the accessors of a Java record to template properties only while the exposure level
 * is lower than <code>EXPOSE_PROPERTIES_ONLY</code>, so a record handed in as the template
 * context would otherwise render nothing at all - and a record is the natural shape for the
 * little view object a workflow module builds for its titles.
 */
public class FreemarkerTemplating implements Templating {

  private static final Logger logger = LoggerFactory.getLogger(FreemarkerTemplating.class);

  private static final freemarker.template.Version FREEMARKER_VERSION = Configuration.VERSION_2_3_34;

  private final Configuration configuration;

  /**
   * @param templateLoaderPath The directory templates are loaded from
   * @throws IllegalStateException If the directory cannot be read - which is a configuration
   *           defect and is reported at startup, not at the first event
   */
  public FreemarkerTemplating(
      final String templateLoaderPath) {

    this(newConfiguration());
    try {
      configuration.setDirectoryForTemplateLoading(new File(templateLoaderPath));
    } catch (final IOException e) {
      throw new IllegalStateException(
          """
              The directory '%s' configured as the Business Cockpit's template loader path cannot \
              be read. Point the property at a directory holding the templates, or remove it and \
              let the cockpit report the names written in the BPMN."""
              .formatted(templateLoaderPath), e);
    }

  }

  /**
   * @param configuration A configuration built elsewhere - a test loading its templates from
   *          the classpath, or an application which needs settings of its own
   */
  public FreemarkerTemplating(
      final Configuration configuration) {

    this.configuration = configuration;

  }

  /**
   * @return A Freemarker configuration with the settings the cockpit's templates are written
   *         against, without a template loader
   */
  public static Configuration newConfiguration() {

    final var configuration = new Configuration(FREEMARKER_VERSION);
    configuration.setTemplateLookupStrategy(TemplateLookupStrategy.DEFAULT_2_3_0);
    configuration.setLocalizedLookup(true);
    configuration.setRecognizeStandardFileExtensions(true);
    configuration.setObjectWrapper(objectWrapper());
    return configuration;

  }

  /**
   * The wrapper deciding what a template can read from the context object.
   * <p>
   * <code>EXPOSE_SAFE</code> is what makes a record work: Freemarker promotes the accessors of
   * a record to template properties only below <code>EXPOSE_PROPERTIES_ONLY</code>, and a
   * record is the natural shape of the little view object a workflow module builds.
   * <p>
   * Where <code>no.api.freemarker:freemarker-java8</code> is on the classpath its wrapper is
   * used instead, which additionally lets a template read a <code>java.time</code> value.
   * Version 1 required that library; here it is a choice the application makes, and a
   * template rendering a date says so by failing rather than by an application carrying a
   * dependency it may never need.
   */
  private static BeansWrapper objectWrapper() {

    try {
      final var java8Wrapper = Class
          .forName("no.api.freemarker.java8.Java8ObjectWrapper")
          .getConstructor(freemarker.template.Version.class)
          .newInstance(FREEMARKER_VERSION);
      final var wrapper = (BeansWrapper) java8Wrapper;
      wrapper.setExposureLevel(BeansWrapper.EXPOSE_SAFE);
      return wrapper;
    } catch (final ReflectiveOperationException | LinkageError e) {
      final var wrapper = new BeansWrapper(FREEMARKER_VERSION);
      wrapper.setExposureLevel(BeansWrapper.EXPOSE_SAFE);
      return wrapper;
    }

  }

  @Override
  public Optional<String> render(
      final List<String> lookupPaths,
      final String templateName,
      final Locale locale,
      final Object templateContext,
      final Map<String, Object> additionalValues) {

    final var template = findTemplate(lookupPaths, templateName, locale);
    if (template == null) {
      return Optional.empty();
    }
    try {
      final var writer = new StringWriter();
      final var environment = template.createProcessingEnvironment(templateContext, writer);
      for (final var value : additionalValues.entrySet()) {
        environment
            .setGlobalVariable(
                value.getKey(),
                environment.getObjectWrapper().wrap(value.getValue()));
      }
      environment.process();
      return Optional.of(writer.toString());
    } catch (final Exception e) {
      logger.error("Could not render the template '{}'", templateName, e);
      return Optional.empty();
    }

  }

  private Template findTemplate(
      final List<String> lookupPaths,
      final String templateName,
      final Locale locale) {

    for (final var lookupPath : lookupPaths) {
      final var candidate = lookupPath.isEmpty()
          ? templateName
          : lookupPath + File.separator + templateName;
      try {
        return configuration.getTemplate(candidate, locale);
      } catch (final TemplateNotFoundException e) {
        // the next, less specific path is asked - a template of this name simply does not
        // exist here, which is the normal case for all but one of the paths
      } catch (final Exception e) {
        logger.error("Could not read the template '{}'", candidate, e);
        return null;
      }
    }
    return null;

  }

}
