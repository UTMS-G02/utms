package edu.iztech.utms.g02.utms_app.bl.auth;

import edu.iztech.utms.g02.utms_app.dal.user.entity.User;
import edu.iztech.utms.g02.utms_app.dal.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Implementation of Spring Security's {@link UserDetailsService}.
 * Loads a user from the database by email so Spring Security can authenticate them.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by email and returns a Spring Security {@link UserDetails} object.
     * Called by the JWT filter on every authenticated request.
     *
     * @param email the user's email address
     * @return UserDetails containing email, password hash, and granted authorities
     * @throws UsernameNotFoundException if no user exists with the given email or the account is inactive
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Inactive accounts are rejected before reaching the filter chain
        if (!user.isActive()) {
            throw new UsernameNotFoundException("User account is inactive.");
        }

        // Prefixing roles with ROLE_ is required by Spring Security's authorization conventions
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}