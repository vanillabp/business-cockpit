package io.vanillabp.cockpit.adapter.common.usertask.events;

import java.time.OffsetDateTime;

public abstract class UserTaskLifecycleEvent implements UserTaskEvent {

  private String eventId;

  private String workflowModuleId;

  private String userTaskId;

  private String initiator;

  private OffsetDateTime timestamp;

  private String source;

  private String comment;

  public UserTaskLifecycleEvent() {
  }

  @Override
  public String getEventId() {
    return eventId;
  }

  @Override
  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getUserTaskId() {
    return userTaskId;
  }

  public void setUserTaskId(String userTaskId) {
    this.userTaskId = userTaskId;
  }

  public String getInitiator() {
    return initiator;
  }

  public void setInitiator(String initiator) {
    this.initiator = initiator;
  }

  @Override
  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  @Override
  public void setTimestamp(OffsetDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  @Override
  public String getComment() {
    return comment;
  }

  @Override
  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getWorkflowModuleId() {
    return workflowModuleId;
  }

  public void setWorkflowModuleId(String workflowModuleId) {
    this.workflowModuleId = workflowModuleId;
  }
}

