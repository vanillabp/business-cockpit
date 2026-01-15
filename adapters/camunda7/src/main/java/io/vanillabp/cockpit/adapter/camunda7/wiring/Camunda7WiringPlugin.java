package io.vanillabp.cockpit.adapter.camunda7.wiring;

import java.util.LinkedList;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.persistence.deploy.Deployer;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.impl.util.ParseUtil;

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

        final var engineInfo = ParseUtil.parseProcessEngineVersion(true);
        final var versionParts = engineInfo.getVersion().split("\\.");
        final var majorVersion = Integer.parseInt(versionParts[0]);
        final var minorVersion = Integer.parseInt(versionParts[1]);
        final var patchVersion = Integer.parseInt(versionParts[2]);

        final var before_7_20_3 = (majorVersion <= 7)
                && (minorVersion <= 20)
                && (patchVersion < 3);
        if (before_7_20_3) {
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
