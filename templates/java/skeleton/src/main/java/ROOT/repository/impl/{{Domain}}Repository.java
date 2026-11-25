package {{rootPackage}}.repository.impl;

import {{rootPackage}}.entity.common.AuditEntity_;
import {{rootPackage}}.entity.common.ExternalEntity_;
import {{rootPackage}}.entity.{{Domain}};
import {{rootPackage}}.entity.{{Domain}}_;
import {{rootPackage}}.repository.I{{Domain}}Repository;
import {{rootPackage}}.repository.projection.{{Domain}}ItemProjection;
import {{rootPackage}}.repository.projection.{{Domain}}Projection;
import {{rootPackage}}.service.model.{{Domain}}ListFilter;
import {{rootPackage}}.util.Check;
import {{rootPackage}}.util.SortHelper;
import {{rootPackage}}.util.StringHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Criteria API on the shared {@link EntityManager}. Attributes come from the generated metamodel
 * ({@code {{Domain}}_}), never from string names, so a renamed field breaks the build, not a request.
 */
@Repository
public class {{Domain}}Repository implements I{{Domain}}Repository {

    /** What a client may sort by besides {@code id}; anything else is a 400. */
    private static final Set<String> SORTABLE = Set.of(
            {{Domain}}_.NAME, {{Domain}}_.STATUS, AuditEntity_.CREATED_UTC);

    private final EntityManager entityManager;

    /**
     * Constructor with dependency injection.
     *
     * @param entityManager shared entity manager
     */
    public {{Domain}}Repository(final EntityManager entityManager) {
        Check.isNotNull(entityManager, "entityManager");

        this.entityManager = entityManager;
    }

    /**
     * Count first with the same predicates; the page query runs only when there is something to read.
     * Designed for extension.
     *
     * @param filter filter, every field optional
     * @param pageable page, size and sort
     * @return page of list rows with the total count
     */
    @Override
    public Page<{{Domain}}ItemProjection> findPagedList(final {{Domain}}ListFilter filter,
                                                        final Pageable pageable) {
        Check.isNotNull(filter, "filter");
        Check.isNotNull(pageable, "pageable");

        long total = count(filter);
        if (total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<{{Domain}}ItemProjection> query =
                criteriaBuilder.createQuery({{Domain}}ItemProjection.class);
        Root<{{Domain}}> root = query.from({{Domain}}.class);

        query.select(criteriaBuilder.construct({{Domain}}ItemProjection.class,
                        root.get(ExternalEntity_.entityId),
                        root.get({{Domain}}_.name),
                        root.get({{Domain}}_.status)))
                .where(predicates(criteriaBuilder, root, filter))
                .orderBy(SortHelper.getOrders(criteriaBuilder, root, pageable, SORTABLE));

        List<{{Domain}}ItemProjection> items = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(items, pageable, total);
    }

    /**
     * Designed for extension.
     *
     * @param entityId public id
     * @return the projection, empty when there is no such row
     */
    @Override
    public Optional<{{Domain}}Projection> findProjectionByEntityId(final String entityId) {
        Check.isNotEmpty(entityId, "entityId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<{{Domain}}Projection> query =
                criteriaBuilder.createQuery({{Domain}}Projection.class);
        Root<{{Domain}}> root = query.from({{Domain}}.class);

        query.select(criteriaBuilder.construct({{Domain}}Projection.class,
                        root.get(ExternalEntity_.entityId),
                        root.get({{Domain}}_.name),
                        root.get({{Domain}}_.description),
                        root.get({{Domain}}_.status),
                        root.get(AuditEntity_.createdBy),
                        root.get(AuditEntity_.createdUtc)))
                .where(criteriaBuilder.equal(root.get(ExternalEntity_.entityId), entityId));

        return entityManager.createQuery(query).getResultStream().findFirst();
    }

    /**
     * Designed for extension.
     *
     * @param entityId public id
     * @return the entity, empty when there is no such row
     */
    @Override
    public Optional<{{Domain}}> findByEntityId(final String entityId) {
        Check.isNotEmpty(entityId, "entityId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<{{Domain}}> query = criteriaBuilder.createQuery({{Domain}}.class);
        Root<{{Domain}}> root = query.from({{Domain}}.class);

        query.where(criteriaBuilder.equal(root.get(ExternalEntity_.entityId), entityId));

        return entityManager.createQuery(query).getResultStream().findFirst();
    }

    /**
     * Designed for extension.
     *
     * @param entityId public id
     * @return true when a row has this id
     */
    @Override
    public boolean existsByEntityId(final String entityId) {
        Check.isNotEmpty(entityId, "entityId");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = criteriaBuilder.createQuery(String.class);
        Root<{{Domain}}> root = query.from({{Domain}}.class);

        query.select(root.get(ExternalEntity_.entityId))
                .where(criteriaBuilder.equal(root.get(ExternalEntity_.entityId), entityId));

        return !entityManager.createQuery(query).setMaxResults(1).getResultList().isEmpty();
    }

    /**
     * Designed for extension.
     *
     * @param name name
     * @param excludedEntityId a row to ignore, or null
     * @return true when another row has this name
     */
    @Override
    public boolean existsByName(final String name, final String excludedEntityId) {
        Check.isNotEmpty(name, "name");

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = criteriaBuilder.createQuery(String.class);
        Root<{{Domain}}> root = query.from({{Domain}}.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get({{Domain}}_.name)),
                name.toLowerCase(Locale.ROOT)));
        if (excludedEntityId != null) {
            predicates.add(criteriaBuilder.notEqual(root.get(ExternalEntity_.entityId), excludedEntityId));
        }

        query.select(root.get(ExternalEntity_.entityId)).where(predicates.toArray(new Predicate[0]));

        return !entityManager.createQuery(query).setMaxResults(1).getResultList().isEmpty();
    }

    /**
     * Designed for extension.
     *
     * @param entity new entity
     */
    @Override
    public void save(final {{Domain}} entity) {
        Check.isNotNull(entity, "entity");

        entityManager.persist(entity);
    }

    /**
     * Designed for extension.
     *
     * @param entity managed entity to delete
     */
    @Override
    public void delete(final {{Domain}} entity) {
        Check.isNotNull(entity, "entity");

        entityManager.remove(entity);
    }

    private long count(final {{Domain}}ListFilter filter) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<{{Domain}}> root = query.from({{Domain}}.class);

        query.select(criteriaBuilder.count(root)).where(predicates(criteriaBuilder, root, filter));

        return entityManager.createQuery(query).getSingleResult();
    }

    // One builder for the page and the count query, so both always filter alike.
    private Predicate[] predicates(final CriteriaBuilder criteriaBuilder, final Root<{{Domain}}> root,
                                   final {{Domain}}ListFilter filter) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter.ids() != null && !filter.ids().isEmpty()) {
            predicates.add(root.get(ExternalEntity_.entityId).in(filter.ids()));
        }

        if (filter.status() != null) {
            predicates.add(criteriaBuilder.equal(root.get({{Domain}}_.status), filter.status()));
        }

        if (!StringHelper.isEmpty(filter.textSearch())) {
            String pattern = SortHelper.containsPattern(filter.textSearch().trim());
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get({{Domain}}_.name)),
                            pattern, SortHelper.LIKE_ESCAPE),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get({{Domain}}_.description)),
                            pattern, SortHelper.LIKE_ESCAPE)));
        }

        return predicates.toArray(new Predicate[0]);
    }
}
