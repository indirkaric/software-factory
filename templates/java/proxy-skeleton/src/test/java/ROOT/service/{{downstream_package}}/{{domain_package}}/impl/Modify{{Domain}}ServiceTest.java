package {{rootPackage}}.service.{{downstream_package}}.{{domain_package}}.impl;

import {{rootPackage}}.client.{{downstream_package}}.{{Domain}}Client;
import {{rootPackage}}.client.response.ClientResponseWrapper;
import {{rootPackage}}.exception.EntityNotFoundException;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.Modify{{Domain}}Request;
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
class Modify{{Domain}}ServiceTest {

    private static final String ID = "ABC1234";

    private final Modify{{Domain}}Request request =
            new Modify{{Domain}}Request("n", "d", (short) 1);

    @Mock
    private {{Domain}}Client client;

    private Modify{{Domain}}Service service;

    @BeforeEach
    void setUp() {
        service = new Modify{{Domain}}Service(client);
    }

    @Test
    void shouldModify() {
        when(client.modify(ID, request)).thenReturn(new ClientResponseWrapper(HttpStatus.NO_CONTENT, null));

        assertThatCode(() -> service.modify(ID, request)).doesNotThrowAnyException();
    }

    @Test
    void shouldPassOnNotFound() {
        when(client.modify(ID, request)).thenReturn(new ClientResponseWrapper(HttpStatus.NOT_FOUND, ""));

        assertThatThrownBy(() -> service.modify(ID, request)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> new Modify{{Domain}}Service(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.modify("", request)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.modify(ID, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
