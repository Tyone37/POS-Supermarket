package pos.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pos.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByAccountId(Long accountId);
}
