package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

/**
 * The annotation @EntityScan cannot be used here because when
 * placing in an auto-configuration class it disables auto-scanning
 * for the entire application. If an application does not use
 * @EntityScan but we would here then the application's
 * entities would not be found.
 * <p>
 * Therefor JPA entities have to be added programmatically by
 * this configuration.
 */
@Configuration
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
public class JpaEntityConfiguration {

    @Bean
    public BeanPostProcessor camunda8BusinessCockpitJpaBeanPostProcessor() {

        return new BeanPostProcessor() {

            @Override
            public Object postProcessBeforeInitialization(
                    final Object bean,
                    final String beanName) throws BeansException {

                if (!(bean instanceof LocalContainerEntityManagerFactoryBean)) {
                    return bean;
                }

                ((LocalContainerEntityManagerFactoryBean) bean)
                        .setPersistenceUnitPostProcessors(camunda8BusinessCockpitJpaPersistenceUnitPostProcessor());

                return bean;

            }

        };

    }

    public PersistenceUnitPostProcessor camunda8BusinessCockpitJpaPersistenceUnitPostProcessor() {

        return pui -> {
                pui.addManagedClassName(DeploymentResource.class.getName());
                pui.addManagedClassName(DeployedBpmn.class.getName());
                pui.addManagedClassName(Deployment.class.getName());
                pui.addManagedClassName(DeployedProcess.class.getName());
                pui.addManagedClassName(ProcessInstanceEntity.class.getName());
            };

    }

}
