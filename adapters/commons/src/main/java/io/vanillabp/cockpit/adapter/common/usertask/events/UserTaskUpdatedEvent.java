package io.vanillabp.cockpit.adapter.common.usertask.events;


import java.util.List;

public class UserTaskUpdatedEvent extends UserTaskCreatedEvent {
  public UserTaskUpdatedEvent(String workflowModuleId, List<String> i18nLanguages) {
    super(workflowModuleId, i18nLanguages);
  }
}
