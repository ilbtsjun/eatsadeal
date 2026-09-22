package com.backend.mail;

import com.backend.mail.service.MailService;
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

import java.util.Properties;

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

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private MailService mailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mailService, "senderEmail", SENDER);
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
            assertEquals("[EatsADeal]회원가입 인증 테스트 메일입니다.", sent.getSubject());
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
            assertEquals("[EatsADeal]패스워드 인증 테스트 메일입니다.", sent.getSubject());
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
}