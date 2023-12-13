package io.vanillabp.cockpit.adapter.common.usertask.events;

import java.time.OffsetDateTime;

public class UserTaskLifecycleEvent implements UserTaskEvent {

  private String id;

  private String userTaskId;

  private String initiator;

  private OffsetDateTime timestamp;

  private String source;

  private String comment;

  private String apiVersion;

  public UserTaskLifecycleEvent() {
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
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

  @Override
  public String getApiVersion() {
    return apiVersion;
  }

  @Override
  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }
}

