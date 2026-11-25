package {{rootPackage}}.repository;

import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.repository.projection.{{Domain}}ItemProjection;
import {{rootPackage}}.repository.projection.{{Domain}}Projection;
import {{rootPackage}}.service.model.{{Domain}}ListFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * {{Domain}} persistence. Reads return projections; only the write paths load the entity.
 */
public interface I{{Domain}}Repository {

    /**
     * One page of the list, filtered and sorted.
     *
     * @param filter filter, every field optional
     * @param pageable page, size and sort
     * @return page of list rows with the total count
     */
    Page<{{Domain}}ItemProjection> findPagedList({{Domain}}ListFilter filter, Pageable pageable);

    /**
     * The detail read.
     *
     * @param entityId public id
     * @return the projection, empty when there is no such row
     */
    Optional<{{Domain}}Projection> findProjectionByEntityId(String entityId);

    /**
     * The managed entity, for a write.
     *
     * @param entityId public id
     * @return the entity, empty when there is no such row
     */
    Optional<{{Domain}}> findByEntityId(String entityId);

    /**
     * @param entityId public id
     * @return true when a row has this id
     */
    boolean existsByEntityId(String entityId);

    /**
     * Case-insensitive name check.
     *
     * @param name name
     * @param excludedEntityId a row to ignore (the one being modified), or null
     * @return true when another row has this name
     */
    boolean existsByName(String name, String excludedEntityId);

    /**
     * Inserts a new row. Changes to a loaded entity need no call: it is managed and flushed on commit.
     *
     * @param entity new entity
     */
    void save({{Domain}} entity);

    /**
     * @param entity managed entity to delete
     */
    void delete({{Domain}} entity);
}
