package {{rootPackage}}.exception;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProxyExceptionsTest {

    @Test
    void shouldCarryMessages() {
        assertThat(new BadRequestException(List.of("a", "b")).getMessages()).containsExactly("a", "b");
        assertThat(new ConflictException("state")).hasMessage("state");
        assertThat(new ServiceUnavailableException("down")).hasMessage("down");
    }

    @Test
    void shouldRejectEmptyMessages() {
        assertThatThrownBy(() -> new BadRequestException(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConflictException("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ServiceUnavailableException(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
