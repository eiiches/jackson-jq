package net.thisptr.jackson.jq.extra.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import net.thisptr.jackson.jq.Function;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import net.thisptr.jackson.jq.internal.BuiltinFunction;
import net.thisptr.jackson.jq.internal.misc.Preconditions;

/**
 * Excerpt from JQ documentation:
 * jq provides some basic date handling functionality, with some high-level and low-level builtins.  In all cases these
 * builtins deal exclusively with time in UTC.

  The `fromdateiso8601` builtin parses datetimes in the ISO 8601 format to a number of seconds since the Unix epoch
 (1970-01-01T00:00:00Z).  The `todateiso8601` builtin does the inverse.

 * This implementation has the default behaviour as described above
 */
@BuiltinFunction({ "todateiso8601/0"})
public class ToIso8601Function implements Function {

  @Override
  public List<JsonNode> apply(final Scope scope, final List<JsonQuery> args, final JsonNode in) throws JsonQueryException {
    Preconditions.checkInputType("todateiso8601", in, JsonNodeType.NUMBER);
    try{
      String formattedDate = getFormattedDate(in.asLong());
      return Collections.<JsonNode> singletonList(new TextNode(formattedDate));
    } catch (Exception e) {
      e.printStackTrace();
      throw new JsonQueryException(e);
    }
  }

  private static String getFormattedDate(long timeInMillis) {
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    Instant instant = Instant.ofEpochMilli(timeInMillis);
    return sdf.format(Date.from(instant));
  }

}