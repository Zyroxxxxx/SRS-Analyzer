package com.vlu.srsanalyzer.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import java.io.File;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class EmailService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s,;]+@[^@\\s,;]+\\.[^@\\s,;]+$");
    private static final int MAX_RECIPIENTS = 5;

    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final String username;

    public EmailService(ObjectProvider<JavaMailSender> sender,
                        @Value("${app.mail.from:}") String from,
                        @Value("${spring.mail.username:}") String username) {
        this.sender = sender;
        this.from = from;
        this.username = username;
    }

    // attachments: ten file -> {noi dung, content-type}
    public void send(List<String> to, String subject, String body, Map<String, Object[]> attachments) {
        JavaMailSender mail = sender.getIfAvailable();
        if (mail == null)
            throw new RuntimeException("Chua cau hinh SMTP: spring.mail.host dang trong. Backend tim file application-local.properties -> trong resources: " + new ClassPathResource("application-local.properties").exists() + ", ./: " + new File("application-local.properties").exists() + ", ./backend/: " + new File("backend/application-local.properties").exists() + ". Thu muc chay hien tai: " + new File(".").getAbsolutePath() + ". Hay dien spring.mail.host, username, password vao file do roi KHOI DONG LAI backend.");
        if (to == null || to.isEmpty()) throw new RuntimeException("Vui long nhap it nhat 1 dia chi email");
        if (to.size() > MAX_RECIPIENTS) throw new RuntimeException("Toi da " + MAX_RECIPIENTS + " nguoi nhan moi lan gui");
        for (String a : to) if (!EMAIL.matcher(a.trim()).matches()) throw new RuntimeException("Email khong hop le: " + a);
        try {
            MimeMessage msg = mail.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
            String f = !from.isBlank() ? from : username;
            if (!f.isBlank()) h.setFrom(f);
            h.setTo(to.stream().map(String::trim).toArray(String[]::new));
            h.setSubject(subject);
            h.setText(body, false);
            for (var e : attachments.entrySet())
                h.addAttachment(e.getKey(), new ByteArrayResource((byte[]) e.getValue()[0]), (String) e.getValue()[1]);
            mail.send(msg);
        } catch (MailAuthenticationException e) {
            throw new RuntimeException("Gmail tu choi dang nhap (sai tai khoan hoac mat khau). Hay dung App Password 16 ky tu viet lien khong dau cach, va tai khoan phai bat xac minh 2 buoc.");
        } catch (MailSendException e) {
            throw new RuntimeException("Khong ket noi/gui duoc qua may chu SMTP (kiem tra host, port 587, mang; Render goi free chan SMTP). Chi tiet: " + e.getMostSpecificCause().getMessage());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Gui email that bai: " + e.getMessage());
        }
    }
}
