package com.backend.report.service;

import com.backend.auth.service.CurrentUserService;
import com.backend.common.log.CudLogging;
import com.backend.mail.service.MailService;
import com.backend.report.dto.ReportRequest;
import com.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportService {

//    private final ReportRepository reportRepository;
    private final CurrentUserService currentUserService;
    private final ReportPolicy reportPolicy;
//    private final ApplicationEventPublisher eventPublisher;
    private final MailService mailService;

    @CudLogging("제보 접수")
    @Transactional
    public void create(ReportRequest request) {
        User user = currentUserService.getOptionalUser();
        Long userId = (user != null) ? user.getId() : null;
        String clientIp = currentUserService.getClientIp();

        String title = request.title().trim();
        String content = request.content().trim();
        //String subject = (userId != null) ? "user:" + userId : "ip:" + clientIp;
        String subject = (userId != null) ? "user:" + userId : "ip:" + clientIp;
        reportPolicy.validateReportAllowed(subject, title, content);

        mailService.sendReportMessage("임시 제보자", title, content, LocalDateTime.now());

//        Report report = reportRepository.save(Report.create(title, content, userId, clientIp));

//        eventPublisher.publishEvent(new ReportCreatedEvent(
//                report.getId(), title, content, userId, clientIp, report.getCreatedAt()
//        ));
    }
}
