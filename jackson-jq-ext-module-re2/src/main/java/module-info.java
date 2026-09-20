import org.jspecify.annotations.NullMarked;

import net.thisptr.jackson.jq.v2.spi.module.Module;

@NullMarked
module net.thisptr.jackson.jq.v2.ext.module.re2 {
	requires re2j;
	requires net.thisptr.jackson.jq.v2.json;
	requires transitive net.thisptr.jackson.jq.v2.spi;
	requires static org.jspecify;

	provides Module with
			net.thisptr.jackson.jq.v2.ext.re2.ModuleImpl,
			net.thisptr.jackson.jq.v2.ext.re2.InternalModuleImpl;
}
