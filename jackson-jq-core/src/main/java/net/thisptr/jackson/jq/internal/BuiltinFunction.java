package net.thisptr.jackson.jq.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated Use {@link net.thisptr.jackson.jq.BuiltinFunction} instead.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Deprecated
public @interface BuiltinFunction {
	String[] value();

	String version() default "";
}
