package io.vanillabp.cockpit.adapter.camunda8.utils;

import io.camunda.client.api.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AggregateIdParserTest {

    // Test implementation of the interface
    private static class TestAggregateIdParser implements AggregateIdParser {
        private static final Logger logger = LoggerFactory.getLogger(TestAggregateIdParser.class);

        @Override
        public Logger getLogger() {
            return logger;
        }
    }

    // Custom class with valueOf method for testing
    public static class CustomId {
        private final String value;

        private CustomId(String value) {
            this.value = value;
        }

        public static CustomId valueOf(String value) {
            return new CustomId(value);
        }

        public String getValue() {
            return value;
        }
    }

    // Custom class without valueOf method
    public static class InvalidCustomId {
        private final String value;

        public InvalidCustomId(String value) {
            this.value = value;
        }
    }

    @Mock
    private JsonMapper jsonMapper;

    private TestAggregateIdParser parser;

    @BeforeEach
    void setUp() {
        parser = new TestAggregateIdParser();
    }

    @Test
    void transformBusinessKeyInGivenVariables_withMatchingKey_transformsValue() {
        // Set up variables with aggregate ID
        final Map<String, Object> variables = new HashMap<>();
        variables.put("businessKey", "BK-123");
        variables.put("otherField", "value");

        // Create a simple parser function
        final AggregateIdParser.AggregateIdParserFunction parserFunction =
                (processInstanceKey, variableValue, valueIsJson) -> "TRANSFORMED-" + variableValue;

        // Transform the variables
        final var result = parser.transformBusinessKeyInGivenVariables(
                "businessKey",
                parserFunction,
                12345L,
                variables);

        // Verify transformation
        assertThat(result.get("businessKey")).isEqualTo("TRANSFORMED-BK-123");
        assertThat(result.get("otherField")).isEqualTo("value");
    }

    @Test
    void transformBusinessKeyInGivenVariables_withoutMatchingKey_returnsOtherVariables() {
        // Set up variables without aggregate ID
        final Map<String, Object> variables = new HashMap<>();
        variables.put("otherField", "value");

        // Create a parser function
        final AggregateIdParser.AggregateIdParserFunction parserFunction =
                (processInstanceKey, variableValue, valueIsJson) -> variableValue;

        // Transform the variables
        final var result = parser.transformBusinessKeyInGivenVariables(
                "businessKey",
                parserFunction,
                12345L,
                variables);

        // Verify other variables are preserved
        assertThat(result.get("otherField")).isEqualTo("value");
        assertThat(result).doesNotContainKey("businessKey");
    }

    @Test
    void transformBusinessKeyInGivenVariables_withNullValue_returnsWithoutKey() {
        // Set up variables with null aggregate ID
        final Map<String, Object> variables = new HashMap<>();
        variables.put("businessKey", null);
        variables.put("otherField", "value");

        // Create a parser function
        final AggregateIdParser.AggregateIdParserFunction parserFunction =
                (processInstanceKey, variableValue, valueIsJson) -> variableValue;

        // Transform the variables
        final var result = parser.transformBusinessKeyInGivenVariables(
                "businessKey",
                parserFunction,
                12345L,
                variables);

        // Verify null handling
        assertThat(result.get("otherField")).isEqualTo("value");
        assertThat(result).doesNotContainKey("businessKey");
    }

    @Test
    void determineBusinessKeyToIdMapper_withStringClass_returnsStringParser() throws Exception {
        // Set up json mapper for String
        when(jsonMapper.fromJson(eq("\"test-id\""), eq(String.class))).thenReturn("test-id");

        // Get the mapper for String type
        final var mapper = parser.determineBusinessKeyToIdMapper(
                jsonMapper,
                Object.class,
                String.class,
                "businessKey");

        // Parse a JSON string
        final var result = mapper.parse(12345L, "\"test-id\"", true);

        // Verify parsing
        assertThat(result).isEqualTo("test-id");
    }

    @Test
    void determineBusinessKeyToIdMapper_withIntClass_returnsIntParser() throws Exception {
        // Set up json mapper for Integer
        when(jsonMapper.fromJson(eq("42"), eq(Integer.class))).thenReturn(42);

        // Get the mapper for int type
        final var mapper = parser.determineBusinessKeyToIdMapper(
                jsonMapper,
                Object.class,
                int.class,
                "businessKey");

        // Parse an integer
        final var result = mapper.parse(12345L, "42", true);

        // Verify parsing
        assertThat(result).isEqualTo(42);
    }

    @Test
    void determineBusinessKeyToIdMapper_withLongClass_returnsLongParser() throws Exception {
        // Set up json mapper for Long
        when(jsonMapper.fromJson(eq("12345678901234"), eq(Long.class))).thenReturn(12345678901234L);

        // Get the mapper for long type
        final var mapper = parser.determineBusinessKeyToIdMapper(
                jsonMapper,
                Object.class,
                long.class,
                "businessKey");

        // Parse a long
        final var result = mapper.parse(12345L, "12345678901234", true);

        // Verify parsing
        assertThat(result).isEqualTo(12345678901234L);
    }

    @Test
    void determineBusinessKeyToIdMapper_withFloatClass_returnsFloatParser() throws Exception {
        // Set up json mapper for Float
        when(jsonMapper.fromJson(eq("3.14"), eq(Float.class))).thenReturn(3.14f);

        // Get the mapper for float type
        final var mapper = parser.determineBusinessKeyToIdMapper(
                jsonMapper,
                Object.class,
                float.class,
                "businessKey");

        // Parse a float
        final var result = mapper.parse(12345L, "3.14", true);

        // Verify parsing
        assertThat(result).isEqualTo(3.14f);
    }

    @Test
    void determineBusinessKeyToIdMapper_withDoubleClass_returnsDoubleParser() throws Exception {
        // Set up json mapper for Double
        when(jsonMapper.fromJson(eq("3.14159265359"), eq(Double.class))).thenReturn(3.14159265359);

        // Get the mapper for double type
        final var mapper = parser.determineBusinessKeyToIdMapper(
                jsonMapper,
                Object.class,
                double.class,
                "businessKey");

        // Parse a double
        final var result = mapper.parse(12345L, "3.14159265359", true);

        // Verify parsing
        assertThat(result).isEqualTo(3.14159265359);
    }

    @Test
    void determineBusinessKeyToIdMapper_withByteClass_returnsByteParser() throws Exception {
        // Set up json mapper for Byte
        when(jsonMapper.fromJson(eq("127"), eq(Byte.class))).thenReturn((byte) 127);

        // Get the mapper for byte type
        final var mapper = parser.determineBusinessKeyToIdMapper(
                jsonMapper,
                Object.class,
                byte.class,
                "businessKey");

        // Parse a byte
        final var result = mapper.parse(12345L, "127", true);

        // Verify parsing
        assertThat(result).isEqualTo((byte) 127);
    }

    @Test
    void determineBusinessKeyToIdMapper_withBigIntegerClass_returnsBigIntegerParser() throws Exception {
        // Set up json mapper for BigInteger
        final var bigInt = new BigInteger("123456789012345678901234567890");
        when(jsonMapper.fromJson(any(), eq(BigInteger.class))).thenReturn(bigInt);

        // Get the mapper for BigInteger type
        final var mapper = parser.determineBusinessKeyToIdMapper(
                jsonMapper,
                Object.class,
                BigInteger.class,
                "businessKey");

        // Parse a BigInteger
        final var result = mapper.parse(12345L, "123456789012345678901234567890", true);

        // Verify parsing
        assertThat(result).isEqualTo(bigInt);
    }

    @Test
    void determineBusinessKeyToIdMapper_withCustomClassWithValueOf_usesValueOfMethod() throws Exception {
        // Get the mapper for CustomId type
        final var mapper = parser.determineBusinessKeyToIdMapper(
                jsonMapper,
                Object.class,
                CustomId.class,
                "businessKey");

        // Parse using valueOf - JSON string value is used directly
        final var result = mapper.parse(12345L, "custom-123", true);

        // Verify parsing uses valueOf
        assertThat(result).isInstanceOf(CustomId.class);
        assertThat(((CustomId) result).getValue()).isEqualTo("custom-123");
    }

    @Test
    void determineBusinessKeyToIdMapper_withCustomClassWithValueOf_nonJson_usesToJson() throws Exception {
        // Set up json mapper to convert object to JSON
        when(jsonMapper.toJson(any())).thenReturn("converted-value");

        // Get the mapper for CustomId type
        final var mapper = parser.determineBusinessKeyToIdMapper(
                jsonMapper,
                Object.class,
                CustomId.class,
                "businessKey");

        // Parse using non-JSON value
        final var result = mapper.parse(12345L, new Object(), false);

        // Verify valueOf is called with JSON-converted value
        assertThat(result).isInstanceOf(CustomId.class);
        assertThat(((CustomId) result).getValue()).isEqualTo("converted-value");
    }

    @Test
    void determineBusinessKeyToIdMapper_withClassWithoutValueOf_throwsException() {
        // Try to get mapper for InvalidCustomId type (no valueOf method)
        assertThatThrownBy(() ->
                parser.determineBusinessKeyToIdMapper(
                        jsonMapper,
                        Object.class,
                        InvalidCustomId.class,
                        "businessKey")
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("valueOf");
    }

    @Test
    void determineBusinessKeyToIdMapper_withNullValue_returnsNull() throws Exception {
        // Get the mapper for String type
        final var mapper = parser.determineBusinessKeyToIdMapper(
                jsonMapper,
                Object.class,
                String.class,
                "businessKey");

        // Parse null value (no mocking needed, null is handled before calling jsonMapper)
        final var result = mapper.parse(12345L, null, true);

        // Verify null handling
        assertThat(result).isNull();
    }

    @Test
    void determineBusinessKeyToIdMapper_withTransformMode_usesTransform() throws Exception {
        // Set up json mapper for transform mode
        when(jsonMapper.transform(eq("test-value"), eq(String.class))).thenReturn("transformed");

        // Get the mapper for String type
        final var mapper = parser.determineBusinessKeyToIdMapper(
                jsonMapper,
                Object.class,
                String.class,
                "businessKey");

        // Parse with valueIsJson=false (transform mode)
        final var result = mapper.parse(12345L, "test-value", false);

        // Verify transform was used
        assertThat(result).isEqualTo("transformed");
    }

}
