package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository(DeployedBpmnRepository.BEAN_NAME)
public interface DeployedBpmnRepository extends JpaRepository<DeployedBpmn, Integer> {

    String BEAN_NAME = "Camunda8BusinessCockpitDeployedBpmnRepository";

    List<DeployedBpmn> findByDeployments_packageIdNot(int packageId);

}
