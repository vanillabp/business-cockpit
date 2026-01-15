package io.vanillabp.cockpit.adapter.camunda8.utils;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.JsonMapper;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

public interface AggregateIdParser {

    @FunctionalInterface
    interface AggregateIdParserFunction {
        Object parse(long processInstanceKey, Object variableValue, boolean valueIsJson);
    };
    
    Logger getLogger();

    /**
     * Events other than "created" do not include variables. That's why they need to be loaded in addition.
     */
    default Map<String, Object> loadBusinessKeyVariablesForProcessInstanceKey(
            final CamundaClient client,
            final String aggregateIdPropertyName,
            final AggregateIdParserFunction parseWorkflowAggregateIdFromBusinessKey,
            final long processInstanceKey) {

        final var businessKeyVariable = client
                .newVariableSearchRequest()
                .filter(filter -> filter
                        .processInstanceKey(processInstanceKey)
                        .name(aggregateIdPropertyName))
                .withFullValues()
                .send()
                .join()
                .singleItem();

        if (businessKeyVariable == null) {
            throw new RuntimeException("Variables for process instance "
                    + processInstanceKey
                    + " are not yet available!");
        }

        final var parsedBusinessKey = parseWorkflowAggregateIdFromBusinessKey
                .parse(processInstanceKey, businessKeyVariable.getValue(),
                        // result of variable queries are JSON strings e.g. "a string" is ""a string""
                        true);

        return Map.of(businessKeyVariable.getName(), parsedBusinessKey);

    }


    /**
     * Variables provided by Zeebe events are objects, not JSON. That's why they need to be transformed
     * into the actual ID's class, not parsed.
     */
    default Map<String, Object> transformBusinessKeyInGivenVariables(
            final String aggregateIdPropertyName,
            final AggregateIdParserFunction parseWorkflowAggregateIdFromBusinessKey,
            final long processInstanceKey,
            final Map<String, Object> variables) {

        final var idObject = variables.get(aggregateIdPropertyName);

        final var adoptedVariables = new HashMap<String, Object>();
        variables
                .entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals(aggregateIdPropertyName))
                // cannot use 'collect' because map values may be null what is not allowed when using 'collect'
                .forEach(entry -> adoptedVariables.put(entry.getKey(), entry.getValue()));

        if (idObject == null) {
            return adoptedVariables;
        }

        final var id = parseWorkflowAggregateIdFromBusinessKey
                .parse(processInstanceKey, idObject, false);
        adoptedVariables.put(aggregateIdPropertyName, id);

        return adoptedVariables;

    }

    private Object parseBusinessKeyVariable(
            final JsonMapper camundaJsonMapper,
            final long processInstanceKey,
            final String name,
            final Class<?> type,
            final Object value,
            final boolean valueIsJson) {

        try {
            if (value == null) {
                return null;
            }
            if (valueIsJson) {
                return camundaJsonMapper.fromJson(value.toString(), type);
            } else {
                return camundaJsonMapper.transform(value, type);
            }
        } catch (Exception e) {
            getLogger().warn("Could not deserialize business key of process instance '{}' of variable '{}' from JSON to String: {}",
                    processInstanceKey, name, value);
            return null;
        }

    }
    
    default AggregateIdParserFunction determineBusinessKeyToIdMapper(
            final JsonMapper camundaJsonMapper,
            final Class<?> workflowAggregateClass,
            final Class<?> workflowAggregateIdClass,
            final String aggregateIdPropertyName) {

        if (String.class.isAssignableFrom(workflowAggregateIdClass)) {
            return (processInstanceKey, variableValue, valueIsJson) ->
                    parseBusinessKeyVariable(camundaJsonMapper, processInstanceKey, aggregateIdPropertyName, String.class, variableValue, valueIsJson);
        } else if (int.class.isAssignableFrom(workflowAggregateIdClass)) {
            return (processInstanceKey, variableValue, valueIsJson) ->
                    parseBusinessKeyVariable(camundaJsonMapper, processInstanceKey, aggregateIdPropertyName, Integer.class, variableValue, valueIsJson);
        } else if (long.class.isAssignableFrom(workflowAggregateIdClass)) {
            return (processInstanceKey, variableValue, valueIsJson) ->
                    parseBusinessKeyVariable(camundaJsonMapper, processInstanceKey, aggregateIdPropertyName, Long.class, variableValue, valueIsJson);
        } else if (float.class.isAssignableFrom(workflowAggregateIdClass)) {
            return (processInstanceKey, variableValue, valueIsJson) ->
                    parseBusinessKeyVariable(camundaJsonMapper, processInstanceKey, aggregateIdPropertyName, Float.class, variableValue, valueIsJson);
        } else if (double.class.isAssignableFrom(workflowAggregateIdClass)) {
            return (processInstanceKey, variableValue, valueIsJson) ->
                    parseBusinessKeyVariable(camundaJsonMapper, processInstanceKey, aggregateIdPropertyName, Double.class, variableValue, valueIsJson);
        } else if (byte.class.isAssignableFrom(workflowAggregateIdClass)) {
            return (processInstanceKey, variableValue, valueIsJson) ->
                    parseBusinessKeyVariable(camundaJsonMapper, processInstanceKey, aggregateIdPropertyName, Byte.class, variableValue, valueIsJson);
        } else if (BigInteger.class.isAssignableFrom(workflowAggregateIdClass)) {
            return (processInstanceKey, variableValue, valueIsJson) ->
                    parseBusinessKeyVariable(camundaJsonMapper, processInstanceKey, aggregateIdPropertyName, BigInteger.class, variableValue, valueIsJson);
        } else {

            try {
                final var valueOfMethod = workflowAggregateIdClass.getMethod("valueOf", String.class);
                return (processInstanceKey, variableValue, valueIsJson) -> {
                    try {
                        final String value;
                        if (valueIsJson) {
                            value = variableValue.toString();
                        } else {
                            value = camundaJsonMapper.toJson(variableValue);
                        }
                        return valueOfMethod.invoke(null, value);
                    } catch (Exception e) {
                        getLogger().warn("Could not determine business key of process instance '{}' of variable '{}' from JSON to String: {}",
                                processInstanceKey, aggregateIdPropertyName, variableValue, e);
                        return null;
                    }
                };
            } catch (Exception e) {
                throw new RuntimeException(
                        String.format(
                                """
                                The id's class '%s' of the workflow-aggregate '%s' does not implement a method
                                   public static %s valueOf(String businessKey)
                                to transform JSON representation into the aggregate's ID!
                                Please add this method required by VanillaBP 'camunda8' Business Cockpit adapter.""",
                                workflowAggregateIdClass.getName(),
                                workflowAggregateClass,
                                workflowAggregateIdClass.getSimpleName()));
            }
        }

    }
    
}
