package net.thisptr.jackson.jq.internal.functions;

import com.google.auto.service.AutoService;
import net.thisptr.jackson.jq.BuiltinFunction;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonNodeType;
import net.thisptr.jackson.jq.JsonProvider;
import net.thisptr.jackson.jq.PathOutput;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;

@AutoService(Function.class)
@BuiltinFunction({ "todateiso8601/0" })
public class ToDateIso8601Function<JsonNode> implements Function<JsonNode>  {
    @Override
    public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
        final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
        Preconditions.checkInputType(jsonProvider, "todateiso8601", in, JsonNodeType.NUMBER);
        final double epochDouble = jsonProvider.asDouble(in);
        if (Double.isNaN(epochDouble))
            throw new JsonQueryException("todateiso8601 cannot be applied to nan");
        if (Double.isInfinite(epochDouble))
            throw new JsonQueryException("todateiso8601 cannot be applied to infinite");
        try {
            long epochSeconds = (long) epochDouble;
            String iso8601String = Instant.ofEpochSecond(epochSeconds).toString();
            output.emit(jsonProvider.createString(iso8601String), null);
        } catch (DateTimeException e) {
            throw new JsonQueryException(e);
        }
    }
}