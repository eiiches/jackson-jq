package net.thisptr.jackson.jq.v2.core.internal.tree;

public class LabelPipeComponent<JsonNode> implements PipeComponent<JsonNode> {
	public final String name;

	public LabelPipeComponent(String name) {
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
