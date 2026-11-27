package {{rootPackage}}.service.{{downstream_package}}.{{domain_package}}.impl;

import {{rootPackage}}.client.{{downstream_package}}.{{Domain}}Client;
import {{rootPackage}}.client.response.ClientResponseWrapper;
import {{rootPackage}}.exception.EntityNotFoundException;
import {{rootPackage}}.exception.ServiceUnavailableException;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.{{Domain}}ItemResponse;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.{{Domain}}ListRequest;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.{{Domain}}ListResponse;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.{{Domain}}Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class {{Domain}}ServiceTest {

    private static final String ID = "ABC1234";

    private static final Pageable PAGE = PageRequest.of(0, 50);

    @Mock
    private {{Domain}}Client client;

    private {{Domain}}Service service;

    @BeforeEach
    void setUp() {
        service = new {{Domain}}Service(client);
    }

    @Test
    void shouldMapThePage() {
        {{Domain}}ListRequest request = new {{Domain}}ListRequest();
        when(client.getList(request, PAGE)).thenReturn(new ClientResponseWrapper(HttpStatus.OK, Map.of(
                "data", List.of(Map.of("id", ID, "name", "n", "status", 1)), "count", 1)));

        {{Domain}}ListResponse response = service.getList(request, PAGE);

        var item = new {{Domain}}ItemResponse(ID, "n", (short) 1);
        assertThat(response).isEqualTo(new {{Domain}}ListResponse(List.of(item), 1));
    }

    @Test
    void shouldReturnAnEmptyPageFor204() {
        {{Domain}}ListRequest request = new {{Domain}}ListRequest();
        when(client.getList(request, PAGE)).thenReturn(new ClientResponseWrapper(HttpStatus.NO_CONTENT, null));

        assertThat(service.getList(request, PAGE)).isEqualTo(new {{Domain}}ListResponse(List.of(), 0));
    }

    @Test
    void shouldMapOne() {
        when(client.get(ID)).thenReturn(new ClientResponseWrapper(HttpStatus.OK, Map.of("id", ID, "name", "n")));

        {{Domain}}Response response = service.get(ID);

        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.name()).isEqualTo("n");
    }

    @Test
    void shouldThrowNotFoundForMissingOrEmpty() {
        when(client.get(ID)).thenReturn(new ClientResponseWrapper(HttpStatus.NOT_FOUND, "{\"message\":\"gone\"}"));
        assertThatThrownBy(() -> service.get(ID)).isInstanceOf(EntityNotFoundException.class).hasMessage("gone");

        when(client.get(ID)).thenReturn(new ClientResponseWrapper(HttpStatus.NO_CONTENT, null));
        assertThatThrownBy(() -> service.get(ID)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldThrowServiceUnavailableWhenUnreachable() {
        assertThatThrownBy(() -> service.get(ID)).isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void shouldRejectMissingArguments() {
        assertThatThrownBy(() -> new {{Domain}}Service(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getList(null, PAGE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.get("")).isInstanceOf(IllegalArgumentException.class);
    }
}
