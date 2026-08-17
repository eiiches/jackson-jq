module net.thisptr.jackson.jq.v2.spi {
	requires transitive net.thisptr.jackson.jq.v2.json;
	requires static org.jspecify;

	exports net.thisptr.jackson.jq.v2.spi;
	exports net.thisptr.jackson.jq.v2.spi.annotations;
	exports net.thisptr.jackson.jq.v2.spi.exception;
	exports net.thisptr.jackson.jq.v2.spi.module;
	exports net.thisptr.jackson.jq.v2.spi.path;
}
