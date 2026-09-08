package io.vanillabp.cockpit.extension.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * What the Business Cockpit has to know about one workflow module. Every value is resolved
 * from the module's own section first and from the extension's global section afterwards, so a
 * setting which is the same for every module is written once.
 *
 * @param workflowModuleId The module these settings belong to
 * @param workflowModuleUri Where this module answers the cockpit's provider APIs
 * @param uiUriType How the cockpit loads this module's forms
 * @param uiUriPath Where this module's forms are served from
 * @param i18nLanguages The languages titles are reported in, in the order they were configured
 * @param bpmnDescriptionLanguage The language the names in the BPMN files are written in
 * @param groupHierarchy Which groups a group stands for, reported to the cockpit so that it
 *          can resolve a user's permissions
 * @param templatePath The segment this module contributes to the template lookup path, which
 *          is the module id where nothing was configured
 */
public record WorkflowModuleConfiguration(
                                          String workflowModuleId,
                                          String workflowModuleUri,
                                          UiUriType uiUriType,
                                          String uiUriPath,
                                          List<String> i18nLanguages,
                                          String bpmnDescriptionLanguage,
                                          Map<String, Collection<String>> groupHierarchy,
                                          String templatePath) {

  public WorkflowModuleConfiguration {
    i18nLanguages = i18nLanguages == null ? List.of() : List.copyOf(i18nLanguages);
    groupHierarchy = groupHierarchy == null ? Map.of() : Map.copyOf(groupHierarchy);
  }

}
