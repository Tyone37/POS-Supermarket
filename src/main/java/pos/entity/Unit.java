package pos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "units")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 12)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "quantity_scale", nullable = false, columnDefinition = "tinyint unsigned")
    private byte quantityScale;

    protected Unit() {
    }

    public Long getId() { return id; }

    public String getCode() { return code; }

    public String getName() { return name; }

    public int getQuantityScale() { return quantityScale; }
}
