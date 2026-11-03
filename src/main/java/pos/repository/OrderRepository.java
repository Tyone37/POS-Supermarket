package pos.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pos.entity.Order;
import pos.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SaleOrder o where o.id = :id")
    Optional<Order> findLockedById(@Param("id") Long id);

    Optional<Order> findFirstByShiftIdAndStatusOrderByIdDesc(Long shiftId, OrderStatus status);
}
