package io.vanillabp.cockpit.extension.spi;

/**
 * Everything needed to address one user task of one BPMS, and nothing else. The same rule as
 * for {@link WorkflowReference} holds: identifiers travel, data is read at dispatch time.
 *
 * @param adapterId The configured adapter id of the BPMS holding this task
 * @param workflowModuleId The workflow module the BPMN process belongs to
 * @param bpmnProcessId The BPMN process id, in the plain form the application wrote it
 * @param workflowAggregateId The workflow aggregate's id, serialized
 * @param workflowId The BPMS' own identifier of the workflow the task belongs to
 * @param userTaskId The BPMS' own identifier of the user task
 * @param taskDefinition What the modeller wrote as the task's form reference - the Camunda 7
 *          form key, the Camunda 8 external form reference. It is one of the two keys a
 *          <code>&#64;UserTaskDetailsProvider</code> method is matched by, and it is what the
 *          cockpit shows a form for
 * @param bpmnTaskId The BPMN element id of the user task, the other key a details provider is
 *          matched by
 */
public record UserTaskReference(
                                String adapterId,
                                String workflowModuleId,
                                String bpmnProcessId,
                                String workflowAggregateId,
                                String workflowId,
                                String userTaskId,
                                String taskDefinition,
                                String bpmnTaskId) {
}
