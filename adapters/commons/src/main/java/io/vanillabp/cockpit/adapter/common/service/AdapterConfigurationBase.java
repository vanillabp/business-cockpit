package io.vanillabp.cockpit.adapter.common.service;

import io.vanillabp.springboot.adapter.SpringDataUtil;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class AdapterConfigurationBase<P extends BusinessCockpitServiceImplementation<?>> {

    private Map<Class<?>, P> connectableServices = new HashMap<>();

    public abstract String getAdapterId();
    
    public abstract <DE> P newBusinessCockpitServiceImplementation(
            final SpringDataUtil springDataUtil,
            final Class<DE> workflowAggregateClass,
            final Class<?> workflowAggregateIdClass,
            final CrudRepository<DE, Object> workflowAggregateRepository);

    protected Collection<P> getConnectableServices() {

        return connectableServices.values();

    }
    
    protected void putConnectableService(
            final Class<?> workflowAggregateClass,
            final P connectableService) {
        
        connectableServices.put(workflowAggregateClass, connectableService);
        
    }

}
