package {{rootPackage}}.modules.{{domain_package}}.service;

import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ListRequest;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ListResponse;
import org.springframework.data.domain.Pageable;

/**
 * Builds the list response.
 */
public interface I{{Domain}}ListResponseCreatorService {

    /**
     * @param request normalized filters
     * @param pageable page, size and sort
     * @return one page and the total count
     */
    {{Domain}}ListResponse create({{Domain}}ListRequest request, Pageable pageable);
}
