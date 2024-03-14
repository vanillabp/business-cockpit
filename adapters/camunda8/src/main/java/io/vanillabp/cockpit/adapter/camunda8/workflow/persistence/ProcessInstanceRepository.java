package io.vanillabp.cockpit.adapter.camunda8.workflow.persistence;


import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository(value = "BusinessCockpitProcessInstanceRepository")
public interface ProcessInstanceRepository extends CrudRepository<ProcessInstanceEntity, Long> {

    List<ProcessInstanceEntity> findProcessInstanceEntityByBusinessKey(String businessKey);
}
