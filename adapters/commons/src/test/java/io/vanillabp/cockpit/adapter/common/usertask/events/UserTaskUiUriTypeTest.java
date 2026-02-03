package io.vanillabp.cockpit.adapter.common.usertask.events;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTaskUiUriTypeTest {

    @Test
    void getValue_EXTERNAL_returnsCorrectValue() {
        assertThat(UserTaskUiUriType.EXTERNAL.getValue()).isEqualTo("EXTERNAL");
    }

    @Test
    void getValue_WEBPACK_MF_REACT_returnsCorrectValue() {
        assertThat(UserTaskUiUriType.WEBPACK_MF_REACT.getValue()).isEqualTo("WEBPACK_MF_REACT");
    }

    @Test
    void toString_EXTERNAL_returnsStringValue() {
        assertThat(UserTaskUiUriType.EXTERNAL.toString()).isEqualTo("EXTERNAL");
    }

    @Test
    void toString_WEBPACK_MF_REACT_returnsStringValue() {
        assertThat(UserTaskUiUriType.WEBPACK_MF_REACT.toString()).isEqualTo("WEBPACK_MF_REACT");
    }

    @Test
    void fromValue_EXTERNAL_returnsCorrectEnum() {
        assertThat(UserTaskUiUriType.fromValue("EXTERNAL")).isEqualTo(UserTaskUiUriType.EXTERNAL);
    }

    @Test
    void fromValue_WEBPACK_MF_REACT_returnsCorrectEnum() {
        assertThat(UserTaskUiUriType.fromValue("WEBPACK_MF_REACT")).isEqualTo(UserTaskUiUriType.WEBPACK_MF_REACT);
    }

    @Test
    void fromValue_invalidValue_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> UserTaskUiUriType.fromValue("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unexpected value 'INVALID'");
    }

    @Test
    void values_containsAllEnumValues() {
        assertThat(UserTaskUiUriType.values())
                .containsExactly(UserTaskUiUriType.EXTERNAL, UserTaskUiUriType.WEBPACK_MF_REACT);
    }
}
