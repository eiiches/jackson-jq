package net.thisptr.jackson.jq.v2.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

public interface JqLibrary {
	// TODO: fix stringly-typed api
	class JqFunc {
		public final String name;
		public final List<String> args;
		public final String body;
		public final @Nullable VersionRange version;

		public JqFunc(String name, List<String> args, String body, @Nullable VersionRange version) {
			this.name = Objects.requireNonNull(name, "name");
			this.args = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(args, "args")));
			this.body = Objects.requireNonNull(body, "body");
			this.version = version;
		}
	}

	List<JqFunc> getFunctions();
}
