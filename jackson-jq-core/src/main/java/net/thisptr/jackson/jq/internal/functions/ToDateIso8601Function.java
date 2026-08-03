package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.auto.service.AutoService;
import net.thisptr.jackson.jq.*;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.misc.Preconditions;
import net.thisptr.jackson.jq.path.Path;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;

@AutoService(Function.class)
@BuiltinFunction({ "todateiso8601/0" })
public class ToDateIso8601Function implements Function  {
    @Override
    public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
        Preconditions.checkInputType("todateiso8601", in, JsonNodeType.NUMBER);
        try {
            long epochSeconds = in.asLong();
            String iso8601String = Instant.ofEpochSecond(epochSeconds).toString();
            output.emit(new TextNode(iso8601String), null);
        } catch (DateTimeException e) {
            throw new JsonQueryException(e);
        }        
    }
}