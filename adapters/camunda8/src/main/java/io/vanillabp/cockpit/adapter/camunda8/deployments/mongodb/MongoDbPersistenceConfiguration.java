package io.vanillabp.cockpit.adapter.camunda8.deployments.mongodb;

import io.vanillabp.cockpit.adapter.camunda8.deployments.DeploymentPersistence;
import io.vanillabp.cockpit.adapter.camunda8.deployments.ProcessInstancePersistence;
import io.vanillabp.springboot.utils.MongoDbSpringDataUtil;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

@Configuration
@AutoConfigureAfter(MongoRepositoriesAutoConfiguration.class)
@ConditionalOnBean(MongoDbSpringDataUtil.class)
public class MongoDbPersistenceConfiguration {

    private MongoRepositoryFactory mongoRepositoryFactory;

    @Bean(ProcessInstanceRepository.BEAN_NAME)
    @ConditionalOnMissingBean(ProcessInstanceRepository.class)
    public ProcessInstanceRepository camunda8BusinessCockpitMongoDbProcessInstanceRepository(
            final MongoOperations mongoOperations) {

        if (mongoRepositoryFactory == null) {
            mongoRepositoryFactory = new MongoRepositoryFactory(mongoOperations);
        }
        return mongoRepositoryFactory.getRepository(ProcessInstanceRepository.class);

    }

    @Bean(DeployedBpmnRepository.BEAN_NAME)
    @ConditionalOnMissingBean(DeployedBpmnRepository.class)
    public DeployedBpmnRepository camunda8BusinessCockpitMongoDbDeployedBpmnRepository(
            final MongoOperations mongoOperations) {

        if (mongoRepositoryFactory == null) {
            mongoRepositoryFactory = new MongoRepositoryFactory(mongoOperations);
        }
        return mongoRepositoryFactory.getRepository(DeployedBpmnRepository.class);

    }

    @Bean(DeploymentResourceRepository.BEAN_NAME)
    @ConditionalOnMissingBean(DeploymentResourceRepository.class)
    public DeploymentResourceRepository camunda8BusinessCockpitMongoDbDeploymentResourceRepository(
            final MongoOperations mongoOperations) {

        if (mongoRepositoryFactory == null) {
            mongoRepositoryFactory = new MongoRepositoryFactory(mongoOperations);
        }
        return mongoRepositoryFactory.getRepository(DeploymentResourceRepository.class);

    }

    @Bean(DeploymentRepository.BEAN_NAME)
    @ConditionalOnMissingBean(DeploymentRepository.class)
    public DeploymentRepository camunda8BusinessCockpitMongoDbDeploymentRepository(
            final MongoOperations mongoOperations) {

        if (mongoRepositoryFactory == null) {
            mongoRepositoryFactory = new MongoRepositoryFactory(mongoOperations);
        }
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
