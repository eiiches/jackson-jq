package net.thisptr.jackson.jq.v2.core.internal.functions;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.google.auto.service.AutoService;

import net.thisptr.jackson.jq.v2.core.internal.misc.JsonNodeUtils;
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
@FunctionRegistration({ "fromdateiso8601/0" })
public class FromDateIso8601Function implements Function {
    @Override
    public <JsonNode> void apply(final Scope<JsonNode> scope, final List<Expression<JsonNode>> args, final JsonNode in, final Path<JsonNode> ipath, final PathOutput<JsonNode> output, final Version version) throws JsonQueryException {
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
