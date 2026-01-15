package com.make.system.mq;

import com.make.common.annotation.IdempotentConsumer;
import com.make.system.executor.DatabaseBackupExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * System Task Handler (Logic Layer)
 * Separated from Consumer to allow AOP proxying for Idempotency.
 */
@Service
public class SystemTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(SystemTaskHandler.class);

    @Resource
    private DatabaseBackupExecutor databaseBackupExecutor;

    @IdempotentConsumer(key = "#traceId")
    public void handleBackup(String traceId) throws Exception {
        databaseBackupExecutor.executeBackup();
    }
}
