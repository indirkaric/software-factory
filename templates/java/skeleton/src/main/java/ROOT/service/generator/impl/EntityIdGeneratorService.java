package {{rootPackage}}.service.generator.impl;

import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.service.generator.IEntityIdGeneratorService;
import {{rootPackage}}.util.Check;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.function.Predicate;

/**
 * Random ids from {@code A-Z0-9}, retried until one is free.
 */
@Service
public class EntityIdGeneratorService implements IEntityIdGeneratorService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int MAX_LENGTH = 120;

    private static final int MAX_ATTEMPTS = 50;

    private final SecureRandom random = new SecureRandom();

    /**
     * Designed for extension.
     *
     * @param length id length, 1 to 120
     * @param exists true when the id is already taken
     * @return a free id
     */
    @Override
    public String generate(final int length, final Predicate<String> exists) {
        Check.isNotNull(exists, "exists");
        if (length < 1 || length > MAX_LENGTH) {
            throw new IllegalArgumentException("Entity id length must be between 1 and " + MAX_LENGTH + ".");
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String id = randomId(length);
            if (!exists.test(id)) {
                return id;
            }
        }

        throw new BusinessException("Could not generate a free entity id in " + MAX_ATTEMPTS + " attempts.");
    }

    private String randomId(final int length) {
        StringBuilder id = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            id.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }

        return id.toString();
    }
}
