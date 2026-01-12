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
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@AutoService(Function.class)
@BuiltinFunction({ "fromdateiso8601/0" })
public class FromDateIso8601Function<JsonNode> implements Function<JsonNode> {
    @Override
    public void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
        final JsonProvider<JsonNode> jsonProvider = scope.jsonProvider();
        Preconditions.checkInputType(jsonProvider, "fromdateiso8601", in, JsonNodeType.STRING);
        try {
            String iso8601String = jsonProvider.asText(in);
            // In future versions of JQ, it may need to be revisited due to fractional support: https://github.com/jqlang/jq/issues/1409
            if (iso8601String.length() > 20) {
                throw new JsonQueryException(String.format("date \"%s\" does not match format \"%%Y-%%m-%%dT%%H:%%M:%%SZ\"", iso8601String));
            }
            long epochSeconds = Instant.parse(iso8601String).getEpochSecond();
            output.emit(JsonNodeUtils.asNumericNode(jsonProvider, epochSeconds), null);
        } catch (DateTimeParseException e) {
            throw new JsonQueryException(e);
        }
    }
}