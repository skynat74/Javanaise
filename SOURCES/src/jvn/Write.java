package jvn;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method needs a write lock before execution.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
//@interface declares a custom annotation type.
public @interface Write {
}
