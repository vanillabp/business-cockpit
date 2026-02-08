package io.vanillabp.cockpit.adapter.camunda8.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DisableCamundaSpringAutoConfigurationImportFilterTest {

    private DisableCamundaSpringAutoConfigurationImportFilter filter;

    @BeforeEach
    void setUp() {
        filter = new DisableCamundaSpringAutoConfigurationImportFilter();
    }

    @Test
    void match_withCamundaAutoConfiguration_returnsFalse() {
        // Test with Camunda auto configuration class that should be filtered
        final var classNames = new String[] {
                "io.camunda.spring.client.configuration.CamundaAutoConfiguration"
        };

        // Filter should return false for this class
        final var result = filter.match(classNames, null);

        assertThat(result).containsExactly(false);
    }

    @Test
    void match_withOtherConfiguration_returnsTrue() {
        // Test with a regular configuration class
        final var classNames = new String[] {
                "org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration"
        };

        // Filter should return true for this class
        final var result = filter.match(classNames, null);

        assertThat(result).containsExactly(true);
    }

    @Test
    void match_withNullClassName_returnsTrue() {
        // Test with null class name
        final var classNames = new String[] { null };

        // Filter should return true for null
        final var result = filter.match(classNames, null);

        assertThat(result).containsExactly(true);
    }

    @Test
    void match_withMultipleClassNames_filtersCorrectly() {
        // Test with multiple class names including filtered one
        final var classNames = new String[] {
                "org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration",
                "io.camunda.spring.client.configuration.CamundaAutoConfiguration",
                null,
                "org.springframework.boot.autoconfigure.data.MongoAutoConfiguration"
        };

        // Filter should only return false for Camunda auto configuration
        final var result = filter.match(classNames, null);

        assertThat(result).containsExactly(true, false, true, true);
    }

    @Test
    void match_withEmptyArray_returnsEmptyResult() {
        // Test with empty class names array
        final var classNames = new String[] {};

        // Filter should return empty array
        final var result = filter.match(classNames, null);

        assertThat(result).isEmpty();
    }

}
