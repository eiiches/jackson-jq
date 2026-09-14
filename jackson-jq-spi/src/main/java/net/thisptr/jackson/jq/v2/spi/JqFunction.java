package net.thisptr.jackson.jq.v2.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

/**
 * A raw jq-language function definition, equivalent to {@code def name(args): body;}, contributed
 * by a {@link JqLibrary}.
 * <p>
 * Instances are immutable value objects.
 */
public final class JqFunction {
	private final FunctionSignature signature;
	private final List<FunctionParameter> parameters;
	private final String body;
	private final @Nullable VersionRange version;

	/**
	 * Creates a jq function definition.
	 *
	 * @param name the function name
	 * @param parameters the formal parameters, in declaration order (copied defensively)
	 * @param body the jq source of the function body
	 * @param version the jq versions this definition applies to, or {@code null} for all versions
	 * @throws IllegalArgumentException if {@code name} is not a valid jq function name (see
	 * {@link FunctionSignature#of})
	 */
	JqFunction(String name, List<FunctionParameter> parameters, String body, @Nullable VersionRange version) {
		Objects.requireNonNull(parameters, "parameters");
		for (FunctionParameter parameter : parameters)
			Objects.requireNonNull(parameter, "parameters must not contain null elements");
		this.signature = FunctionSignature.of(name, parameters.size());
		this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
		this.body = Objects.requireNonNull(body, "body");
		this.version = version;
	}

	/**
	 * Creates a jq function definition with no version constraint.
	 *
	 * @param name the function name
	 * @param parameters the formal parameters, in declaration order (copied defensively)
	 * @param body the jq source of the function body
	 * @return the function definition
	 * @throws IllegalArgumentException if {@code name} is not a valid jq function name (see
	 * {@link FunctionSignature#of})
	 */
	public static JqFunction of(String name, List<FunctionParameter> parameters, String body) {
		return new JqFunction(name, parameters, body, null);
	}

	/**
	 * Creates a jq function definition with a version constraint.
	 *
	 * @param name the function name
	 * @param parameters the formal parameters, in declaration order (copied defensively)
	 * @param body the jq source of the function body
	 * @param version the jq versions this definition applies to, or {@code null} for all versions
	 * @return the function definition
	 * @throws IllegalArgumentException if {@code name} is not a valid jq function name (see
	 * {@link FunctionSignature#of})
	 */
	public static JqFunction of(String name, List<FunctionParameter> parameters, String body, @Nullable VersionRange version) {
		return new JqFunction(name, parameters, body, version);
	}

	/**
	 * Returns the function name.
	 *
	 * @return the function name
	 */
	public String name() {
		return signature.name();
	}

	/**
	 * Returns the {@code (name, arity)} signature of this function, derived from its name and the
	 * number of {@link #parameters()}.
	 *
	 * @return the function signature
	 */
	public FunctionSignature signature() {
		return signature;
	}

	/**
	 * Returns the formal parameters, in declaration order.
	 *
	 * @return the formal parameters, unmodifiable
	 */
	public List<FunctionParameter> parameters() {
		return parameters;
	}

	/**
	 * Returns the jq source of the function body.
	 *
	 * @return the function body source
	 */
	public String body() {
		return body;
	}

	/**
	 * Returns the jq versions this definition applies to.
	 *
	 * @return the applicable version range, or {@code null} if it applies to all versions
	 */
	public @Nullable VersionRange version() {
		return version;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof JqFunction))
			return false;
		JqFunction that = (JqFunction) o;
		return signature.equals(that.signature)
				&& parameters.equals(that.parameters)
				&& body.equals(that.body)
				&& Objects.equals(version, that.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(signature, parameters, body, version);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("def ").append(signature.name());
		if (!parameters.isEmpty()) {
			sb.append('(');
			for (int i = 0; i < parameters.size(); i++) {
				if (i > 0)
					sb.append("; ");
				sb.append(parameters.get(i));
			}
			sb.append(')');
		}
		sb.append(": ").append(body).append(';');
		if (version != null)
			sb.append(" # ").append(version);
		return sb.toString();
	}
}
