module net.thisptr.jackson.jq.v2.ext.module.random {
	requires static com.google.auto.service;
	requires transitive net.thisptr.jackson.jq.v2.spi;
	requires net.thisptr.jackson.jq.v2.json;

	exports net.thisptr.jackson.jq.v2.ext.random;
	exports net.thisptr.jackson.jq.v2.ext.random.functions;

	provides net.thisptr.jackson.jq.v2.spi.module.Module with
		net.thisptr.jackson.jq.v2.ext.random.ModuleImpl;
}
