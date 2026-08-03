package net.thisptr.jackson.jq.internal.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to inform users of a package, class or method's intended audience.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface InterfaceAudience {
    String value();
}
