module net.thisptr.jackson.jq.v2.json.impl.gson {
	requires transitive com.google.gson;
	requires transitive net.thisptr.jackson.jq.v2.json;
	requires static org.jspecify;

	exports net.thisptr.jackson.jq.v2.json.impl.gson;
}
