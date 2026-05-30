package com.autowashpro.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/tiers/**").permitAll()
                        .requestMatchers("/api/vehicle-types/**").permitAll()
                        .requestMatchers("/api/services/**").permitAll()
                        .requestMatchers("/api/bookings/slots").permitAll()
                        .requestMatchers("/api/transactions/payos/webhook").permitAll()
                        .requestMatchers("/api/promotions/active").permitAll()
                        .requestMatchers("/api/promotions/check/**").permitAll()
                        .requestMatchers("/api/loyalty/rewards").permitAll() // Xem danh sách quà thì ai xem cũng được
                        .requestMatchers("/api/loyalty/**").authenticated() // Tất cả các hành động đổi quà, xem lịch sử
                                                                            // cần phải ĐĂNG NHẬP
                        .requestMatchers(HttpMethod.POST, "/api/contacts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/contacts/my").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/contacts/*/reply-customer").authenticated()
                        .requestMatchers("/api/contacts/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/manager/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider()) // ← thêm dòng này
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/tiers/**").permitAll()
                .requestMatchers("/api/vehicle-types/**").permitAll()
                .requestMatchers("/api/services/**").permitAll()
                .requestMatchers("/api/bookings/slots").permitAll()
                .requestMatchers("/api/transactions/payos/webhook").permitAll()
                .requestMatchers("/api/promotions/active").permitAll()
                .requestMatchers("/api/promotions/check/**").permitAll()
                .requestMatchers("/api/loyalty/rewards").permitAll() // Xem danh sách quà thì ai xem cũng được
                .requestMatchers("/api/loyalty/**").authenticated()   // Tất cả các hành động đổi quà, xem lịch sử cần phải ĐĂNG NHẬP
                .requestMatchers(HttpMethod.POST, "/api/contacts").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/contacts/my").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/contacts/*/reply-customer").authenticated()
                .requestMatchers("/api/contacts/**").hasAnyRole("STAFF","MANAGER","ADMIN")
                .requestMatchers("/api/feedbacks/public").permitAll()
                .requestMatchers("/api/feedbacks/booking/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/feedbacks").authenticated()
                .requestMatchers("/api/feedbacks/**").hasAnyRole("STAFF","MANAGER","ADMIN")
                .requestMatchers("/api/walkin/**").hasAnyRole("STAFF","MANAGER","ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/manager/**").hasAnyRole("MANAGER","ADMIN")
                .requestMatchers("/api/staff/**").hasAnyRole("STAFF","MANAGER","ADMIN")
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider()) // ← thêm dòng này
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ← thêm bean này — quan trọng nhất!
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
