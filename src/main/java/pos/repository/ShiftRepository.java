package pos.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pos.entity.Shift;
import pos.entity.ShiftStatus;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    Optional<Shift> findFirstByEmployeeIdAndStatusOrderByClosedAtDescIdDesc(Long employeeId, ShiftStatus status);
}
