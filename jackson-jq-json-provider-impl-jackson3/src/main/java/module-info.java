module net.thisptr.jackson.jq.v2.json.impl.jackson3 {
	requires transitive net.thisptr.jackson.jq.v2.json;
	requires transitive tools.jackson.databind;
	requires static com.google.errorprone.annotations;

	exports net.thisptr.jackson.jq.v2.json.impl.jackson3;
}
