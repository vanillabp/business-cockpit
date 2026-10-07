package io.vanillabp.cockpit.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.vanillabp.cockpit.commons.exceptions.BcInvalidRequestException;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * The paths of the filters of a list request follow the rule of a sort path. Without it a client
 * could filter on a field the GUI API never shows and guess its values from what the list answers.
 * {@code SortIndexesTest} holds the rule itself, path by path.
 */
@ExtendWith(SuppressOutputExtension.class)
class ListPathsTest {

    private final ListPaths listPaths = new ListPaths(
            Set.of("title", ListPaths.FULLTEXT), Optional.empty());

    private static List<SearchQuery> filtersOn(
            final String... paths) {

        return Arrays
                .stream(paths)
                .map(path -> new SearchQuery(path, "Meier", true))
                .toList();

    }

    @Test
    void aFilterWithoutAPathIsTheFullTextSearchAndAllowed() {

        assertThatCode(() -> listPaths.checkFilters(filtersOn(null, "")))
                .doesNotThrowAnyException();

    }

    @Test
    void noFiltersAtAllAreAllowed() {

        assertThatCode(() -> listPaths.checkFilters(null)).doesNotThrowAnyException();

    }

    @Test
    void filtersOnFieldsOfTheListAndOnBusinessDataAreAllowed() {

        assertThatCode(() -> listPaths.checkFilters(
                filtersOn("title.de", ListPaths.FULLTEXT, "details.customer.name", "details.order-id")))
                .doesNotThrowAnyException();

    }

    @Test
    void aFilterOnAFieldTheApiDoesNotShowIsRefusedAndTheParameterIsNamed() {

        assertThatThrownBy(() -> listPaths.checkFilters(filtersOn("title", "excludedCandidateUsers.id")))
                .isInstanceOf(BcInvalidRequestException.class)
                .hasMessage("'searchQueries.path' may only name fields of the list or keys below 'details.'. "
                        + "A part of a path holds letters, digits and '_', and below 'details.' also '-'");

    }

    @Test
    void aSuggestionPathIsCheckedUnderItsOwnName() {

        assertThatThrownBy(() -> listPaths.check("path", "details.$where"))
                .isInstanceOf(BcInvalidRequestException.class)
                .hasMessageStartingWith("'path' may only name");

    }

    @Test
    void noPathAtAllIsRefused() {

        assertThatThrownBy(() -> listPaths.check("path", null))
                .isInstanceOf(BcInvalidRequestException.class);

    }

}
