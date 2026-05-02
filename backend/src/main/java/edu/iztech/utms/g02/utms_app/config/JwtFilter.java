package edu.iztech.utms.g02.utms_app.config;

import edu.iztech.utms.g02.utms_app.bl.auth.JwtService;
import edu.iztech.utms.g02.utms_app.bl.auth.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter that runs once per request.
 * Reads the Authorization header, validates the token, and sets the authenticated
 * user in Spring Security's SecurityContext so that protected endpoints can be accessed.
 *
 * <p>If the token is missing, invalid, or expired, the filter passes the request
 * through without setting authentication. Spring Security then rejects the request
 * with HTTP 401 if the endpoint requires authentication.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // If there is no Bearer token, pass the request through.
        // SecurityConfig will still block protected endpoints.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Strip "Bearer " prefix
        try {
            userEmail = jwtService.extractEmail(jwt);

            // Only authenticate if the token contains an email and the user
            // is not already authenticated in this request
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load full user details (including roles) from the database
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt)) {
                    // Create an authentication token and register it in the SecurityContext
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Invalid or expired token — authentication is skipped.
            // Spring Security will return HTTP 401 for protected endpoints.
        }

        filterChain.doFilter(request, response);
    }
}