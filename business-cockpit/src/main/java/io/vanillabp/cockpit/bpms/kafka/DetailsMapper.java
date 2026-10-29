package io.vanillabp.cockpit.bpms.kafka;

import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsArrayValue;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsValue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the business data of a protobuf report into the plain maps, lists and values the cockpit
 * stores. Numbers become {@link java.math.BigDecimal}.
 */
public class DetailsMapper {

    /**
     * Creates the mapper. All its methods are static, so nothing needs an instance.
     */
    public DetailsMapper() {
    }

    static class NullableValueEntry<K, V> implements Map.Entry<K, V> {
        private K key;
        private V value;

        private NullableValueEntry() {}

        public static <K, V> Map.Entry<K, V> of(K key, V value) {
            final var result = new NullableValueEntry<K, V>();
            result.key = key;
            result.value = value;
            return result;
        }
        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            this.value = value;
            return this.value;
        }

        public void setKey(K key) {
            this.key = key;
        }
    }

    static Map<String, Object> mapMapValue(
            final DetailsMap detailsMap) {

        return detailsMap
                .getDetailsMap()
                .entrySet()
                .stream()
                .collect(HashMap::new,
                        (m, v) -> m.put(v.getKey(), mapArrayValue(v.getValue())),
                        HashMap::putAll);

    }

    /**
     * Turns one value of the business data into a Java value. A value which is no array is sent as an
     * array with one element, and this method unwraps it.
     *
     * @param arrayValue The value as it was reported
     * @return A list for an array, otherwise the single value. An empty list where a single value has
     *         no element.
     */
    protected static Object mapArrayValue(
            final DetailsArrayValue arrayValue) {

        if (!arrayValue.getIsArray()) {
            if (arrayValue.getArrayValuesCount() == 0) {
                return List.of();
            }
            return mapDetailsValue(arrayValue.getArrayValues(0));
        }

        return arrayValue
                .getArrayValuesList()
                .stream()
                .map(DetailsMapper::mapDetailsValue)
                .toList();

    }

    /**
     * Turns a single protobuf value into a Java value. A value of a type this mapper does not know
     * ends in an {@link UnsupportedOperationException}.
     *
     * @param value The value as it was reported
     * @return {@code null}, a {@link java.math.BigDecimal}, a string, a boolean or a map
     */
    protected static Object mapDetailsValue(
            final DetailsValue value) {

        if (value.hasNullValue()
                && value.getNullValue()) {
            return null;
        }

        if (value.hasNumericValue()) {
            return new BigDecimal(value.getNumericValue());
        }

        if (value.hasStringValue()) {
            return value.getStringValue();
        }

        if (value.hasBoolValue()) {
            return value.getBoolValue();
        }

        if (value.hasMapValue()) {
            return mapMapValue(value.getMapValue());
        }

        throw new UnsupportedOperationException(
                "Unsupported protobuf type in DetailsValue: "
                        + value);

    }

}
