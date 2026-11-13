
package com.employeeallocation.employee_allocation_backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final PythonServiceTokenFilter pythonServiceTokenFilter;

    public SecurityConfig(
            PythonServiceTokenFilter pythonServiceTokenFilter) {
        this.pythonServiceTokenFilter = pythonServiceTokenFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()

                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login"
                        ).permitAll()

                        // Python service and authorized users can read data.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/employees",
                                "/api/projects/*",
                                "/api/projects/*/skills",
                                "/api/employees/*/skills",
                                "/api/employees/*/availability",
                                "/api/employees/*/allocations"
                        ).hasAnyRole("ADMIN", "MANAGER", "SYSTEM")

                        // Python service only.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/allocations/bulk"
                        ).hasRole("SYSTEM")

                        // All remaining API requests require authentication.
                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                )

                // Register the Python service filter exactly once.
                .addFilterBefore(
                        pythonServiceTokenFilter,
                        BearerTokenAuthenticationFilter.class
                );

        return http.build();
    }

    // Prevent automatic servlet-container registration.
    // The filter is managed through Spring Security only.
    @Bean
    public FilterRegistrationBean<PythonServiceTokenFilter>
    pythonServiceFilterRegistration() {

        FilterRegistrationBean<PythonServiceTokenFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(pythonServiceTokenFilter);
        registration.setEnabled(false);

        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        config.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "X-SERVICE-TOKEN"
                )
        );

        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
