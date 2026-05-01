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

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // E-posta ile veritabanından kullanıcı aranıyor
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));

        // Kullanıcı hesabı pasif ise (dondurulmuşsa) engelliyoruz
        if (!user.isActive()) {
            throw new UsernameNotFoundException("Kullanıcı aktif değil.");
        }

        // Spring Security'nin kendi UserDetails nesnesini dönüyoruz
        // Rolleri ROLE_ prefixi ile tanımlamak bir standarttır.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
