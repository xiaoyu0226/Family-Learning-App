package com.example.demo.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.assertj.core.util.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

@Service
@AllArgsConstructor
public class EmailService implements EmailSender{

    private final String EMAIL_TEMPLATE_LOADING_ERROR = "error loading email html template";
    private final String EMAIL_TEMPLATE_NOT_FOUND = "email html template not found";
    private final String IO_ERRORS = "IO exceptions thrown";

    private final static Logger LOGGER = LoggerFactory
            .getLogger(EmailService.class);

    private final JavaMailSender mailSender;    // if you see error with bean can't autowire, make sure your yml is correctly set requires these to have bean initialized properly
    @Override
    @Async
    public void send(String to, String link, String firstname) {
        String email = buildEmail(firstname, link);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setText(email, true);
            helper.setTo(to);
            helper.setSubject("Confirm your email registration");
            helper.setFrom("signup@test.com");
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            LOGGER.error("failed to send email", e);
            throw new IllegalStateException("failed to send email");
        }
    }

    // Function to load HTML template from file
    private String loadEmailTemplate(String userName, String activationLink) {
        String filePath = "src/main/java/com/example/demo/email/email_template.html";
        try {
            StringBuilder html = new StringBuilder();
            FileReader fr = new FileReader(filePath);

            BufferedReader br = new BufferedReader(fr);
            String val;

            while ((val = br.readLine()) != null) {
                html.append(val);
            }
            br.close();
            String result = html.toString();
            result = result.replace("[User Name]", userName);
            result = result.replace("[activation_link]", activationLink);

            return result;
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(EMAIL_TEMPLATE_NOT_FOUND);
        } catch (IOException e) {
            throw new IllegalStateException(IO_ERRORS);
        } catch (Exception e) {
            throw new IllegalStateException(EMAIL_TEMPLATE_LOADING_ERROR);    // note Illegal state is Runtime Exception, don't have to catch in outer
        }
    }

    private String buildEmail(String name, String link) {

        return loadEmailTemplate(name, link);
    }
}