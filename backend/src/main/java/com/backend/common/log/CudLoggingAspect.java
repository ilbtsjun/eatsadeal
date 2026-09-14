package com.backend.common.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class CudLoggingAspect {

    @Around("@annotation(cudLogging)")
    public Object logCudAction(ProceedingJoinPoint joinPoint, CudLogging cudLogging) throws Throwable {
        String actionName = cudLogging.value();
        String methodName = joinPoint.getSignature().toShortString();

        log.info("[CUD START] 작업명: '{}' | 실행 메서드: {}", actionName, methodName);

        try {
            Object result = joinPoint.proceed();

            log.info("[CUD SUCCESS] 작업명: '{}' 완료", actionName);
            return result;
        } catch (Exception e) {
            log.error("[CUD FAIL] 작업명: '{}' 실패 | 에러 메시지: {}", actionName, e.getMessage());
            throw e;
        }
    }
}