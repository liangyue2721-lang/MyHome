//package com.make.quartz.config;
//
//import org.quartz.DisallowConcurrentExecution;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.quartz.SchedulerFactoryBean;
//
//import javax.sql.DataSource;
//import java.time.LocalTime;
//import java.util.Properties;
//
///**
// * 定时任务配置类（Quartz）
// * <p>
// * 说明：
// * - 若为单机部署，可删除此类以及相关的 QRTZ 数据库表，使用内存方式更高效；
// * - 若为集群部署或需要持久化任务数据，保留此类以配置基于数据库的 Quartz；
// * <p>
// * Quartz 是一个功能强大的任务调度库，本配置类用于初始化 SchedulerFactoryBean，
// * 并配置 Quartz 的相关属性，包括线程池、任务存储、集群等参数。
// * 关键修改：
// * - 保留 lockHandler.class 指向自定义 RedisQuartzSemaphore（可以临时注释掉用于排查）。
// * - threadCount 根据实际情况调整，示例使用 20 作为较保守的起始值。
// *
// * @author ruoyi
// */
//@DisallowConcurrentExecution
//@Configuration
//public class ScheduleConfig {
//    private static final Logger log = LoggerFactory.getLogger(ScheduleConfig.class);
//
//    @Value("${spring.quartz.scheduler-name:MakeScheduler}")
//    private String schedulerName;
//
//    @Value("${spring.quartz.properties.org.quartz.scheduler.instanceName:MakeScheduler}")
//    private String instanceName;
//
//    @Value("${spring.quartz.properties.org.quartz.scheduler.instance-id:myapp-server}")
//    private String instanceId;
//
//    @Value("${spring.quartz.properties.org.quartz.threadPool.class:org.quartz.simpl.SimpleThreadPool}")
//    private String threadPoolClass;
//
//    @Value("${spring.quartz.properties.org.quartz.threadPool.threadCount:20}")
//    private String threadCount;
//
//    @Value("${spring.quartz.properties.org.quartz.threadPool.threadPriority:5}")
//    private String threadPriority;
//
//    @Value("${spring.quartz.properties.org.quartz.jobStore.class:org.springframework.scheduling.quartz.LocalDataSourceJobStore}")
//    private String jobStoreClass;
//
//    @Value("${spring.quartz.properties.org.quartz.jobStore.isClustered:true}")
//    private String isClustered;
//
//    @Value("${spring.quartz.properties.org.quartz.jobStore.clusterCheckinInterval:30000}")
//    private String clusterCheckinInterval;
//
//    @Value("${spring.quartz.properties.org.quartz.jobStore.maxMisfiresToHandleAtATime:20}")
//    private String maxMisfiresToHandleAtATime;
//
//    @Value("${spring.quartz.properties.org.quartz.jobStore.txIsolationLevelSerializable:false}")
//    private String txIsolationLevelSerializable;
//
//    @Value("${spring.quartz.properties.org.quartz.jobStore.misfireThreshold:60000}")
//    private String misfireThreshold;
//
//    @Value("${spring.quartz.properties.org.quartz.jobStore.tablePrefix:QRTZ_}")
//    private String tablePrefix;
//
//    /**
//     * 创建 SchedulerFactoryBean，用于启动 Quartz Scheduler
//     *
//     * 注意：
//     * - lockHandler.class 指向 com.example.lock.RedisQuartzSemaphore（Quartz 会反射创建该类实例，
//     *   但我们通过 RedissonClientHolder 保证其能拿到 RedissonClient）。
//     * - 如果你怀疑是 Redis 锁导致问题，可临时注释掉 lockHandler 配置，回退到数据库默认锁以排查。
//     */
//    @Bean
//    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
//        String currentTime = LocalTime.now().toString();
//        String threadName = Thread.currentThread().getName();
//        long threadId = Thread.currentThread().getId();
//
//        log.info("🔧 [{}] Quartz调度器配置初始化开始 | 线程: {}(ID:{})", currentTime, threadName, threadId);
//        log.info("📊 [{}] Quartz线程池配置 - 类名: {} | 线程数: {} | 线程优先级: {} | 线程: {}(ID:{})",
//                currentTime, threadPoolClass, threadCount, threadPriority, threadName, threadId);
//        log.info("💾 [{}] Quartz任务存储配置 - 类名: {} | 是否集群: {} | 集群检查间隔: {}ms | 线程: {}(ID:{})",
//                currentTime, jobStoreClass, isClustered, clusterCheckinInterval, threadName, threadId);
//
//        SchedulerFactoryBean factory = new SchedulerFactoryBean();
//        factory.setDataSource(dataSource);
//
//        // quartz参数
//        Properties prop = new Properties();
//        prop.put("org.quartz.scheduler.instanceName", instanceName);
//        prop.put("org.quartz.scheduler.instanceId", instanceId);
//        // 线程池配置
//        prop.put("org.quartz.threadPool.class", threadPoolClass);
//        prop.put("org.quartz.threadPool.threadCount", threadCount);
//        prop.put("org.quartz.threadPool.threadPriority", threadPriority);
//        // JobStore配置
//        prop.put("org.quartz.jobStore.class", jobStoreClass);
//        // 集群配置
//        prop.put("org.quartz.jobStore.isClustered", isClustered);
//        prop.put("org.quartz.jobStore.clusterCheckinInterval", clusterCheckinInterval);
//        prop.put("org.quartz.jobStore.maxMisfiresToHandleAtATime", maxMisfiresToHandleAtATime);
//        prop.put("org.quartz.jobStore.txIsolationLevelSerializable", txIsolationLevelSerializable);
//        prop.put("org.quartz.jobStore.misfireThreshold", misfireThreshold);
//        prop.put("org.quartz.jobStore.tablePrefix", tablePrefix);
//
//        // 配置使用Redis分布式锁处理器替代默认数据库锁
//        prop.put("org.quartz.jobStore.lockHandler.class", "com.make.quartz.config.RedisQuartzSemaphore");
//
//        factory.setQuartzProperties(prop);
//
//        factory.setSchedulerName(schedulerName);
//        // 延时启动
//        factory.setStartupDelay(1);
//        factory.setApplicationContextSchedulerContextKey("applicationContextKey");
//        // 可选，QuartzScheduler
//        // 启动时更新己存在的Job，这样就不用每次修改targetObject后删除qrtz_job_details表对应记录了
//        factory.setOverwriteExistingJobs(true);
//        // 设置自动启动，默认为true
//        factory.setAutoStartup(true);
//        // 确保关闭时等待任务完成
//        factory.setWaitForJobsToCompleteOnShutdown(true);
//        // 设置Phase为Integer.MAX_VALUE，确保scheduler在Spring关闭时尽早关闭（stop first）
//        // 在SmartLifecycle中，Phase越大，start越晚，stop越早
//        factory.setPhase(Integer.MAX_VALUE);
//
//        log.info("✅ [{}] Quartz调度器配置初始化完成 | 调度器名称: {} | 线程: {}(ID:{})",
//                LocalTime.now().toString(), schedulerName, threadName, threadId);
//
//        return factory;
//    }
//}
