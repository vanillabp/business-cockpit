package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import io.vanillabp.cockpit.adapter.camunda8.deployments.DeployedBpmn;
import io.vanillabp.cockpit.adapter.camunda8.deployments.DeployedProcess;
import io.vanillabp.cockpit.adapter.camunda8.deployments.Deployment;
import io.vanillabp.cockpit.adapter.camunda8.deployments.DeploymentPersistence;
import io.vanillabp.cockpit.adapter.camunda8.deployments.DeploymentResource;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class JpaDeploymentPersistence implements DeploymentPersistence {

    private final DeploymentResourceRepository deploymentResourceRepository;

    private final DeploymentRepository deploymentRepository;

    private final DeployedBpmnRepository deployedBpmnRepository;

    public JpaDeploymentPersistence(
            final DeploymentResourceRepository deploymentResourceRepository,
            final DeploymentRepository deploymentRepository,
            final DeployedBpmnRepository deployedBpmnRepository) {

        this.deploymentResourceRepository = deploymentResourceRepository;
        this.deploymentRepository = deploymentRepository;
        this.deployedBpmnRepository = deployedBpmnRepository;

    }

    @Override
    public Optional<? extends Deployment> findDeployedProcess(
            final long definitionKey) {

        return deploymentRepository.findByDefinitionKey(definitionKey);

    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends DeployedProcess> R addDeployedProcess(
            final long definitionKey,
            final int version,
            final int packageId,
            final String bpmnProcessId,
            final DeployedBpmn bpmn,
            final OffsetDateTime publishedAt) {

        final var deployedProcess = new io.vanillabp.cockpit.adapter.camunda8.deployments.jpa.DeployedProcess();

        final var bpmnEntity = bpmn instanceof io.vanillabp.cockpit.adapter.camunda8.deployments.jpa.DeployedBpmn
                ? (io.vanillabp.cockpit.adapter.camunda8.deployments.jpa.DeployedBpmn) bpmn
                : deploymentResourceRepository.findById(bpmn.getFileId()).orElse(null);

        deployedProcess.setDefinitionKey(definitionKey);
        deployedProcess.setVersion(version);
        deployedProcess.setPackageId(packageId);
        deployedProcess.setBpmnProcessId(bpmnProcessId);
        deployedProcess.setDeployedResource(bpmnEntity);
        deployedProcess.setPublishedAt(OffsetDateTime.now());

        return (R) deploymentRepository.save(deployedProcess);

    }

    @Override
    public Optional<? extends DeploymentResource> findDeploymentResource(
            final int fileId) {

        return deploymentResourceRepository.findById(fileId);

    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends DeployedBpmn> R addDeployedBpmn(
            final int fileId,
            final String resourceName,
            final byte[] resource) {

        final var bpmn = new io.vanillabp.cockpit.adapter.camunda8.deployments.jpa.DeployedBpmn();

        bpmn.setFileId(fileId);
        bpmn.setResource(resource);
        bpmn.setResourceName(resourceName);

        return (R) deploymentResourceRepository.save(bpmn);

    }

    @Override
    public List<? extends DeployedBpmn> getBpmnNotOfPackage(
            final int packageId) {

        return deployedBpmnRepository
                .findByDeployments_packageIdNot(packageId)
                .stream()
                .distinct() // Oracle doesn't support distinct queries including blob columns, hence the job is done here
                .toList();
    }

}
