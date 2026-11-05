package pos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import pos.service.CustomUserDetailsService;
import pos.repository.AccountRepository;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService users, PasswordEncoder encoder) {
        // Check credentials before account status so a wrong password does not disclose locked accounts.
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        provider.setPreAuthenticationChecks(user -> { });
        provider.setPostAuthenticationChecks(user -> {
            if (!user.isEnabled()) {
                throw new org.springframework.security.authentication.DisabledException("Tài khoản đã bị khóa.");
            }
        });
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider provider,
                                                   AccountRepository accounts)
            throws Exception {
        http.authenticationProvider(provider)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/assets/**", "/demo/**", "/error").permitAll()
                        .requestMatchers("/workspace/cashier").hasRole("CASHIER")
                        .requestMatchers("/workspace/manager").hasRole("MANAGER")
                        .requestMatchers("/workspace/admin").hasRole("ADMIN")
                        .requestMatchers("/accounts", "/accounts/**").hasRole("ADMIN")
                        .requestMatchers("/products", "/products/**").hasRole("MANAGER")
                        .requestMatchers("/shifts", "/shifts/**", "/sales", "/sales/**").hasRole("CASHIER")
                        .requestMatchers("/invoices/**").hasAnyRole("CASHIER", "MANAGER")
                        .requestMatchers("/").authenticated()
                        .anyRequest().denyAll())
                .addFilterBefore(new ActiveAccountFilter(accounts), AuthorizationFilter.class)
                .formLogin(form -> form.loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureHandler((request, response, exception) -> {
                            String reason = exception instanceof
                                    org.springframework.security.authentication.DisabledException ? "locked" : "invalid";
                            response.sendRedirect(request.getContextPath() + "/login?error=" + reason);
                        }).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID"));
        return http.build();
    }
}
