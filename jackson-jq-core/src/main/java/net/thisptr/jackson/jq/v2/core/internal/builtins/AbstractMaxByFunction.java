package net.thisptr.jackson.jq.v2.core.internal.builtins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.function.utils.FunctionBody;
import net.thisptr.jackson.jq.v2.core.internal.function.utils.Preconditions;
import net.thisptr.jackson.jq.v2.core.internal.json.JsonNodeUtils;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.Cardinality;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.path.UntrackedPath;
import net.thisptr.jackson.jq.v2.spi.version.Version;

public abstract class AbstractMaxByFunction implements Function {

	private String fname;

	public AbstractMaxByFunction(String fname) {
		this.fname = fname;
	}

	@Override
	public <Context, JsonNode> Expression<Context, JsonNode> bindArguments(JsonProvider<JsonNode> jsonProvider, List<Expression<Context, JsonNode>> args, Version version) {
		return FunctionBody.builder(args).usesInput(true).cardinality(Cardinality.ONE).build((frame, in, ipath, output) -> {
			Preconditions.checkInputType(jsonProvider, fname, in, JsonNodeType.ARRAY);

			@Var JsonNode maxItem = jsonProvider.createNull();
			@Var JsonNode maxValue = jsonProvider.createNull();
			@Var boolean seen = false;
			Iterator<JsonNode> iter = jsonProvider.getArrayElements(in);
			while (iter.hasNext()) {
				JsonNode i = iter.next();
				List<JsonNode> valueList = new ArrayList<>();
				args.get(0).apply(frame, i, UntrackedPath.getInstance(), (v, opath) -> valueList.add(v));
				JsonNode value = JsonNodeUtils.asArrayNode(jsonProvider, valueList);
				if (!seen || !isLarger(jsonProvider, maxValue, value)) {
					maxValue = value;
					maxItem = i;
					seen = true;
				}
			}

			output.emit(maxItem, UntrackedPath.getInstance());
		});
	}

	protected abstract <JsonNode> boolean isLarger(JsonProvider<JsonNode> jsonProvider, JsonNode criteria, JsonNode value);
}
