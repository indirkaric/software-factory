package {{rootPackage}}.modules.{{domain_package}}.service.impl;

import {{rootPackage}}.dto.{{Domain}}DTO;
import {{rootPackage}}.mapper.I{{Domain}}Mapper;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ListRequest;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}ListResponse;
import {{rootPackage}}.modules.{{domain_package}}.service.I{{Domain}}ListResponseCreatorService;
import {{rootPackage}}.service.model.PagedList;
import {{rootPackage}}.service.model.{{Domain}}ListFilter;
import {{rootPackage}}.service.{{domain_package}}.I{{Domain}}Service;
import {{rootPackage}}.util.Check;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * API filters → service filter → {@link PagedList} → {@code {{Domain}}ListResponse(data, count)}.
 */
@Service
public class {{Domain}}ListResponseCreatorService implements I{{Domain}}ListResponseCreatorService {

    private final I{{Domain}}Service service;

    private final I{{Domain}}Mapper mapper;

    /**
     * Constructor with dependency injection.
     *
     * @param service {{domain}} read service
     * @param mapper {{domain}} mapper
     */
    public {{Domain}}ListResponseCreatorService(final I{{Domain}}Service service,
                                                final I{{Domain}}Mapper mapper) {
        Check.isNotNull(service, "service");
        Check.isNotNull(mapper, "mapper");

        this.service = service;
        this.mapper = mapper;
    }

    /**
     * Designed for extension.
     *
     * @param request normalized filters
     * @param pageable page, size and sort
     * @return one page and the total count
     */
    @Override
    public {{Domain}}ListResponse create(final {{Domain}}ListRequest request,
                                         final Pageable pageable) {
        Check.isNotNull(request, "request");
        Check.isNotNull(pageable, "pageable");

        var filter = new {{Domain}}ListFilter(request.getIds(), request.getStatus(), request.getTextSearch());
        PagedList<{{Domain}}DTO> page = service.getPagedList(filter, pageable);

        return new {{Domain}}ListResponse(mapper.toItemResponseList(page.items()), page.count());
    }
}
