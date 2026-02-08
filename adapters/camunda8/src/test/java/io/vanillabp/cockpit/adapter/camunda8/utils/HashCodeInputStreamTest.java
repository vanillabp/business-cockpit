package io.vanillabp.cockpit.adapter.camunda8.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashCodeInputStreamTest {

    // --- Constructor ---

    @Test
    void constructor_withDelegate_initializesHashCodeToZero() throws IOException {
        // Create stream with simple content
        final var delegate = new ByteArrayInputStream(new byte[0]);
        final var stream = new HashCodeInputStream(delegate);

        // Hash code should be zero before reading
        assertThat(stream.hashCode()).isEqualTo(0);
        stream.close();
    }

    @Test
    void constructor_withPreviousHashCode_initializesTotalHashCode() throws IOException {
        // Create stream with previous hash code
        final var delegate = new ByteArrayInputStream(new byte[0]);
        final var stream = new HashCodeInputStream(delegate, 42);

        // Total hash code should be initialized with previous value
        assertThat(stream.getTotalHashCode()).isEqualTo(42);
        stream.close();
    }

    // --- read ---

    @Test
    void read_singleByte_updatesHashCode() throws IOException {
        // Create stream with single byte
        final var delegate = new ByteArrayInputStream(new byte[]{65}); // 'A'
        final var stream = new HashCodeInputStream(delegate);

        // Read single byte
        final var readByte = stream.read();

        // Verify byte was read correctly
        assertThat(readByte).isEqualTo(65);

        // Hash code should be updated: 31 * 0 + 65 = 65
        assertThat(stream.hashCode()).isEqualTo(65);
        assertThat(stream.getTotalHashCode()).isEqualTo(65);
        stream.close();
    }

    @Test
    void read_multipleBytes_accumulatesHashCode() throws IOException {
        // Create stream with multiple bytes
        final var delegate = new ByteArrayInputStream(new byte[]{1, 2, 3});
        final var stream = new HashCodeInputStream(delegate);

        // Read all bytes
        stream.read();
        stream.read();
        stream.read();

        // Hash code formula: 31 * (31 * (31 * 0 + 1) + 2) + 3 = 31 * (31 * 1 + 2) + 3 = 31 * 33 + 3 = 1026
        assertThat(stream.hashCode()).isEqualTo(1026);
        stream.close();
    }

    @Test
    void read_withPreviousHashCode_updatesTotalHashCodeIndependently() throws IOException {
        // Create stream with previous hash code
        final var delegate = new ByteArrayInputStream(new byte[]{1});
        final var stream = new HashCodeInputStream(delegate, 100);

        // Read single byte
        stream.read();

        // Regular hash code: 31 * 0 + 1 = 1
        assertThat(stream.hashCode()).isEqualTo(1);

        // Total hash code includes previous: 31 * 100 + 1 = 3101
        assertThat(stream.getTotalHashCode()).isEqualTo(3101);
        stream.close();
    }

    @Test
    void read_endOfStream_returnsMinusOne() throws IOException {
        // Create empty stream
        final var delegate = new ByteArrayInputStream(new byte[0]);
        final var stream = new HashCodeInputStream(delegate);

        // Read from empty stream
        final var result = stream.read();

        // Should return -1 for end of stream
        assertThat(result).isEqualTo(-1);
        stream.close();
    }

    // --- mark and reset ---

    @Test
    void markAndReset_restoresHashCode() throws IOException {
        // Create stream with mark support
        final var delegate = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5});
        final var stream = new HashCodeInputStream(delegate);

        // Read two bytes
        stream.read();
        stream.read();
        final var hashCodeAtMark = stream.hashCode();

        // Mark current position
        stream.mark(10);

        // Read more bytes
        stream.read();
        stream.read();
        assertThat(stream.hashCode()).isNotEqualTo(hashCodeAtMark);

        // Reset to mark
        stream.reset();

        // Hash code should be restored
        assertThat(stream.hashCode()).isEqualTo(hashCodeAtMark);
        stream.close();
    }

    // --- markSupported ---

    @Test
    void markSupported_delegatesToUnderlyingStream() throws IOException {
        // ByteArrayInputStream supports mark
        final var delegate = new ByteArrayInputStream(new byte[]{1, 2, 3});
        final var stream = new HashCodeInputStream(delegate);

        // Verify mark is supported
        assertThat(stream.markSupported()).isTrue();
        stream.close();
    }

    // --- available ---

    @Test
    void available_delegatesToUnderlyingStream() throws IOException {
        // Create stream with known content
        final var delegate = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5});
        final var stream = new HashCodeInputStream(delegate);

        // Verify available bytes
        assertThat(stream.available()).isEqualTo(5);

        // Read some bytes
        stream.read();
        stream.read();

        // Verify remaining bytes
        assertThat(stream.available()).isEqualTo(3);
        stream.close();
    }

    // --- skip ---

    @Test
    void skip_delegatesToUnderlyingStream() throws IOException {
        // Create stream with content
        final var delegate = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5});
        final var stream = new HashCodeInputStream(delegate);

        // Skip 2 bytes
        final var skipped = stream.skip(2);

        // Verify skip was delegated
        assertThat(skipped).isEqualTo(2);
        assertThat(stream.available()).isEqualTo(3);
        stream.close();
    }

    // --- transferTo ---

    @Test
    void transferTo_delegatesToUnderlyingStream() throws IOException {
        // Create stream with content
        final var delegate = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5});
        final var stream = new HashCodeInputStream(delegate);
        final var output = new ByteArrayOutputStream();

        // Transfer all bytes
        final var transferred = stream.transferTo(output);

        // Verify transfer was delegated
        assertThat(transferred).isEqualTo(5);
        assertThat(output.toByteArray()).containsExactly(1, 2, 3, 4, 5);
        stream.close();
    }

    // --- close ---

    @Test
    void close_delegatesToUnderlyingStream() throws IOException {
        // Create stream with content
        final var delegate = new ByteArrayInputStream(new byte[]{1, 2, 3});
        final var stream = new HashCodeInputStream(delegate);

        // Close should not throw
        stream.close();
    }

    // --- Hash consistency ---

    @Test
    void hashCode_sameContent_producesSameHash() throws IOException {
        // Create two streams with identical content
        final var content = "Hello World".getBytes();
        final var stream1 = new HashCodeInputStream(new ByteArrayInputStream(content));
        final var stream2 = new HashCodeInputStream(new ByteArrayInputStream(content));

        // Read all bytes from both streams
        while (stream1.read() != -1) { }
        while (stream2.read() != -1) { }

        // Hash codes should be identical
        assertThat(stream1.hashCode()).isEqualTo(stream2.hashCode());
        assertThat(stream1.getTotalHashCode()).isEqualTo(stream2.getTotalHashCode());

        stream1.close();
        stream2.close();
    }

    @Test
    void hashCode_differentContent_producesDifferentHash() throws IOException {
        // Create two streams with different content
        final var stream1 = new HashCodeInputStream(new ByteArrayInputStream("Hello".getBytes()));
        final var stream2 = new HashCodeInputStream(new ByteArrayInputStream("World".getBytes()));

        // Read all bytes from both streams
        while (stream1.read() != -1) { }
        while (stream2.read() != -1) { }

        // Hash codes should be different
        assertThat(stream1.hashCode()).isNotEqualTo(stream2.hashCode());

        stream1.close();
        stream2.close();
    }

}
