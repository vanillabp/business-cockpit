package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessInstanceEntityTest {

    @Test
    void defaultConstructor_createsEmptyInstance() {
        // Default constructor for JPA
        final var entity = new ProcessInstanceEntity();
        assertThat(entity).isNotNull();
    }

    @Test
    void constructor_withKeyAndBusinessKey_setsValues() {
        // Constructor with process instance key and business key
        final var entity = new ProcessInstanceEntity(12345L, "BIZ-001");

        assertThat(entity.getProcessInstanceKey()).isEqualTo(12345L);
        assertThat(entity.getBusinessKey()).isEqualTo("BIZ-001");
    }

    @Test
    void setAndGetProcessInstanceKey_storesAndReturnsValue() {
        final var entity = new ProcessInstanceEntity();
        entity.setProcessInstanceKey(12345L);
        assertThat(entity.getProcessInstanceKey()).isEqualTo(12345L);
    }

    @Test
    void setAndGetBusinessKey_storesAndReturnsValue() {
        final var entity = new ProcessInstanceEntity();
        entity.setBusinessKey("BIZ-001");
        assertThat(entity.getBusinessKey()).isEqualTo("BIZ-001");
    }

    @Test
    void setAndGetBpmnProcessId_storesAndReturnsValue() {
        final var entity = new ProcessInstanceEntity();
        entity.setBpmnProcessId("order-process");
        assertThat(entity.getBpmnProcessId()).isEqualTo("order-process");
    }

    @Test
    void setAndGetVersion_storesAndReturnsValue() {
        final var entity = new ProcessInstanceEntity();
        entity.setVersion(3L);
        assertThat(entity.getVersion()).isEqualTo(3L);
    }

    @Test
    void setAndGetProcessDefinitionKey_storesAndReturnsValue() {
        final var entity = new ProcessInstanceEntity();
        entity.setProcessDefinitionKey(2251799813685249L);
        assertThat(entity.getProcessDefinitionKey()).isEqualTo(2251799813685249L);
    }

    @Test
    void setAndGetTenantId_storesAndReturnsValue() {
        final var entity = new ProcessInstanceEntity();
        entity.setTenantId("tenant-1");
        assertThat(entity.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void allFieldsNull_whenNotSet() {
        final var entity = new ProcessInstanceEntity();
        assertThat(entity.getBusinessKey()).isNull();
        assertThat(entity.getBpmnProcessId()).isNull();
        assertThat(entity.getVersion()).isNull();
        assertThat(entity.getProcessDefinitionKey()).isNull();
        assertThat(entity.getTenantId()).isNull();
    }

}
