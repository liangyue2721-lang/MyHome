package com.make.framework.aspectj;

import com.make.common.annotation.IdempotentConsumer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Aspect for Idempotent Consumer
 */
@Aspect
@Component
public class IdempotentAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotentAspect.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(idempotentConsumer)")
    public Object around(ProceedingJoinPoint joinPoint, IdempotentConsumer idempotentConsumer) throws Throwable {
        String keyExpression = idempotentConsumer.key();
        String uniqueKey = parseKey(keyExpression, joinPoint);

        if (uniqueKey == null || uniqueKey.isEmpty()) {
            log.warn("[Idempotency] Key is empty, proceeding without idempotency check.");
            return joinPoint.proceed();
        }

        String redisKey = "mq:idempotent:" + uniqueKey;

        // Check if key exists
        Boolean exists = stringRedisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(exists)) {
            log.info("[Idempotency] Duplicate message detected, skipping execution. Key: {}", uniqueKey);
            return null; // Skip execution
        }

        // Execute
        Object result = joinPoint.proceed();

        // Mark as processed
        stringRedisTemplate.opsForValue().set(redisKey, "1", idempotentConsumer.expire(), TimeUnit.SECONDS);

        return result;
    }

    private String parseKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // Standard SpEL parsing
        EvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        try {
            Expression expression = parser.parseExpression(keyExpression);
            return expression.getValue(context, String.class);
        } catch (Exception e) {
            log.error("[Idempotency] Failed to parse SpEL key: {}", keyExpression, e);
            return null;
        }
    }
}
