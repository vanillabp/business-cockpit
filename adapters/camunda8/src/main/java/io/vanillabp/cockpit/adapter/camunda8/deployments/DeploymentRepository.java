package io.vanillabp.cockpit.adapter.camunda8.deployments;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository(value = "BusinessCockpitDeploymentRepository")
public interface DeploymentRepository extends CrudRepository<Deployment, DeploymentId> {

    /**
     * <pre>
     * select distinct p.deployedResource from DeployedProcess p where not p.packageId = ?1
     * </pre>
     */
    Optional<Deployment> findByDefinitionKey(long packageId);

}
