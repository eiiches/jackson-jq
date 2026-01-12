package net.thisptr.jackson.jq.gson;

import com.google.gson.JsonElement;

/**
 * A custom JsonElement representing jq's "missing" concept.
 * Gson doesn't have a native equivalent to Jackson's MissingNode,
 * so we create this singleton class to represent missing values.
 */
public final class GsonMissingNode extends JsonElement {
	private static final long serialVersionUID = 1L;

	public static final GsonMissingNode INSTANCE = new GsonMissingNode();

	private GsonMissingNode() {
	}

	@Override
	public JsonElement deepCopy() {
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this || obj instanceof GsonMissingNode;
	}

	@Override
	public int hashCode() {
		return GsonMissingNode.class.hashCode();
	}

	@Override
	public String toString() {
		return "missing";
	}
}
