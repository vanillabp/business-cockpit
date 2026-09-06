package io.vanillabp.cockpit.bpms.kafka;

import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsArrayValue;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsValue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetailsMapper {

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
