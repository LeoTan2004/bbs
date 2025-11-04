package edu.xtu.bbs.user.config;

import edu.xtu.bbs.user.filter.EmailCodeAuthenticationFilter;
import edu.xtu.bbs.user.filter.EmailCodeAuthenticationProvider;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    UserDetailsService userDetailsService(UserService userService) {
        return userService::findByUsername;
    }

    @Bean
    BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, VerificationService verificationService, UserRepository userRepository) {
        final DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);

        final EmailCodeAuthenticationProvider emailCodeAuthenticationProvider = new EmailCodeAuthenticationProvider(verificationService, userRepository);

        return new ProviderManager(daoAuthenticationProvider, emailCodeAuthenticationProvider);

    }


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, EmailCodeAuthenticationFilter emailCodeAuthenticationFilter, AuthenticationManager manager) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);
        http.addFilterBefore(emailCodeAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.formLogin(form ->
                form.loginProcessingUrl("/auth/login").successForwardUrl("/home")
        );

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login/**", "/auth/**").permitAll()
                .anyRequest().authenticated()
        ).sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        ).authenticationManager(manager);
        return http.build();
    }
}