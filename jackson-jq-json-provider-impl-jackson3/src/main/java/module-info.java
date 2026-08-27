import org.jspecify.annotations.NullMarked;

@NullMarked
module net.thisptr.jackson.jq.v2.json.impl.jackson3 {
	requires transitive net.thisptr.jackson.jq.v2.json;
	requires transitive tools.jackson.databind;
	requires static com.google.errorprone.annotations;
	requires static transitive org.jspecify;

	exports net.thisptr.jackson.jq.v2.json.impl.jackson3;
}
