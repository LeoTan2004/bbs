package edu.xtu.bbs.user.config;

import edu.xtu.bbs.user.filter.EmailCodeAuthenticationProvider;
import edu.xtu.bbs.user.filter.JwtAuthorizationFilter;
import edu.xtu.bbs.user.filter.JwtEmailCodeAuthenticationFilter;
import edu.xtu.bbs.user.filter.JwtUsernamePasswordAuthenticationFilter;
import edu.xtu.bbs.user.repo.UserRepository;
import edu.xtu.bbs.user.service.UserService;
import edu.xtu.bbs.verification.VerificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Bean
    BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder, VerificationService verificationService, UserRepository userRepository, UserService userService) {
        final DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userService::findByUsername);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);

        final EmailCodeAuthenticationProvider emailCodeAuthenticationProvider = new EmailCodeAuthenticationProvider(verificationService, userRepository);

        return new ProviderManager(daoAuthenticationProvider, emailCodeAuthenticationProvider);

    }


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtEmailCodeAuthenticationFilter jwtEmailCodeAuthenticationFilter,
                                            JwtUsernamePasswordAuthenticationFilter jwtUsernamePasswordAuthenticationFilter,
                                            JwtAuthorizationFilter jwtAuthorizationFilter,
                                            AuthenticationManager manager) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);

        // Disable default login forms
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);

        /*
        Spring Security Filter Chain Execution Order:
        
        request → [Spring Security Filter Chain] → controller
        
        Filter Chain Order:
        1. SecurityContextPersistenceFilter (Manage SecurityContext)
        2. LogoutFilter (Handle logout requests)
        3. JwtAuthorizationFilter ← Custom JWT authorization filter
           ├─ Path matches /auth/** → Skip JWT verification (avoid interfering with login flow)
           ├─ No Authorization header/Non-Bearer → Continue to next filter
           ├─ Valid JWT → Set authentication info, subsequent auth filters may be skipped
           └─ Invalid JWT → Clear SecurityContext, continue to next filter
        
        4. JwtEmailCodeAuthenticationFilter ← Custom email code authentication
           ├─ Path matches: POST /auth/login-with-code → Process authentication
           └─ No match → Continue to next filter
        
        5. JwtUsernamePasswordAuthenticationFilter ← Custom username/password authentication
           ├─ Path matches: POST /auth/login → Process authentication
           └─ No match → Continue to next filter
        
        6. AnonymousAuthenticationFilter (Handle anonymous users)
        7. ExceptionTranslationFilter (Exception handling)
        8. FilterSecurityInterceptor (Authorization checks)
        
        Notes: 
        - All custom filters are part of the Spring Security Filter Chain
        - JWT authorization filter skips /auth/** paths to avoid interfering with login flow
        - After successful authentication, subsequent authentication filters are usually skipped
        */

        // Add custom filters to the filter chain
        // JWT authorization filter should be before authentication filters but after security context establishment
        http.addFilterBefore(jwtAuthorizationFilter, BasicAuthenticationFilter.class);

        // Add custom authentication filters
        http.addFilterAfter(jwtEmailCodeAuthenticationFilter, jwtAuthorizationFilter.getClass());
        http.addFilterAfter(jwtUsernamePasswordAuthenticationFilter, jwtEmailCodeAuthenticationFilter.getClass());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .anyRequest().authenticated()
        ).sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        ).authenticationManager(manager);
        return http.build();
    }
}