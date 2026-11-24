package {{rootPackage}}.security;

import {{rootPackage}}.constants.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceCommunicationIdFilterTest {

    private ServiceCommunicationIdFilter filter;

    private MockHttpServletRequest request;

    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new ServiceCommunicationIdFilter();
        request = new MockHttpServletRequest();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGrantFullAuthoritiesToTrustedClient() throws Exception {
        request.addHeader(Constants.SERVICE_COMMUNICATION_ID, Constants.UI_CLIENT);

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo(Constants.UI_CLIENT);
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly(Constants.ApiScopes.ADMIN_SCOPE, Constants.ApiScopes.MANAGER_SCOPE);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void shouldGrantNothingToUnknownClient() throws Exception {
        request.addHeader(Constants.SERVICE_COMMUNICATION_ID, "Unknown.Client");

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).isEmpty();
    }

    @Test
    void shouldLeaveContextEmptyWithoutHeader() throws Exception {
        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }
}
