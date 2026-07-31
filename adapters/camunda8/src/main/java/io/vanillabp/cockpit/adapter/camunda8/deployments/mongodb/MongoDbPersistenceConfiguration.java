package io.vanillabp.cockpit.adapter.camunda8.deployments.mongodb;

import io.vanillabp.cockpit.adapter.camunda8.deployments.DeploymentPersistence;
import io.vanillabp.cockpit.adapter.camunda8.deployments.ProcessInstancePersistence;
import io.vanillabp.springboot.utils.MongoDbSpringDataUtil;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

// registered through the .imports file, so it has to be an @AutoConfiguration - only then are
// @AutoConfigureBefore/After honoured
@AutoConfiguration
@AutoConfigureAfter(DataMongoRepositoriesAutoConfiguration.class)
@ConditionalOnBean(MongoDbSpringDataUtil.class)
public class MongoDbPersistenceConfiguration {

    /**
     * A bean rather than a lazily filled field: the four repository beans below all need the same factory,
     * and the previous "if (field == null) field = new .." in each of them is not safe when bean creation
     * runs in parallel. Spring instantiates a bean exactly once, which is the guarantee that was missing.
     */
    @Bean
    public MongoRepositoryFactory camunda8BusinessCockpitMongoRepositoryFactory(
            final MongoOperations mongoOperations) {

        return new MongoRepositoryFactory(mongoOperations);

    }

    @Bean(ProcessInstanceRepository.BEAN_NAME)
    @ConditionalOnMissingBean(ProcessInstanceRepository.class)
    public ProcessInstanceRepository camunda8BusinessCockpitMongoDbProcessInstanceRepository(
            final MongoRepositoryFactory mongoRepositoryFactory) {

        return mongoRepositoryFactory.getRepository(ProcessInstanceRepository.class);

    }

    @Bean(DeployedBpmnRepository.BEAN_NAME)
    @ConditionalOnMissingBean(DeployedBpmnRepository.class)
    public DeployedBpmnRepository camunda8BusinessCockpitMongoDbDeployedBpmnRepository(
            final MongoRepositoryFactory mongoRepositoryFactory) {

        return mongoRepositoryFactory.getRepository(DeployedBpmnRepository.class);

    }

    @Bean(DeploymentResourceRepository.BEAN_NAME)
    @ConditionalOnMissingBean(DeploymentResourceRepository.class)
    public DeploymentResourceRepository camunda8BusinessCockpitMongoDbDeploymentResourceRepository(
            final MongoRepositoryFactory mongoRepositoryFactory) {

        return mongoRepositoryFactory.getRepository(DeploymentResourceRepository.class);

    }

    @Bean(DeploymentRepository.BEAN_NAME)
    @ConditionalOnMissingBean(DeploymentRepository.class)
    public DeploymentRepository camunda8BusinessCockpitMongoDbDeploymentRepository(
            final MongoRepositoryFactory mongoRepositoryFactory) {

        return mongoRepositoryFactory.getRepository(DeploymentRepository.class);

    }

    @Bean
    public DeploymentPersistence camunda8BusinessCockpitDeploymentPersistence(
            final DeploymentResourceRepository deploymentResourceRepository,
            final DeploymentRepository deploymentRepository,
            final DeployedBpmnRepository deployedBpmnRepository) {

        return new MongoDbDeploymentPersistence(
                deploymentResourceRepository,
                deploymentRepository,
                deployedBpmnRepository);

    }

    @Bean
    public ProcessInstancePersistence camunda8BusinessCockpitProcessInstancePersistence(
            final ProcessInstanceRepository processInstanceRepository) {

        return new MongoDbProcessInstancePersistence(processInstanceRepository);

    }

}
