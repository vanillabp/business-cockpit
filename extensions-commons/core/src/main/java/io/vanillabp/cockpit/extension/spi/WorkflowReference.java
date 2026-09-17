package io.vanillabp.cockpit.extension.spi;

/**
 * Everything needed to address one workflow of one BPMS, and nothing else.
 * <p>
 * A reference travels through the outbox, so it consists of identifiers only. Whatever the
 * cockpit is told about the workflow is read again while the entry is dispatched, which is why
 * a pending update never carries stale data.
 *
 * @param adapterId The configured adapter id of the BPMS holding this workflow. Which BPMS
 *          that is changes per workflow during a migration, so it belongs to the reference
 *          and not to the extension
 * @param workflowModuleId The workflow module the BPMN process belongs to
 * @param bpmnProcessId The BPMN process id, in the plain form the application wrote it
 * @param processVersion The version of the deployed BPMN process this workflow runs on, spelled
 *          the way the BPMS reports it, or <code>null</code> where the BPMS reports none. It
 *          picks between <code>&#64;WorkflowDetailsProvider</code> methods which serve different
 *          versions of one model, so it is the plain version and never a version dressed up for
 *          a screen
 * @param workflowAggregateId The workflow aggregate's id, serialized
 * @param workflowId The BPMS' own identifier of the workflow: a process-instance id for
 *          Camunda 7, a process-instance key for Camunda 8
 */
public record WorkflowReference(
                                String adapterId,
                                String workflowModuleId,
                                String bpmnProcessId,
                                String processVersion,
                                String workflowAggregateId,
                                String workflowId) {
}
