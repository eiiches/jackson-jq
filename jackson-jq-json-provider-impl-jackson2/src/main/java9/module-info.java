module net.thisptr.jackson.jq.v2.json.impl.jackson2 {
	requires transitive com.fasterxml.jackson.databind;
	requires transitive net.thisptr.jackson.jq.v2.json;
	requires static transitive org.jspecify;

	exports net.thisptr.jackson.jq.v2.json.impl.jackson2;
}
