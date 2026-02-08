package io.vanillabp.cockpit.tasklist.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UiUriType} enum.
 */
class UiUriTypeTest {

    @Test
    void values_containsExpectedTypes() {
        // Act
        UiUriType[] values = UiUriType.values();

        // Assert
        assertThat(values).hasSize(2);
        assertThat(values).containsExactlyInAnyOrder(UiUriType.EXTERNAL, UiUriType.WEBPACK_MF_REACT);
    }

    @Test
    void valueOf_external_returnsCorrectEnum() {
        // Act
        UiUriType type = UiUriType.valueOf("EXTERNAL");

        // Assert
        assertThat(type).isEqualTo(UiUriType.EXTERNAL);
    }

    @Test
    void valueOf_webpackMfReact_returnsCorrectEnum() {
        // Act
        UiUriType type = UiUriType.valueOf("WEBPACK_MF_REACT");

        // Assert
        assertThat(type).isEqualTo(UiUriType.WEBPACK_MF_REACT);
    }

    @Test
    void name_external_returnsCorrectString() {
        // Assert
        assertThat(UiUriType.EXTERNAL.name()).isEqualTo("EXTERNAL");
    }

    @Test
    void name_webpackMfReact_returnsCorrectString() {
        // Assert
        assertThat(UiUriType.WEBPACK_MF_REACT.name()).isEqualTo("WEBPACK_MF_REACT");
    }
}
