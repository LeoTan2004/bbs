package edu.xtu.bbs.user.config;

import edu.xtu.bbs.common.trace.TraceIdFilter;
import edu.xtu.bbs.user.filter.*;
import edu.xtu.bbs.user.repo.UserRepository;
import edu.xtu.bbs.user.service.UserBinderService;
import edu.xtu.bbs.user.service.UserService;
import edu.xtu.bbs.user.service.WeChatAppService;
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
import org.springframework.security.web.session.SessionManagementFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Bean
    BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder,
                                                       VerificationService verificationService,
                                                       UserRepository userRepository,
                                                       UserService userService,
                                                       UserBinderService userBinderService,
                                                       WeChatAppService weChatAppService) {
        final DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userService::findByUsername);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);

        final EmailCodeAuthenticationProvider emailCodeAuthenticationProvider = new EmailCodeAuthenticationProvider(verificationService, userRepository, userBinderService);

        final WeChatAppAuthenticationProvider weChatAppAuthenticationProvider = new WeChatAppAuthenticationProvider(weChatAppService, userBinderService, userRepository);

        return new ProviderManager(daoAuthenticationProvider, emailCodeAuthenticationProvider, weChatAppAuthenticationProvider);

    }


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtEmailCodeAuthenticationFilter jwtEmailCodeAuthenticationFilter,
                                            JwtUsernamePasswordAuthenticationFilter jwtUsernamePasswordAuthenticationFilter,
                                            JwtWeChatAppAuthenticationFilter jwtWeChatAppAuthenticationFilter,
                                            JwtAuthorizationFilter jwtAuthorizationFilter,
                                            TraceIdFilter traceIdFilter,
                                            AuthenticationManager manager) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);


        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .anyRequest().authenticated()
        ).sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        ).authenticationManager(manager);
        // Disable default login forms
        http.formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        /*
        Spring Security Filter Chain Execution Order:

        request → [Spring Security Filter Chain] → controller

        Filter Chain Order:
        0. TraceIdFilter ← Custom TraceId filter (HIGHEST PRIORITY)
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

        6. JwtWeChatAppAuthenticationFilter ← Custom WeChat authentication
           ├─ Path matches: POST /auth/login-with-wechat → Process authentication
           └─ No match → Continue to next filter

        7. AnonymousAuthenticationFilter (Handle anonymous users)
        8. ExceptionTranslationFilter (Exception handling)
        9. FilterSecurityInterceptor (Authorization checks)

        Notes:
        - TraceIdFilter is placed BEFORE all other filters to ensure TraceId is available throughout the entire request
        - All custom filters are part of the Spring Security Filter Chain
        - JWT authorization filter skips /auth/** paths to avoid interfering with login flow
        - After successful authentication, subsequent authentication filters are usually skipped
        */

        // Add custom filters to the filter chain
        // TraceIdFilter should be the FIRST filter to execute
        http.addFilterBefore(traceIdFilter, SessionManagementFilter.class)
                // JWT authorization filter should be before authentication filters but after security context establishment
                .addFilterBefore(jwtAuthorizationFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(jwtEmailCodeAuthenticationFilter, JwtAuthorizationFilter.class)
                .addFilterAfter(jwtUsernamePasswordAuthenticationFilter, JwtAuthorizationFilter.class)
                .addFilterAfter(jwtWeChatAppAuthenticationFilter, JwtAuthorizationFilter.class);
        return http.build();
    }
}