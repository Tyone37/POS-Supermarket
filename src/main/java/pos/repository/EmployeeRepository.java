package pos.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pos.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByAccountId(Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> findLockedById(@Param("id") Long id);
}
