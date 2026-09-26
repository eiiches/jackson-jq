import org.jspecify.annotations.NullMarked;

import net.thisptr.jackson.jq.v2.spi.module.Module;

@NullMarked
module net.thisptr.jackson.jq.v2.ext.module.joni {
	requires org.jruby.joni;
	requires net.thisptr.jackson.jq.v2.json;
	requires transitive net.thisptr.jackson.jq.v2.spi;
	requires static org.jspecify;

	exports net.thisptr.jackson.jq.v2.ext.joni;

	provides Module with
			net.thisptr.jackson.jq.v2.ext.joni.JoniRegexModule,
			net.thisptr.jackson.jq.v2.ext.joni.InternalModuleImpl;
}
