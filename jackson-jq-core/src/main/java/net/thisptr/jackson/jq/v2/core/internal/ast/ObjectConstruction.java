package net.thisptr.jackson.jq.v2.core.internal.ast;

import java.util.ArrayList;
import java.util.List;

import com.google.errorprone.annotations.Var;

public class ObjectConstruction implements AstNode {
	public final List<FieldConstructionAst> fields = new ArrayList<>();

	public ObjectConstruction() {}

	public void add(FieldConstructionAst field) {
		fields.add(field);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		@Var String sep = "";
		for (FieldConstructionAst field : fields) {
			builder.append(sep);
			builder.append(field);
			sep = ",";
		}
		builder.append("}");
		return builder.toString();
	}
}
