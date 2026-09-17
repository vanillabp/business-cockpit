package io.vanillabp.cockpit.extension.spi;

/**
 * Everything needed to address one user task of one BPMS, and nothing else. The rule of
 * {@link WorkflowReference} holds here too: a reference is identifiers, and what the cockpit is
 * told is the report built at the event.
 *
 * @param adapterId The configured adapter id of the BPMS holding this task
 * @param workflowModuleId The workflow module the BPMN process belongs to
 * @param bpmnProcessId The BPMN process id, in the plain form the application wrote it
 * @param processVersion The version of the deployed BPMN process this task belongs to, spelled
 *          the way the BPMS reports it, or <code>null</code> where the BPMS reports none. It
 *          picks between <code>&#64;UserTaskDetailsProvider</code> methods which serve
 *          different versions of one model, so it is the plain version and never a version
 *          dressed up for a screen
 * @param workflowAggregateId The workflow aggregate's id, serialized
 * @param workflowId The BPMS' own identifier of the workflow the task belongs to
 * @param userTaskId The BPMS' own identifier of the user task
 * @param taskDefinition What the modeller wrote as the task's form reference: the Camunda 7
 *          form key, or the Camunda 8 external form reference. It is one of the two keys a
 *          <code>&#64;UserTaskDetailsProvider</code> method is matched by, and it is what the
 *          cockpit shows a form for
 * @param bpmnTaskId The BPMN element id of the user task, the other key a details provider is
 *          matched by
 */
public record UserTaskReference(
                                String adapterId,
                                String workflowModuleId,
                                String bpmnProcessId,
                                String processVersion,
                                String workflowAggregateId,
                                String workflowId,
                                String userTaskId,
                                String taskDefinition,
                                String bpmnTaskId) {
}
