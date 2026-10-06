package io.vanillabp.cockpit.gui.api.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.context.ApplicationEvent;

/**
 * A wake-up call for the lists in the browser. It says which kind of entity changed, its id and
 * what happened to it, and nothing about who it concerns. Who it concerns is decided per update
 * stream, by the same visibility the lists use, see {@link UpdateStreams}.
 */
public class GuiEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final String entityId;

    private final Object event;

    /**
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

    }

    public Object getEvent() {
        return event;
    }

    /**
     * The id the stream decides on. It is not written to the browser, which reads the id from
     * {@link #getEvent()}.
     */
    @JsonIgnore
    public String getEntityId() {
        return entityId;
    }

    @JsonIgnore
    public String getKindOfEntity() {
        return (String) getSource();
    }

}
