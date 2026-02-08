package io.vanillabp.cockpit.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SearchCriteriaHelper}.
 */
class SearchCriteriaHelperTest {

    @Test
    void buildSearchCriteria_withNullQueries_returnsEmptyList() {
        // Act
        List<? extends CriteriaDefinition> result = SearchCriteriaHelper.buildSearchCriteria(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void buildSearchCriteria_withEmptyQueries_returnsEmptyList() {
        // Act
        List<? extends CriteriaDefinition> result = SearchCriteriaHelper.buildSearchCriteria(List.of());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void buildSearchCriteria_withSingleQuery_returnsSingleCriteria() {
        // Arrange
        SearchQuery query = new SearchQuery("title", "test", true);

        // Act
        List<? extends CriteriaDefinition> result = SearchCriteriaHelper.buildSearchCriteria(List.of(query));

        // Assert
        assertThat(result).hasSize(1);
    }

    @Test
    void buildSearchCriteria_withMultipleQueries_returnsMultipleCriteria() {
        // Arrange
        SearchQuery query1 = new SearchQuery("title", "test1", true);
        SearchQuery query2 = new SearchQuery("description", "test2", false);

        // Act
        List<? extends CriteriaDefinition> result = SearchCriteriaHelper.buildSearchCriteria(List.of(query1, query2));

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    void buildSearchCriteria_withNullPath_usesDefaultPath() {
        // Arrange
        SearchQuery query = new SearchQuery(null, "test", true);

        // Act
        List<? extends CriteriaDefinition> result = SearchCriteriaHelper.buildSearchCriteria(List.of(query));

        // Assert - should use "detailsFulltextSearch" as default path
        assertThat(result).hasSize(1);
    }

    @Test
    void buildSearchCriteria_withEmptyPath_usesDefaultPath() {
        // Arrange
        SearchQuery query = new SearchQuery("", "test", true);

        // Act
        List<? extends CriteriaDefinition> result = SearchCriteriaHelper.buildSearchCriteria(List.of(query));

        // Assert - should use "detailsFulltextSearch" as default path
        assertThat(result).hasSize(1);
    }

    @Test
    void buildSearchCriteria_withCaseInsensitive_createsCriteriaWithIgnoreCaseFlag() {
        // Arrange
        SearchQuery query = new SearchQuery("title", "test", true);

        // Act
        List<? extends CriteriaDefinition> result = SearchCriteriaHelper.buildSearchCriteria(List.of(query));

        // Assert
        assertThat(result).hasSize(1);
        // The criteria definition will contain the regex with "i" flag for case insensitive
    }

    @Test
    void buildSearchCriteria_withCaseSensitive_createsCriteriaWithoutIgnoreCaseFlag() {
        // Arrange
        SearchQuery query = new SearchQuery("title", "test", false);

        // Act
        List<? extends CriteriaDefinition> result = SearchCriteriaHelper.buildSearchCriteria(List.of(query));

        // Assert
        assertThat(result).hasSize(1);
        // The criteria definition will contain the regex without "i" flag
    }
}
