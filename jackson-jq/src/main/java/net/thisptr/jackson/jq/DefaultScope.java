package net.thisptr.jackson.jq;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Default {@link Scope} implementation. Use {@link Scope#newEmptyScope()} to create a new reference.
 */
public class DefaultScope extends AbstractScope {

    DefaultScope(final Scope parentScope) {
        super(parentScope);
    }

    @JsonProperty("functions")
    private Map<String, String> debugFunctions() {
        final Map<String, String> result = new TreeMap<>();
        for (final Entry<String, Function> f : functions.entrySet()) {
            result.put(f.getKey(), f.getValue().toString());
        }
        return result;
    }
}
