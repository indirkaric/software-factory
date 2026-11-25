package {{rootPackage}}.modules.{{domain_package}};

import {{rootPackage}}.exception.BusinessException;
import {{rootPackage}}.exception.EntityNotFoundException;
import {{rootPackage}}.exception.GlobalExceptionHandler;
import {{rootPackage}}.modules.{{domain_package}}.model.Create{{Domain}}Request;
import {{rootPackage}}.modules.{{domain_package}}.model.Modify{{Domain}}Request;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ItemResponse;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ListRequest;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ListResponse;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}Response;
import {{rootPackage}}.modules.{{domain_package}}.service.I{{Domain}}ListResponseCreatorService;
import {{rootPackage}}.modules.{{domain_package}}.service.I{{Domain}}ResponseCreatorService;
import {{rootPackage}}.service.{{domain_package}}.ICreate{{Domain}}Service;
import {{rootPackage}}.service.{{domain_package}}.IDelete{{Domain}}Service;
import {{rootPackage}}.service.{{domain_package}}.IModify{{Domain}}Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The HTTP contract of the reference module: request body in, status and response body out. Standalone
 * MockMvc, so security is not applied here; the factory's local check proves the authorities.
 */
@ExtendWith(MockitoExtension.class)
class {{Domain}}ControllerTest {

    private static final String URL = "/{{api-prefix}}/v1/{{domain-path}}";

    private static final String ID = "ABC1234";

    @Mock
    private I{{Domain}}ListResponseCreatorService listResponseCreator;

    @Mock
    private I{{Domain}}ResponseCreatorService responseCreator;

    @Mock
    private ICreate{{Domain}}Service createService;

    @Mock
    private IModify{{Domain}}Service modifyService;

    @Mock
    private IDelete{{Domain}}Service deleteService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new {{Domain}}Controller(listResponseCreator, responseCreator, createService,
                modifyService, deleteService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void shouldCreateWithTrimmedNameAndReturn201WithId() throws Exception {
        when(createService.create(any())).thenReturn(ID);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  First   one \",\"description\":\"d\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().json("{\"id\":\"" + ID + "\"}"));

        var request = ArgumentCaptor.forClass(Create{{Domain}}Request.class);
        verify(createService).create(request.capture());
        assertThat(request.getValue().getName()).isEqualTo("First one");
    }

    @Test
    void shouldReturn400ForBlankName() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages[0]").value("name: Name is required."));

        verifyNoInteractions(createService);
    }

    @Test
    void shouldReturn422WhenTheValidatorRejects() throws Exception {
        when(createService.create(any())).thenThrow(new BusinessException("taken"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Taken\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages[0]").value("taken"));
    }

    @Test
    void shouldListWithFiltersFromTheQueryString() throws Exception {
        {{Domain}}ListResponse response = new {{Domain}}ListResponse(
                List.of(new {{Domain}}ItemResponse(ID, "n", (short) 1)), 1);
        when(listResponseCreator.create(any(), any())).thenReturn(response);

        mockMvc.perform(get(URL).param("ids", "abc1234").param("textSearch", " n ").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"data\":[{\"id\":\"" + ID + "\",\"name\":\"n\",\"status\":1}],"
                        + "\"count\":1}"));

        var request = ArgumentCaptor.forClass({{Domain}}ListRequest.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(listResponseCreator).create(request.capture(), pageable.capture());
        assertThat(request.getValue().getIds()).containsExactly(ID);
        assertThat(request.getValue().getTextSearch()).isEqualTo("n");
        assertThat(pageable.getValue().getPageSize()).isEqualTo(1);
    }

    @Test
    void shouldReturn204ForAnEmptyPage() throws Exception {
        when(listResponseCreator.create(any(), any())).thenReturn(new {{Domain}}ListResponse(List.of(), 0));

        mockMvc.perform(get(URL)).andExpect(status().isNoContent());
    }

    @Test
    void shouldGetOneByUpperCasedId() throws Exception {
        var response = new {{Domain}}Response(ID, "n", "d", (short) 1, "U1", null);
        when(responseCreator.create(ID)).thenReturn(response);

        mockMvc.perform(get(URL + "/abc1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.name").value("n"));
    }

    @Test
    void shouldReturn404WhenMissing() throws Exception {
        when(responseCreator.create(ID)).thenThrow(new EntityNotFoundException("missing"));

        mockMvc.perform(get(URL + "/" + ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("missing"));
    }

    @Test
    void shouldModifyAndReturn204() throws Exception {
        mockMvc.perform(put(URL + "/" + ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"status\":0}"))
                .andExpect(status().isNoContent());

        verify(modifyService).modify(eq(ID), any(Modify{{Domain}}Request.class));
    }

    @Test
    void shouldReturn400ForAnInvalidStatus() throws Exception {
        mockMvc.perform(put(URL + "/" + ID).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"status\":2}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(modifyService);
    }

    @Test
    void shouldDeleteAndReturn204() throws Exception {
        mockMvc.perform(delete(URL + "/" + ID)).andExpect(status().isNoContent());

        verify(deleteService).delete(ID);
    }

    @Test
    void shouldReturn422WhenDeletingAnActiveOne() throws Exception {
        doThrow(new BusinessException("active")).when(deleteService).delete(ID);

        mockMvc.perform(delete(URL + "/" + ID)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldRejectMissingDependency() {
        assertThatThrownBy(() -> new {{Domain}}Controller(null, responseCreator, createService, modifyService,
                deleteService)).isInstanceOf(IllegalArgumentException.class);
    }
}
