package com.backend.mail.service;

import com.backend.common.log.CudLogging;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${report.mail.to}")
    private String reportRecipient;

    @Async
    @CudLogging("회원가입 메일 송신")
    public void sendSignUpMessage(String sendEmail, String code) {
        String title = "[EatsADeal]회원가입 인증 메일입니다.";
        String body = "";
        body += "<h3>요청하신 인증 번호입니다.</h3>";
        body += "<h1>" + code + "</h1>";
        body += "<h3>감사합니다.</h3>";
        body += "인증의 유효기간은 10분입니다. 10분내로 인증하지 않을 시, 다시 회원가입을 진행해주시기 바랍니다.";

        try {
            MimeMessage message = createMail(sendEmail, title, body);
            javaMailSender.send(message);
        } catch (MailException | MessagingException e) {
            log.error("메일 전송 실패 [Target: {}]: {}", sendEmail, e.getMessage());
        }
    }

    @Async
    @CudLogging("비밀번호 변경 메일 송신")
    public void sendPasswordMessage(String sendEmail, String code) {
        String title = "[EatsADeal]패스워드 인증 메일입니다.";
        String body = "";
        body += "<h3>요청하신 인증 번호입니다.</h3>";
        body += "<h1>" + code + "</h1>";
        body += "<h3>감사합니다.</h3>";
        body += "인증의 유효기간은 10분입니다. 10분내로 인증하지 않을 시, 다시 비밀번호 찾기를 진행해주시기 바랍니다.";

        try {
            MimeMessage message = createMail(sendEmail, title, body);
            javaMailSender.send(message);
        } catch (MailException | MessagingException e) {
            log.error("메일 전송 실패 [Target: {}]: {}", sendEmail, e.getMessage());
        }
    }

    //public boolean sendReportMessage(/*Long reportId*/, String reporter, String title, String content, LocalDateTime createdAt) {
    public boolean sendReportMessage(String reporter, String title, String content, LocalDateTime createdAt) {
        String safeSubject = "[EatsADeal 제보] " + title.replaceAll("[\\r\\n]+", " ");

        String body = "";
        body += "<h3>새 제보가 접수되었습니다.</h3>";
//        body += "<p><b>제보 번호:</b> " + reportId + "<br>";
        body += "<b>제보자:</b> " + HtmlUtils.htmlEscape(reporter) + "<br>";
        body += "<b>접수 시각:</b> " + createdAt + "</p><hr>";
        body += "<h4>" + HtmlUtils.htmlEscape(title) + "</h4>";
        body += "<p>" + HtmlUtils.htmlEscape(content).replace("\r\n", "\n").replace("\n", "<br>") + "</p>";

        try {
            MimeMessage message = createMail(reportRecipient, safeSubject, body);
            javaMailSender.send(message);
            return true;
        } catch (MailException | MessagingException e) {
            //log.error("제보 메일 전송 실패 [reportId: {}]: {}", reportId, e.getMessage());
            log.error("제보 메일 전송 실패 {}", e.getMessage());
            return false;
        }
    }

    public MimeMessage createMail(String mail, String title, String body) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();

        message.setFrom(senderEmail);
        message.setRecipients(MimeMessage.RecipientType.TO, mail);
        message.setSubject(title);

        message.setText(body, "UTF-8", "html");

        return message;
    }
}
