package pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "units")
public class UnitEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 12)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "quantity_scale", nullable = false, columnDefinition = "tinyint unsigned")
    private Integer quantityScale;

    public UnitEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantityScale() {
        return quantityScale;
    }

    public void setQuantityScale(Integer quantityScale) {
        this.quantityScale = quantityScale;
    }
}
