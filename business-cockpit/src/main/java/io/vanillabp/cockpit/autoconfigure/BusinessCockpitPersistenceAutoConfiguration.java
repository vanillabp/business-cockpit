package io.vanillabp.cockpit.autoconfigure;

import com.mongodb.client.MongoClient;
import com.phactum.mongodb.changesets.ChangesetApplier;
import io.vanillabp.cockpit.commons.mongo.changestreams.ChangeStreamUtils;
import io.vanillabp.cockpit.commons.mongo.updateinfo.UpdateInformationEventListener;
import io.vanillabp.cockpit.config.MongoDbConfiguration;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Where the cockpit keeps user tasks, workflows, modules, users and pending notifications: the
 * MongoDB template with the cockpit's converters and write concern, the repositories reading it, the
 * changesets creating what they read, and the change streams the user interface is updated from.
 * <p>
 * Ordered ahead of Spring Boot's own MongoDB configuration, which is what lets
 * {@link MongoDbConfiguration} contribute the {@code MongoTemplate} and the converters. Spring
 * Boot's versions of both stand aside for a bean which is already there. So whoever registers first
 * decides, and leaving that to chance would mean two sets of converters on some starts and a
 * duplicate template on others.
 */
@AutoConfiguration(before = DataMongoAutoConfiguration.class)
@ConditionalOnClass({ MongoClient.class, MongoTemplate.class })
@Import({
    BusinessCockpitPersistenceAutoConfiguration.CockpitDocumentPackages.class,
    MongoDbConfiguration.class,
    UpdateInformationEventListener.class,
    io.vanillabp.cockpit.notification.model.changesets.V000001.class,
    io.vanillabp.cockpit.tasklist.model.changesets.V000001.class,
    io.vanillabp.cockpit.users.model.changesets.V000001.class,
    io.vanillabp.cockpit.workflowlist.model.changesets.V000001.class,
    io.vanillabp.cockpit.workflowmodules.model.changesets.V000001.class
})
public class BusinessCockpitPersistenceAutoConfiguration {

    /**
     * Everything of the cockpit lives below this package, which is also what the component scan of
     * the former base class covered. {@code EveryCockpitBeanIsRegisteredTest} reads it and fails when
     * it stops holding the library's classes.
     */
    public static final String COCKPIT_PACKAGE = "io.vanillabp.cockpit";

    /**
     * Subscribes the cockpit's services to the change streams of their collections.
     *
     * @param changesetsHaveBeenApplied not read, and not replaceable by an annotation: a change
     *      stream is opened on a collection, so the changesets which create the collections have to
     *      have run. Asking for the bean which runs them is what says so at compile time. It has to
     *      be {@link ChangesetApplier} and not the auto-configuration which declares it. A
     *      configuration class exists before its own beans do, so asking for it would compile,
     *      start and guarantee nothing.
     */
    @Bean
    public ChangeStreamUtils changeStreamUtils(
            final ChangesetApplier changesetsHaveBeenApplied) {

        return new ChangeStreamUtils();

    }

    /**
     * Tells Spring Boot to look for Spring Data repositories and documents in the cockpit's package as
     * well as in the application's own.
     * <p>
     * Spring Boot scans the package of the class carrying {@code @SpringBootApplication}, and the
     * cockpit's repositories live in this jar. Adding to that list is the one way to have them
     * found without taking the scan over. An {@code @EnableMongoRepositories} of our own would make
     * Spring Boot's stand aside completely, and the application's own repositories would then be
     * the ones nobody looks for.
     * <p>
     * The package added is the whole of {@link #COCKPIT_PACKAGE} rather than the five packages the
     * repositories sit in. Spring Boot keeps the list free of duplicates, and the delivered
     * {@code container} contributes exactly this package because that is where its own application
     * class lives. Naming the five would leave that application with a scan of the package and a scan
     * of each package inside it, which registers every repository twice and ends the start.
     */
    static class CockpitDocumentPackages implements ImportBeanDefinitionRegistrar {

        @Override
        public void registerBeanDefinitions(
                final AnnotationMetadata importingClassMetadata,
                final BeanDefinitionRegistry registry) {

            AutoConfigurationPackages.register(registry, COCKPIT_PACKAGE);

        }

    }

}
