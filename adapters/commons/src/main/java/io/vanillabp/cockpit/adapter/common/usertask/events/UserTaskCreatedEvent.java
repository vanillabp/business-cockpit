package io.vanillabp.cockpit.adapter.common.usertask.events;


import java.util.List;

public class UserTaskCreatedEvent extends UserTaskEventImpl {
  public UserTaskCreatedEvent(String workflowModuleId, List<String> i18nLanguages) {
    super(workflowModuleId, i18nLanguages);
  }
}
