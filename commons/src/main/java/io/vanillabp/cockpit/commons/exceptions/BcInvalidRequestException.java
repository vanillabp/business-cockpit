package io.vanillabp.cockpit.commons.exceptions;

import java.util.List;

/**
 * A request the schema lets through, but which the server cannot answer as it is. A missing page
 * size is one example: the schema calls it optional, because one endpoint has a default for it, but
 * the list of user tasks has none.
 * <p>
 * {@link RestfulExceptionHandler} answers it with {@code 400 Bad Request}, in the same words it uses
 * for a request which breaks its schema. So a client reads one kind of answer, whichever check
 * found the mistake.
 */
public class BcInvalidRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> violations;

    /**
     * @param violations What is wrong, one entry per field, each one naming the field in quotes,
     *        like {@code 'pageSize' is missing}. Never a value the request sent.
     */
    public BcInvalidRequestException(
            final String... violations) {

        super(String.join(", ", violations));
        this.violations = List.of(violations);

    }

    public List<String> getViolations() {
        return violations;
    }

}
