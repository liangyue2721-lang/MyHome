package com.make.quartz.util;//package com.make.quartz.util;
//
//import com.make.quartz.domain.SysJob;
//import com.make.quartz.service.ISysJobService;
//import com.make.quartz.service.TaskReplenishmentService;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.MockitoJUnitRunner;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.core.SetOperations;
//
//import java.util.Collections;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//
//@RunWith(MockitoJUnitRunner.class)
//public class TaskReplenishmentTest {
//
//    @InjectMocks
//    private TaskReplenishmentService taskReplenishmentService;
//
//    @Mock
//    private ISysJobService sysJobService;
//
//    @Mock
//    private SchedulerManager schedulerManager;
//
//    @Mock
//    private TaskDistributor taskDistributor;
//
//    @Mock
//    private RedisTemplate<String, String> redisTemplate;
//
//    @Mock
//    private SetOperations<String, String> setOperations;
//
//    @Test
//    public void testReplenishment_MasterNode_MarkedJob_Missing() {
//        // Arrange
//        when(schedulerManager.isMasterNode()).thenReturn(true);
//        when(redisTemplate.opsForSet()).thenReturn(setOperations);
//
//        SysJob job = new SysJob();
//        job.setJobId(1L);
//        job.setJobName("TestJob");
//        job.setRemark("This is a task. AUTO_REPLENISH enabled.");
//
//        when(sysJobService.selectJobList(any(SysJob.class))).thenReturn(Collections.singletonList(job));
//
//        // Mock Redis: Not pending, Not executing
//        when(setOperations.isMember(eq(RedisMessageQueue.PENDING_TASKS_SET), anyString())).thenReturn(false);
//        when(setOperations.isMember(eq(RedisMessageQueue.EXECUTING_TASKS_SET), anyString())).thenReturn(false);
//
//        // Act
//        taskReplenishmentService.periodicQueueHealthCheck();
//
//        // Assert
//        verify(taskDistributor, times(1)).distributeTask(eq(job));
//    }
//
//    @Test
//    public void testReplenishment_NotMaster() {
//        // Arrange
//        when(schedulerManager.isMasterNode()).thenReturn(false);
//
//        // Act
//        taskReplenishmentService.periodicQueueHealthCheck();
//
//        // Assert
//        verify(sysJobService, never()).selectJobList(any());
//        verify(taskDistributor, never()).distributeTask(any());
//    }
//
//    @Test
//    public void testReplenishment_NotMarked() {
//        // Arrange
//        when(schedulerManager.isMasterNode()).thenReturn(true);
//
//        SysJob job = new SysJob();
//        job.setJobId(2L);
//        job.setJobName("NormalJob");
//        job.setRemark("Regular task");
//
//        when(sysJobService.selectJobList(any(SysJob.class))).thenReturn(Collections.singletonList(job));
//
//        // Act
//        taskReplenishmentService.periodicQueueHealthCheck();
//
//        // Assert
//        verify(taskDistributor, never()).distributeTask(any());
//    }
//}
