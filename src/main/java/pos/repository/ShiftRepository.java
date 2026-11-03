package pos.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pos.entity.Shift;
import pos.entity.ShiftStatus;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shift s where s.employee.id = :employeeId and s.status = pos.entity.ShiftStatus.OPEN")
    Optional<Shift> findOpenLocked(@Param("employeeId") Long employeeId);

    Optional<Shift> findFirstByEmployeeIdAndStatusOrderByClosedAtDescIdDesc(Long employeeId, ShiftStatus status);

    Optional<Shift> findByOpenRequestKey(String openRequestKey);
}
