package {{rootPackage}}.constants;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every permission the code checks is declared in permissions.txt, which the software factory
 * copies into the development realm; a forgotten line would only fail at runtime with a 403.
 */
class PermissionsDeclaredTest {

    @Test
    void everyScopeInConstantsIsInPermissionsTxt() throws IOException {
        Set<String> declared = Files.readAllLines(Path.of("permissions.txt")).stream()
                .map(line -> line.split("#", 2)[0].trim())
                .filter(line -> !line.isEmpty())
                .map(line -> line.split("\\s+")[0])
                .collect(Collectors.toSet());

        Set<String> used = Arrays.stream(Constants.ApiScopes.class.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == String.class)
                .map(PermissionsDeclaredTest::value)
                .filter(value -> value.startsWith(Constants.ApiScopes.SCOPE)
                        && value.length() > Constants.ApiScopes.SCOPE.length())
                .map(value -> value.substring(Constants.ApiScopes.SCOPE.length()))
                .collect(Collectors.toSet());

        assertThat(declared).containsAll(used);
    }

    private static String value(final Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
