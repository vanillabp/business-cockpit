package io.vanillabp.cockpit.adapter.common.protobuf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetailsConverterTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void mapDetailsJsonToProtobuf_withStringValue_mapsCorrectly() throws Exception {
        String json = "{\"name\": \"John\"}";
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).containsKey("name");
        assertThat(result.getDetailsMap().get("name").getIsArray()).isFalse();
        assertThat(result.getDetailsMap().get("name").getArrayValues(0).getStringValue()).isEqualTo("John");
    }

    @Test
    void mapDetailsJsonToProtobuf_withNumericValue_mapsCorrectly() throws Exception {
        String json = "{\"age\": 25}";
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).containsKey("age");
        assertThat(result.getDetailsMap().get("age").getArrayValues(0).getNumericValue()).isEqualTo("25");
    }

    @Test
    void mapDetailsJsonToProtobuf_withDecimalValue_mapsCorrectly() throws Exception {
        String json = "{\"price\": 19.99}";
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).containsKey("price");
        assertThat(result.getDetailsMap().get("price").getArrayValues(0).getNumericValue()).isEqualTo("19.99");
    }

    @Test
    void mapDetailsJsonToProtobuf_withBooleanTrue_mapsCorrectly() throws Exception {
        String json = "{\"active\": true}";
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).containsKey("active");
        assertThat(result.getDetailsMap().get("active").getArrayValues(0).getBoolValue()).isTrue();
    }

    @Test
    void mapDetailsJsonToProtobuf_withBooleanFalse_mapsCorrectly() throws Exception {
        String json = "{\"active\": false}";
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).containsKey("active");
        assertThat(result.getDetailsMap().get("active").getArrayValues(0).getBoolValue()).isFalse();
    }

    @Test
    void mapDetailsJsonToProtobuf_withNullValue_mapsCorrectly() throws Exception {
        String json = "{\"empty\": null}";
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).containsKey("empty");
        assertThat(result.getDetailsMap().get("empty").getArrayValues(0).getNullValue()).isTrue();
    }

    @Test
    void mapDetailsJsonToProtobuf_withArray_mapsCorrectly() throws Exception {
        String json = "{\"tags\": [\"a\", \"b\", \"c\"]}";
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).containsKey("tags");
        assertThat(result.getDetailsMap().get("tags").getIsArray()).isTrue();
        assertThat(result.getDetailsMap().get("tags").getArrayValuesCount()).isEqualTo(3);
        assertThat(result.getDetailsMap().get("tags").getArrayValues(0).getStringValue()).isEqualTo("a");
        assertThat(result.getDetailsMap().get("tags").getArrayValues(1).getStringValue()).isEqualTo("b");
        assertThat(result.getDetailsMap().get("tags").getArrayValues(2).getStringValue()).isEqualTo("c");
    }

    @Test
    void mapDetailsJsonToProtobuf_withNestedObject_mapsCorrectly() throws Exception {
        String json = "{\"address\": {\"city\": \"Vienna\", \"zip\": \"1010\"}}";
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).containsKey("address");
        DetailsMap nested = result.getDetailsMap().get("address").getArrayValues(0).getMapValue();
        assertThat(nested.getDetailsMap()).containsKey("city");
        assertThat(nested.getDetailsMap().get("city").getArrayValues(0).getStringValue()).isEqualTo("Vienna");
        assertThat(nested.getDetailsMap()).containsKey("zip");
        assertThat(nested.getDetailsMap().get("zip").getArrayValues(0).getStringValue()).isEqualTo("1010");
    }

    @Test
    void mapDetailsJsonToProtobuf_withMixedArray_mapsCorrectly() throws Exception {
        String json = "{\"mixed\": [\"text\", 123, true, null]}";
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).containsKey("mixed");
        assertThat(result.getDetailsMap().get("mixed").getIsArray()).isTrue();
        assertThat(result.getDetailsMap().get("mixed").getArrayValuesCount()).isEqualTo(4);
        assertThat(result.getDetailsMap().get("mixed").getArrayValues(0).getStringValue()).isEqualTo("text");
        assertThat(result.getDetailsMap().get("mixed").getArrayValues(1).getNumericValue()).isEqualTo("123");
        assertThat(result.getDetailsMap().get("mixed").getArrayValues(2).getBoolValue()).isTrue();
        assertThat(result.getDetailsMap().get("mixed").getArrayValues(3).getNullValue()).isTrue();
    }

    @Test
    void mapDetailsJsonToProtobuf_withEmptyObject_mapsCorrectly() throws Exception {
        String json = "{}";
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).isEmpty();
    }

    @Test
    void mapDetailsJsonToProtobuf_withEmptyArray_mapsCorrectly() throws Exception {
        String json = "{\"items\": []}";
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).containsKey("items");
        assertThat(result.getDetailsMap().get("items").getIsArray()).isTrue();
        assertThat(result.getDetailsMap().get("items").getArrayValuesCount()).isEqualTo(0);
    }

    @Test
    void mapDetailsJsonToProtobuf_withNonObjectNode_throwsUnsupportedOperationException() {
        ArrayNode arrayNode = objectMapper.createArrayNode();

        assertThatThrownBy(() -> DetailsConverter.mapDetailsJsonToProtobuf(arrayNode))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mapDetailsJsonToProtobuf_withComplexStructure_mapsCorrectly() throws Exception {
        String json = """
                {
                    "customer": {
                        "name": "Acme Corp",
                        "contacts": ["alice@acme.com", "bob@acme.com"],
                        "active": true,
                        "rating": 4.5
                    },
                    "orderCount": 42
                }
                """;
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).containsKey("customer");
        assertThat(result.getDetailsMap()).containsKey("orderCount");
        assertThat(result.getDetailsMap().get("orderCount").getArrayValues(0).getNumericValue()).isEqualTo("42");

        DetailsMap customer = result.getDetailsMap().get("customer").getArrayValues(0).getMapValue();
        assertThat(customer.getDetailsMap().get("name").getArrayValues(0).getStringValue()).isEqualTo("Acme Corp");
        assertThat(customer.getDetailsMap().get("active").getArrayValues(0).getBoolValue()).isTrue();
        assertThat(customer.getDetailsMap().get("rating").getArrayValues(0).getNumericValue()).isEqualTo("4.5");
        assertThat(customer.getDetailsMap().get("contacts").getIsArray()).isTrue();
        assertThat(customer.getDetailsMap().get("contacts").getArrayValuesCount()).isEqualTo(2);
    }

    @Test
    void mapDetailsJsonToProtobuf_withMultipleFields_mapsAllFields() throws Exception {
        String json = "{\"field1\": \"value1\", \"field2\": \"value2\", \"field3\": \"value3\"}";
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);

        DetailsMap result = DetailsConverter.mapDetailsJsonToProtobuf(node);

        assertThat(result.getDetailsMap()).hasSize(3);
        assertThat(result.getDetailsMap()).containsKeys("field1", "field2", "field3");
    }
}
