package io.vanillabp.cockpit.adapter.common.service;

import io.vanillabp.spi.cockpit.BusinessCockpitService;
import io.vanillabp.spi.cockpit.usertask.UserTask;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A business-cockpit service which is aware of multiple adapter-specific business-cockpit services.
 * <p>
 * For operations each adapter is tried to complete the respective action.
 * As the particular workflow was started before using one of the configured
 * adapters, the action should complete successfully.
 * <p>
 * @see VanillaBpProperties#getDefaultAdapter()
 * @see VanillaBpProperties.WorkflowModuleAdapterProperties#getDefaultAdapter()
 * @see VanillaBpProperties.WorkflowAdapterProperties#getDefaultAdapter()
 */
public class AdapterAwareBusinessCockpitService<WA> implements BusinessCockpitService<WA> {

    private final VanillaBpProperties properties;

    private final Map<String, BusinessCockpitServiceImplementation<WA>> bcServicesByAdapter;

    private final List<String> wiredAdapterIds = new LinkedList<>();

    private String workflowModuleId;

    private String primaryBpmnProcessId;
    
    private final Set<String> bpmnProcessIds = new HashSet<>();

    private final Class<?> workflowAggregateIdClass;
    
    private final Class<?> workflowAggregateClass;
    
    public AdapterAwareBusinessCockpitService(
            final VanillaBpProperties properties,
            final Map<String, BusinessCockpitServiceImplementation<WA>> bcServicesByAdapter,
            final Class<?> workflowAggregateIdClass,
            final Class<?> workflowAggregateClass) {
        
        this.properties = properties;
        this.bcServicesByAdapter = bcServicesByAdapter;
        this.workflowAggregateIdClass = workflowAggregateIdClass;
        this.workflowAggregateClass = workflowAggregateClass;
        
        bcServicesByAdapter.forEach((adapterId, adapter) -> adapter.setParent(this));
        
    }
    
    public CrudRepository<WA, ?> getWorkflowAggregateRepository() {
        
        return bcServicesByAdapter
                .values()
                .stream()
                .findFirst()
                .map(BusinessCockpitServiceImplementation::getWorkflowAggregateRepository)
                .orElse(null);
        
    }
    
    public Class<?> getWorkflowAggregateIdClass() {
        
        return workflowAggregateIdClass;
        
    }
    
    public Class<?> getWorkflowAggregateClass() {
        
        return workflowAggregateClass;
        
    }
    
    public String getPrimaryBpmnProcessId() {
        
        return primaryBpmnProcessId;
        
    }
    
    public Collection<String> getBpmnProcessIds() {
        
        return bpmnProcessIds;
        
    }
    
    public String getWorkflowModuleId() {
        
        return workflowModuleId;
        
    }
    
    public void wire(
            final String adapterId,
            final String workflowModuleId,
            final String bpmnProcessId,
            final boolean isPrimary) {
        
        if ((this.workflowModuleId != null)
                && (workflowModuleId != null)
                && !this.workflowModuleId.equals(workflowModuleId)) {
            
            final var listOfAdapters = String.join(
                    "', '",
                    bcServicesByAdapter.keySet());
            
            throw new RuntimeException("Wiring the workflowModuleId '"
                    + workflowModuleId
                    + "' given by VanillaBP adapter '"
                    + adapterId
                    + "' to workflow-aggregate-class '"
                    + bcServicesByAdapter.values().iterator().next().getWorkflowAggregateClass()
                    + "' is not possible, because it was wired to '"
                    + this.workflowModuleId
                    + "' by these adapters before: '"
                    + listOfAdapters
                    + "'!");
            
        }

        if (bpmnProcessIds.contains(bpmnProcessId)
                && !bcServicesByAdapter.containsKey(adapterId)) {
            
            final var listOfAdapters = String.join(
                    "', '",
                    bcServicesByAdapter.keySet());
            
            throw new RuntimeException("Wiring the bpmnProcessId '"
                    + bpmnProcessId
                    + (this.workflowModuleId != null
                            ? "' from workflowModuleId '"
                                + workflowModuleId
                            : "")
                    + "' given by VanillaBP adapter '"
                    + adapterId
                    + "' to workflow-aggregate-class '"
                    + bcServicesByAdapter.values().iterator().next().getWorkflowAggregateClass()
                    + "' is not possible, because it was already wired by these adapters: '"
                    + listOfAdapters
                    + "'!");
            
        }

        if (this.workflowModuleId == null) {
            this.workflowModuleId = workflowModuleId;
        }

        if (isPrimary) {
            this.primaryBpmnProcessId = bpmnProcessId;
        }

        wiredAdapterIds.add(adapterId);
        if (wiredAdapterIds.size() == bcServicesByAdapter.size()) { // all adapters wired for this service
            properties.validatePropertiesFor(
                    wiredAdapterIds,
                    workflowModuleId,
                    bpmnProcessId);
        }

        this.bpmnProcessIds.add(bpmnProcessId);
        
    }

    private List<String> getAdapterIds() {
        
        return properties.getDefaultAdapterFor(workflowModuleId, primaryBpmnProcessId);

    }
    
    private String determinePrimaryAdapterId() {
        
        return getAdapterIds().get(0);
        
    }
    
    @Override
    public void aggregateChanged(
            final WA workflowAggregate) {

        bcServicesByAdapter
                .get(determinePrimaryAdapterId())
                .aggregateChanged(workflowAggregate);
        
    }
    
    @Override
    public void aggregateChanged(
            final WA workflowAggregate,
            final String... userTaskIds) {

        bcServicesByAdapter
                .get(determinePrimaryAdapterId())
                .aggregateChanged(workflowAggregate, userTaskIds);
        
    }

    @Override
    public Optional<UserTask> getUserTask(
            final WA workflowAggregate,
            final String userTaskId) {

        return bcServicesByAdapter
                .get(determinePrimaryAdapterId())
                .getUserTask(workflowAggregate, userTaskId);

    }

}
