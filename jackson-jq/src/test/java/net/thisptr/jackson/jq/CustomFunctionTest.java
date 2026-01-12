package net.thisptr.jackson.jq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.jackson2.Jackson2JsonProviderImpl;
import net.thisptr.jackson.jq.path.Path;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// end-to-end test of custom function
public class CustomFunctionTest {

    @Test
    public void testCustomFunction() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Version version = Versions.JQ_1_6;

        Scope<JsonNode> rootScope = Scope.newEmptyScope(Jackson2JsonProviderImpl.getInstance());

        BuiltinFunctionLoader.getInstance().loadFunctions(version, rootScope);

        rootScope.addFunction("times100", 1, new Function<JsonNode>() {
            @Override
            public void apply(Scope<JsonNode> scope, List<Expression<JsonNode>> args, JsonNode in, Path<JsonNode> path, PathOutput<JsonNode> output, Version version) throws JsonQueryException {
                args.get(0).apply(scope, in, (numberNode) -> {
                    assert (numberNode.isIntegralNumber());
                    output.emit(new IntNode(numberNode.asInt() * 100), null);
                });
            }
        });

        String input = "{ \"a\": 5 }";

        Scope<JsonNode> childScope = Scope.newChildScope(rootScope);

        JsonQuery<JsonNode> query = JsonQuery.compile("{ \"a\": times100(.a) }", version);

        final List<JsonNode> out = new ArrayList<>();
        query.apply(childScope, mapper.readTree(input), out::add);
        assertThat(out).hasSize(1);
        assertThat(out.get(0)).isInstanceOf(ObjectNode.class);
        assertThat(out.get(0).toString()).isEqualTo("{\"a\":500}");
    }
}
