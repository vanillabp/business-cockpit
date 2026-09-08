package io.vanillabp.cockpit.extension.config;

import java.util.List;
import java.util.Map;

/**
 * What one workflow of a workflow module says about the Business Cockpit, and what its user
 * tasks say. Every value here is what the workflow differs from its module in, so a value the
 * workflow left out is <code>null</code> rather than a copy of the module's - the module is
 * asked next, and copying would make a later change of the module's value invisible.
 *
 * @param bpmnProcessId The workflow these settings belong to
 * @param i18nLanguages The languages this workflow's titles are reported in, or
 *          <code>null</code> where the module decides
 * @param bpmnDescriptionLanguage The language this workflow's BPMN names are written in, or
 *          <code>null</code>
 * @param templatePath The segment this workflow contributes to the template lookup path, or
 *          <code>null</code> where the BPMN process id is the segment
 * @param templatePathPerUserTask The segment a single user task contributes, by task
 *          definition, where it is not the task definition itself
 */
public record WorkflowConfiguration(
                                    String bpmnProcessId,
                                    List<String> i18nLanguages,
                                    String bpmnDescriptionLanguage,
                                    String templatePath,
                                    Map<String, String> templatePathPerUserTask) {

  public WorkflowConfiguration {
    i18nLanguages = i18nLanguages == null ? null : List.copyOf(i18nLanguages);
    templatePathPerUserTask = templatePathPerUserTask == null
        ? Map.of()
        : Map.copyOf(templatePathPerUserTask);
  }

}
