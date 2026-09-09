package io.vanillabp.cockpit.extension.spi;

/**
 * Everything needed to address one workflow of one BPMS, and nothing else.
 * <p>
 * A reference travels through the outbox, so it consists of identifiers only. Whatever the
 * cockpit is told about the workflow is read again while the entry is dispatched, which is why
 * a pending update never carries stale data.
 *
 * @param adapterId The configured adapter id of the BPMS holding this workflow - which BPMS
 *          that is changes per workflow during a migration, so it is part of the reference
 *          rather than of the extension
 * @param workflowModuleId The workflow module the BPMN process belongs to
 * @param bpmnProcessId The BPMN process id, in the plain form the application wrote it
 * @param workflowAggregateId The workflow aggregate's id, serialized
 * @param workflowId The BPMS' own identifier of the workflow: a process-instance id for
 *          Camunda 7, a process-instance key for Camunda 8
 */
public record WorkflowReference(
                                String adapterId,
                                String workflowModuleId,
                                String bpmnProcessId,
                                String workflowAggregateId,
                                String workflowId) {
}
