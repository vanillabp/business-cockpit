package io.vanillabp.cockpit.adapter.camunda8.deployments;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository(value = "BusinessCockpitDeployedProcessRepository")
public interface DeployedProcessRepository extends CrudRepository<DeployedProcess, DeploymentId> {

}
