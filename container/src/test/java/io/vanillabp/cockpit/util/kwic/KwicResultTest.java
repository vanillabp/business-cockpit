package io.vanillabp.cockpit.util.kwic;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KwicResult} record.
 */
class KwicResultTest {

    @Test
    void constructor_withValidParameters_createsKwicResult() {
        // Act
        KwicResult result = new KwicResult("testItem", 5);

        // Assert
        assertThat(result.item()).isEqualTo("testItem");
        assertThat(result.count()).isEqualTo(5);
    }

    @Test
    void constructor_withZeroCount_createsKwicResult() {
        // Act
        KwicResult result = new KwicResult("item", 0);

        // Assert
        assertThat(result.count()).isZero();
    }

    @Test
    void constructor_withNullItem_acceptsNull() {
        // Act
        KwicResult result = new KwicResult(null, 1);

        // Assert
        assertThat(result.item()).isNull();
    }

    @Test
    void equals_withSameValues_returnsTrue() {
        // Arrange
        KwicResult result1 = new KwicResult("item", 5);
        KwicResult result2 = new KwicResult("item", 5);

        // Assert
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void equals_withDifferentItem_returnsFalse() {
        // Arrange
        KwicResult result1 = new KwicResult("item1", 5);
        KwicResult result2 = new KwicResult("item2", 5);

        // Assert
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void equals_withDifferentCount_returnsFalse() {
        // Arrange
        KwicResult result1 = new KwicResult("item", 5);
        KwicResult result2 = new KwicResult("item", 10);

        // Assert
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void hashCode_withSameValues_returnsSameHashCode() {
        // Arrange
        KwicResult result1 = new KwicResult("item", 5);
        KwicResult result2 = new KwicResult("item", 5);

        // Assert
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void toString_containsAllFields() {
        // Arrange
        KwicResult result = new KwicResult("testItem", 5);

        // Act
        String stringRepresentation = result.toString();

        // Assert
        assertThat(stringRepresentation).contains("testItem");
        assertThat(stringRepresentation).contains("5");
    }
}
