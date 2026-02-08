package io.vanillabp.cockpit.util.protobuf;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProtobufHelper}.
 */
class ProtobufHelperTest {

    @Test
    void map_withValidTimestamp_returnsCorrectOffsetDateTime() {
        // Arrange
        long seconds = 1609459200L; // 2021-01-01 00:00:00 UTC
        int nanos = 500_000_000; // 0.5 seconds
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(seconds)
                .setNanos(nanos)
                .build();

        // Act
        OffsetDateTime result = ProtobufHelper.map(timestamp);

        // Assert
        Instant expectedInstant = Instant.ofEpochSecond(seconds, nanos);
        OffsetDateTime expected = OffsetDateTime.ofInstant(expectedInstant, ZoneId.systemDefault());
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void map_withZeroTimestamp_returnsEpoch() {
        // Arrange
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(0)
                .setNanos(0)
                .build();

        // Act
        OffsetDateTime result = ProtobufHelper.map(timestamp);

        // Assert
        Instant expectedInstant = Instant.EPOCH;
        OffsetDateTime expected = OffsetDateTime.ofInstant(expectedInstant, ZoneId.systemDefault());
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void map_withOnlySeconds_handlesNanosAsZero() {
        // Arrange
        long seconds = 1625140800L; // 2021-07-01 12:00:00 UTC
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(seconds)
                .setNanos(0)
                .build();

        // Act
        OffsetDateTime result = ProtobufHelper.map(timestamp);

        // Assert
        Instant expectedInstant = Instant.ofEpochSecond(seconds);
        OffsetDateTime expected = OffsetDateTime.ofInstant(expectedInstant, ZoneId.systemDefault());
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void map_withMaxNanos_handlesCorrectly() {
        // Arrange
        long seconds = 1609459200L;
        int nanos = 999_999_999; // max nanos value
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(seconds)
                .setNanos(nanos)
                .build();

        // Act
        OffsetDateTime result = ProtobufHelper.map(timestamp);

        // Assert
        Instant expectedInstant = Instant.ofEpochSecond(seconds, nanos);
        OffsetDateTime expected = OffsetDateTime.ofInstant(expectedInstant, ZoneId.systemDefault());
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void map_preservesTimeZone() {
        // Arrange
        long seconds = 1609459200L;
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(seconds)
                .setNanos(0)
                .build();

        // Act
        OffsetDateTime result = ProtobufHelper.map(timestamp);

        // Assert - result should be in system default time zone
        assertThat(result.getOffset()).isEqualTo(ZoneId.systemDefault().getRules().getOffset(Instant.ofEpochSecond(seconds)));
    }
}
