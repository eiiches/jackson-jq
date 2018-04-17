package net.thisptr.jackson.jq.extra.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import org.junit.Test;

public class NowFunctionTest {

  @Test
  public void testIfTimeIsInMillisEpoch() throws JsonQueryException {
    List<JsonNode> output = new NowFunction().apply(new Scope(), Collections.<JsonQuery> emptyList(), null);
    assertEquals(1 , output.size());
    assertEpochTimeStampIsValid(output.get(0).asLong());
  }

  private void assertEpochTimeStampIsValid(long timeInMillis) {
    Instant instant = Instant.ofEpochMilli(timeInMillis);
    Date date = Date.from(instant);
    //verify date is parseable
    assertNotNull(date);
    assertTrue(Date.from(Instant.now()).compareTo(date) != -1);

  }
}
