import net.thisptr.jackson.jq.v2.spi.Function;

module net.thisptr.jackson.jq.v2.regex.impl.joni {
	requires static com.google.auto.service;
	requires net.thisptr.jackson.jq.v2.json;
	requires net.thisptr.jackson.jq.v2.spi;
	requires org.jruby.joni;
	requires static org.jspecify;

	provides net.thisptr.jackson.jq.v2.spi.JqLibrary with
		net.thisptr.jackson.jq.v2.regex.impl.joni.RegexJqLibrary;

	provides Function with
		net.thisptr.jackson.jq.v2.regex.impl.joni._MatchImplFunction,
		net.thisptr.jackson.jq.v2.regex.impl.joni._SubImplFunction;
}
