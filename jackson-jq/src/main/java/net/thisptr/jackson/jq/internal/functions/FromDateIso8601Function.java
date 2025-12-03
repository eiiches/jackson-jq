package net.thisptr.jackson.jq.internal.functions;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeType;
import com.google.auto.service.AutoService;
import net.thisptr.jackson.jq.*;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@AutoService(Function.class)
@BuiltinFunction({ "fromdateiso8601/0" })
public class FromDateIso8601Function implements Function {
    @Override
    public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
        Preconditions.checkInputType("fromdateiso8601", in, JsonNodeType.STRING);
        try {
            String iso8601String = in.asString();
            // In future versions of JQ, it may need to be revisited due to fractional support: https://github.com/jqlang/jq/issues/1409
            if (iso8601String.length() > 20) {
                throw new JsonQueryException(String.format("date \"%s\" does not match format \"%%Y-%%m-%%dT%%H:%%M:%%SZ\"", iso8601String));
            }
            long epochSeconds = Instant.parse(iso8601String).getEpochSecond();
            output.emit(JsonNodeUtils.asNumericNode(epochSeconds), null);
        } catch (DateTimeParseException e) {
            throw new JsonQueryException(e);
        }
    }
}