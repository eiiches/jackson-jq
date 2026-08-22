package net.thisptr.jackson.jq.v2.spi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FunctionRegistrations.class)
public @interface FunctionRegistration {
	String name();

	/**
	 * The number of arguments this function takes. A negative value registers
	 * the function as variadic: it is bound under {@code name} alone (no {@code /nargs} suffix), matching the arity-agnostic
	 * fallback which looks up {@code name/nargs} first and falls back to bare
	 * {@code name} for any arity.
	 */
	int nargs();

	VersionRangeSpec version() default @VersionRangeSpec;
}
