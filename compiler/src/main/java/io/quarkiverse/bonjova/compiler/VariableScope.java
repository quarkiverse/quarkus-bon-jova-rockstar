package io.quarkiverse.bonjova.compiler;

import io.quarkus.gizmo.FieldDescriptor;

import java.util.HashMap;
import java.util.Map;

public class VariableScope {

    private final Map<String, FieldDescriptor> variables = new HashMap<>();

    public FieldDescriptor get(String variableName) {
        return variables.get(variableName);
    }

    public Object[] getAllKnownVariables() {
        return variables.keySet().toArray();
    }

    public void put(String variableName, FieldDescriptor field) {
        variables.put(variableName, field);
    }
}
