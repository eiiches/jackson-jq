package net.thisptr.jackson.jq.extra.functions;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.Collections;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.exception.IllegalJsonInputException;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import org.junit.Test;

public class ToIso8601FunctionTest {


  @Test(expected = IllegalJsonInputException.class)
  public void failOnInvalidInputAsText() throws JsonQueryException {
    new ToIso8601Function().apply(new Scope(), Collections.<JsonQuery> emptyList(), new TextNode("some-text"));
  }

  @Test
  public void validInputs() throws JsonQueryException {
    assertEquals("1970-01-17T11:59:59Z", toIso8601Function(1425599507l));
    assertEquals("2011-12-03T10:15:30Z", toIso8601Function(1322907330000l));
  }

  private String toIso8601Function(long timeinMillis) throws JsonQueryException {
    return new ToIso8601Function().apply(new Scope(), Collections.<JsonQuery> emptyList(), new LongNode(timeinMillis)).get(0).asText();
  }

}
