package {{rootPackage}}.constants;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

/**
 * Application-wide constants.
 */
public final class Constants {

    public static final String APP_NAME = "{{service-name}}_app";

    public static final String ENTITY_ID = "entityId";

    public static final int LIMIT_PAGE_SIZE = 2000;

    public static final String BEARER_KEY_VALUE = "bearer-key";

    public static final String SECURITY_SCHEME = "bearer";

    public static final String SECURITY_FORMAT = "JWT";

    public static final String SERVICE_COMMUNICATION_ID = "service-communication-id";

    public static final String CORRELATION_ID = "correlation-id";

    public static final String USER_ID = "user-id";

    /** Inbound caller ids, format "&lt;Caller&gt;.&lt;Callee&gt;". */
    public static final String UI_CLIENT = "UI.{{ServiceName}}";

    /** Every inbound caller id granted full authorities by the client-id filter. */
    public static final Set<String> TRUSTED_CLIENTS = Set.of(UI_CLIENT);

    private Constants() {
    }

    /**
     * Keycloak scopes.
     */
    public static final class ApiScopes {

        public static final String SCOPE = "SCOPE_";

        public static final String ADMIN_SCOPE = SCOPE + "ADMIN";

        public static final String MANAGER_SCOPE = SCOPE + "MANAGER";

        private ApiScopes() {
        }
    }

    /**
     * {@code @PreAuthorize} expressions and the authority sets behind them.
     */
    public static final class Authority {

        public static final String FULL_AUTHORITIES = "hasAuthority('" + ApiScopes.ADMIN_SCOPE + "')";

        public static final String RESTRICTED_AUTHORITIES = "hasAnyAuthority('"
                + ApiScopes.MANAGER_SCOPE + "','" + ApiScopes.ADMIN_SCOPE + "')";

        private Authority() {
        }

        /**
         * Authorities granted to a trusted service-to-service caller.
         *
         * @return authorities
         */
        public static List<SimpleGrantedAuthority> fullAuthorities() {
            return List.of(new SimpleGrantedAuthority(ApiScopes.ADMIN_SCOPE),
                    new SimpleGrantedAuthority(ApiScopes.MANAGER_SCOPE));
        }
    }
}
