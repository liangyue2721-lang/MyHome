package com.make.quartz.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NodeMonitorTest {

    @InjectMocks
    private NodeMonitor nodeMonitor;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    @Before
    public void setUp() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        // Note: NodeMonitor no longer uses opsForValue, so we don't mock it to avoid UnnecessaryStubbingException
    }

    @Test
    public void testCheckNodeStatus_AllNodesAlive() {
        // Arrange
        Set<String> nodes = new HashSet<>();
        nodes.add("192.168.1.1");
        nodes.add("192.168.1.2");
        when(setOperations.members("mq:nodes:alive")).thenReturn(nodes);

        when(redisTemplate.hasKey("mq:nodes:ttl:192.168.1.1")).thenReturn(true);
        when(redisTemplate.hasKey("mq:nodes:ttl:192.168.1.2")).thenReturn(true);

        // Act
        nodeMonitor.checkNodeStatus();

        // Assert
        verify(redisTemplate, never()).delete(anyString());
        verify(setOperations, never()).remove(anyString(), anyString());
    }

    @Test
    public void testCheckNodeStatus_NodeOffline_MigratesTasks() {
        // Arrange
        Set<String> nodes = new HashSet<>();
        nodes.add("192.168.1.1"); // Alive
        nodes.add("192.168.1.99"); // Dead
        when(setOperations.members("mq:nodes:alive")).thenReturn(nodes);

        when(redisTemplate.hasKey("mq:nodes:ttl:192.168.1.1")).thenReturn(true);
        when(redisTemplate.hasKey("mq:nodes:ttl:192.168.1.99")).thenReturn(false);

        // Mock Task Processing Queue for dead node
        String deadNodeQueue = "mq:task:processing:192.168.1.99";
        String task1 = "exec1|NORMAL\n{\"jobId\":1}";
        String task2 = "exec2|HIGH\n{\"jobId\":2}";

        when(listOperations.rightPop(deadNodeQueue))
                .thenReturn(task1)
                .thenReturn(task2)
                .thenReturn(null); // End of list

        // Act
        nodeMonitor.checkNodeStatus();

        // Assert
        // 1. Dead node removed from set
        verify(setOperations).remove("mq:nodes:alive", "192.168.1.99");

        // 2. TTL key cleaned (just in case)
        verify(redisTemplate).delete("mq:nodes:ttl:192.168.1.99");

        // 3. Queue drained and migrated
        // Task 1 -> NORMAL
        verify(listOperations).leftPush("mq:task:global:normal", task1);
        // Task 2 -> HIGH
        verify(listOperations).leftPush("mq:task:global:high", task2);

        // 4. Queue deleted
        verify(redisTemplate).delete(deadNodeQueue);
    }
}
