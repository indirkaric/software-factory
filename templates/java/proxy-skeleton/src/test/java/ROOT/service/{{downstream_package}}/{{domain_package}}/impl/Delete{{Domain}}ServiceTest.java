package {{rootPackage}}.service.{{downstream_package}}.{{domain_package}}.impl;

import {{rootPackage}}.client.{{downstream_package}}.{{Domain}}Client;
import {{rootPackage}}.client.response.ClientResponseWrapper;
import {{rootPackage}}.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Delete{{Domain}}ServiceTest {

    private static final String ID = "ABC1234";

    @Mock
    private {{Domain}}Client client;

    private Delete{{Domain}}Service service;

    @BeforeEach
    void setUp() {
        service = new Delete{{Domain}}Service(client);
    }

    @Test
    void shouldDelete() {
        when(client.delete(ID)).thenReturn(new ClientResponseWrapper(HttpStatus.NO_CONTENT, null));

        assertThatCode(() -> service.delete(ID)).doesNotThrowAnyException();
    }

    @Test
    void shouldPassOnConflict() {
        when(client.delete(ID))
                .thenReturn(new ClientResponseWrapper(HttpStatus.CONFLICT, "{\"message\":\"in use\"}"));

        assertThatThrownBy(() -> service.delete(ID)).isInstanceOf(ConflictException.class).hasMessage("in use");
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> new Delete{{Domain}}Service(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.delete("")).isInstanceOf(IllegalArgumentException.class);
    }
}
