import org.jspecify.annotations.NullMarked;

import net.thisptr.jackson.jq.v2.spi.module.Module;

@NullMarked
module net.thisptr.jackson.jq.v2.ext.module.binary {
	requires net.thisptr.jackson.jq.v2.json;
	requires transitive net.thisptr.jackson.jq.v2.spi;
	requires static org.jspecify;

	provides Module with
			net.thisptr.jackson.jq.v2.ext.binary.ModuleImpl;
}
