package net.thisptr.jackson.jq.v2.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface JqLibrary {
	// TODO: fix stringly-typed api
	class JqFunc {
		public String name;
		public List<String> args;
		public String body;
		public String version;

		public JqFunc(String name, List<String> args, String body, String version) {
			this.name = Objects.requireNonNull(name, "name");
			this.args = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(args, "args")));
			this.body = Objects.requireNonNull(body, "body");
			this.version = version;
		}
	}

	List<JqFunc> getFunctions();
}
