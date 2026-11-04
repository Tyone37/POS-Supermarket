package pos.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Validator;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import pos.dto.CurrentActor;
import pos.dto.OpenShiftRequest;
import pos.dto.RegisterOption;
import pos.dto.ShiftOpeningResult;
import pos.entity.Employee;
import pos.entity.ReferenceStatus;
import pos.entity.Register;
import pos.entity.Shift;
import pos.entity.ShiftStatus;
import pos.exception.FormValidationException;
import pos.repository.EmployeeRepository;
import pos.repository.RegisterRepository;
import pos.repository.ShiftRepository;

@Service
@PreAuthorize("hasAuthority('SHIFT_OPEN')")
public class ShiftOpeningService {
    private final CurrentActorService actors;
    private final EmployeeRepository employees;
    private final RegisterRepository registers;
    private final ShiftRepository shifts;
    private final Validator validator;
    private final EntityManager entityManager;

    public ShiftOpeningService(CurrentActorService actors, EmployeeRepository employees,
            RegisterRepository registers, ShiftRepository shifts, Validator validator, EntityManager entityManager) {
        this.actors = actors;
        this.employees = employees;
        this.registers = registers;
        this.shifts = shifts;
        this.validator = validator;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<RegisterOption> registers() {
        actors.requireEmployee();
        return registers.findByStatusOrderByNameAscIdAsc(ReferenceStatus.ACTIVE).stream()
                .map(register -> new RegisterOption(register.getId(), register.getName())).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShiftOpeningResult open(OpenShiftRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (request == null) {
            throw new FormValidationException(Map.of("", "Yêu cầu không hợp lệ."));
        }
        validator.validate(request).forEach(violation ->
                errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        if (!errors.isEmpty()) {
            throw new FormValidationException(errors);
        }
        CurrentActor actor = actors.requireEmployee();
        Employee employee = employees.findLockedById(actor.employeeId())
                .orElseThrow(() -> new AccessDeniedException("Không có quyền truy cập."));
        // requireEmployee loaded this entity before the lock; reload its current state.
        entityManager.refresh(employee, LockModeType.PESSIMISTIC_WRITE);
        if (employee.getStatus() != ReferenceStatus.ACTIVE
                || !employee.getAccount().getId().equals(actor.accountId())) {
            throw new AccessDeniedException("Không có quyền truy cập.");
        }
        Shift existing = shifts.findByOpenRequestKey(request.requestKey()).orElse(null);
        if (existing != null) {
            if (!existing.getEmployee().getId().equals(employee.getId())
                    || !existing.getRegister().getId().equals(request.registerId())
                    || existing.getOpeningCash().compareTo(request.openingCash()) != 0) {
                throw new FormValidationException(Map.of("requestKey", "Yêu cầu bị trùng với dữ liệu khác."));
            }
            return new ShiftOpeningResult(existing.getId(), false);
        }
        Shift open = shifts.findFirstByEmployeeIdAndStatusOrderByClosedAtDescIdDesc(employee.getId(), ShiftStatus.OPEN)
                .orElse(null);
        if (open != null) {
            return new ShiftOpeningResult(open.getId(), false);
        }
        Register register = registers.findByIdAndStatus(request.registerId(), ReferenceStatus.ACTIVE)
                .orElseThrow(() -> new FormValidationException(Map.of("registerId", "Quầy không hợp lệ.")));
        String shiftNo = "CA" + UUID.randomUUID().toString().replace("-", "").substring(0, 28).toUpperCase(java.util.Locale.ROOT);
        Shift shift = new Shift(employee, register, shiftNo, request.requestKey(), request.openingCash(),
                Instant.now().truncatedTo(ChronoUnit.MILLIS));
        shifts.saveAndFlush(shift);
        return new ShiftOpeningResult(shift.getId(), true);
    }
}
