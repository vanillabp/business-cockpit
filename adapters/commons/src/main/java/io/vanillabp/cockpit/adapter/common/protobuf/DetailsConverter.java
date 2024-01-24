package io.vanillabp.cockpit.adapter.common.protobuf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsArrayValue;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsValue;

public class DetailsConverter {

    public static DetailsMap mapDetailsJsonToProtobuf(
            final JsonNode node) {

        if (!(node instanceof ObjectNode objectNode)) {
            throw new UnsupportedOperationException();
        }

        final var builder = DetailsMap.newBuilder();

        objectNode
                .fields()
                .forEachRemaining(entry -> {
                    final var detailsArrayValue = mapDetailsArrayJsonToProtobuf(entry.getValue());
                    builder.putDetails(entry.getKey(), detailsArrayValue);
                });

        return builder.build();

    }

    private static DetailsArrayValue mapDetailsArrayJsonToProtobuf(
            final JsonNode node) {

        final var builder = DetailsArrayValue.newBuilder();
        if (node instanceof ArrayNode arrayNode) {
            builder.setIsArray(true);
            arrayNode
                    .forEach(itemNode -> {
                        final var detailsValue = mapDetailsValueJsonToProtobuf(itemNode);
                        builder.addArrayValues(detailsValue);
                    });
        } else {
            builder.setIsArray(false);
            final var detailsValue = mapDetailsValueJsonToProtobuf(node);
            builder.addArrayValues(detailsValue);
        }

        return builder.build();

    }

    private static DetailsValue mapDetailsValueJsonToProtobuf(
            final JsonNode node) {

        final var builder = DetailsValue.newBuilder();
        if (node instanceof NullNode) {
            builder.setNullValue(true);
            return builder.build();
        } else {
            builder.setNullValue(false);
        }

        if (node instanceof NumericNode numericNode) {
            builder.setNumericValue(numericNode.decimalValue().toString());
            return builder.build();
        }

        if (node instanceof TextNode textNode) {
            builder.setStringValue(textNode.textValue());
            return builder.build();
        }

        if (node instanceof BooleanNode booleanNode) {
            builder.setBoolValue(booleanNode.booleanValue());
            return builder.build();
        }

        if (node instanceof ObjectNode objectNode) {
            final var map = mapDetailsJsonToProtobuf(objectNode);
            builder.setMapValue(map);
            return builder.build();
        }

        throw new UnsupportedOperationException("Unsupported JSON node type '"
                + node.getClass().getName()
                + "': "
                + node.toPrettyString());

    }

}
