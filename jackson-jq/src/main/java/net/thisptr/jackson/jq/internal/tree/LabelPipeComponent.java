package net.thisptr.jackson.jq.internal.tree;

public class LabelPipeComponent implements PipeComponent {
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
