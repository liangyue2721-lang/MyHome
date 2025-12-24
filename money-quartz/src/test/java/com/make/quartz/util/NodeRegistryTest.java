package com.make.quartz.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NodeRegistryTest {

    private NodeRegistry nodeRegistry;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Before
    public void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        nodeRegistry = new NodeRegistry(redisTemplate);
    }

    @Test
    public void testStart_StartsHeartbeat() {
        // Act
        nodeRegistry.start();

        // Assert
        assertTrue(nodeRegistry.isRunning());
    }

    @Test
    public void testStop_StopsRunning() {
        nodeRegistry.start();
        nodeRegistry.stop();
        // Assert
        assertTrue(!nodeRegistry.isRunning());
    }
}
