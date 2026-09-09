package io.vanillabp.cockpit.extension.transport;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsArrayValue;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsValue;

/**
 * Turns the business data a details provider put into the event into the protobuf shape the
 * Kafka transport sends.
 * <p>
 * Protobuf has no free-form value, so the data takes a detour through JSON: the object graph
 * becomes a tree, and the tree becomes the nested <code>DetailsMap</code> the cockpit reads.
 * Two properties of that shape are worth knowing. Every value is wrapped in an array, with a
 * flag saying whether it really was one, so that a reader needs one case less. And a number
 * travels as its decimal STRING, because a JSON number has no width and the cockpit must not
 * silently turn a business amount into a double.
 */
public final class DetailsConverter {

  private DetailsConverter() {
  }

  /**
   * @param objectMapper The mapper turning the business objects into a tree
   * @param details What the details provider put into the event
   * @return The protobuf shape, empty where there is nothing
   */
  public static DetailsMap toProtobuf(
      final ObjectMapper objectMapper,
      final Map<String, Object> details) {

    if ((details == null) || details.isEmpty()) {
      return DetailsMap.newBuilder().build();
    }
    return toProtobuf(objectMapper.<JsonNode>valueToTree(details));

  }

  /**
   * @param node The tree, which has to be an object
   * @return The protobuf shape
   */
  public static DetailsMap toProtobuf(
      final JsonNode node) {

    if (!(node instanceof final ObjectNode objectNode)) {
      throw new IllegalArgumentException(
          """
              The details of a Business Cockpit event have to be a map of named values, but they \
              became a '%s' when serialized. Put the values into a map or into an object with \
              named properties."""
              .formatted(node.getNodeType()));
    }
    final var builder = DetailsMap.newBuilder();
    final var converted = new LinkedHashMap<String, DetailsArrayValue>();
    objectNode
        .properties()
        .forEach(property -> converted.put(property.getKey(), arrayOf(property.getValue())));
    builder.putAllDetails(converted);
    return builder.build();

  }

  private static DetailsArrayValue arrayOf(
      final JsonNode node) {

    final var builder = DetailsArrayValue.newBuilder();
    if (node instanceof final ArrayNode arrayNode) {
      builder.setIsArray(true);
      arrayNode.forEach(item -> builder.addArrayValues(valueOf(item)));
      return builder.build();
    }
    builder.setIsArray(false);
    builder.addArrayValues(valueOf(node));
    return builder.build();

  }

  private static DetailsValue valueOf(
      final JsonNode node) {

    final var builder = DetailsValue.newBuilder();
    if (node.isNull()) {
      builder.setNullValue(true);
      return builder.build();
    }
    builder.setNullValue(false);
    if (node.isNumber()) {
      builder.setNumericValue(node.decimalValue().toString());
    } else if (node.isTextual()) {
      builder.setStringValue(node.textValue());
    } else if (node.isBoolean()) {
      builder.setBoolValue(node.booleanValue());
    } else if (node instanceof ObjectNode) {
      builder.setMapValue(toProtobuf(node));
    } else {
      throw new IllegalArgumentException(
          """
              A value of the details of a Business Cockpit event became a '%s' when serialized, \
              which the cockpit's protobuf shape has no place for: %s"""
              .formatted(node.getNodeType(), node.toPrettyString()));
    }
    return builder.build();

  }

}
