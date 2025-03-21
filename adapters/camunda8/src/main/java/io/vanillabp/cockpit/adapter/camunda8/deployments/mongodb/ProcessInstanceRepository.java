package io.vanillabp.cockpit.adapter.camunda8.deployments.mongodb;


import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository(value = ProcessInstanceRepository.BEAN_NAME)
public interface ProcessInstanceRepository extends MongoRepository<ProcessInstanceEntity, Long> {

    String BEAN_NAME = "Camunda8BusinessCockpitProcessInstanceRepository";

    List<ProcessInstanceEntity> findByBusinessKey(String businessKey);

}
