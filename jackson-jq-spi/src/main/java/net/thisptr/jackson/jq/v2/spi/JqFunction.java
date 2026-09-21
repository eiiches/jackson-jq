package net.thisptr.jackson.jq.v2.spi;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.thisptr.jackson.jq.v2.spi.type.FunctionType;
import net.thisptr.jackson.jq.v2.spi.type.TypeScheme;
import net.thisptr.jackson.jq.v2.spi.version.VersionRange;

/**
 * A raw jq-language function definition, equivalent to {@code def name(args): body;}, contributed
 * by a {@link JqLibrary}.
 * <p>
 * A definition may publish {@linkplain #typeSchemes() type schemes}, as a Java {@link Function} does. A
 * call is then checked against them rather than against the body it is about to run -- which is what lets
 * a definition state the input it is written for, instead of leaving that to whatever its body happens to
 * do. Publish one only where it says at least as much as the body does: a definition whose output is not a
 * substitution of its input, as {@code flatten} and {@code walk} are not, is better left unpublished.
 * <p>
 * Instances are immutable value objects.
 */
public final class JqFunction {
	private final FunctionSignature signature;
	private final List<FunctionParameter> parameters;
	private final String body;
	private final @Nullable VersionRange version;
	private final List<TypeScheme<FunctionType>> typeSchemes;

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
	JqFunction(String name, List<FunctionParameter> parameters, String body, @Nullable VersionRange version,
			   List<? extends TypeScheme<FunctionType>> typeSchemes) {
		Objects.requireNonNull(parameters, "parameters");
		for (FunctionParameter parameter : parameters)
			Objects.requireNonNull(parameter, "parameters must not contain null elements");
		Objects.requireNonNull(typeSchemes, "typeSchemes");
		this.signature = FunctionSignature.of(name, parameters.size());
		this.parameters = List.copyOf(parameters);
		this.body = Objects.requireNonNull(body, "body");
		this.version = version;
		this.typeSchemes = List.copyOf(typeSchemes);
		for (TypeScheme<FunctionType> scheme : this.typeSchemes) {
			if (scheme.body().parameterTypes().size() != this.parameters.size())
				throw new IllegalArgumentException("A type scheme for " + signature
						+ " must take " + this.parameters.size() + " parameters, not "
						+ scheme.body().parameterTypes().size());
		}
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
		return new JqFunction(name, parameters, body, null, List.of());
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
		return new JqFunction(name, parameters, body, version, List.of());
	}

	/**
	 * Creates a jq function definition that publishes the types it accepts.
	 *
	 * @param name the function name
	 * @param parameters the formal parameters, in declaration order (copied defensively)
	 * @param body the jq source of the function body
	 * @param typeSchemes the overloads this definition accepts, each taking as many parameters as
	 * {@code parameters} declares
	 * @return the function definition
	 * @throws IllegalArgumentException if {@code name} is not a valid jq function name (see
	 * {@link FunctionSignature#of}), or a scheme declares the wrong number of parameters
	 */
	public static JqFunction of(String name, List<FunctionParameter> parameters, String body,
								List<? extends TypeScheme<FunctionType>> typeSchemes) {
		return new JqFunction(name, parameters, body, null, typeSchemes);
	}

	/**
	 * Creates a jq function definition with a version constraint that publishes the types it accepts.
	 *
	 * @param name the function name
	 * @param parameters the formal parameters, in declaration order (copied defensively)
	 * @param body the jq source of the function body
	 * @param version the jq versions this definition applies to, or {@code null} for all versions
	 * @param typeSchemes the overloads this definition accepts, each taking as many parameters as
	 * {@code parameters} declares
	 * @return the function definition
	 * @throws IllegalArgumentException if {@code name} is not a valid jq function name (see
	 * {@link FunctionSignature#of}), or a scheme declares the wrong number of parameters
	 */
	public static JqFunction of(String name, List<FunctionParameter> parameters, String body,
								@Nullable VersionRange version, List<? extends TypeScheme<FunctionType>> typeSchemes) {
		return new JqFunction(name, parameters, body, version, typeSchemes);
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

	/**
	 * Returns the overloads this definition publishes, or an empty list when it publishes none and a call
	 * is to be checked against the body instead.
	 *
	 * @return the type schemes, unmodifiable
	 */
	public List<TypeScheme<FunctionType>> typeSchemes() {
		return typeSchemes;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (!(o instanceof JqFunction that))
			return false;
		return signature.equals(that.signature)
				&& parameters.equals(that.parameters)
				&& body.equals(that.body)
				&& Objects.equals(version, that.version)
				&& typeSchemes.equals(that.typeSchemes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(signature, parameters, body, version, typeSchemes);
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
