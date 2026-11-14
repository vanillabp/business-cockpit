package io.vanillabp.cockpit.gui.api.v1;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

import org.springframework.context.ApplicationEvent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A wake-up call for the lists in the browser. It says which kind of entity changed, its id and
 * what happened to it, and nothing about who it concerns. Who it concerns is decided per update
 * stream, by the same visibility the lists use, see {@link UpdateStreams}.
 *
 * <p>It also names the workflow module of the entity. A list in the browser which shows only some
 * workflow modules skips the wake-up calls of the others. The stream itself cannot do that,
 * because a request of a list does not say which tab it comes from.
 */
public class GuiEvent extends ApplicationEvent {

  private static final long serialVersionUID = 1L;

  /** The id of the entity which changed, see {@link #getEntityId()}. */
  private final String entityId;

  /** What the browser gets to read, see {@link #getEvent()}. */
  private final Object event;

  /** The workflow module of the entity, see {@link #getWorkflowModuleId()}. */
  private final String workflowModuleId;

  /**
   * Creates the wake-up call for one change.
   *
   * @param kindOfEntity The kind of entity which changed. It is also the name of the event in the
   *        stream, so a list in the browser listens to the kind it shows
   * @param entityId The id of the entity which changed, or {@code null} for an event which is
   *        about no single entity and therefore reaches every stream
   * @param event What the browser gets to read
   */
  public GuiEvent(
      final String kindOfEntity,
      final String entityId,
      final Object event) {

    super(kindOfEntity);
    this.entityId = entityId;
    this.event = event;
    this.workflowModuleId = null;

  }

  /**
   * A copy of another event with the workflow module added. The copy keeps the timestamp, so the
   * browser reads when the change was noticed and not when its module was looked up.
   */
  private GuiEvent(
      final GuiEvent original,
      final String workflowModuleId) {

    super(
        original.getSource(), Clock.fixed(Instant.ofEpochMilli(original.getTimestamp()), ZoneOffset.UTC));
    this.entityId = original.entityId;
    this.event = original.event;
    this.workflowModuleId = workflowModuleId;

  }

  /**
   * The same event, naming the workflow module of its entity. The update streams add it once
   * they have read the changed entity, because the change itself does not always carry it: MongoDB
   * reports an update without the changed document.
   *
   * @param workflowModuleId The workflow module of the entity, or {@code null} if it is unknown
   * @return A new event, or this one if the workflow module is the same
   */
  public GuiEvent withWorkflowModuleId(
      final String workflowModuleId) {

    if (Objects.equals(this.workflowModuleId, workflowModuleId)) {
      return this;
    }
    return new GuiEvent(this, workflowModuleId);

  }

  /**
   * What the browser gets to read. It is written to the stream as it is.
   *
   * @return The payload for the browser
   */
  public Object getEvent() {
    return event;
  }

  /**
   * The id the stream decides on. It is not written to the browser, which reads the id from
   * {@link #getEvent()}.
   *
   * @return The id, or {@code null} for an event which is about no single entity
   */
  @JsonIgnore
  public String getEntityId() {
    return entityId;
  }

  /**
   * The kind of entity which changed. It is the name of the event in the stream as well.
   *
   * @return The kind of entity
   */
  @JsonIgnore
  public String getKindOfEntity() {
    return (String) getSource();
  }

  /**
   * The workflow module of the entity which changed. It is written to the browser, so a list
   * limited to some workflow modules can skip a change of another one. It is left out when it is
   * unknown: for a reload, and for an entity the cockpit no longer holds.
   *
   * @return The id of the workflow module, or {@code null} if it is unknown
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public String getWorkflowModuleId() {
    return workflowModuleId;
  }

}
