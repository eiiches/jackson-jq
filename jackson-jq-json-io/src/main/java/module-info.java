import org.jspecify.annotations.NullMarked;

@NullMarked
module net.thisptr.jackson.jq.v2.json.io {
	requires net.thisptr.jackson.jq.v2.json;
	requires static org.jspecify;

	exports net.thisptr.jackson.jq.v2.json.internal.io to
			net.thisptr.jackson.jq.v2.core,
			net.thisptr.jackson.jq.v2.ext.module.fs,
			net.thisptr.jackson.jq.v2.ext.module.http,
			net.thisptr.jackson.jq.v2.ext.module.joni,
			net.thisptr.jackson.jq.v2.ext.module.re2,
			net.thisptr.jackson.jq.v2.ext.module.time;
}
