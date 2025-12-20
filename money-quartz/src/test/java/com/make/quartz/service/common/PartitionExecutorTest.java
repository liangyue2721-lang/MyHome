package com.make.quartz.service.common;

import org.junit.Assert;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class PartitionExecutorTest {

    @Test
    public void testExecute() {
        PartitionExecutor executor = new PartitionExecutor();
        ExecutorService threadPool = Executors.newFixedThreadPool(2);

        List<Integer> inputs = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // 模拟处理逻辑：返回平方，偶数返回 null (模拟失败/过滤)
        List<Integer> results = executor.execute(inputs, 3, threadPool, (num) -> {
            if (num % 2 == 0) return null;
            return num * num;
        }, "test-trace", "TestTask");

        // 期望结果：1, 9, 25, 49, 81 (5个奇数的平方)
        Assert.assertEquals(5, results.size());
        Assert.assertTrue(results.contains(1));
        Assert.assertTrue(results.contains(81));

        threadPool.shutdown();
    }
}
