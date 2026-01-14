package com.make.framework.aspectj;

import com.make.common.util.TraceIdUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka Consumer Logging Aspect
 * Traces consumption flow and manages MDC context.
 */
@Aspect
@Component
public class KafkaLogAspect {

    private static final Logger log = LoggerFactory.getLogger(KafkaLogAspect.class);

    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object[] args = joinPoint.getArgs();

        // Extract context
        String topic = "unknown";
        String key = "unknown";
        String traceId = TraceIdUtil.generateTraceId(); // Default new traceId if missing

        if (args != null && args.length > 0) {
            Object arg = args[0];
            if (arg instanceof ConsumerRecord) {
                ConsumerRecord<?, ?> record = (ConsumerRecord<?, ?>) arg;
                topic = record.topic();
                key = String.valueOf(record.key());
                // Try to use key as traceId if it looks like one (UUID length approx 32-36)
                if (key != null && key.length() >= 32) {
                    traceId = key;
                }
            } else if (arg instanceof List) {
                List<?> list = (List<?>) arg;
                if (!list.isEmpty() && list.get(0) instanceof ConsumerRecord) {
                    ConsumerRecord<?, ?> first = (ConsumerRecord<?, ?>) list.get(0);
                    topic = first.topic() + " (Batch " + list.size() + ")";
                    // For batch, we use the first one or generate new
                    String firstKey = String.valueOf(first.key());
                    if (firstKey != null && firstKey.length() >= 32) {
                        traceId = firstKey;
                    }
                }
            }
        }

        // Setup MDC
        TraceIdUtil.putTraceId(traceId);

        try {
            log.info("[CONSUME_START] Topic={}, Key={}", topic, key);

            Object result = joinPoint.proceed();

            long cost = System.currentTimeMillis() - start;
            log.info("[CONSUME_END] Topic={}, Key={}, Cost={}ms", topic, key, cost);
            return result;
        } catch (Throwable e) {
            log.error("[CONSUME_ERROR] Topic={}, Key={}, Error={}", topic, key, e.getMessage());
            throw e;
        } finally {
            TraceIdUtil.clearTraceId();
        }
    }
}
