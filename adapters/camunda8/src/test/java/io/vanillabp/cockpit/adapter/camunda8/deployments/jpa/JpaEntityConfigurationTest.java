package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;

/**
 * The adapter adds its own entities to the application's persistence unit by mutating the
 * {@link PersistenceManagedTypes} bean from a {@code BeanPostProcessor} - {@code @EntityScan} cannot be used
 * in an auto-configuration, because it would switch off the application's own entity scanning.
 *
 * <p>That mutation rests on an assumption nothing enforces: that {@code getManagedClassNames()} hands out the
 * live, modifiable list rather than a copy or an unmodifiable view. Should Spring ever return a copy, the
 * entities are dropped <b>silently</b> and the first symptom is a missing table under {@code ddl-auto} or an
 * "unknown entity" at runtime. An unmodifiable list would at least throw. This test pins the assumption down
 * for Spring Framework 7 / Hibernate 7.
 */
class JpaEntityConfigurationTest {

    /**
     * Built the way Spring Boot builds the bean: by scanning packages. {@code PersistenceManagedTypes.of(..)}
     * is <b>not</b> equivalent - it hands out a fixed-size list and the post processor fails on it, see
     * {@link #theFactoryMethodProducesAnImmutableListUnlikeTheScanner()}.
     */
    private PersistenceManagedTypes scanned() {

        return new PersistenceManagedTypesScanner(new PathMatchingResourcePatternResolver())
                .scan("io.vanillabp.cockpit.adapter.camunda8.deployments.mongodb");

    }

    @Test
    void theAdapterEntitiesAreAddedToThePersistenceUnit() {

        final var managedTypes = scanned();

        new JpaEntityConfiguration()
                .camunda8BusinessCockpitJpaBeanPostProcessor()
                .postProcessBeforeInitialization(managedTypes, "persistenceManagedTypes");

        assertThat(managedTypes.getManagedClassNames())
                .as("the mutation has to be visible on the very instance the application uses")
                .contains(
                        DeploymentResource.class.getName(),
                        DeployedBpmn.class.getName(),
                        Deployment.class.getName(),
                        DeployedProcess.class.getName(),
                        ProcessInstanceEntity.class.getName());

    }

    /**
     * Documents how narrow the assumption is: the same post processor applied to an instance from the
     * factory method blows up. Spring makes no promise about the list being modifiable, so if the scanner
     * ever switches to a copy or an unmodifiable view - as the factory method already does - the entities
     * are gone. This test is here so that difference is on record rather than rediscovered.
     */
    @Test
    void theFactoryMethodProducesAnImmutableListUnlikeTheScanner() {

        final var fromFactoryMethod = PersistenceManagedTypes.of("com.example.ApplicationEntity");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new JpaEntityConfiguration()
                .camunda8BusinessCockpitJpaBeanPostProcessor()
                .postProcessBeforeInitialization(fromFactoryMethod, "persistenceManagedTypes"))
                .isInstanceOf(UnsupportedOperationException.class);

    }

    /**
     * Anything that is not a {@code PersistenceManagedTypes} has to be handed back untouched - the post
     * processor sees every bean in the context.
     */
    @Test
    void otherBeansArePassedThrough() {

        final var other = new Object();

        assertThat(new JpaEntityConfiguration()
                .camunda8BusinessCockpitJpaBeanPostProcessor()
                .postProcessBeforeInitialization(other, "somethingElse"))
                .isSameAs(other);

    }

}
