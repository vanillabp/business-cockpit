package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import io.vanillabp.cockpit.extension.BusinessCockpitAssembly;
import io.vanillabp.cockpit.extension.transport.DetailsConverter;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The corners of the conversion of business data into the cockpit's protobuf shape: a value
 * which really is null, a number which must not lose precision, and a shape the cockpit has no
 * place for.
 */
@ExtendWith(SuppressOutputExtension.class)
public class DetailsConverterTest {

  @Test
  @DisplayName("A null inside a list is reported as null rather than dropped")
  public void aNullInsideAListSurvives() {

    final var tree = JsonNodeFactory.instance.objectNode();
    tree.putArray("tags").add("first").addNull();

    final var details = DetailsConverter.toProtobuf(tree).getDetailsMap();

    assertTrue(details.get("tags").getIsArray());
    assertEquals("first", details.get("tags").getArrayValues(0).getStringValue());
    assertTrue(details.get("tags").getArrayValues(1).getNullValue());

  }

  @Test
  @DisplayName("A number travels as its decimal text, so nothing rounds it on the way")
  public void numbersTravelAsText() {

    final var details = DetailsConverter
        .toProtobuf(
            BusinessCockpitAssembly.objectMapper(),
            Map.of("amount", new BigDecimal("1234567890.12345")))
        .getDetailsMap();

    assertEquals(
        "1234567890.12345", details.get("amount").getArrayValues(0).getNumericValue());

  }

  @Test
  @DisplayName("Nothing to report becomes an empty map rather than a failure")
  public void nothingToReportIsEmpty() {

    assertEquals(
        0,
        DetailsConverter
            .toProtobuf(BusinessCockpitAssembly.objectMapper(), null)
            .getDetailsCount());

  }

  @Test
  @DisplayName("Details which are not a map of named values say so")
  public void detailsHaveToBeNamedValues() {

    final var message = assertThrows(
        IllegalArgumentException.class,
        () -> DetailsConverter.toProtobuf(JsonNodeFactory.instance.arrayNode()))
        .getMessage();

    assertTrue(message.contains("map of named values"), message);

  }

  @Test
  @DisplayName("A value of a shape the cockpit has no place for is named")
  public void anUnsupportedValueIsNamed() {

    final var tree = JsonNodeFactory.instance.objectNode();
    tree.putRawValue("odd", new com.fasterxml.jackson.databind.util.RawValue("<xml/>"));

    final var message = assertThrows(
        IllegalArgumentException.class,
        () -> DetailsConverter.toProtobuf(tree)).getMessage();

    assertTrue(message.contains("no place for"), message);

  }

}
