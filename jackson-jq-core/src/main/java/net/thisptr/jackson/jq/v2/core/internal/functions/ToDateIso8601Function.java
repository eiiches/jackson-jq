package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.misc.Preconditions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.PathOutput;
import net.thisptr.jackson.jq.v2.spi.Scope;
import net.thisptr.jackson.jq.v2.spi.Version;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;

@AutoService(Function.class)
@FunctionRegistration({ "todateiso8601/0" })
public class ToDateIso8601Function implements Function  {
    @Override
    public <JsonNode> void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
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
