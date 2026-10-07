package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * What the two lists of the GUI API do with the path of a filter and with the path of the
 * suggestions for a search field. Both went into the database query unchecked, so a client could
 * filter on a field the GUI API never shows and guess its values. Now both follow the rule of a
 * sort path: a field of the list or a key below {@code details.}, see {@code SortPathsTest}.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class FilterPathsTest extends ItestBase {

    private static final String RULE = "may only name fields of the list or keys below 'details.'. "
            + "A part of a path holds letters, digits and '_', and below 'details.' also '-'.";

    private String cookie;

    @BeforeEach
    void login() {
        cookie = loginToGui(USER_MARTIN);
    }

    private HttpResponse<String> listFilteredOn(
            final String list,
            final String path) {

        return guiPost(cookie, "/" + list, """
                { "pageNumber": 0, "pageSize": 1, "searchQueries": [ { "path": "%s", "query": "a" } ] }
                """.formatted(path));

    }

    private HttpResponse<String> suggestionsFor(
            final String list,
            final String path) {

        return send(HttpRequest
                .newBuilder(URI.create(url("/gui/api/v1/" + list + "?query=abc&path="
                        + URLEncoder.encode(path, StandardCharsets.UTF_8))))
                .header("Cookie", cookie)
                .header("Content-Type", "application/json")
                .method("OPTIONS", HttpRequest.BodyPublishers.ofString("{ \"searchQueries\": [] }"))
                .build());

    }

    @Test
    void theTaskListIsFilteredOnItsFieldsAndOnBusinessData() {

        assertThat(listFilteredOn("usertask", "title.en").statusCode()).isEqualTo(200);
        assertThat(listFilteredOn("usertask", "details.order-id").statusCode()).isEqualTo(200);
        assertThat(suggestionsFor("usertask", "details.customer.name").statusCode()).isEqualTo(200);

    }

    @Test
    void theCaseListIsFilteredOnItsFieldsAndOnBusinessData() {

        assertThat(listFilteredOn("workflow", "businessId").statusCode()).isEqualTo(200);
        assertThat(listFilteredOn("workflow", "details.order-id").statusCode()).isEqualTo(200);
        assertThat(suggestionsFor("workflow", "title.en").statusCode()).isEqualTo(200);

    }

    @Test
    void aFilterOnAFieldTheTaskListDoesNotShowIsRefused() {

        final var answer = listFilteredOn("usertask", "excludedCandidateUsers.id");

        assertThat(answer.statusCode()).isEqualTo(400);
        assertThat(answer.body()).isEqualTo("The request is not valid: 'searchQueries.path' " + RULE);

    }

    @Test
    void aFilterOnAFieldTheCaseListDoesNotShowIsRefused() {

        assertThat(listFilteredOn("workflow", "knownFromItsEndAlone").statusCode()).isEqualTo(400);
        assertThat(listFilteredOn("workflow", "dueDate").statusCode()).isEqualTo(400);

    }

    @Test
    void aFilterPathWithSpecialCharactersIsRefusedByBothLists() {

        assertThat(listFilteredOn("usertask", "details.$where").statusCode()).isEqualTo(400);
        assertThat(listFilteredOn("workflow", "details.a b").statusCode()).isEqualTo(400);

    }

    @Test
    void suggestionsForAPathTheListDoesNotShowAreRefused() {

        final var answer = suggestionsFor("usertask", "admittedUsers.id");

        assertThat(answer.statusCode()).isEqualTo(400);
        assertThat(answer.body()).isEqualTo("The request is not valid: 'path' " + RULE);
        assertThat(suggestionsFor("workflow", "details.$where").statusCode()).isEqualTo(400);

    }

}
