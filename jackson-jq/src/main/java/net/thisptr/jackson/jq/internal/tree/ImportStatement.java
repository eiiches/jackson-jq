package net.thisptr.jackson.jq.internal.tree;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.internal.utils.ExpressionUtils;

public class ImportStatement {
	public final String path;
	public final boolean dollarImport;
	public final String name;
	public final JsonNode metadata;

	public ImportStatement(final String path, final boolean dollarImport, final String name, final Expression metadataExpr) {
		this.path = path;
		this.dollarImport = dollarImport;
		this.name = name;

		if (metadataExpr != null) {
			this.metadata = ExpressionUtils.evaluateLiteralExpression(metadataExpr);
			if (metadata == null)
				throw new IllegalArgumentException("Module metadata must be constant");
			if (!metadata.isObject())
				throw new IllegalArgumentException("Module metadata must be an object");
		} else {
			this.metadata = null;
		}
	}

	@Override
	public String toString() {
		final StringBuilder s = new StringBuilder();
		s.append("import ");
		s.append(new TextNode(path).toString());
		s.append(" as ");
		if (dollarImport)
			s.append('$');
		s.append(name);
		if (metadata != null) {
			s.append(' ');
			s.append(metadata);
		}
		return s.toString();
	}
}
