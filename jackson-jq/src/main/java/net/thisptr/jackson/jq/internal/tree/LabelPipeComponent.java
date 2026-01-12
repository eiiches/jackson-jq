package net.thisptr.jackson.jq.internal.tree;

public class LabelPipeComponent<JsonNode> implements PipeComponent<JsonNode> {
	public final String name;

	public LabelPipeComponent(final String name) {
		this.name = name;
	}

	@Override
	public boolean canTerminatePipe() {
		return false;
	}

	@Override
	public String toString() {
		return "label $" + name;
	}
}
