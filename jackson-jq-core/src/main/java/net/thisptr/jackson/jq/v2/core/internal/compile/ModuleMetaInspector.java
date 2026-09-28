package net.thisptr.jackson.jq.v2.core.internal.compile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.errorprone.annotations.Var;

import net.thisptr.jackson.jq.v2.core.internal.ast.AstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.FunctionDefinitionAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.SemicolonOperatorAstNode;
import net.thisptr.jackson.jq.v2.core.internal.ast.TopLevelAstNode;
import net.thisptr.jackson.jq.v2.core.internal.module.SimpleModuleMeta;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.internal.javacc.AstParser;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.module.JavaModule;
import net.thisptr.jackson.jq.v2.spi.module.JqModule;
import net.thisptr.jackson.jq.v2.spi.module.Module;
import net.thisptr.jackson.jq.v2.spi.module.ModuleMeta;
import net.thisptr.jackson.jq.v2.spi.version.Version;

/**
 * Builds jq's modulemeta value from a raw module, without compiling its source.
 */
final class ModuleMetaInspector {
	private ModuleMetaInspector() {
	}

	static <JsonNode> JsonNode inspect(Module module, JsonProvider<JsonNode> jsonProvider, Version jqVersion) {
		@Var ModuleMeta moduleMeta = module.getModuleMeta();
		boolean includeDefinitions = jqVersion.compareTo(Versions.JQ_1_7) >= 0;
		Set<FunctionSignature> definitions = new LinkedHashSet<>();
		if (module instanceof JqModule jqModule) {
			AstNode ast = AstParser.parse(jqModule.getSourceCode() + " null", jqVersion);
			moduleMeta = SimpleModuleMeta.fromAst(ast);
			if (includeDefinitions)
				collectDefinitions(ast instanceof TopLevelAstNode top ? top.expr() : ast, definitions);
		}
		if (includeDefinitions && module instanceof JavaModule javaModule)
			javaModule.getFunctions().keySet().stream().sorted(Comparator.comparing(FunctionSignature::toString)).forEach(definitions::add);

		Map<String, JsonNode> result = new LinkedHashMap<>(moduleMeta.getMetadata(jsonProvider));
		List<JsonNode> dependencies = new ArrayList<>();
		for (ModuleMeta.Dependency dependency : moduleMeta.getDependencies()) {
			Map<String, JsonNode> entry = new LinkedHashMap<>(dependency.getImportMetadata(jsonProvider));
			if (dependency.getAlias() != null)
				entry.put("as", jsonProvider.createString(dependency.getAlias()));
			entry.put("is_data", jsonProvider.createBoolean(dependency.isData()));
			entry.put("relpath", jsonProvider.createString(dependency.getRelpath()));
			dependencies.add(jsonProvider.createObject(entry));
		}
		result.put("deps", jsonProvider.createArray(dependencies));
		if (includeDefinitions) {
			List<JsonNode> defs = definitions.stream().map(signature -> jsonProvider.createString(signature.toString())).toList();
			result.put("defs", jsonProvider.createArray(defs));
		}
		return jsonProvider.createObject(result);
	}

	private static void collectDefinitions(AstNode expression, Set<FunctionSignature> definitions) {
		if (expression instanceof FunctionDefinitionAstNode definition) {
			definitions.add(definition.signature());
		} else if (expression instanceof SemicolonOperatorAstNode sequence) {
			for (AstNode part : sequence.expressions()) {
				if (part instanceof FunctionDefinitionAstNode definition)
					definitions.add(definition.signature());
			}
		}
	}
}
