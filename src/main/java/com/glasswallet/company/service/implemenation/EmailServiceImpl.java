package com.glasswallet.company.service.implemenation;

import com.glasswallet.company.service.interfaces.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendInvite(String to, String link) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("ClyraFi Backoffice - Invitation");
        msg.setText("You were invited. Accept: " + link);
        mailSender.send(msg);
    }

    @Override
    public void sendPasswordReset(String to, String link) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("ClyraFi Backoffice - Password Reset");
        msg.setText("Reset your password: " + link);
        mailSender.send(msg);
    }
}
