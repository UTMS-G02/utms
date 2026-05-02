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

        // Header'da "Bearer " yoksa, isteği serbest bırak (SecurityConfig'de korunanlar yine engellenecek)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // "Bearer " sonrasını al
        try {
            userEmail = jwtService.extractEmail(jwt); // Token'dan emaili çıkar

            // Token'da e-posta var ve kullanıcı henüz SecurityContext'te doğrulanmamışsa:
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Kullanıcıyı veritabanından çek (Roller vs. yüklenir)
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Token geçerliliği doğrulanıyorsa
                if (jwtService.isTokenValid(jwt)) {
                    // Kullanıcı için yetkilendirme objesi (Token) oluştur
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // İşlemler başarılı, artık kullanıcı kimliği doğrulanmış olarak kaydedilir
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token süresi geçmiş veya hatalı ise yakalanır.
            // İşlem sessizce devam eder ve Spring Security erişimi reddeder (HTTP 401/403)
        }

        // Filtre zincirinin çalışmasına devam et
        filterChain.doFilter(request, response);
    }
}
