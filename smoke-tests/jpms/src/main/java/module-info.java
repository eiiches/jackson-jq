import org.jspecify.annotations.NullMarked;

@NullMarked
module net.thisptr.jackson.jq.v2.smoketests.jpms {
	requires net.thisptr.jackson.jq.v2.core;
	requires net.thisptr.jackson.jq.v2.ext.module.debug;
	requires net.thisptr.jackson.jq.v2.ext.module.random;
	requires net.thisptr.jackson.jq.v2.ext.module.time;
	requires net.thisptr.jackson.jq.v2.ext.module.uri;
	requires net.thisptr.jackson.jq.v2.ext.module.uuid;
	requires net.thisptr.jackson.jq.v2.json.impl.jackson2;
	requires net.thisptr.jackson.jq.v2.json.impl.jackson3;
	requires net.thisptr.jackson.jq.v2.json.impl.gson;
	requires net.thisptr.jackson.jq.v2.regex.impl.joni;
	requires net.thisptr.jackson.jq.v2.spi;
}
