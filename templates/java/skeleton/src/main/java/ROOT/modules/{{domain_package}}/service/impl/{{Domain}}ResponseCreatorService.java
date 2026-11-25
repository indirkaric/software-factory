package {{rootPackage}}.modules.{{domain_package}}.service.impl;

import {{rootPackage}}.mapper.I{{Domain}}Mapper;
import {{rootPackage}}.modules.{{domain_package}}.model.{{Domain}}Response;
import {{rootPackage}}.modules.{{domain_package}}.service.I{{Domain}}ResponseCreatorService;
import {{rootPackage}}.service.{{domain_package}}.I{{Domain}}Service;
import {{rootPackage}}.util.Check;
import org.springframework.stereotype.Service;

/**
 * Read service + mapper → API model.
 */
@Service
public class {{Domain}}ResponseCreatorService implements I{{Domain}}ResponseCreatorService {

    private final I{{Domain}}Service service;

    private final I{{Domain}}Mapper mapper;

    /**
     * Constructor with dependency injection.
     *
     * @param service {{domain}} read service
     * @param mapper {{domain}} mapper
     */
    public {{Domain}}ResponseCreatorService(final I{{Domain}}Service service,
                                            final I{{Domain}}Mapper mapper) {
        Check.isNotNull(service, "service");
        Check.isNotNull(mapper, "mapper");

        this.service = service;
        this.mapper = mapper;
    }

    /**
     * Designed for extension.
     *
     * @param entityId public id
     * @return the response
     */
    @Override
    public {{Domain}}Response create(final String entityId) {
        Check.isNotEmpty(entityId, "entityId");

        return mapper.toResponse(service.getByEntityId(entityId));
    }
}
