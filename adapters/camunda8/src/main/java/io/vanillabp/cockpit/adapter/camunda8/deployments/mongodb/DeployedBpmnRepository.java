package io.vanillabp.cockpit.adapter.camunda8.deployments.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository(DeployedBpmnRepository.BEAN_NAME)
public interface DeployedBpmnRepository extends MongoRepository<DeployedBpmn, Integer> {

    String BEAN_NAME = "Camunda8BcDeployedBpmnRepository";

}
