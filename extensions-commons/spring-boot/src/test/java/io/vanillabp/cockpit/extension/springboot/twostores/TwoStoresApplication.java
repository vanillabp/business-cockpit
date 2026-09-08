package io.vanillabp.cockpit.extension.springboot.twostores;

import java.util.List;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.vanillabp.bpmsdouble.DummyTaskAwarenessSource;
import io.vanillabp.cockpit.extension.springboot.stores.RecordingOutbox;
import io.vanillabp.cockpit.extension.springboot.test.RecordingBpmsBridge;
import io.vanillabp.integration.adapter.spi.WorkflowAwareness;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.integration.spi.PhaseTwoOutboxAware;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;

/**
 * An application whose two workflow aggregates live in two outbox stores, each named by the
 * application itself.
 * <p>
 * Decision 12 refused such an application: an event a BPMS observed names identifiers and no
 * class, so there seemed to be nothing to attribute it by. There is - the workflow service
 * serving the BPMN process says which aggregate it is written for - and this application is
 * what shows that each report reaches the store of its own workflow.
 */
@SpringBootApplication
public class TwoStoresApplication {

  @Bean
  public DummyTaskAwarenessSource workflowAwareness() {

    return (
        adapterId,
        workflowAggregateId,
        taskId) -> WorkflowAwareness.ACTIVE;

  }

  @Bean
  public RecordingBpmsBridge recordingBpmsBridge() {

    return new RecordingBpmsBridge();

  }

  @Bean
  public RecordingOutbox relationalStore() {

    return new RecordingOutbox();

  }

  @Bean
  public RecordingOutbox documentStore() {

    return new RecordingOutbox();

  }

  @Bean
  public PhaseTwoOutboxAware<RelationalAggregate> relationalStoreOfItsAggregate(
      final RecordingOutbox relationalStore) {

    return storeOf(RelationalAggregate.class, relationalStore);

  }

  @Bean
  public PhaseTwoOutboxAware<DocumentAggregate> documentStoreOfItsAggregate(
      final RecordingOutbox documentStore) {

    return storeOf(DocumentAggregate.class, documentStore);

  }

  @Bean
  public WorkflowModuleDetailsProvider workflowModuleDetailsProvider() {

    return new WorkflowModuleDetailsProvider() {

      @Override
      public List<String> getAccessibleToGroups() {

        return List.of("clerks");

      }

      @Override
      public String getWorkflowModuleId() {

        return "test-module";

      }

    };

  }

  private static <A> PhaseTwoOutboxAware<A> storeOf(
      final Class<A> workflowAggregateClass,
      final PhaseTwoOutbox store) {

    return new PhaseTwoOutboxAware<A>() {

      @Override
      public Class<A> getAggregateClass() {

        return workflowAggregateClass;

      }

      @Override
      public PhaseTwoOutbox getPhaseTwoOutbox() {

        return store;

      }

    };

  }

}
