package {{rootPackage}}.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Trims a String and collapses inner whitespace to one space. Applied in place by {@code
 * CustomFieldProcessor.processAnnotatedFields}, which the controller calls first thing for every
 * request model.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trimmed {
}
