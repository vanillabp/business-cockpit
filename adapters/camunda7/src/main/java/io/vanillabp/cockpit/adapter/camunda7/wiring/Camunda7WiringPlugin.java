package io.vanillabp.cockpit.adapter.camunda7.wiring;

import java.util.LinkedList;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.persistence.deploy.Deployer;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;

public class Camunda7WiringPlugin extends AbstractProcessEnginePlugin {

    private final WiringBpmnParseListener wiringBpmnParseListener;
    
    public Camunda7WiringPlugin(
            final WiringBpmnParseListener wiringBpmnParseListener) {

        this.wiringBpmnParseListener = wiringBpmnParseListener;
        
    }

    @Override
    public void preInit(
            final ProcessEngineConfigurationImpl configuration) {
        
        if (configuration.getCustomPreBPMNParseListeners() == null) {
            configuration.setCustomPreBPMNParseListeners(new LinkedList<>());
        }
        configuration
                .getCustomPreBPMNParseListeners()
                .add(wiringBpmnParseListener);

        if (configuration.getCustomPreDeployers() == null) {
            configuration.setCustomPreDeployers(new LinkedList<>());
        }
        configuration.getCustomPreDeployers().add(new Deployer() {
                @Override
                public void deploy(
                        final DeploymentEntity deployment) {
                    WiringBpmnParseListener.workflowModuleId.set(deployment.getName());
                }
            });
        
    }
    
}
