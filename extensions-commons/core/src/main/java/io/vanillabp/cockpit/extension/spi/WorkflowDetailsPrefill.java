package io.vanillabp.cockpit.extension.spi;

/**
 * What a BPMS knows about a workflow before the application's
 * <code>&#64;WorkflowDetailsProvider</code> method had its say. Everything is optional, the
 * same way {@link UserTaskDetailsPrefill} is.
 *
 * @param bpmnProcessVersion The version of the deployed process, as the BPMS counts it
 * @param businessId The business key the workflow was started with
 * @param bpmnProcessName The BPMN name of the process, the fallback title the cockpit shows
 * @param initiator Who started the workflow, if the BPMS records it
 */
public record WorkflowDetailsPrefill(
                                     String bpmnProcessVersion,
                                     String businessId,
                                     String bpmnProcessName,
                                     String initiator) {
}
