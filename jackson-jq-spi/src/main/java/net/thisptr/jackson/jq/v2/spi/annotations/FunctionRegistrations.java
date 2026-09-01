package net.thisptr.jackson.jq.v2.spi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for repeated {@link FunctionRegistration} annotations.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FunctionRegistrations {
	/**
	 * Returns the repeated registrations.
	 *
	 * @return the repeated registrations
	 */
	FunctionRegistration[] value();
}
