package net.thisptr.jackson.jq.extra.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.auto.service.AutoService;
import net.thisptr.jackson.jq.*;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.extra.internal.misc.Preconditions;
import net.thisptr.jackson.jq.internal.misc.JsonNodeUtils;
import net.thisptr.jackson.jq.path.Path;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@AutoService(Function.class)
@BuiltinFunction({ "fromdateiso8601/0" })
public class FromDateIso8601Function implements Function {
    @Override
    public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
        Preconditions.checkInputType("fromdateiso8601", in, JsonNodeType.STRING);
        try {
            long epochSeconds = ZonedDateTime.parse(in.asText()).toEpochSecond();
            output.emit(JsonNodeUtils.asNumericNode(epochSeconds), null);
        } catch (DateTimeParseException e) {
            throw new JsonQueryException(e);
        }
    }
}
