package net.thisptr.jackson.jq;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.thisptr.jackson.jq.path.Path;

/**
 * Base {@link Scope} implementation without Jackson's ObjectMapper Initialization
 */
public abstract class AbstractScope implements Scope {

    @JsonIgnore
    private final ObjectMapperProvider objectMapperProvider;
    @JsonIgnore
    protected Map<String, Function> functions;
    @JsonProperty("parent")
    private final Scope parentScope;
    @JsonProperty("variables")
    private Map<String, ValueWithPath> values;

    protected AbstractScope(final Scope parentScope) {
        this.parentScope = parentScope;
        if (parentScope != null && parentScope.getObjectMapperProvider() != null) {
            this.objectMapperProvider = parentScope.getObjectMapperProvider();
        } else {
            this.objectMapperProvider = new ObjectMapperDefaultProvider();
        }
    }

    protected AbstractScope(final Scope parentScope, ObjectMapperProvider objectMapperProvider) {
        this.parentScope = parentScope;
        this.objectMapperProvider = objectMapperProvider;
    }

    public void addFunction(final String name, final int n, final Function q) {
        addFunction(name + "/" + n, q);
    }

    public void addFunction(final String name, final Function q) {
        if (functions == null) {
            functions = new HashMap<>();
        }
        functions.put(name, q);
    }

    public Function getFunction(final String name, final int nargs) {
        final Function f = getFunctionRecursive(name + "/" + nargs);
        if (f != null) {
            return f;
        }
        return getFunctionRecursive(name);
    }

    public Function getFunctionRecursive(final String name) {
        if (functions != null) {
            final Function q = functions.get(name);
            if (q != null) {
                return q;
            }
        }
        if (parentScope == null) {
            return null;
        }
        return parentScope.getFunctionRecursive(name);
    }

    public void setValue(final String name, final JsonNode value) {
        setValueWithPath(name, value, null);
    }

    public void setValueWithPath(final String name, final JsonNode value, final Path path) {
        if (values == null) {
            values = new HashMap<>();
        }
        values.put(name, new ValueWithPathImpl(value, path));
    }

    public ValueWithPath getValueWithPath(final String name) {
        if (values != null) {
            final ValueWithPath value = values.get(name);
            if (value != null) {
                return value;
            }
        }
        if (parentScope == null) {
            return null;
        }
        return parentScope.getValueWithPath(name);
    }

    public JsonNode getValue(final String name) {
        final ValueWithPath value = getValueWithPath(name);
        if (value == null) {
            return null;
        }
        return value.value();
    }

    private static class ValueWithPathImpl implements ValueWithPath {

        @JsonProperty("value")
        private final JsonNode value;

        @JsonProperty("path")
        private final Path path;

        public ValueWithPathImpl(final JsonNode value, final Path path) {
            this.value = value;
            this.path = path;
        }

        @Override
        public JsonNode value() {
            return value;
        }

        @Override
        public Path path() {
            return path;
        }
    }

    public Map<String, Function> getFunctions() {
        return functions;
    }

    public Map<String, ValueWithPath> getValues() {
        return values;
    }

    public void setFunctions(Map<String, Function> functions) {
        this.functions = functions;
    }

    public void setValues(Map<String, ValueWithPath> values) {
        this.values = values;
    }

    // we don't need to have the ObjectMapper reference in memory if it's not being used
    @Override
    @JsonIgnore
    public ObjectMapper getObjectMapper() {
        return this.objectMapperProvider.get();
    }

    @Override
    @JsonIgnore
    public ObjectMapperProvider getObjectMapperProvider() {
        return objectMapperProvider;
    }
}
