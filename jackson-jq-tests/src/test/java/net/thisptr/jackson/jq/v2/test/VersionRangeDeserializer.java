package net.thisptr.jackson.jq.v2.test;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.core.VersionRange;

public class VersionRangeDeserializer extends StdDeserializer<VersionRange> {
	private static final long serialVersionUID = -4054473248484615401L;

	public VersionRangeDeserializer() {
		super(VersionRange.class);
	}

	@Override
	public @Nullable VersionRange deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		String text = p.readValueAs(String.class);
		if (text == null)
			return null;
		return VersionRange.valueOf(text);
	}
}
