package io.vanillabp.cockpit.extension.templating;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.TemplateLookupStrategy;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;
import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.cockpit.extension.config.TemplateDirectory;

/**
 * {@link Templating} on Freemarker, configured the way the Business Cockpit needs it.
 * <p>
 * The one setting worth explaining is the exposure level of the object wrapper. Freemarker
 * promotes the accessors of a Java record to template properties only while the exposure level
 * is lower than <code>EXPOSE_PROPERTIES_ONLY</code>, so a record handed in as the template
 * context would otherwise render nothing at all. And a record is the natural shape for the
 * little view object a workflow module builds for its titles.
 */
public class FreemarkerTemplating implements Templating {

  private static final Logger logger = LoggerFactory.getLogger(FreemarkerTemplating.class);

  private static final Version FREEMARKER_VERSION = Configuration.VERSION_2_3_34;

  private final Configuration configuration;

  /**
   * Creates the templating for the configured template directory.
   *
   * @param templateLoaderPath Where templates are loaded from: a directory of the classpath,
   *          written with <code>classpath:</code> or <code>classpath*:</code> in front of it, or
   *          one of the file system, written with <code>file:</code> in front of it. The
   *          configuration check refuses a directory without one of the prefixes, so a path which
   *          gets here has already said where it is
   * @throws IllegalStateException If the location cannot be read. That is a configuration
   *           defect, and it is reported at startup and not at the first event
   */
  public FreemarkerTemplating(
      final String templateLoaderPath) {

    this(newConfiguration());
    if (templateLoaderPath.startsWith(Templating.CLASSPATH_PREFIX) || templateLoaderPath
        .startsWith(Templating.EVERY_CLASSPATH_PREFIX)) {
      configuration
          .setTemplateLoader(
              new ClassTemplateLoader(
                  Templating.classLoader(), classpathBase(templateLoaderPath)));
      return;
    }
    final var directory = templateLoaderPath.substring(Templating.FILE_PREFIX.length());
    try {
      configuration.setDirectoryForTemplateLoading(new File(directory));
    } catch (final IOException e) {
      throw new IllegalStateException(
          """
              The directory '%s' configured as the Business Cockpit's template loader path cannot \
              be read. Point '%s' at a directory holding the templates, or remove it and let the \
              cockpit report the names written in the BPMN."""
              .formatted(
                  directory,
                  ConfigurationKeys.globalKey(ConfigurationKeys.TEMPLATE_LOADER_PATH)), e);
    }

  }

  /**
   * Creates the templating for the workflow modules of an application, each with its own
   * directory.
   * <p>
   * The first segment of every template name is the id of a workflow module. It chooses the
   * directory, and the rest of the name is looked for below it. {@link EventTitles#lookupPaths}
   * builds the names that way.
   *
   * @param directoryPerWorkflowModule Where the templates of each module are, by module id
   * @throws IllegalStateException If a directory of the file system cannot be read. That is a
   *           configuration defect, and it is reported at startup and not at the first event
   */
  public FreemarkerTemplating(
      final Map<String, TemplateDirectory> directoryPerWorkflowModule) {

    this(newConfiguration());
    final var loaders = new LinkedHashMap<String, TemplateLoader>();
    final var below = new LinkedHashMap<String, String>();
    directoryPerWorkflowModule
        .forEach((
            workflowModuleId,
            directory) -> {
          loaders.put(workflowModuleId, loaderOf(directory.loaderPath(), directory.key()));
          below.put(workflowModuleId, directory.below());
        });
    configuration.setTemplateLoader(new WorkflowModuleTemplateLoader(loaders, below));

  }

  private static TemplateLoader loaderOf(
      final String templateLoaderPath,
      final String key) {

    if (templateLoaderPath.startsWith(Templating.CLASSPATH_PREFIX) || templateLoaderPath
        .startsWith(Templating.EVERY_CLASSPATH_PREFIX)) {
      return new ClassTemplateLoader(
          Templating.classLoader(), classpathBase(templateLoaderPath));
    }
    final var directory = templateLoaderPath.substring(Templating.FILE_PREFIX.length());
    try {
      return new FileTemplateLoader(new File(directory));
    } catch (final IOException e) {
      throw new IllegalStateException(
          """
              The directory '%s' configured as the Business Cockpit's template loader path cannot \
              be read. Point '%s' at a directory holding the templates, or remove it and let the \
              cockpit report the names written in the BPMN."""
              .formatted(directory, key), e);
    }

  }

  /**
   * Sends the name of a template to the directory of the workflow module the name starts with.
   * Freemarker keeps one cache for all modules, and the module id in front keeps the names of two
   * modules apart.
   */
  private static final class WorkflowModuleTemplateLoader implements TemplateLoader {

    /** A template found in the directory of one module, with the loader which found it. */
    private record Source(
                          TemplateLoader loader,
                          Object source) {
    }

    private final Map<String, TemplateLoader> loaders;

    private final Map<String, String> below;

    private WorkflowModuleTemplateLoader(
        final Map<String, TemplateLoader> loaders,
        final Map<String, String> below) {

      this.loaders = Map.copyOf(loaders);
      this.below = Map.copyOf(below);

    }

    @Override
    public Object findTemplateSource(
        final String name) throws IOException {

      final var slash = name.indexOf('/');
      if (slash < 0) {
        return null;
      }
      final var workflowModuleId = name.substring(0, slash);
      final var loader = loaders.get(workflowModuleId);
      if (loader == null) {
        return null;
      }
      final var path = below.get(workflowModuleId);
      final var rest = name.substring(slash + 1);
      final var source = loader.findTemplateSource(path.isEmpty()
          ? rest
          : path
              + "/"
              + rest);
      return source == null ? null : new Source(loader, source);

    }

    @Override
    public long getLastModified(
        final Object templateSource) {

      final var source = (Source) templateSource;
      return source.loader().getLastModified(source.source());

    }

    @Override
    public Reader getReader(
        final Object templateSource,
        final String encoding) throws IOException {

      final var source = (Source) templateSource;
      return source.loader().getReader(source.source(), encoding);

    }

    @Override
    public void closeTemplateSource(
        final Object templateSource) throws IOException {

      final var source = (Source) templateSource;
      source.loader().closeTemplateSource(source.source());

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
   * Creates the templating on a Freemarker configuration which is already complete.
   *
   * @param configuration A configuration built elsewhere, for example a test loading its
   *          templates from the classpath, or an application which needs settings of its own
   */
  public FreemarkerTemplating(
      final Configuration configuration) {

    this.configuration = configuration;

  }

  /**
   * Creates a Freemarker configuration as the cockpit needs it. A caller adds the template loader.
   *
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
   * Version 1 required that library. Here it is a choice the application makes. A template which
   * renders a date fails without it, which is better than every application carrying a
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
      // a Freemarker template name is a path of its own, always separated by '/'. What a
      // file system calls a separator has nothing to do with it
      final var candidate = lookupPath.isEmpty()
          ? templateName
          : lookupPath
              + "/"
              + templateName;
      try {
        return configuration.getTemplate(candidate, locale);
      } catch (final TemplateNotFoundException e) {
        // the next, less specific path is asked. A template of this name simply does not
        // exist here, and that is the normal case for all but one of the paths
      } catch (final Exception e) {
        logger.error("Could not read the template '{}'", candidate, e);
        return null;
      }
    }
    return null;

  }

}
