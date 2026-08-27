import org.jspecify.annotations.NullMarked;

@NullMarked
module net.thisptr.jackson.jq.v2.ext.module.debug {
	requires static org.jspecify;
	requires static com.google.auto.service;
	requires transitive net.thisptr.jackson.jq.v2.spi;
	requires net.thisptr.jackson.jq.v2.json;

	provides net.thisptr.jackson.jq.v2.spi.module.Module with
			net.thisptr.jackson.jq.v2.ext.debug.ModuleImpl;
}
