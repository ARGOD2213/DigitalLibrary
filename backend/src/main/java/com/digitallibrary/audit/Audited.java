package com.digitallibrary.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate any controller method to record an audit log entry automatically via AOP.
 * Example: @Audited(action = "BOOK_UPLOAD", entity = "Book")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String action();
    String entity() default "";
    String status() default "SUCCESS";
}
