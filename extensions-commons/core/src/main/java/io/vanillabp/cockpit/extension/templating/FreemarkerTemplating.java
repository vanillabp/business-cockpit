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

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLookupStrategy;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;

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

  private static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_34;

  /** What a template loader path starts with to be read from the classpath. */
  static final String CLASSPATH_PREFIX = "classpath:";

  /**
   * What version 1 wrote for a classpath location which is to be searched in the jars of the
   * workflow modules as well. Freemarker asks the class loader per template, which searches
   * every jar anyway, so both spellings load the same templates.
   */
  static final String EVERY_CLASSPATH_PREFIX = "classpath*:";

  /** What a template loader path starts with to be read from the file system. */
  static final String FILE_PREFIX = "file:";

  private final Configuration configuration;

  /**
   * @param templateLoaderPath Where templates are loaded from: a directory of the file system,
   *          written plainly or with <code>file:</code> in front of it, or a directory of the
   *          classpath, written with <code>classpath:</code> or <code>classpath*:</code> in
   *          front of it - the spellings version 1 accepted
   * @throws IllegalStateException If the location cannot be read - which is a configuration
   *           defect and is reported at startup, not at the first event
   */
  public FreemarkerTemplating(
      final String templateLoaderPath) {

    this(newConfiguration());
    if (templateLoaderPath.startsWith(CLASSPATH_PREFIX) || templateLoaderPath
        .startsWith(EVERY_CLASSPATH_PREFIX)) {
      configuration
          .setTemplateLoader(
              new ClassTemplateLoader(
                  FreemarkerTemplating.class.getClassLoader(), classpathBase(templateLoaderPath)));
      return;
    }
    final var directory = templateLoaderPath.startsWith(FILE_PREFIX)
        ? templateLoaderPath.substring(FILE_PREFIX.length())
        : templateLoaderPath;
    try {
      configuration.setDirectoryForTemplateLoading(new File(directory));
    } catch (final IOException e) {
      throw new IllegalStateException(
          """
              The directory '%s' configured as the Business Cockpit's template loader path cannot \
              be read. Point the property at a directory holding the templates - a directory of \
              the file system, or one of the classpath written as 'classpath:my-templates' -, or \
              remove it and let the cockpit report the names written in the BPMN."""
              .formatted(directory), e);
    }

  }

  /**
   * @param templateLoaderPath A classpath location as it was configured
   * @return The directory below which the class loader looks, without the prefix and without
   *         the leading and trailing slashes Freemarker adds itself
   */
  private static String classpathBase(
      final String templateLoaderPath) {

    final var base = templateLoaderPath
        .substring(templateLoaderPath.indexOf(':') + 1);
    return base.replaceAll("^/+", "").replaceAll("/+$", "");

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
          .getConstructor(Version.class)
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
      // a Freemarker template name is a path of its own, always separated by '/' - what a
      // file system calls a separator has nothing to do with it
      final var candidate = lookupPath.isEmpty()
          ? templateName
          : lookupPath
              + "/"
              + templateName;
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
