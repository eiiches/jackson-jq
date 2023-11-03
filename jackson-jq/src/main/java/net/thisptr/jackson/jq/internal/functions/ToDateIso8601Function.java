package net.thisptr.jackson.jq.internal.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.auto.service.AutoService;
import net.thisptr.jackson.jq.*;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.exception.JsonQueryTypeException;
import net.thisptr.jackson.jq.path.Path;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@AutoService(Function.class)
@BuiltinFunction({ "todateiso8601/0" })
public class ToDateIso8601Function implements Function {
    @Override
    public void apply(final Scope scope, final List<Expression> args, final JsonNode in, final Path ipath, final PathOutput output, final Version version) throws JsonQueryException {
        if (in.isTextual()) {
            output.emit(in, null);
        } else if (in.isNumber()) {
            try {
                long epochSeconds = in.longValue();
                ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(epochSeconds),
                        ZoneOffset.UTC
                );
                String formattedDateTime = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(zonedDateTime);
                output.emit(TextNode.valueOf(formattedDateTime), null);

            } catch (DateTimeParseException e) {
                throw new JsonQueryException(e);
            }
        } else {
            throw new JsonQueryTypeException("%s cannot be parsed as number of Epoch seconds", in);
        }
    }
}
