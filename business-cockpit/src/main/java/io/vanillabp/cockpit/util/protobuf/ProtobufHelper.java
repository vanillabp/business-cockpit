package io.vanillabp.cockpit.util.protobuf;

import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Converts values of the Protocol Buffers messages the cockpit gets from Kafka.
 */
public class ProtobufHelper {

    /**
     * Only static methods, so nobody needs an instance.
     */
    private ProtobufHelper() {

    }

    /**
     * Converts a Protocol Buffers timestamp into a date and time in the time zone of the server.
     *
     * @param value The timestamp
     * @return The same point in time
     */
    public static OffsetDateTime map(Timestamp value) {
        Instant instant = Instant.ofEpochSecond(value.getSeconds(), value.getNanos());
        return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
