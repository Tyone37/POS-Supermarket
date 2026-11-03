package pos.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pos.dto.CurrentShiftView;
import pos.dto.OpenShiftRequest;
import pos.dto.ShiftOpeningResult;
import pos.exception.FormValidationException;
import pos.service.CurrentActorService;
import pos.service.ShiftOpeningService;
import pos.service.ShiftService;

@Controller
@RequestMapping("/shifts")
public class ShiftController {

    private final ShiftService shifts;
    private final ShiftOpeningService openings;
    private final CurrentActorService actors;

    public ShiftController(ShiftService shifts, ShiftOpeningService openings, CurrentActorService actors) {
        this.shifts = shifts;
        this.openings = openings;
        this.actors = actors;
    }

    @GetMapping("/current")
    public String current(Model model) {
        CurrentShiftView view = shifts.current();
        model.addAttribute("currentShift", view);
        model.addAttribute("actorName", view.actorName());
        model.addAttribute("sectionTitle", "Ca làm việc");
        return "shift/current";
    }

    @GetMapping("/new")
    public String form(Model model) {
        model.addAttribute("opening", new OpenShiftRequest(null, null, UUID.randomUUID().toString()));
        populateForm(model);
        return "shift/new";
    }

    @PostMapping
    public String open(@Valid @ModelAttribute("opening") OpenShiftRequest request, BindingResult errors,
            Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                ShiftOpeningResult result = openings.open(request);
                redirect.addFlashAttribute("successMessage", result.created() ? "Mở ca thành công"
                        : "Yêu cầu đã được xử lý hoặc bạn đã có ca đang mở. Không tạo thêm ca.");
                return "redirect:/shifts/current";
            } catch (FormValidationException exception) {
                exception.getFieldErrors().forEach((field, message) -> {
                    if (field.isEmpty()) {
                        errors.reject("shift.open", message);
                    } else {
                        errors.rejectValue(field, "shift.open", message);
                    }
                });
            }
        }
        populateForm(model);
        return "shift/new";
    }

    private void populateForm(Model model) {
        model.addAttribute("registers", openings.registers());
        model.addAttribute("actorName", actors.requireEmployee().fullName());
        model.addAttribute("sectionTitle", "Mở ca làm việc");
    }
}
