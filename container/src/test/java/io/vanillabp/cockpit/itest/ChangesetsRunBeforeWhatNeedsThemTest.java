package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import com.phactum.mongodb.changesets.ChangesetApplier;
import com.phactum.mongodb.changesets.ChangesetAutoConfiguration;
import io.vanillabp.cockpit.commons.mongo.changestreams.ChangeStreamUtils;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Two beans of the cockpit may only be built after the MongoDB migration has run. The change
 * streams are opened on collections a changeset creates, and the proxy registry is filled from a
 * collection a changeset creates. Both say so by taking the bean which runs the migration as a
 * parameter, and Spring builds what a bean depends on first.
 *
 * <p>This test reads the running application rather than the source. It asks the bean factory in
 * which order it built its singletons, and which beans those two were built from. A parameter
 * called {@code changesetsHaveBeenApplied} proves nothing by itself. While the cockpit carried the
 * mechanism itself, the parameter was of type {@code ChangesetAutoConfiguration}, and a
 * configuration class is built before the beans it declares. That version compiled, started, and
 * had no order at all. Nothing would have gone red.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ChangesetsRunBeforeWhatNeedsThemTest extends ItestBase {

    @Autowired
    private ConfigurableApplicationContext context;

    /**
     * The names of the singletons in the order the bean factory built them.
     */
    private List<String> orderTheBeansWereBuiltIn() {

        return List.of(context.getBeanFactory().getSingletonNames());

    }

    private String theOnlyBeanOfType(
            final Class<?> type) {

        final var names = context.getBeanNamesForType(type);
        assertThat(names)
                .describedAs("beans of type %s", type.getName())
                .hasSize(1);
        return names[0];

    }

    private void assertBuiltAfterTheMigration(
            final Class<?> type) {

        final var applier = theOnlyBeanOfType(ChangesetApplier.class);
        final var waiting = theOnlyBeanOfType(type);
        final var order = orderTheBeansWereBuiltIn();

        assertThat(order.indexOf(applier))
                .describedAs("""
                        Position of the bean which applies the changesets, and position of %s. \
                        The migration has to be the earlier one of the two."""
                        .formatted(waiting))
                .isGreaterThanOrEqualTo(0)
                .isLessThan(order.indexOf(waiting));

        assertThat(context.getBeanFactory().getDependenciesForBean(waiting))
                .describedAs("""
                        The beans %s was built from. It has to be the changeset applier which is \
                        named here. The auto-configuration exists before the applier does, so a \
                        parameter of that type would leave this assertion failing and the \
                        application starting.""".formatted(waiting))
                .contains(applier)
                .doesNotContain(theOnlyBeanOfType(ChangesetAutoConfiguration.class));

    }

    @Test
    void theChangeStreamsAreOpenedAfterTheMigrationHasRun() {

        assertBuiltAfterTheMigration(ChangeStreamUtils.class);

    }

    @Test
    void theProxyRegistryIsFilledAfterTheMigrationHasRun() {

        assertBuiltAfterTheMigration(MicroserviceProxyRegistry.class);

    }

}
