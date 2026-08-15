package net.thisptr.jackson.jq.v2.core;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionFactory;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;

import static org.assertj.core.api.Assertions.assertThat;

// end-to-end test of custom function
public class CustomFunctionTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testCustomFunction() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Version version = Versions.JQ_1_6;

        Scope<JsonNode> rootScope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());

        BuiltinFunctionLoader.getInstance().loadFunctions(version, rootScope);

        rootScope.addFunctionFactory("times100", 1, new FunctionFactory() {
            @Override
            public <N> Function<N> createFunction(JsonProvider<N> jsonProvider, List<Expression> args, Version ver) {
                return (scope, in, path, output) -> {
                    args.get(0).apply(scope, in, (numberNode) -> {
                        JsonNode n = (JsonNode) numberNode;
                        assert (n.isIntegralNumber());
                        output.emit((N) new IntNode(n.asInt() * 100), null);
                    });
                };
            }
        });

        String input = "{ \"a\": 5 }";

        Scope<JsonNode> childScope = Scope.newChildScope(rootScope);

        JsonQuery query = JsonQuery.compile("{ \"a\": times100(.a) }", version);

        List<JsonNode> out = new ArrayList<>();
        query.apply(childScope, mapper.readTree(input), out::add);
        assertThat(out).hasSize(1);
        assertThat(out.get(0)).isInstanceOf(ObjectNode.class);
        assertThat(out.get(0).toString()).isEqualTo("{\"a\":500}");
    }
}
