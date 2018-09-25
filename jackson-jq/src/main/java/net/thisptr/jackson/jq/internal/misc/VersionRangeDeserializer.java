package net.thisptr.jackson.jq.internal.misc;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import net.thisptr.jackson.jq.VersionRange;

public class VersionRangeDeserializer extends StdDeserializer<VersionRange> {
	private static final long serialVersionUID = -4054473248484615401L;

	public VersionRangeDeserializer() {
		super(VersionRange.class);
	}

	@Override
	public VersionRange deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
		final String text = p.readValueAs(String.class);
		if (text == null)
			return null;
		return VersionRange.valueOf(text);
	}
}
