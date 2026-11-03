package pos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pos.entity.Unit;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    Optional<Unit> findByCode(String code);
}
