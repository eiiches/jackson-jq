package net.thisptr.jackson.jq.v2.test.testcase;

import java.io.IOException;
import java.io.Serial;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

public class VersionRangeDeserializer extends StdDeserializer<VersionRange> {
	@Serial
	private static final long serialVersionUID = -4054473248484615401L;

	public VersionRangeDeserializer() {
		super(VersionRange.class);
	}

	@Override
	public @Nullable VersionRange deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		String text = p.readValueAs(String.class);
		if (text == null)
			return null;
		return VersionRange.valueOf(text);
	}
}
