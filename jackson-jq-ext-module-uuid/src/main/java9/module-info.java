module net.thisptr.jackson.jq.v2.ext.module.uuid {
	requires static org.jspecify;
	requires static com.google.auto.service;
	requires transitive net.thisptr.jackson.jq.v2.spi;
	requires net.thisptr.jackson.jq.v2.json;

	exports net.thisptr.jackson.jq.v2.ext.uuid;
	exports net.thisptr.jackson.jq.v2.ext.uuid.functions;

	provides net.thisptr.jackson.jq.v2.spi.module.Module with
		net.thisptr.jackson.jq.v2.ext.uuid.ModuleImpl;
}
