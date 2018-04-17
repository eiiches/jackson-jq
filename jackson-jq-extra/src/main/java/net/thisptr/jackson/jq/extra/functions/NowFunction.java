package net.thisptr.jackson.jq.extra.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;

@BuiltinFunction({ "now/0" })
public class NowFunction implements Function {
  @Override
  public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
    return Collections.<JsonNode> singletonList(new LongNode(Instant.now().toEpochMilli()));
  }
}