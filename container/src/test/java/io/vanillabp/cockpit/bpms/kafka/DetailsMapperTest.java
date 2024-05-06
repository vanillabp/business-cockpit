package io.vanillabp.cockpit.bpms.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vanillabp.cockpit.adapter.common.protobuf.DetailsConverter;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.users.TestUserDetailsImpl;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// https://stackoverflow.com/questions/37950296/spring-data-mongodb-bigdecimal-support

public class DetailsMapperTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    public void init() {
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testString() throws Exception {

        final var test = Map.of("test1", (Object) "a");

        final var serialized = toByteArray(test);
        final var protobuf = toProtobufObjects(serialized);

        final var result = DetailsMapper.mapMapValue(protobuf);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(test, result);

    }

    @Test
    public void testBoolean() throws Exception {

        final var test = Map.of(
                "test1", (Object) true,
                "test2", false);

        final var serialized = toByteArray(test);
        final var protobuf = toProtobufObjects(serialized);

        final var result = DetailsMapper.mapMapValue(protobuf);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(test, result);

    }

    @Test
    public void testNull() throws Exception {

        final var test = new HashMap<String, Object>();
        test.put("test1", "anything");
        test.put("test2", null);

        final var serialized = toByteArray(test);
        final var protobuf = toProtobufObjects(serialized);

        final var result = DetailsMapper.mapMapValue(protobuf);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(test, result);

    }

    @Test
    public void testNumeric() throws Exception {

        final var bigdec = new BigDecimal("1.234567890123456789E+19");
        final var test = Map.of(
                "test1", (Object) 10,
                "test2", 47.11,
                "test3", 20L,
                "test4", bigdec);

        final var serialized = toByteArray(test);
        final var protobuf = toProtobufObjects(serialized);

        final var result = DetailsMapper.mapMapValue(protobuf);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(BigDecimal.valueOf(10), result.get("test1"));
        Assertions.assertEquals(BigDecimal.valueOf(47.11), result.get("test2"));
        Assertions.assertEquals(BigDecimal.valueOf(20L), result.get("test3"));
        final var test4 = (BigDecimal) result.get("test4");
        Assertions.assertEquals(bigdec.unscaledValue(), test4.unscaledValue());
        Assertions.assertEquals(bigdec.scale(), test4.scale());

    }

    @Test
    public void testMap() throws Exception {

        final var test = Map.of(
                "test1", (Object) Map.of(
                        "test2", "4711"));

        final var serialized = toByteArray(test);
        final var protobuf = toProtobufObjects(serialized);

        final var result = DetailsMapper.mapMapValue(protobuf);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(test, result);

    }

    @Test
    public void testArray() throws Exception {

        final var test = Map.of(
                "test1", (Object) List.of("4711"));

        final var serialized = toByteArray(test);
        final var protobuf = toProtobufObjects(serialized);

        final var result = DetailsMapper.mapMapValue(protobuf);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(test, result);

    }

    @Test
    public void testArrayInMap() throws Exception {

        final var test = Map.of(
                "test1", (Object) Map.of(
                        "test2", List.of("4711")));

        final var serialized = toByteArray(test);
        final var protobuf = toProtobufObjects(serialized);

        final var result = DetailsMapper.mapMapValue(protobuf);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(test, result);

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testComplex() throws Exception {

        final var complex = new TestUserDetailsImpl(
                "4711",
                "a@b",
                "First",
                "Last",
                List.of("A", "B"));


        final var test = Map.of(
                "test1", (Object) complex);

        final var serialized = toByteArray(test);
        final var protobuf = toProtobufObjects(serialized);

        final var result = DetailsMapper.mapMapValue(protobuf);

        Assertions.assertNotNull(result);
        final var target = (Map<String, Object>) result.get("test1");
        Assertions.assertEquals(complex.getId(), target.get("id"));
        Assertions.assertEquals(complex.getEmail(), target.get("email"));
        Assertions.assertEquals(complex.getFirstName(), target.get("firstName"));
        Assertions.assertEquals(complex.getLastName(), target.get("lastName"));
        Assertions.assertEquals(complex.getAuthorities(), target.get("authorities"));

    }

    private byte[] toByteArray(
            final Map<String, Object> details) {

        final var tree = objectMapper.valueToTree(details);
        final var protobuf = DetailsConverter.mapDetailsJsonToProtobuf(tree);
        return protobuf.toByteArray();

    }

    private DetailsMap toProtobufObjects(
            final byte[] serialized) throws Exception {

        return DetailsMap.parseFrom(serialized);

    }

}
