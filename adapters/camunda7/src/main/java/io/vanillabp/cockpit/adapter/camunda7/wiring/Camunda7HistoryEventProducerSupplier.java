package io.vanillabp.cockpit.adapter.camunda7.wiring;

import org.camunda.bpm.engine.impl.history.producer.HistoryEventProducer;

public class Camunda7HistoryEventProducerSupplier {

    private HistoryEventProducer historyEventProducer;

    public HistoryEventProducer getHistoryEventProducer() {
        return historyEventProducer;
    }
    
    public void setHistoryEventProducer(HistoryEventProducer historyEventProducer) {
        this.historyEventProducer = historyEventProducer;
    }
    
}
