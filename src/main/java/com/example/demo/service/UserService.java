package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.token.ConfirmationToken;
import com.example.demo.token.ConfirmationTokenService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {
    private final static String USER_NOT_FOUND_MSG = "user with email %s not found";
    private final static String USER_ALREADY_EXIST = "user with email %s already exist and enabled";
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final ConfirmationTokenService confirmationTokenService;
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                String.format(USER_NOT_FOUND_MSG, email)
                        ));
    }

    public String signUp(User user) {
        String email = user.getUsername();
        boolean userExists = userRepository.findByEmail(email).isPresent();

        if (userExists && userRepository.isUserEnabledByEmail(email)) {
            throw new IllegalStateException(
                    String.format(USER_ALREADY_EXIST, email)
            );
        }

        if (!userExists) {
            String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
            userRepository.save(user);
        }

        String token = UUID.randomUUID().toString();

        ConfirmationToken tokenObject = new ConfirmationToken(
                token,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(1),
                user
        );

        confirmationTokenService.saveConfirmationToken(tokenObject);

        return token;
    }

    public int enableUser(String email) {
        return userRepository.enableUser(email);
    }

}
