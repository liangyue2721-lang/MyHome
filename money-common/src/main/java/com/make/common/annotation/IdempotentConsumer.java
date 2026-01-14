package com.make.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Idempotent Consumer Annotation
 * Ensures that the method is executed only once for a given key.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentConsumer {

    /**
     * SpEL expression for the idempotent key.
     * Example: "#task.traceId", "#record.key()"
     */
    String key();

    /**
     * Expiration time in seconds (default 24 hours).
     */
    long expire() default 86400;
}
