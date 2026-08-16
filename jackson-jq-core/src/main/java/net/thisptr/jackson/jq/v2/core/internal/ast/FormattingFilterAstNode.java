package net.thisptr.jackson.jq.v2.core.internal.ast;

import net.thisptr.jackson.jq.v2.spi.Version;

public class FormattingFilterAstNode implements AstNode {
	private final String name;
	private final Version version;

	public FormattingFilterAstNode(String name, Version version) {
		this.name = name;
		this.version = version;
	}

	public String name() {
		return name;
	}

	@Override
	public String toString() {
		return "@" + name;
	}
}
