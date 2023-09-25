package io.vanillabp.cockpit.adapter.common.service;

import io.vanillabp.spi.cockpit.BusinessCockpitService;
import org.springframework.data.repository.CrudRepository;

public interface BusinessCockpitServiceImplementation<WA> extends BusinessCockpitService<WA> {

    Class<WA> getWorkflowAggregateClass();

    CrudRepository<WA, ?> getWorkflowAggregateRepository();
    
    void setParent(AdapterAwareBusinessCockpitService<WA> parent);

}
