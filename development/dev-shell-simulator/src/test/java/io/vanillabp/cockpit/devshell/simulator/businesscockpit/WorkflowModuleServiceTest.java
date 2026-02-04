package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModuleRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowModuleServiceTest {

    @Mock
    private WorkflowModuleRepository workflowModules;

    @InjectMocks
    private WorkflowModuleService workflowModuleService;

    private WorkflowModule module;

    @BeforeEach
    void setUp() {
        // Test-Modul mit grundlegenden Eigenschaften
        module = new WorkflowModule();
        module.setId("module-1");
        module.setUri("http://localhost:8081");
    }

    // --- registerWorkflowModule ---

    @Test
    void registerWorkflowModule_withValidInput_savesModule() {
        // Modul registrieren
        workflowModuleService.registerWorkflowModule("module-1", module);

        // Modul muss im Repository gespeichert werden
        verify(workflowModules).save(module);
    }

    @Test
    void registerWorkflowModule_withNullId_throwsIllegalArgumentException() {
        // Null-ID muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> workflowModuleService.registerWorkflowModule(null, module))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void registerWorkflowModule_withNullModule_throwsIllegalArgumentException() {
        // Null-Modul muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> workflowModuleService.registerWorkflowModule("module-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    // --- getWorkflowModule ---

    @Test
    void getWorkflowModule_withExistingId_returnsModule() {
        // Repository gibt das Modul zurueck
        when(workflowModules.findById("module-1")).thenReturn(Optional.of(module));

        // Modul abrufen und pruefen
        final var result = workflowModuleService.getWorkflowModule("module-1");

        assertThat(result).isSameAs(module);
    }

    @Test
    void getWorkflowModule_withNullId_throwsIllegalArgumentException() {
        // Null-ID muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> workflowModuleService.getWorkflowModule(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void getWorkflowModule_withNonExistingId_throwsIllegalStateException() {
        // Repository findet kein Modul
        when(workflowModules.findById("unknown")).thenReturn(Optional.empty());

        // Erwarte IllegalStateException
        assertThatThrownBy(() -> workflowModuleService.getWorkflowModule("unknown"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

}
