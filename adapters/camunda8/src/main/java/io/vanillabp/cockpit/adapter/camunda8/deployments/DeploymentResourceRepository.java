package io.vanillabp.cockpit.adapter.camunda8.deployments;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository(value = "BusinessCockpitDeploymentResourceRepository")
public interface DeploymentResourceRepository extends CrudRepository<DeploymentResource, Integer> {

    List<DeployedBpmn> findByTypeAndDeployments_packageIdNot(String type, int packageId);
    
}
