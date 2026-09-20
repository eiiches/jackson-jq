import org.jspecify.annotations.NullMarked;

@NullMarked
module net.thisptr.jackson.jq.v2.ext.module.http {
	requires net.thisptr.jackson.jq.v2.json;
	requires transitive net.thisptr.jackson.jq.v2.spi;
	requires static org.jspecify;

	provides net.thisptr.jackson.jq.v2.spi.module.Module with
			net.thisptr.jackson.jq.v2.ext.http.ModuleImpl;
}
