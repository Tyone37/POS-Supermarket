package pos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import pos.dto.CurrentShiftView;
import pos.service.ShiftService;

@Controller
@RequestMapping("/shifts")
public class ShiftController {

    private final ShiftService shifts;

    public ShiftController(ShiftService shifts) {
        this.shifts = shifts;
    }

    @GetMapping("/current")
    public String current(Model model) {
        CurrentShiftView view = shifts.current();
        model.addAttribute("currentShift", view);
        model.addAttribute("actorName", view.actorName());
        model.addAttribute("sectionTitle", "Ca làm việc");
        return "shift/current";
    }
}
