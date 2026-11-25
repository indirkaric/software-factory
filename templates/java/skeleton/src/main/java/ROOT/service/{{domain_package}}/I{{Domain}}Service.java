package {{rootPackage}}.service.{{domain_package}};

import {{rootPackage}}.dto.{{Domain}}DTO;
import {{rootPackage}}.service.model.PagedList;
import {{rootPackage}}.service.model.{{Domain}}ListFilter;
import org.springframework.data.domain.Pageable;

/**
 * {{Domain}} reads.
 */
public interface I{{Domain}}Service {

    /**
     * @param filter filter, every field optional
     * @param pageable page, size and sort
     * @return one page and the total count
     */
    PagedList<{{Domain}}DTO> getPagedList({{Domain}}ListFilter filter, Pageable pageable);

    /**
     * @param entityId public id
     * @return the {{domain}}
     * @throws {{rootPackage}}.exception.EntityNotFoundException when there is none (404)
     */
    {{Domain}}DTO getByEntityId(String entityId);
}
