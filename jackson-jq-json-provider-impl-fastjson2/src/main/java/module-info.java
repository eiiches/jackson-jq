import org.jspecify.annotations.NullMarked;

@NullMarked
module net.thisptr.jackson.jq.v2.json.impl.fastjson2 {
	requires transitive com.alibaba.fastjson2;
	requires transitive net.thisptr.jackson.jq.v2.json;
	requires static transitive org.jspecify;

	exports net.thisptr.jackson.jq.v2.json.impl.fastjson2;
}
