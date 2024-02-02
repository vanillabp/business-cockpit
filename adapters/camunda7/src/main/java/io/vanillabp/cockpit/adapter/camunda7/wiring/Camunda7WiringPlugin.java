package io.vanillabp.cockpit.adapter.camunda7.wiring;

import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.persistence.deploy.Deployer;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;

import java.util.LinkedList;

public class Camunda7WiringPlugin extends AbstractProcessEnginePlugin {

    private final CockpitProperties cockpitProperties;

    private final WiringBpmnParseListener wiringBpmnParseListener;
    
    private final Camunda7HistoryEventProducerSupplier historyEventProducerSupplier;
    
    public Camunda7WiringPlugin(
            final CockpitProperties cockpitProperties,
            final WiringBpmnParseListener wiringBpmnParseListener,
            final Camunda7HistoryEventProducerSupplier historyEventProducerSupplier) {

        this.cockpitProperties = cockpitProperties;
        this.wiringBpmnParseListener = wiringBpmnParseListener;
        this.historyEventProducerSupplier = historyEventProducerSupplier;
        
    }

    @Override
    public void preInit(
            final ProcessEngineConfigurationImpl configuration) {

        if (cockpitProperties.isBefore7203()) {
            // before 7.20.3-ee builtin listeners were registered in the wrong order
            if (configuration.getCustomPreBPMNParseListeners() == null) {
                configuration.setCustomPreBPMNParseListeners(new LinkedList<>());
            }
            configuration
                    .getCustomPreBPMNParseListeners()
                    .add(wiringBpmnParseListener);
        } else {
            // As we need cockpit-task-listeners to be executed after vanilla-bp-wiring-task-listeners this listener
            // is a POST parse listener and vanilla-bp-wiring-task-listeners is a pre parse listener:
            if (configuration.getCustomPostBPMNParseListeners() == null) {
                configuration.setCustomPostBPMNParseListeners(new LinkedList<>());
            }
            configuration
                    .getCustomPostBPMNParseListeners()
                    .add(wiringBpmnParseListener);
        }

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
