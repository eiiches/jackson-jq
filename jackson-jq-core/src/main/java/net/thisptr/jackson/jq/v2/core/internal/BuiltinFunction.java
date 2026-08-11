package net.thisptr.jackson.jq.v2.core.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.thisptr.jackson.jq.v2.spi.FunctionRegistration;

/**
 * @deprecated Use {@link FunctionRegistration} instead.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface BuiltinFunction {
	String[] value();

	String version() default "";
}
