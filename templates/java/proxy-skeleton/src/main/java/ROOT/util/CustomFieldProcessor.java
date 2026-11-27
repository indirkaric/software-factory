package {{rootPackage}}.util;

import {{rootPackage}}.annotations.Lowercase;
import {{rootPackage}}.annotations.Trimmed;
import {{rootPackage}}.annotations.Uppercase;
import {{rootPackage}}.annotations.UppercaseList;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;

/**
 * Applies {@link Trimmed}, {@link Uppercase}, {@link Lowercase} and {@link UppercaseList} to a request
 * model in place. Request models are therefore classes with mutable fields, not records.
 */
public final class CustomFieldProcessor {

    private CustomFieldProcessor() {
    }

    /**
     * Normalizes every annotated field of the object and of its superclasses.
     *
     * @param object request model
     * @param <T> model type
     */
    public static <T> void processAnnotatedFields(final T object) {
        Check.isNotNull(object, "object");

        Class<?> type = object.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && isAnnotated(field)) {
                    process(field, object);
                }
            }

            type = type.getSuperclass();
        }
    }

    private static boolean isAnnotated(final Field field) {
        return field.isAnnotationPresent(Trimmed.class) || field.isAnnotationPresent(Uppercase.class)
                || field.isAnnotationPresent(Lowercase.class) || field.isAnnotationPresent(UppercaseList.class);
    }

    private static void process(final Field field, final Object object) {
        try {
            field.setAccessible(true);
            field.set(object, transform(field, field.get(object)));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot normalize field " + field.getName(), e);
        }
    }

    private static Object transform(final Field field, final Object value) {
        if (value instanceof String text) {
            String result = text;
            if (field.isAnnotationPresent(Trimmed.class)) {
                result = result.trim().replaceAll("\\s+", " ");
            }

            if (field.isAnnotationPresent(Uppercase.class)) {
                result = result.toUpperCase(Locale.ROOT);
            }

            if (field.isAnnotationPresent(Lowercase.class)) {
                result = result.toLowerCase(Locale.ROOT);
            }

            return result;
        }

        if (value instanceof List<?> list && field.isAnnotationPresent(UppercaseList.class)) {
            return list.stream()
                    .map(item -> item == null ? null : item.toString().trim().toUpperCase(Locale.ROOT))
                    .toList();
        }

        return value;
    }
}
