package {{rootPackage}}.service.{{downstream_package}}.{{domain_package}}.impl;

import {{rootPackage}}.client.{{downstream_package}}.{{Domain}}Client;
import {{rootPackage}}.client.response.ClientResponseWrapper;
import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.Create{{Domain}}Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Create{{Domain}}ServiceTest {

    private final Create{{Domain}}Request request = new Create{{Domain}}Request("n", "d");

    @Mock
    private {{Domain}}Client client;

    private Create{{Domain}}Service service;

    @BeforeEach
    void setUp() {
        service = new Create{{Domain}}Service(client);
    }

    @Test
    void shouldReturnTheNewId() {
        when(client.create(request))
                .thenReturn(new ClientResponseWrapper(HttpStatus.CREATED, Map.of("id", "ABC1234")));

        assertThat(service.create(request)).isEqualTo("ABC1234");
    }

    @Test
    void shouldPassOnTheDownstreamRejection() {
        when(client.create(request)).thenReturn(new ClientResponseWrapper(HttpStatus.UNPROCESSABLE_ENTITY,
                "{\"messages\":[\"taken\"]}"));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(BusinessException.class).hasMessage("taken");
    }

    @Test
    void shouldFailWithoutAnId() {
        when(client.create(request)).thenReturn(new ClientResponseWrapper(HttpStatus.CREATED, null));

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> new Create{{Domain}}Service(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
