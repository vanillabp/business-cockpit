package io.vanillabp.cockpit.adapter.camunda8.deployments;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository(value = "BusinessCockpitDeploymentResourceRepository")
public interface DeploymentResourceRepository extends CrudRepository<DeploymentResource, Integer> {

    List<DeployedBpmn> findDistinctByTypeAndDeployments_packageIdNot(String type, int packageId);
    
}
