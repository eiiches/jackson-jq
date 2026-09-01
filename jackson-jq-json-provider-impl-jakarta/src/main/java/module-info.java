import org.jspecify.annotations.NullMarked;

@NullMarked
module net.thisptr.jackson.jq.v2.json.impl.jakarta {
	requires transitive jakarta.json;
	requires transitive net.thisptr.jackson.jq.v2.json;
	requires static com.google.errorprone.annotations;
	requires static transitive org.jspecify;

	exports net.thisptr.jackson.jq.v2.json.impl.jakarta;
}
