package io.vanillabp.cockpit.util.protobuf;

import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class ProtobufHelper {
    public static OffsetDateTime map(Timestamp value) {
        Instant instant = Instant.ofEpochSecond(value.getSeconds(), value.getNanos());
        return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
