package {{rootPackage}}.service.{{domain_package}}.impl;

import {{rootPackage}}.dto.{{Domain}}DTO;
import {{rootPackage}}.exception.EntityNotFoundException;
import {{rootPackage}}.mapper.I{{Domain}}Mapper;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import {{rootPackage}}.service.model.PagedList;
import {{rootPackage}}.service.model.{{Domain}}ListFilter;
import {{rootPackage}}.service.{{domain_package}}.I{{Domain}}Service;
import {{rootPackage}}.util.Check;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {{Domain}} reads. {@code readOnly = true} is also the routing decision: it sends the queries to the
 * read-only datasource.
 */
@Service
@Transactional(readOnly = true)
public class {{Domain}}Service implements I{{Domain}}Service {

    private final I{{Domain}}Repository repository;

    private final I{{Domain}}Mapper mapper;

    /**
     * Constructor with dependency injection.
     *
     * @param repository {{domain}} repository
     * @param mapper {{domain}} mapper
     */
    public {{Domain}}Service(final I{{Domain}}Repository repository,
                             final I{{Domain}}Mapper mapper) {
        Check.isNotNull(repository, "repository");
        Check.isNotNull(mapper, "mapper");

        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Designed for extension.
     *
     * @param filter filter, every field optional
     * @param pageable page, size and sort
     * @return one page and the total count
     */
    @Override
    public PagedList<{{Domain}}DTO> getPagedList(final {{Domain}}ListFilter filter,
                                                 final Pageable pageable) {
        Check.isNotNull(filter, "filter");
        Check.isNotNull(pageable, "pageable");

        var page = repository.findPagedList(filter, pageable);

        return new PagedList<>(mapper.toItemDtoList(page.getContent()), page.getTotalElements());
    }

    /**
     * Designed for extension.
     *
     * @param entityId public id
     * @return the {{domain}}
     */
    @Override
    public {{Domain}}DTO getByEntityId(final String entityId) {
        Check.isNotEmpty(entityId, "entityId");

        return repository.findProjectionByEntityId(entityId)
                .map(mapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("{{Domain}} with id { id: " + entityId
                        + " } doesn't exist."));
    }
}
