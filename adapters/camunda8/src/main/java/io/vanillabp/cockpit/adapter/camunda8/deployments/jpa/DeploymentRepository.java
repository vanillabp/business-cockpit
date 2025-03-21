package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository(DeploymentRepository.BEAN_NAME)
public interface DeploymentRepository extends JpaRepository<Deployment, DeploymentId> {

    String BEAN_NAME = "Camunda8BcDeploymentRepository";

    Optional<Deployment> findByDefinitionKey(long definitionKey);
    
}
