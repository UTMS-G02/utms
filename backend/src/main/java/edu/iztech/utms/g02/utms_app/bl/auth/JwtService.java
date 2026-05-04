package edu.iztech.utms.g02.utms_app.bl.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Service for generating and validating JWT tokens.
 * Tokens are signed with HMAC-SHA and contain the user's email as the subject.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Generates a signed JWT token for the given email.
     *
     * @param email the user's email, stored as the token subject
     * @return signed JWT token string
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the email (subject) from a JWT token.
     *
     * @param token the JWT token string
     * @return the email stored in the token
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public String extractEmail(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Checks whether a JWT token is valid (well-formed, correctly signed, not expired).
     *
     * @param token the JWT token string
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates a short-lived (15 min) password reset token.
     * Carries a "type=RESET" claim so it cannot be used for authentication.
     */
    public String generateResetToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claims(Map.of("type", "RESET"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Returns the email from a reset token after validating its signature,
     * expiry, and type claim.
     *
     * @throws JwtException if the token is invalid, expired, or not a reset token
     */
    public String extractEmailFromResetToken(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!"RESET".equals(claims.get("type", String.class))) {
            throw new JwtException("Not a password reset token");
        }

        return claims.getSubject();
    }

    /**
     * Decodes the Base64 secret from application.yml and returns an HMAC-SHA signing key.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}