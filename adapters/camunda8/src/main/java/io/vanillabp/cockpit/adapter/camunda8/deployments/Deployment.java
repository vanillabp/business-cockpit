package io.vanillabp.cockpit.adapter.camunda8.deployments;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "CAMUNDA8_DEPLOYMENTS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE")
public abstract class Deployment {

    /** the key of the deployed process */
    @Id
    @Column(name = "DEFINITION_KEY")
    private long definitionKey;

    /** the version of the deployed process */
    @Version
    @Column(name = "VERSION")
    private int version;

    @Column(name = "PACKAGE_ID")
    private int packageId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "RESOURCE", nullable = false, updatable = false)
    private DeploymentResource deployedResource;

    @Column(name = "PUBLISHED_AT", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime publishedAt;
    
    @Column(name = "TYPE", updatable = false, insertable = false)
    private String type;

    public long getDefinitionKey() {
        return definitionKey;
    }

    public void setDefinitionKey(long definitionKey) {
        this.definitionKey = definitionKey;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getPackageId() {
        return packageId;
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public DeploymentResource getDeployedResource() {
        return deployedResource;
    }

    public void setDeployedResource(DeploymentResource deployedResource) {
        this.deployedResource = deployedResource;
    }

    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
}
