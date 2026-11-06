package com.backend.mail.service;

import com.backend.common.log.CudLogging;
import com.backend.crawler.dto.CrawlReport;
import com.backend.crawler.dto.CrawlRunResponse;
import com.backend.event.dto.GetEventListResponse;
import com.backend.notification.dto.EventDigest;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${report.mail.to}")
    private String reportRecipient;

    @Value("${app.site-url:https://www.eatsadeal.kr}")
    private String siteUrl;

    private static final DateTimeFormatter DIGEST_DATE = DateTimeFormatter.ofPattern("M/d");

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

    public boolean sendCrawlAlertMessage(CrawlReport report, List<CrawlRunResponse> problems) {
        String subject = String.format("[EatsADeal 크롤링 알림] 실패 %d / 일부 실패 %d / 수집 0건 %d",
                report.failed().size(), report.partial().size(), report.empty().size());

        StringBuilder body = new StringBuilder("<h3>예약 크롤링에서 확인이 필요한 결과가 있습니다.</h3>");
        body.append("<p>수집 ").append(report.saved()).append("건</p>");
        body.append("<table border=\"1\" cellpadding=\"6\" style=\"border-collapse:collapse\">");
        body.append("<tr><th>브랜드</th><th>상태</th><th>수집</th><th>오류</th><th>메시지</th></tr>");
        for (CrawlRunResponse run : problems) {
            body.append("<tr><td>").append(HtmlUtils.htmlEscape(run.brandKey())).append("</td>")
                    .append("<td>").append(run.status()).append("</td>")
                    .append("<td>").append(run.collectedCount()).append("</td>")
                    .append("<td>").append(run.errorCount()).append("</td>")
                    .append("<td>").append(HtmlUtils.htmlEscape(run.message() == null ? "" : run.message()))
                    .append("</td></tr>");
        }
        body.append("</table>");

        return sendCrawlMail(subject, body.toString());
    }

    public boolean sendCrawlErrorMessage(Exception cause) {
        String message = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        return sendCrawlMail("[EatsADeal 크롤링 알림] 예약 크롤링 실행 실패",
                "<h3>예약 크롤링이 실행 중 예외로 중단되었습니다.</h3><p>" + HtmlUtils.htmlEscape(message) + "</p>");
    }

    private boolean sendCrawlMail(String subject, String body) {
        try {
            MimeMessage message = createMail(reportRecipient, subject, body);
            javaMailSender.send(message);
            return true;
        } catch (MailException | MessagingException e) {
            log.error("크롤링 알림 메일 전송 실패 {}", e.getMessage());
            return false;
        }
    }

    public boolean sendEventDigestMessage(EventDigest digest) {
        String subject = "(광고) [EatsADeal] 구독 브랜드 소식 " + digest.totalCount() + "건";

        StringBuilder body = new StringBuilder();
        body.append("<h3>").append(HtmlUtils.htmlEscape(digest.nickname()))
                .append("님이 구독한 브랜드의 이벤트 소식입니다.</h3>");
        appendDigestSection(body, "새 이벤트", digest.newEvents());
        appendDigestSection(body, "내일 마감", digest.endingSoonEvents());
        appendAdvertiserFooter(body);

        try {
            MimeMessage message = createMail(digest.email(), subject, body.toString());
            javaMailSender.send(message);
            return true;
        } catch (MailException | MessagingException e) {
            log.error("알림 메일 전송 실패 [userId: {}]: {}", digest.userId(), e.getMessage());
            return false;
        }
    }

    private void appendDigestSection(StringBuilder body, String title, List<GetEventListResponse> events) {
        if (events.isEmpty()) {
            return;
        }
        body.append("<h4>").append(title).append(" ").append(events.size()).append("건</h4><ul>");
        for (GetEventListResponse event : events) {
            body.append("<li><a href=\"").append(siteUrl).append("/events/").append(event.id()).append("\">")
                    .append("[").append(HtmlUtils.htmlEscape(event.brandName())).append("] ")
                    .append(HtmlUtils.htmlEscape(event.title())).append("</a> (")
                    .append(formatPeriod(event.startDate(), event.endDate())).append(")</li>");
        }
        body.append("</ul>");
    }

    private String formatPeriod(LocalDateTime start, LocalDateTime end) {
        String from = start == null ? "" : start.format(DIGEST_DATE);
        return from + " ~ " + (end == null ? "상시" : end.format(DIGEST_DATE));
    }

    private void appendAdvertiserFooter(StringBuilder body) {
        body.append("<hr><p style=\"color:#747d8c;font-size:12px\">")
                .append("본 메일은 광고성 정보 수신에 동의하신 분께 발송됩니다.<br>")
                .append("전송자: EatsADeal | 문의: ").append(HtmlUtils.htmlEscape(reportRecipient)).append("<br>")
                .append("<a href=\"").append(siteUrl).append("/subscriptions\">구독 브랜드 관리</a>")
                .append("</p>");
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
