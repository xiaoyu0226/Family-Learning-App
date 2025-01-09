package com.example.demo.registration;

import com.example.demo.email.EmailSender;
import com.example.demo.email.EmailValidator;
import com.example.demo.model.AppUserRole;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import com.example.demo.token.ConfirmationToken;
import com.example.demo.token.ConfirmationTokenService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Getter
@Setter
@AllArgsConstructor
public class RegistrationService {

    private final EmailValidator emailValidator;
    private final EmailSender emailSender;
    private final UserService userService;
    private final ConfirmationTokenService confirmationTokenService;
    private final String TOKEN_NOT_FOUND_ERROR = "token is not found";
    private final String ACCOUNT_ALREADY_CONFIRMED = "account email already confirmed";
    private final String TOKEN_EXPIRED_ERROR = "token already expired";

    public String register(RegistrationRequest request) {
        String email = request.getEmail();
        boolean emailValid = emailValidator.validate(email);

        if (!emailValid) {
            throw new IllegalStateException("email not valid");
        }

        User user = new User(
                request.getFirstname(),
                request.getLastname(),
                request.getEmail(),
                request.getPassword(),
                AppUserRole.ROLE_USER
        );

        String token = userService.signUp(user);

        String link = "http://localhost:8080/app/registration/confirm?token=" + token;
        String firstname = request.getFirstname();
        emailSender.send(
                email,
                link,
                firstname);

        return token;
    }

    // note this part is from amigo code

    public String confirmToken(String token) {
        ConfirmationToken confirmationToken = confirmationTokenService
                .getToken(token).orElseThrow(() -> new IllegalStateException(TOKEN_NOT_FOUND_ERROR));

        if (confirmationToken.getConfirmedAt() != null) {
            throw new IllegalStateException(ACCOUNT_ALREADY_CONFIRMED);
        }

        LocalDateTime expiredAt = confirmationToken.getExpiresAt();
        if (expiredAt.isBefore(LocalDateTime.now())) {
//            throw new IllegalStateException(TOKEN_EXPIRED_ERROR);

            // want to send another email to the user
            User user = confirmationTokenService.getUser(token);
            String newToken = userService.signUp(user);
            String link = "http://localhost:8080/app/registration/confirm?token=" + newToken;
            emailSender.send(
                    user.getEmail(),
                    link,
                    user.getFirstname()
                    );

            return "your token expired, please check your email for new token";
        }

        confirmationTokenService.setConfirmedAt(token);
        userService.enableUser(
                confirmationToken.getUser().getUsername());

        return "confirmed";
    }
}
