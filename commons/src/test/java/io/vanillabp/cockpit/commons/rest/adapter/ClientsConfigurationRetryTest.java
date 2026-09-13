package io.vanillabp.cockpit.commons.rest.adapter;

import feign.DefaultRetryer;
import feign.Feign;
import feign.Request;
import feign.RequestTemplate;
import feign.Retryer;
import feign.RetryableException;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.Duration;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * How the retry properties of a client reach Feign. The retryer is read back off the builder
 * and then asked to retry, because the numbers a client was configured with are invisible from
 * outside and only show in what the retryer does.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ClientsConfigurationRetryTest {

    private static class TestConfiguration extends ClientsConfigurationBase {
    }

    /** A builder which hands back the retryer it was given. */
    private static class RetryerReadingBuilder extends Feign.Builder {

        Retryer chosenRetryer() {
            return retryer;
        }

    }

    private static Client clientWithRetry(
            final Retry retry) {

        final var client = new Client();
        client.setRetry(retry);
        return client;

    }

    private static RetryableException retryableAt(
            final long retryAfter) {

        final var request = Request.create(
                Request.HttpMethod.GET,
                "http://localhost/api/v1/usertask",
                Map.of(),
                Request.Body.empty(),
                new RequestTemplate());

        return new RetryableException(401, "Unauthorized", Request.HttpMethod.GET, retryAfter, request);

    }

    @Test
    public void testNoRetryPropertiesMeansNoRetry() {

        final var builder = new RetryerReadingBuilder();

        new TestConfiguration().configureRetry(builder, new Client());

        Assertions.assertThat(builder.chosenRetryer()).isSameAs(Retryer.NEVER_RETRY);

    }

    @Test
    public void testSwitchedOffRetryMeansNoRetry() {

        final var builder = new RetryerReadingBuilder();
        final var retry = new Retry();
        retry.setEnabled(false);

        new TestConfiguration().configureRetry(builder, clientWithRetry(retry));

        Assertions.assertThat(builder.chosenRetryer()).isSameAs(Retryer.NEVER_RETRY);

    }

    @Test
    public void testTheConfiguredNumberOfAttemptsIsWhatFeignGets() {

        final var builder = new RetryerReadingBuilder();
        final var retry = new Retry();
        retry.setEnabled(true);
        retry.setMaxAttempts(2);
        retry.setPeriod(Duration.ofMillis(1));
        retry.setMaxPeriod(Duration.ofMillis(1));

        new TestConfiguration().configureRetry(builder, clientWithRetry(retry));

        final var retryer = builder.chosenRetryer();
        Assertions.assertThat(retryer).isExactlyInstanceOf(DefaultRetryer.class);

        // a moment in the past leaves nothing to wait for, so only the attempt counting is left
        final var alreadyDue = retryableAt(System.currentTimeMillis() - 1000);

        Assertions.assertThatNoException().isThrownBy(() -> retryer.continueOrPropagate(alreadyDue));
        Assertions
                .assertThatThrownBy(() -> retryer.continueOrPropagate(alreadyDue))
                .isSameAs(alreadyDue);

    }

    @Test
    public void testTheConfiguredMaximumPeriodCapsTheWait() {

        final var builder = new RetryerReadingBuilder();
        final var retry = new Retry();
        retry.setEnabled(true);
        retry.setMaxAttempts(5);
        retry.setPeriod(Duration.ofMillis(1));
        retry.setMaxPeriod(Duration.ofMillis(20));

        new TestConfiguration().configureRetry(builder, clientWithRetry(retry));

        // a server asking for a minute is made to wait the configured maximum instead
        final var dueInAMinute = retryableAt(System.currentTimeMillis() + 60000);

        final var startedAt = System.currentTimeMillis();
        builder.chosenRetryer().continueOrPropagate(dueInAMinute);
        final var waited = System.currentTimeMillis() - startedAt;

        // well below the 1000 ms Feign would have capped at without the configured value
        Assertions.assertThat(waited).isLessThan(500L);

    }

}
