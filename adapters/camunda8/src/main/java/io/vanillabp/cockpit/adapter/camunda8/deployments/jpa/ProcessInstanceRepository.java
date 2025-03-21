package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository(value = ProcessInstanceRepository.BEAN_NAME)
public interface ProcessInstanceRepository extends JpaRepository<ProcessInstanceEntity, Long> {

    String BEAN_NAME = "Camunda8BusinessCockpitProcessInstanceRepository";

    List<ProcessInstanceEntity> findByBusinessKey(String businessKey);

}
