package io.vanillabp.cockpit.adapter.camunda8.deployments;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity(name = "BusinessCockpitDeploymentResource")
@Table(name = "CAMUNDA8_BC_RESOURCES")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE")
public abstract class DeploymentResource {

    @Id
    @Column(name = "ID")
    private int fileId;

    @Version
    @Column(name = "RECORD_VERSION")
    private int recordVersion;

    @Column(name = "RESOURCE_NAME")
    private String resourceName;

    @OneToMany(mappedBy = "deployedResource", fetch = FetchType.LAZY)
    private List<Deployment> deployments;

    @Lob
    @Column(name = "RESOURCE")
    private byte[] resource;
    
    @Column(name = "TYPE", updatable = false, insertable = false)
    private String type;

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public int getRecordVersion() {
        return recordVersion;
    }

    public void setRecordVersion(int recordVersion) {
        this.recordVersion = recordVersion;
    }

    public byte[] getResource() {
        return resource;
    }

    public void setResource(byte[] resource) {
        this.resource = resource;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public List<Deployment> getDeployments() {
        return deployments;
    }

    public void setDeployments(List<Deployment> deployments) {
        this.deployments = deployments;
    }

    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
}
