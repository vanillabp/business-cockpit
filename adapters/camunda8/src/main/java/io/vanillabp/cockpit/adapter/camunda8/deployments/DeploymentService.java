package io.vanillabp.cockpit.adapter.camunda8.deployments;

import io.camunda.zeebe.client.api.response.Process;
import io.camunda.zeebe.model.bpmn.Bpmn;
import io.camunda.zeebe.model.bpmn.BpmnModelInstance;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeploymentService {
    private final DeploymentRepository deploymentRepository;
    
    private final DeploymentResourceRepository deploymentResourceRepository;

    private final Map<Long, io.camunda.zeebe.model.bpmn.instance.Process> cachedProcesses = new HashMap<>();

    public DeploymentService(
            final DeploymentRepository deploymentRepository,
            final DeploymentResourceRepository deploymentResourceRepository) {

        this.deploymentRepository = deploymentRepository;
        this.deploymentResourceRepository = deploymentResourceRepository;
        
    }
    
    public DeployedBpmn addBpmn(
            final BpmnModelInstance model,
            final int fileId,
            final String resourceName) {
        
        final var previous = deploymentResourceRepository.findById(fileId);
        if (previous.isPresent()) {
            return (DeployedBpmn) previous.get();
        }

        final var outStream = new ByteArrayOutputStream();
        Bpmn.writeModelToStream(outStream, model);
        
        final var bpmn = new DeployedBpmn();
        bpmn.setFileId(fileId);
        bpmn.setResource(outStream.toByteArray());
        bpmn.setResourceName(resourceName);

        return deploymentResourceRepository.save(bpmn);

    }
    
    public DeployedProcess addProcess(
            final int packageId,
            final Process camunda8DeployedProcess,
            final DeployedBpmn bpmn) {
        
        final var versionedId = camunda8DeployedProcess.getProcessDefinitionKey();
        final var previous = deploymentRepository.findByDefinitionKey(versionedId);
        if (previous.isPresent()) {
            return (DeployedProcess) previous.get();
        }

        final var deployedProcess = new DeployedProcess();
        
        deployedProcess.setDefinitionKey(versionedId);
        deployedProcess.setVersion(camunda8DeployedProcess.getVersion());
        deployedProcess.setPackageId(packageId);
        deployedProcess.setBpmnProcessId(camunda8DeployedProcess.getBpmnProcessId());
        deployedProcess.setDeployedResource(bpmn);
        deployedProcess.setPublishedAt(OffsetDateTime.now());

        return deploymentRepository.save(deployedProcess);
    }

    public List<DeployedBpmn> getBpmnNotOfPackage(final int packageId) {

        return deploymentResourceRepository
                .findByTypeAndDeployments_packageIdNot(DeployedBpmn.TYPE, packageId)
                .stream()
                .distinct() // Oracle doesn't support distinct queries including blob columns, hence the job is done here
                .toList();

    }

}
