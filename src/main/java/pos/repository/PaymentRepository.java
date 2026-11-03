package pos.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pos.dto.PaymentScope;
import pos.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("select new pos.dto.PaymentScope(p.order.id, p.order.shift.id) from Payment p where p.id = :id")
    Optional<PaymentScope> findScopeById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findLockedById(@Param("id") Long id);
}
