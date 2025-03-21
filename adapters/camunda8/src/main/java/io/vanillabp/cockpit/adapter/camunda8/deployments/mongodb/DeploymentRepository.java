package io.vanillabp.cockpit.adapter.camunda8.deployments.mongodb;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository(DeploymentRepository.BEAN_NAME)
public interface DeploymentRepository extends MongoRepository<Deployment, String> {

    String BEAN_NAME = "Camunda8BcDeploymentRepository";

    Optional<Deployment> findByDefinitionKey(long definitionKey);
    
}
