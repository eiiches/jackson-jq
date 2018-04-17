package net.thisptr.jackson.jq.extra.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.LongNode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

/**
 * Excerpt from jq documentation:
 * The `fromdateiso8601` builtin parses datetimes in the ISO 8601 format to a number of seconds since the Unix epoch
 *(1970-01-01T00:00:00Z).
 *
 * This implementation has the default behaviour as described above and an additional
 * variant that accepts zoneHours as argument in case the timezone being matched is different than current timezone.
 */
@BuiltinFunction({ "fromdateiso8601/0", "fromdateiso8601/1"})
public class FromIso8601Function implements Function {

  @Override
  public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
    Preconditions.checkInputType("fromdateiso8601", in, JsonNodeType.STRING);

    final List<JsonNode> out = new ArrayList<>();

    try{
      LocalDateTime date = LocalDateTime.parse(in.asText(), DateTimeFormatter.ISO_DATE_TIME);
      Instant is = null;

      if(args.size() == 1) {
        for (final JsonNode offset : args.get(0).apply(in)) {
          if (!offset.canConvertToInt()) throw JsonQueryException.format("Offset must be an integer");
            is = date.toInstant(ZoneOffset.ofHours(offset.intValue()));
        }
      } else {
        is = date.toInstant(ZoneOffset.UTC);
      }
      out.add(new LongNode(is.toEpochMilli()));

    } catch (Exception e) {
      throw new JsonQueryException(e);
    }
    return out;
  }

}