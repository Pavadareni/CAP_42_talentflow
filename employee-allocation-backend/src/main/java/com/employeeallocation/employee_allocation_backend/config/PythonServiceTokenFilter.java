
package com.employeeallocation.employee_allocation_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class PythonServiceTokenFilter extends OncePerRequestFilter {

    private static final String TOKEN_HEADER = "X-SERVICE-TOKEN";
    private static final String BULK_PATH = "/api/allocations/bulk";

    private final String expectedToken;

    public PythonServiceTokenFilter(
            @Value("${security.integration.python-token}")
            String expectedToken) {

        if (expectedToken == null || expectedToken.isBlank()) {
            throw new IllegalStateException(
                    "PYTHON_SERVICE_TOKEN must be configured"
            );
        }

        this.expectedToken = expectedToken;
    }

    private boolean isPythonReadEndpoint(
            HttpServletRequest request) {

        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = request.getServletPath();

        return path.equals("/api/employees")
                || path.matches("/api/projects/\\d+")
                || path.matches("/api/projects/\\d+/skills")
                || path.matches("/api/employees/\\d+/skills")
                || path.matches("/api/employees/\\d+/availability")
                || path.matches("/api/employees/\\d+/allocations");
    }

    private boolean isBulkEndpoint(
            HttpServletRequest request) {

        return "POST".equalsIgnoreCase(request.getMethod())
                && BULK_PATH.equals(request.getServletPath());
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request) {

        boolean bulkRequest = isBulkEndpoint(request);
        boolean readRequest = isPythonReadEndpoint(request);

        if (!bulkRequest && !readRequest) {
            return true;
        }

        // Normal JWT users can access read endpoints without
        // providing the Python service token.
        if (readRequest &&
                request.getHeader(TOKEN_HEADER) == null) {
            return true;
        }

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String suppliedToken = request.getHeader(TOKEN_HEADER);

        boolean valid = suppliedToken != null
                && MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8)
        );

        if (!valid) {
            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType("application/json");

            response.getWriter().write(
                    "{\"error\":\"Invalid or missing service token\"}"
            );

            return;
        }

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "allocation-engine",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_SYSTEM")
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
