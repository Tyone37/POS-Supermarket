package pos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pos.entity.ReferenceStatus;
import pos.entity.Register;

public interface RegisterRepository extends JpaRepository<Register, Long> {
    Optional<Register> findByIdAndStatus(Long id, ReferenceStatus status);

    List<Register> findByStatusOrderByNameAscIdAsc(ReferenceStatus status);
}
