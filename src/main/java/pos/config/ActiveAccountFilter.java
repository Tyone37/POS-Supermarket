package pos.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import pos.repository.AccountRepository;

public class ActiveAccountFilter extends OncePerRequestFilter {
    private final AccountRepository accounts;
    public ActiveAccountFilter(AccountRepository accounts) { this.accounts = accounts; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/login") || path.equals("/error") || path.startsWith("/css/")
                || path.startsWith("/img/") || path.startsWith("/js/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            var account = accounts.findByUsername(authentication.getName());
            if (account.isEmpty() || !"ACTIVE".equals(account.get().status())) {
                if (request.getSession(false) != null) { request.getSession(false).invalidate(); }
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/login?error=locked");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
