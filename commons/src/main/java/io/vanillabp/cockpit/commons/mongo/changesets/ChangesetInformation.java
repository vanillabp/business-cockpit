package io.vanillabp.cockpit.commons.mongo.changesets;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * This entity is used to persist changeset information.
 */
@Document(ChangesetInformation.COLLECTION_NAME)
public class ChangesetInformation {

    public static final String COLLECTION_NAME = "Changesets";
    
    @Id
    private String id;
    
    @Version
    private long version;
    
    private String author;
    
    private OffsetDateTime timestamp;
    
    private int order;
    
    private List<String> rollbackScripts;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public List<String> getRollbackScripts() {
        return rollbackScripts;
    }
    
    public void setRollbackScripts(List<String> rollbackScripts) {
        this.rollbackScripts = rollbackScripts;
    }
    
    public long getVersion() {
        return version;
    }
    
    public void setVersion(long version) {
        this.version = version;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ChangesetInformation)) {
            return false;
        }
        return ((ChangesetInformation) obj).getId().equals(getId());
    }
    
    @Override
    public int hashCode() {
        return getId().hashCode();
    }
    
}
