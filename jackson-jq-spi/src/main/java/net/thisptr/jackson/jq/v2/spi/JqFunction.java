package net.thisptr.jackson.jq.v2.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A raw jq-language function definition, equivalent to {@code def name(args): body;}, contributed
 * by a {@link JqLibrary}.
 * <p>
 * Instances are immutable value objects.
 */
// TODO: don't expose fields
public class JqFunction {
	/** The function name. */
	public final String name;
	/** The formal parameter names, in declaration order. Defensively copied and unmodifiable. */
	public final List<String> args; // FIXME: better arg type
	/** The jq source of the function body. */
	public final String body;
	/** The jq versions this definition applies to, or {@code null} if it applies to all of them. */
	public final @Nullable VersionRange version;

	/**
	 * Creates a jq function definition.
	 *
	 * @param name the function name
	 * @param args the formal parameter names, in declaration order (copied defensively)
	 * @param body the jq source of the function body
	 * @param version the jq versions this definition applies to, or {@code null} for all versions
	 */
	public JqFunction(String name, List<String> args, String body, @Nullable VersionRange version) {
		this.name = Objects.requireNonNull(name, "name");
		this.args = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(args, "args")));
		this.body = Objects.requireNonNull(body, "body");
		this.version = version;
	}
}
