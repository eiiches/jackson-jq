package net.thisptr.jackson.jq.extra.functions;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.IllegalJsonInputException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import org.junit.Test;

public class FromIso8601FunctionTest {


  @Test(expected = IllegalJsonInputException.class)
  public void failOnInvalidInputAsInt() throws JsonQueryException {
    new FromIso8601Function().apply(new Scope(), Collections.<JsonQuery> emptyList(), new IntNode(12));
  }

  @Test(expected = IllegalJsonInputException.class)
  public void failOnInvalidInputAsDouble() throws JsonQueryException {
    new FromIso8601Function().apply(new Scope(), Collections.<JsonQuery> emptyList(), new DoubleNode(123241324.1234));
  }


  @Test
  public void validInputs() throws JsonQueryException {

    assertEquals(expectedTimeInMillis("1970-01-01T00:00:00Z"),
        fromIso8601Function("1970-01-01T00:00:00Z"));

    assertEquals(expectedTimeInMillis("2011-12-03T10:15:30-07:00"),
        fromIso8601Function("2011-12-03T10:15:30-07:00"));

    assertEquals(expectedTimeInMillis("2018-12-03T10:15:30+07:00"),
        fromIso8601Function("2018-12-03T10:15:30+07:00"));

  }

  @Test
  public void validInputsWithOffset() throws JsonQueryException {

    assertEquals(expectedTimeInMillisWithZoneAdjustment("1970-01-01T00:00:00Z", 0),
        fromIso8601Function("1970-01-01T00:00:00Z", 0));

   assertEquals(expectedTimeInMillisWithZoneAdjustment("2011-12-03T10:15:30+00:00", -7),
        fromIso8601Function("2011-12-03T10:15:30+07:00", -7));

    assertEquals(expectedTimeInMillisWithZoneAdjustment("2018-12-03T10:15:30+17:00", 10),
        fromIso8601Function("2018-12-03T10:15:30+07:00", 10));

  }

  private long fromIso8601Function(String stringDate) throws JsonQueryException {
    return new FromIso8601Function().apply(new Scope(), Collections.<JsonQuery> emptyList(), new TextNode(stringDate)).get(0).asLong();
  }

  private long fromIso8601Function(String stringDate, int offset) throws JsonQueryException {
    final JsonQuery q = JsonQuery.compile("("+ offset + ")");
    List<JsonQuery> list = new ArrayList<>();
    list.add(q);
    return new FromIso8601Function().apply(new Scope(), list, new TextNode(stringDate)).get(0).asLong();
  }

  private static long expectedTimeInMillis(String expectedInputTimeAsString) {
    LocalDateTime expected = LocalDateTime.parse(expectedInputTimeAsString, DateTimeFormatter.ISO_DATE_TIME);
    return expected.toEpochSecond(ZoneOffset.UTC) * 1000;
  }

  private static long expectedTimeInMillisWithZoneAdjustment(String expectedInputTimeAsString, int zoneHoursOffset) {
    LocalDateTime expected = LocalDateTime.parse(expectedInputTimeAsString, DateTimeFormatter.ISO_DATE_TIME);
    return expected.toEpochSecond(ZoneOffset.ofHours(zoneHoursOffset)) * 1000;
  }

}
