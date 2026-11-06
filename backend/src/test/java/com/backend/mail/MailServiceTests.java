package com.backend.mail;

import com.backend.event.dto.GetEventListResponse;
import com.backend.mail.service.MailService;
import com.backend.notification.dto.ConsentType;
import com.backend.notification.dto.EventDigest;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 단위 테스트에서는 @Async가 동작하지 않아 메서드가 호출 스레드에서 동기적으로 실행된다.
 * (비동기 실행 자체는 @EnableAsync가 필요한 통합 테스트의 영역)
 */
@ExtendWith(MockitoExtension.class)
class MailServiceTests {

    private static final String SENDER = "sender@eatsadeal.com";
    private static final String RECEIVER = "user@test.com";
    private static final String CODE = "123456";
    private static final String SITE_URL = "https://www.eatsadeal.kr";
    private static final String CONTACT = "report@eatsadeal.com";
    private static final String TOKEN = "abc-DEF_123";

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private MailService mailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "senderEmail", SENDER);
        ReflectionTestUtils.setField(mailService, "siteUrl", SITE_URL);
        ReflectionTestUtils.setField(mailService, "reportRecipient", CONTACT);
    }


    private MimeMessage realMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private MimeMessage failingMimeMessage() throws MessagingException {
        MimeMessage message = mock(MimeMessage.class);
        doThrow(new MessagingException("메일 생성 실패")).when(message).setSubject(anyString());
        return message;
    }

    private MimeMessage captureSentMessage() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender, times(1)).send(captor.capture());
        return captor.getValue();
    }

    private String recipientOf(MimeMessage message) throws MessagingException {
        Address[] recipients = message.getRecipients(Message.RecipientType.TO);
        assertEquals(1, recipients.length);
        return ((InternetAddress) recipients[0]).getAddress();
    }

    @Nested
    @DisplayName("createMail")
    class CreateMailTests {

        @Test
        @DisplayName("성공: 발신자/수신자/제목이 설정되고 본문은 UTF-8 HTML로 담긴다")
        void buildsHtmlMessage() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());

            MimeMessage message = mailService.createMail(RECEIVER, "테스트 제목", "<h1>본문</h1>");

            assertEquals(SENDER, ((InternetAddress) message.getFrom()[0]).getAddress());
            assertEquals(RECEIVER, recipientOf(message));
            assertEquals("테스트 제목", message.getSubject());
            assertEquals("<h1>본문</h1>", message.getContent());
            String contentType = message.getDataHandler().getContentType();
            assertTrue(contentType.contains("text/html"), "본문은 HTML이어야 한다: " + contentType);
            assertTrue(contentType.contains("UTF-8"), "본문은 UTF-8이어야 한다: " + contentType);
        }

        @Test
        @DisplayName("실패: MimeMessage 설정 중 MessagingException이 나면 그대로 던진다 (호출자가 처리)")
        void propagatesMessagingException() throws Exception {
            MimeMessage message = failingMimeMessage();
            when(javaMailSender.createMimeMessage()).thenReturn(message);

            assertThrows(MessagingException.class, () -> mailService.createMail(RECEIVER, "제목", "본문"));
        }
    }

    @Nested
    @DisplayName("sendSignUpMessage")
    class SendSignUpMessageTests {

        @Test
        @DisplayName("성공: 회원가입 제목/안내 문구와 인증 코드가 담긴 메일을 수신자에게 한 번 발송한다")
        void success() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());

            mailService.sendSignUpMessage(RECEIVER, CODE);

            MimeMessage sent = captureSentMessage();
            assertEquals(RECEIVER, recipientOf(sent));
            assertEquals("[EatsADeal]회원가입 인증 메일입니다.", sent.getSubject());
            String body = (String) sent.getContent();
            assertTrue(body.contains("<h1>" + CODE + "</h1>"), "인증 코드가 본문에 있어야 한다");
            assertTrue(body.contains("유효기간은 10분"));
            assertTrue(body.contains("회원가입"));
            assertFalse(body.contains("비밀번호 찾기"), "비밀번호 메일 문구가 섞이면 안 된다");
        }

        @Test
        @DisplayName("발송 실패(MailException): 예외를 밖으로 던지지 않고 로그만 남긴다")
        void mailExceptionIsSwallowed() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());
            doThrow(new MailSendException("SMTP 서버 연결 실패")).when(javaMailSender).send(any(MimeMessage.class));

            assertDoesNotThrow(() -> mailService.sendSignUpMessage(RECEIVER, CODE));

            verify(javaMailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("메일 생성 실패(MessagingException): 예외를 밖으로 던지지 않고, 발송은 시도하지 않는다")
        void messagingExceptionIsSwallowed() throws Exception {
            MimeMessage message = failingMimeMessage();
            when(javaMailSender.createMimeMessage()).thenReturn(message);

            assertDoesNotThrow(() -> mailService.sendSignUpMessage(RECEIVER, CODE));

            verify(javaMailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("sendPasswordMessage")
    class SendPasswordMessageTests {

        @Test
        @DisplayName("성공: 패스워드 제목/안내 문구와 인증 코드가 담긴 메일을 수신자에게 한 번 발송한다")
        void success() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());

            mailService.sendPasswordMessage(RECEIVER, CODE);

            MimeMessage sent = captureSentMessage();
            assertEquals(RECEIVER, recipientOf(sent));
            assertEquals("[EatsADeal]패스워드 인증 메일입니다.", sent.getSubject());
            String body = (String) sent.getContent();
            assertTrue(body.contains("<h1>" + CODE + "</h1>"), "인증 코드가 본문에 있어야 한다");
            assertTrue(body.contains("유효기간은 10분"));
            assertTrue(body.contains("비밀번호 찾기"));
            assertFalse(body.contains("회원가입"), "회원가입 메일 문구가 섞이면 안 된다");
        }

        @Test
        @DisplayName("발송 실패(MailException): 예외를 밖으로 던지지 않고 로그만 남긴다")
        void mailExceptionIsSwallowed() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());
            doThrow(new MailSendException("SMTP 서버 연결 실패")).when(javaMailSender).send(any(MimeMessage.class));

            assertDoesNotThrow(() -> mailService.sendPasswordMessage(RECEIVER, CODE));

            verify(javaMailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("메일 생성 실패(MessagingException): 예외를 밖으로 던지지 않고, 발송은 시도하지 않는다")
        void messagingExceptionIsSwallowed() throws Exception {
            MimeMessage message = failingMimeMessage();
            when(javaMailSender.createMimeMessage()).thenReturn(message);

            assertDoesNotThrow(() -> mailService.sendPasswordMessage(RECEIVER, CODE));

            verify(javaMailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("sendEventDigestMessage")
    class SendEventDigestMessageTests {

        private GetEventListResponse digestEvent(Long id, String brandName, String title, LocalDateTime endDate) {
            return new GetEventListResponse(id, title, null, "https://event.url/" + id, "https://image.url/" + id + ".png",
                    LocalDateTime.of(2026, 11, 1, 0, 0), endDate, 0L, true, 1L, brandName, Set.of(), false);
        }

        private EventDigest digest(List<GetEventListResponse> newEvents, List<GetEventListResponse> endingSoonEvents) {
            return new EventDigest(1L, RECEIVER, "회원1", newEvents, endingSoonEvents, TOKEN);
        }

        @Test
        @DisplayName("성공: (광고) 제목과 건수, 두 섹션, 상세·구독 관리·수신거부 링크가 담긴 메일을 보내고 true를 반환한다")
        void success() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());
            EventDigest digest = digest(
                    List.of(digestEvent(10L, "BHC", "새 이벤트 제목", LocalDateTime.of(2026, 11, 30, 23, 59))),
                    List.of(digestEvent(20L, "BHC", "마감 이벤트 제목", LocalDateTime.of(2026, 11, 7, 23, 59))));

            boolean result = mailService.sendEventDigestMessage(digest);

            assertTrue(result);
            MimeMessage sent = captureSentMessage();
            assertEquals(RECEIVER, recipientOf(sent));
            assertEquals("(광고) [EatsADeal] 구독 브랜드 소식 2건", sent.getSubject());
            String body = (String) sent.getContent();
            assertTrue(body.contains("새 이벤트 1건"));
            assertTrue(body.contains("내일 마감 1건"));
            assertTrue(body.contains(SITE_URL + "/events/10"));
            assertTrue(body.contains(SITE_URL + "/events/20"));
            assertTrue(body.contains("11/1 ~ 11/30"));
            assertTrue(body.contains(SITE_URL + "/subscriptions"));
            assertTrue(body.contains(SITE_URL + "/unsubscribe?token=" + TOKEN));
            assertTrue(body.contains("전송자: EatsADeal"));
            assertTrue(body.contains("문의: " + CONTACT));
        }

        @Test
        @DisplayName("원클릭 수신거부 헤더(List-Unsubscribe, List-Unsubscribe-Post)를 붙인다")
        void addsOneClickUnsubscribeHeaders() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());

            mailService.sendEventDigestMessage(digest(
                    List.of(digestEvent(10L, "BHC", "새 이벤트 제목", null)), List.of()));

            MimeMessage sent = captureSentMessage();
            assertEquals("<" + SITE_URL + "/api/notifications/unsubscribe/one-click?token=" + TOKEN + ">",
                    sent.getHeader("List-Unsubscribe", null));
            assertEquals("List-Unsubscribe=One-Click", sent.getHeader("List-Unsubscribe-Post", null));
        }

        @Test
        @DisplayName("닉네임·브랜드명·제목은 HTML 이스케이프하고, 종료일이 없으면 상시로 표시한다")
        void escapesHtmlAndShowsOngoing() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());
            EventDigest digest = new EventDigest(1L, RECEIVER, "<i>회원</i>",
                    List.of(digestEvent(10L, "<b>BHC</b>", "1+1 & 반값", null)), List.of(), TOKEN);

            mailService.sendEventDigestMessage(digest);

            String body = (String) captureSentMessage().getContent();
            assertTrue(body.contains("&lt;i&gt;회원&lt;/i&gt;"));
            assertTrue(body.contains("[&lt;b&gt;BHC&lt;/b&gt;]"));
            assertTrue(body.contains("1+1 &amp; 반값"));
            assertFalse(body.contains("<b>BHC</b>"));
            assertTrue(body.contains("11/1 ~ 상시"));
        }

        @Test
        @DisplayName("비어 있는 섹션은 본문에 넣지 않는다")
        void omitsEmptySection() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());

            mailService.sendEventDigestMessage(digest(
                    List.of(digestEvent(10L, "BHC", "새 이벤트 제목", null)), List.of()));

            String body = (String) captureSentMessage().getContent();
            assertTrue(body.contains("새 이벤트 1건"));
            assertFalse(body.contains("내일 마감"));
        }

        @Test
        @DisplayName("발송 실패(MailException): 예외를 던지지 않고 false를 반환한다")
        void mailExceptionReturnsFalse() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());
            doThrow(new MailSendException("SMTP 서버 연결 실패")).when(javaMailSender).send(any(MimeMessage.class));

            boolean result = mailService.sendEventDigestMessage(digest(
                    List.of(digestEvent(10L, "BHC", "새 이벤트 제목", null)), List.of()));

            assertFalse(result);
            verify(javaMailSender, times(1)).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("sendConsentResultMessage")
    class SendConsentResultMessageTests {

        private static final LocalDateTime PROCESSED_AT = LocalDateTime.of(2026, 11, 6, 14, 30);

        private Map<ConsentType, Boolean> changes() {
            Map<ConsentType, Boolean> changes = new LinkedHashMap<>();
            changes.put(ConsentType.MARKETING_USE, true);
            changes.put(ConsentType.AD_EMAIL, false);
            return changes;
        }

        @Test
        @DisplayName("성공: (광고) 없는 제목, 처리 일자, 항목별 동의/철회를 순서대로, 마이페이지 링크와 문의처를 담는다")
        void success() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());

            mailService.sendConsentResultMessage(RECEIVER, changes(), PROCESSED_AT);

            MimeMessage sent = captureSentMessage();
            assertEquals(RECEIVER, recipientOf(sent));
            assertEquals("[EatsADeal] 광고성 정보 수신 동의 처리 결과 안내", sent.getSubject());
            String body = (String) sent.getContent();
            assertTrue(body.contains("2026년 11월 6일"));
            String marketing = ConsentType.MARKETING_USE.getTitle() + ": 동의";
            String email = ConsentType.AD_EMAIL.getTitle() + ": 철회";
            assertTrue(body.contains(marketing));
            assertTrue(body.contains(email));
            assertTrue(body.indexOf(marketing) < body.indexOf(email), "변경 항목은 넘겨받은 순서대로 표시한다");
            assertTrue(body.contains(SITE_URL + "/mypage"));
            assertTrue(body.contains("전송자: EatsADeal"));
            assertTrue(body.contains("문의: " + CONTACT));
            assertNull(sent.getHeader("List-Unsubscribe"), "광고 메일이 아니므로 수신거부 헤더를 붙이지 않는다");
        }

        @Test
        @DisplayName("발송 실패(MailException): 예외를 밖으로 던지지 않고 로그만 남긴다")
        void mailExceptionIsSwallowed() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());
            doThrow(new MailSendException("SMTP 서버 연결 실패")).when(javaMailSender).send(any(MimeMessage.class));

            assertDoesNotThrow(() -> mailService.sendConsentResultMessage(RECEIVER, changes(), PROCESSED_AT));

            verify(javaMailSender, times(1)).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("sendConsentReconfirmMessage")
    class SendConsentReconfirmMessageTests {

        private static final LocalDateTime AGREED_AT = LocalDateTime.of(2024, 11, 5, 20, 0);

        @Test
        @DisplayName("성공: 동의 일자·항목, 유지 안내, 수신거부·마이페이지 링크를 담아 보내고 true를 반환한다")
        void success() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());

            boolean result = mailService.sendConsentReconfirmMessage(RECEIVER, "<i>회원</i>", AGREED_AT, TOKEN);

            assertTrue(result);
            MimeMessage sent = captureSentMessage();
            assertEquals(RECEIVER, recipientOf(sent));
            assertEquals("[EatsADeal] 광고성 정보 수신 동의 여부 확인 안내", sent.getSubject());
            String body = (String) sent.getContent();
            assertTrue(body.contains("&lt;i&gt;회원&lt;/i&gt;님"));
            assertFalse(body.contains("<i>회원</i>"));
            assertTrue(body.contains("2024년 11월 5일"));
            assertTrue(body.contains(ConsentType.AD_EMAIL.getTitle()));
            assertTrue(body.contains("유지"));
            assertTrue(body.contains(SITE_URL + "/unsubscribe?token=" + TOKEN));
            assertTrue(body.contains(SITE_URL + "/mypage"));
            assertTrue(body.contains("전송자: EatsADeal"));
            assertTrue(body.contains("문의: " + CONTACT));
        }

        @Test
        @DisplayName("발송 실패(MailException): 예외를 던지지 않고 false를 반환한다")
        void mailExceptionReturnsFalse() throws Exception {
            when(javaMailSender.createMimeMessage()).thenReturn(realMimeMessage());
            doThrow(new MailSendException("SMTP 서버 연결 실패")).when(javaMailSender).send(any(MimeMessage.class));

            boolean result = mailService.sendConsentReconfirmMessage(RECEIVER, "회원1", AGREED_AT, TOKEN);

            assertFalse(result);
        }
    }
}