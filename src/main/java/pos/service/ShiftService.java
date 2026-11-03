package pos.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pos.dto.CurrentActor;
import pos.dto.CurrentShiftView;
import pos.entity.Shift;
import pos.entity.ShiftStatus;
import pos.repository.ShiftRepository;

@Service
@PreAuthorize("hasAuthority('SHIFT_VIEW')")
public class ShiftService {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private final CurrentActorService actors;
    private final ShiftRepository shifts;

    public ShiftService(CurrentActorService actors, ShiftRepository shifts) {
        this.actors = actors;
        this.shifts = shifts;
    }

    @Transactional(readOnly = true)
    public CurrentShiftView current() {
        CurrentActor actor = actors.requireEmployee();
        Shift shift = shifts.findFirstByEmployeeIdAndStatusOrderByClosedAtDescIdDesc(actor.employeeId(), ShiftStatus.OPEN)
                .or(() -> shifts.findFirstByEmployeeIdAndStatusOrderByClosedAtDescIdDesc(actor.employeeId(), ShiftStatus.CLOSED))
                .orElse(null);
        if (shift == null) {
            return new CurrentShiftView("NOT_OPEN", null, null, null, null, null, null, actor.fullName());
        }
        return new CurrentShiftView(shift.getStatus().name(), shift.getId(), shift.getShiftNo(),
                shift.getRegister().getName(), format(shift.getOpenedAt()), format(shift.getClosedAt()),
                shift.getOpeningCash(), actor.fullName());
    }

    private String format(Instant value) {
        return value == null ? null : DISPLAY_TIME.format(value);
    }
}
