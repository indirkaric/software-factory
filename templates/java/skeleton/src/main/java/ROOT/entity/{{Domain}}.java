package {{rootPackage}}.entity;

import {{rootPackage}}.entity.common.ExternalEntity;
import {{rootPackage}}.entity.listener.BaseEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * {{Domain}}: the reference entity. Internal {@code id}, public {@code entityId} and the audit columns
 * come from {@link ExternalEntity}; {@link BaseEntityListener} fills the audit columns.
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@EntityListeners(BaseEntityListener.class)
@Table(name = "{{domain_table}}", schema = "{{schema}}")
public class {{Domain}} extends ExternalEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "status", nullable = false)
    private Short status;
}
