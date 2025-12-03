package net.thisptr.jackson.jq.internal.misc;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

import net.thisptr.jackson.jq.VersionRange;

public class VersionRangeDeserializer extends StdDeserializer<VersionRange> {
	private static final long serialVersionUID = -4054473248484615401L;

	public VersionRangeDeserializer() {
		super(VersionRange.class);
	}

	@Override
	public VersionRange deserialize(final JsonParser p, final DeserializationContext ctxt) {
		final String text = p.getString();
		if (text == null)
			return null;
		return VersionRange.valueOf(text);
	}
}
