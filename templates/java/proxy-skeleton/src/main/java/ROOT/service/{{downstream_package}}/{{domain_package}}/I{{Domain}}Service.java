package {{rootPackage}}.service.{{downstream_package}}.{{domain_package}};

import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.{{Domain}}ListRequest;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.{{Domain}}ListResponse;
import {{rootPackage}}.modules.{{downstream_package}}.{{domain_package}}.model.{{Domain}}Response;
import org.springframework.data.domain.Pageable;

/**
 * Reads {{domain}}s from {{downstream}}.
 */
public interface I{{Domain}}Service {

    /**
     * @param request filters
     * @param pageable page, size and sort
     * @return one page; empty data when nothing matches
     */
    {{Domain}}ListResponse getList({{Domain}}ListRequest request, Pageable pageable);

    /**
     * @param id public id
     * @return the {{domain}}
     */
    {{Domain}}Response get(String id);
}
