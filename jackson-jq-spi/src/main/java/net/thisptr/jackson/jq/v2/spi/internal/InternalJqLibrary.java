package net.thisptr.jackson.jq.v2.spi.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// TODO: move to exported package
public interface InternalJqLibrary {
	// TODO: fix stringly-typed api
	class JqFunc {
		public final String name;
		public final List<String> args;
		public final String body;
		public final String version;

		public JqFunc(final String name, final List<String> args, final String body, final String version) {
			this.name = Objects.requireNonNull(name, "name");
			this.args = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(args, "args")));
			this.body = Objects.requireNonNull(body, "body");
			this.version = version;
		}
	}

	List<JqFunc> getFunctions();
}
