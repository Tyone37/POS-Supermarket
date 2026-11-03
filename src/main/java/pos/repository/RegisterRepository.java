package pos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pos.entity.Register;

public interface RegisterRepository extends JpaRepository<Register, Long> { }
