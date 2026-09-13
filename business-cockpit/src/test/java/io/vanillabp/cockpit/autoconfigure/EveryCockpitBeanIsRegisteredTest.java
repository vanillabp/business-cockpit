package io.vanillabp.cockpit.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;

/**
 * The cockpit is wired by explicit imports and no longer by a component scan, so a class carrying a
 * stereotype annotation is not a bean until somebody names it. This test is what notices when
 * somebody did not.
 *
 * <p>Without it the failure arrives much later and reads like something else. A service nobody
 * registered is missing from the context of an application built on the library, while the delivered
 * application keeps working: it lives in {@code io.vanillabp.cockpit} itself, so its own component
 * scan finds the library's classes and hides the gap for exactly the one application the suite boots
 * most often.
 *
 * <p>Both sides are read from the classpath rather than listed here. On one side every concrete class
 * below {@code io.vanillabp.cockpit} which is a component, on the other everything the
 * auto-configurations of this jar reach, following {@code @Import} as far as it goes and counting what
 * {@code @Bean} methods produce.
 */
@ExtendWith(SuppressOutputExtension.class)
class EveryCockpitBeanIsRegisteredTest {

    private static final String COMPONENT_ANNOTATION = "org.springframework.stereotype.Component";

    private static final String IMPORT_ANNOTATION = "org.springframework.context.annotation.Import";

    private static final String BEAN_ANNOTATION = "org.springframework.context.annotation.Bean";

    private static final String CLASSES_OF_THE_COCKPIT = "classpath*:"
            + BusinessCockpitPersistenceAutoConfiguration.COCKPIT_PACKAGE.replace('.', '/')
            + "/**/*.class";

    private static final String IMPORTS_RESOURCE =
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

    private final PathMatchingResourcePatternResolver classpath = new PathMatchingResourcePatternResolver();

    private final MetadataReaderFactory metadata = new CachingMetadataReaderFactory(classpath);

    @Test
    void everyComponentOfTheLibraryIsReachedByAnAutoConfiguration() {

        final var components = componentsOfTheLibrary();
        assertThat(components)
                .describedAs("""
                        Components found below %s. A number far below what the library holds means the                         package name has moved and the auto-configurations point at nothing."""
                        .formatted(BusinessCockpitPersistenceAutoConfiguration.COCKPIT_PACKAGE))
                .hasSizeGreaterThan(30);

        final var forgotten = new TreeSet<>(components);
        forgotten.removeAll(everythingTheAutoConfigurationsReach());

        assertThat(forgotten)
                .describedAs("""
                        Classes carrying a stereotype annotation which no auto-configuration of \
                        io.vanillabp.cockpit.autoconfigure reaches. Add each of them to the @Import of \
                        the auto-configuration of its area, or drop the annotation where the class is \
                        not meant to be a bean.""")
                .isEmpty();

    }

    /**
     * Every concrete class below {@code io.vanillabp.cockpit} which a component scan would register,
     * taken from the compiled library and from {@code commons}. Test classes are left out: they are
     * beans of a test context and no auto-configuration has to know them.
     */
    private Set<String> componentsOfTheLibrary() {

        return Arrays
                .stream(classesOfTheCockpit())
                .filter(resource -> !resource.getDescription().contains("test-classes"))
                .map(this::annotationsOf)
                .filter(AnnotationMetadata::isIndependent)
                .filter(AnnotationMetadata::isConcrete)
                .filter(annotations -> annotations.isAnnotated(COMPONENT_ANNOTATION))
                .map(AnnotationMetadata::getClassName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

    }

    private Set<String> everythingTheAutoConfigurationsReach() {

        final var reached = new LinkedHashSet<String>();
        final var toRead = new ArrayDeque<>(autoConfigurationNames());
        reached.addAll(toRead);

        while (!toRead.isEmpty()) {
            final var configuration = annotationsOf(toRead.poll());
            for (final var imported : importsOf(configuration)) {
                if (reached.add(imported)) {
                    toRead.add(imported);
                }
            }
            configuration
                    .getAnnotatedMethods(BEAN_ANNOTATION)
                    .forEach(beanMethod -> reached.add(beanMethod.getReturnTypeName()));
        }
        return reached;

    }

    private List<String> importsOf(
            final AnnotationMetadata configuration) {

        final var attributes = configuration.getAnnotationAttributes(IMPORT_ANNOTATION, true);
        if (attributes == null) {
            return List.of();
        }
        return List.of((String[]) attributes.get("value"));

    }

    private Resource[] classesOfTheCockpit() {

        try {
            return classpath.getResources(CLASSES_OF_THE_COCKPIT);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list '" + CLASSES_OF_THE_COCKPIT + "'", e);
        }

    }

    private AnnotationMetadata annotationsOf(
            final Resource classFile) {

        try {
            return metadata.getMetadataReader(classFile).getAnnotationMetadata();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read '" + classFile + "'", e);
        }

    }

    private AnnotationMetadata annotationsOf(
            final String className) {

        try {
            return metadata.getMetadataReader(className).getAnnotationMetadata();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read '" + className + "'", e);
        }

    }

    /**
     * The auto-configurations of every jar on the classpath, not only of this one. {@code commons}
     * registers two of its own, and a class they reach is registered as surely as one this jar names.
     */
    private List<String> autoConfigurationNames() {

        try {
            return Arrays
                    .stream(classpath.getResources("classpath*:" + IMPORTS_RESOURCE))
                    .flatMap(this::linesOf)
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list '" + IMPORTS_RESOURCE + "'", e);
        }

    }

    private Stream<String> linesOf(
            final Resource resource) {

        try {
            return new String(resource.getContentAsByteArray(), StandardCharsets.UTF_8).lines();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read '" + resource + "'", e);
        }

    }

}
