package pos.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        var authorities = authentication.getAuthorities();
        if (authorities.stream().anyMatch(item -> item.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/workspace/admin";
        }
        if (authorities.stream().anyMatch(item -> item.getAuthority().equals("ROLE_MANAGER"))) {
            return "redirect:/workspace/manager";
        }
        if (authorities.stream().anyMatch(item -> item.getAuthority().equals("ROLE_CASHIER"))) {
            return "redirect:/workspace/cashier";
        }
        return "auth/no-access";
    }

    @GetMapping("/workspace/cashier")
    public String cashier(Model model) {
        return "redirect:/shifts";
    }

    @GetMapping("/workspace/manager")
    public String manager(Model model) {
        return "redirect:/products";
    }

    @GetMapping("/workspace/admin")
    public String admin(Model model) {
        return "redirect:/accounts";
    }
}
