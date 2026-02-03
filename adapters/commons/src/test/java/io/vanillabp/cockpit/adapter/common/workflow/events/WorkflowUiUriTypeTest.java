package io.vanillabp.cockpit.adapter.common.workflow.events;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowUiUriTypeTest {

    @Test
    void getValue_EXTERNAL_returnsCorrectValue() {
        assertThat(WorkflowUiUriType.EXTERNAL.getValue()).isEqualTo("EXTERNAL");
    }

    @Test
    void getValue_WEBPACK_MF_REACT_returnsCorrectValue() {
        assertThat(WorkflowUiUriType.WEBPACK_MF_REACT.getValue()).isEqualTo("WEBPACK_MF_REACT");
    }

    @Test
    void toString_EXTERNAL_returnsStringValue() {
        assertThat(WorkflowUiUriType.EXTERNAL.toString()).isEqualTo("EXTERNAL");
    }

    @Test
    void toString_WEBPACK_MF_REACT_returnsStringValue() {
        assertThat(WorkflowUiUriType.WEBPACK_MF_REACT.toString()).isEqualTo("WEBPACK_MF_REACT");
    }

    @Test
    void fromValue_EXTERNAL_returnsCorrectEnum() {
        assertThat(WorkflowUiUriType.fromValue("EXTERNAL")).isEqualTo(WorkflowUiUriType.EXTERNAL);
    }

    @Test
    void fromValue_WEBPACK_MF_REACT_returnsCorrectEnum() {
        assertThat(WorkflowUiUriType.fromValue("WEBPACK_MF_REACT")).isEqualTo(WorkflowUiUriType.WEBPACK_MF_REACT);
    }

    @Test
    void fromValue_invalidValue_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> WorkflowUiUriType.fromValue("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unexpected value 'INVALID'");
    }

    @Test
    void values_containsAllEnumValues() {
        assertThat(WorkflowUiUriType.values())
                .containsExactly(WorkflowUiUriType.EXTERNAL, WorkflowUiUriType.WEBPACK_MF_REACT);
    }
}
