package io.vanillabp.cockpit.adapter.camunda7.wiring;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.persistence.deploy.Deployer;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;

import java.util.LinkedList;

public class Camunda7WiringPlugin extends AbstractProcessEnginePlugin {

    private final WiringBpmnParseListener wiringBpmnParseListener;
    
    private final Camunda7HistoryEventProducerSupplier historyEventProducerSupplier;
    
    public Camunda7WiringPlugin(
            final WiringBpmnParseListener wiringBpmnParseListener,
            final Camunda7HistoryEventProducerSupplier historyEventProducerSupplier) {

        this.wiringBpmnParseListener = wiringBpmnParseListener;
        this.historyEventProducerSupplier = historyEventProducerSupplier;
        
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
    
    @Override
    public void postInit(
            final ProcessEngineConfigurationImpl processEngineConfiguration) {
        
        historyEventProducerSupplier.setHistoryEventProducer(
                processEngineConfiguration.getHistoryEventProducer());

    }
    
}
