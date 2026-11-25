package {{rootPackage}}.service.generator;

import java.util.function.Predicate;

/**
 * Generates the public {@code entityId} of a new row.
 */
public interface IEntityIdGeneratorService {

    /**
     * A random upper-case alphanumeric id that {@code exists} reports as free.
     *
     * @param length id length, 1 to 120
     * @param exists true when the id is already taken, usually {@code repository::existsByEntityId}
     * @return a free id
     */
    String generate(int length, Predicate<String> exists);
}
