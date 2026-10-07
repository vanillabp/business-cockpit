package io.vanillabp.cockpit.util;

import io.vanillabp.cockpit.commons.exceptions.BcInvalidRequestException;

/**
 * The values a list request of the GUI API may leave out, and what the server takes instead. The
 * OpenAPI document calls all of them optional and names the same defaults, so a client which keeps
 * to the document gets a page and not a server error.
 * <p>
 * The page size has no default. How many rows a page holds is up to the user interface, and a
 * value the server picked would only hide that the client forgot it. So a missing page size is
 * answered with {@code 400 Bad Request}, which names the field.
 */
public final class ListPaging {

    /** The first page, which is what a client means when it does not say which one. */
    public static final int DEFAULT_PAGE_NUMBER = 0;

    public static final boolean DEFAULT_SORT_ASCENDING = true;

    private ListPaging() {
    }

    public static int pageNumber(
            final Integer requested) {

        return requested == null ? DEFAULT_PAGE_NUMBER : requested;

    }

    /**
     * @throws BcInvalidRequestException if the request names no page size
     */
    public static int pageSize(
            final Integer requested) {

        if (requested == null) {
            throw new BcInvalidRequestException("'pageSize' is missing");
        }
        return requested;

    }

    public static boolean sortAscending(
            final Boolean requested) {

        return requested == null ? DEFAULT_SORT_ASCENDING : requested;

    }

}
