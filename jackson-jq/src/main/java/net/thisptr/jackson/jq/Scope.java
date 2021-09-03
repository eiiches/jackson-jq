package net.thisptr.jackson.jq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.thisptr.jackson.jq.path.Path;

public interface Scope {

    static Scope newEmptyScope() {
        return new DefaultScope(null);
    }

    static Scope newChildScope(final Scope scope) {
        return new DefaultScope(scope);
    }

    void addFunction(final String name, final int n, final Function q);

    void addFunction(final String name, final Function q);

    Function getFunction(final String name, final int nargs);

    Function getFunctionRecursive(final String name);

    void setValue(final String name, final JsonNode value);

    void setValueWithPath(final String name, final JsonNode value, final Path path);

    ValueWithPath getValueWithPath(final String name);

    JsonNode getValue(final String name);

    ObjectMapper getObjectMapper();

    ObjectMapperProvider getObjectMapperProvider();

    interface ValueWithPath {

        JsonNode value();

        Path path();
    }
}
