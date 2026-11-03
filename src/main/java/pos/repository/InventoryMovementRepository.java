package pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pos.entity.InventoryMovement;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
}
