import org.jspecify.annotations.NullMarked;

@NullMarked
module net.thisptr.jackson.jq.v2.core {
	requires transitive net.thisptr.jackson.jq.v2.json;
	requires transitive net.thisptr.jackson.jq.v2.spi;
	requires static transitive org.jspecify;

	exports net.thisptr.jackson.jq.v2.core;
	exports net.thisptr.jackson.jq.v2.core.diagnostic;
	exports net.thisptr.jackson.jq.v2.core.function;
	exports net.thisptr.jackson.jq.v2.core.function.loaders;
	exports net.thisptr.jackson.jq.v2.core.module;
	exports net.thisptr.jackson.jq.v2.core.module.loaders;
	exports net.thisptr.jackson.jq.v2.core.version;

	opens net.thisptr.jackson.jq.v2.core.internal.commons.pair to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.commons.range to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.compile to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.compile.freevars to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.misc to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.binaryop to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.assignment to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.binaryop.comparison to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.fieldaccess to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.literal to net.thisptr.jackson.jq.v2.ext.module.debug;
	opens net.thisptr.jackson.jq.v2.core.internal.tree.matcher to net.thisptr.jackson.jq.v2.ext.module.debug;

	uses net.thisptr.jackson.jq.v2.spi.Function;
	uses net.thisptr.jackson.jq.v2.spi.JqLibrary;
	uses net.thisptr.jackson.jq.v2.spi.module.Module;
}
