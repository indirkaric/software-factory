package {{rootPackage}}.security;

import {{rootPackage}}.constants.Constants;
import {{rootPackage}}.util.Check;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JWT authorities: the {@code scope} claim plus Keycloak realm roles ({@code realm_access.roles}),
 * both as {@code SCOPE_<NAME>}, so {@code @PreAuthorize} reads one naming scheme whichever way
 * Keycloak grants a permission.
 */
public final class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(final Jwt jwt) {
        Check.isNotNull(jwt, "jwt");

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopes.convert(jwt));
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            for (Object role : roles) {
                authorities.add(new SimpleGrantedAuthority(Constants.ApiScopes.SCOPE + role));
            }
        }

        return authorities;
    }
}
