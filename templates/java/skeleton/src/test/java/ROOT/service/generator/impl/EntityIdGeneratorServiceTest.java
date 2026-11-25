package {{rootPackage}}.service.generator.impl;

import {{rootPackage}}.constants.Constants;
import {{rootPackage}}.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityIdGeneratorServiceTest {

    private final EntityIdGeneratorService generator = new EntityIdGeneratorService();

    @Test
    void shouldGenerateUpperCaseAlphanumericId() {
        String id = generator.generate(Constants.DEFAULT_ENTITY_ID_SIZE, candidate -> false);

        assertThat(id).hasSize(Constants.DEFAULT_ENTITY_ID_SIZE).matches("[A-Z0-9]+");
    }

    @Test
    void shouldRetryWhileTaken() {
        AtomicInteger calls = new AtomicInteger();

        generator.generate(1, candidate -> calls.incrementAndGet() < 2);

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void shouldGiveUpWhenEverythingIsTaken() {
        assertThatThrownBy(() -> generator.generate(1, candidate -> true)).isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectBadArguments() {
        assertThatThrownBy(() -> generator.generate(0, candidate -> false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generate(Constants.LIMIT_PAGE_SIZE, candidate -> false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generate(1, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
