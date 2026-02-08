package io.vanillabp.cockpit.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SearchQuery} record.
 */
class SearchQueryTest {

    @Test
    void constructor_withValidParameters_createsSearchQuery() {
        // Act
        SearchQuery query = new SearchQuery("title", "test", true);

        // Assert
        assertThat(query.path()).isEqualTo("title");
        assertThat(query.query()).isEqualTo("test");
        assertThat(query.caseInsensitive()).isTrue();
    }

    @Test
    void constructor_withCaseSensitive_createsCaseSensitiveQuery() {
        // Act
        SearchQuery query = new SearchQuery("description", "test", false);

        // Assert
        assertThat(query.caseInsensitive()).isFalse();
    }

    @Test
    void constructor_withNullPath_acceptsNull() {
        // Act
        SearchQuery query = new SearchQuery(null, "test", true);

        // Assert
        assertThat(query.path()).isNull();
    }

    @Test
    void constructor_withNullQuery_acceptsNull() {
        // Act
        SearchQuery query = new SearchQuery("title", null, true);

        // Assert
        assertThat(query.query()).isNull();
    }

    @Test
    void equals_withSameValues_returnsTrue() {
        // Arrange
        SearchQuery query1 = new SearchQuery("title", "test", true);
        SearchQuery query2 = new SearchQuery("title", "test", true);

        // Assert
        assertThat(query1).isEqualTo(query2);
    }

    @Test
    void equals_withDifferentPath_returnsFalse() {
        // Arrange
        SearchQuery query1 = new SearchQuery("title", "test", true);
        SearchQuery query2 = new SearchQuery("description", "test", true);

        // Assert
        assertThat(query1).isNotEqualTo(query2);
    }

    @Test
    void equals_withDifferentQuery_returnsFalse() {
        // Arrange
        SearchQuery query1 = new SearchQuery("title", "test1", true);
        SearchQuery query2 = new SearchQuery("title", "test2", true);

        // Assert
        assertThat(query1).isNotEqualTo(query2);
    }

    @Test
    void equals_withDifferentCaseInsensitive_returnsFalse() {
        // Arrange
        SearchQuery query1 = new SearchQuery("title", "test", true);
        SearchQuery query2 = new SearchQuery("title", "test", false);

        // Assert
        assertThat(query1).isNotEqualTo(query2);
    }

    @Test
    void hashCode_withSameValues_returnsSameHashCode() {
        // Arrange
        SearchQuery query1 = new SearchQuery("title", "test", true);
        SearchQuery query2 = new SearchQuery("title", "test", true);

        // Assert
        assertThat(query1.hashCode()).isEqualTo(query2.hashCode());
    }

    @Test
    void toString_containsAllFields() {
        // Arrange
        SearchQuery query = new SearchQuery("title", "test", true);

        // Act
        String result = query.toString();

        // Assert
        assertThat(result).contains("title");
        assertThat(result).contains("test");
        assertThat(result).contains("true");
    }
}
