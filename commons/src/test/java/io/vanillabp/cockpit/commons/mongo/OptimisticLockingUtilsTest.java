package io.vanillabp.cockpit.commons.mongo;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptimisticLockingUtilsTest {

    @Test
    void defaultNumberOfRetries_isTwo() {
        assertThat(OptimisticLockingUtils.DEFAULT_NUMBER_OF_RETRIES).isEqualTo(2);
    }

    @Test
    void doWithRetries_supplier_successOnFirstTry_returnsResult() {
        String result = OptimisticLockingUtils.doWithRetries(() -> "success");

        assertThat(result).isEqualTo("success");
    }

    @Test
    void doWithRetries_supplier_successAfterRetry_returnsResult() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = OptimisticLockingUtils.doWithRetries(() -> {
            if (attempts.incrementAndGet() < 2) {
                throw new OptimisticLockingFailureException("Conflict");
            }
            return "success";
        });

        assertThat(result).isEqualTo("success");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void doWithRetries_supplier_failsAfterMaxRetries_throwsException() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThatThrownBy(() -> OptimisticLockingUtils.doWithRetries(() -> {
            attempts.incrementAndGet();
            throw new OptimisticLockingFailureException("Conflict");
        }))
                .isInstanceOf(OptimisticLockingFailureException.class);

        // Default retries (2) + initial attempt = 3 attempts
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void doWithRetries_supplier_customRetries_retriesCorrectNumberOfTimes() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThatThrownBy(() -> OptimisticLockingUtils.doWithRetries(5, () -> {
            attempts.incrementAndGet();
            throw new OptimisticLockingFailureException("Conflict");
        }))
                .isInstanceOf(OptimisticLockingFailureException.class);

        // 5 retries + initial attempt = 6 attempts
        assertThat(attempts.get()).isEqualTo(6);
    }

    @Test
    void doWithRetries_supplier_zeroRetries_throwsOnFirstFailure() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThatThrownBy(() -> OptimisticLockingUtils.doWithRetries(0, () -> {
            attempts.incrementAndGet();
            throw new OptimisticLockingFailureException("Conflict");
        }))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    void doWithRetries_function_successOnFirstTry_returnsResult() {
        String result = OptimisticLockingUtils.doWithRetries("input", input -> input + "-processed");

        assertThat(result).isEqualTo("input-processed");
    }

    @Test
    void doWithRetries_function_successAfterRetry_returnsResult() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = OptimisticLockingUtils.doWithRetries("input", input -> {
            if (attempts.incrementAndGet() < 2) {
                throw new OptimisticLockingFailureException("Conflict");
            }
            return input + "-processed";
        });

        assertThat(result).isEqualTo("input-processed");
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void doWithRetries_function_failsAfterMaxRetries_throwsException() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThatThrownBy(() -> OptimisticLockingUtils.doWithRetries("input", input -> {
            attempts.incrementAndGet();
            throw new OptimisticLockingFailureException("Conflict");
        }))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void doWithRetries_function_customRetries_retriesCorrectNumberOfTimes() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThatThrownBy(() -> OptimisticLockingUtils.doWithRetries(3, "input", input -> {
            attempts.incrementAndGet();
            throw new OptimisticLockingFailureException("Conflict");
        }))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(attempts.get()).isEqualTo(4);
    }

    @Test
    void doWithRetries_function_passesInputCorrectly() {
        Integer input = 42;
        Integer result = OptimisticLockingUtils.doWithRetries(input, value -> value * 2);

        assertThat(result).isEqualTo(84);
    }

    @Test
    void doWithRetries_nonOptimisticLockingException_propagatesImmediately() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThatThrownBy(() -> OptimisticLockingUtils.doWithRetries(() -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Other error");
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Other error");

        // Should fail immediately without retry
        assertThat(attempts.get()).isEqualTo(1);
    }
}
