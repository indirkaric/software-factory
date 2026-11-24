package {{rootPackage}}.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeycloakRolesConverterTest {

    private static final long TTL_SECONDS = 60L;

    private final KeycloakRolesConverter converter = new KeycloakRolesConverter();

    @Test
    void shouldMapScopesAndRealmRoles() {
        Jwt jwt = jwt(Map.of("scope", "profile", "realm_access", Map.of("roles", List.of("ADMIN", "MANAGER"))));

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("SCOPE_profile", "SCOPE_ADMIN", "SCOPE_MANAGER");
    }

    @Test
    void shouldIgnoreMissingRealmAccess() {
        assertThat(converter.convert(jwt(Map.of("scope", "profile")))).extracting(GrantedAuthority::getAuthority)
                .containsExactly("SCOPE_profile");
    }

    @Test
    void shouldRejectNullJwt() {
        assertThatThrownBy(() -> converter.convert(null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Jwt jwt(final Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(TTL_SECONDS)).subject("user");
        claims.forEach(builder::claim);
        return builder.build();
    }
}
