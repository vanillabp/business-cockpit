package io.vanillabp.cockpit.commons.rest.adapter.bearer;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.RetryableException;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.Collection;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * What the interceptor does with a token which the server did not accept: an HTTP 401 has to
 * come back as a retryable exception, so Feign asks for a new token and sends the call again,
 * while every other status stays a plain failure.
 */
@ExtendWith(SuppressOutputExtension.class)
public class BearerTokenBasedAuthInterceptorTest {

    private static final Map<String, Collection<String>> NO_HEADERS = Map.of();

    private static class TestInterceptor extends BearerTokenBasedAuthInterceptor {

        int tokensCreated;

        @Override
        protected String createToken() {
            tokensCreated++;
            return "token-" + tokensCreated;
        }

    }

    private static Response responseWith(
            final int status,
            final String reason) {

        final var request = Request.create(
                Request.HttpMethod.GET,
                "http://localhost/api/v1/usertask",
                NO_HEADERS,
                Request.Body.empty(),
                new RequestTemplate());

        return Response
                .builder()
                .status(status)
                .reason(reason)
                .request(request)
                .headers(NO_HEADERS)
                .build();

    }

    @Test
    public void testTheAuthorizationHeaderCarriesAFreshToken() {

        final var interceptor = new TestInterceptor();
        final var template = new RequestTemplate();
        template.header("Authorization", "Bearer an-expired-one");

        interceptor.apply(template);

        Assertions.assertThat(template.headers().get("Authorization")).containsExactly("Bearer token-1");

    }

    @Test
    public void testUnauthorizedIsWorthRetrying() {

        final var exception = new TestInterceptor().decode("someMethodKey", responseWith(401, "Unauthorized"));

        Assertions.assertThat(exception).isInstanceOf(RetryableException.class);

        final var retryable = (RetryableException) exception;
        Assertions.assertThat(retryable.status()).isEqualTo(401);
        Assertions.assertThat(retryable.method()).isEqualTo(Request.HttpMethod.GET);

        // Feign waits until this moment before it retries, and there is nothing to wait for here
        Assertions
                .assertThat(retryable.retryAfter())
                .isNotNull()
                .isLessThanOrEqualTo(System.currentTimeMillis());

    }

    @Test
    public void testAnyOtherStatusIsAPlainFailure() {

        final var exception = new TestInterceptor().decode("someMethodKey", responseWith(500, "Server Error"));

        Assertions.assertThat(exception).isInstanceOf(FeignException.class);
        Assertions.assertThat(exception).isNotInstanceOf(RetryableException.class);

    }

}
