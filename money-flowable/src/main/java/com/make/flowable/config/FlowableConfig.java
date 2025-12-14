package com.make.flowable.config;

import org.flowable.engine.impl.db.DbIdGenerator;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Configuration;

/**
 * Flowable流程引擎扩展配置类
 * <p>
 * 该类用于配置Flowable流程引擎的相关参数，包括字体设置、ID生成器等。
 * 通过实现EngineConfigurationConfigurer接口，可以在Spring Boot应用启动时
 * 自动配置Flowable引擎参数。
 * </p>
 *
 * @author 27
 * @date 2022-12-26 10:24
 * @see EngineConfigurationConfigurer
 * @see SpringProcessEngineConfiguration
 */
@Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {
    /**
     * 配置Flowable流程引擎参数
     * <p>
     * 该方法在Flowable引擎初始化时被调用，用于设置引擎的各项配置参数：
     * 1. 设置流程图中活动节点的字体为宋体
     * 2. 设置流程图中标签的字体为宋体
     * 3. 设置流程图中注解的字体为宋体
     * 4. 设置ID生成器为DbIdGenerator，使用数据库方式生成ID
     * </p>
     *
     * @param engineConfiguration Spring流程引擎配置对象
     */
    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        // 设置流程图中活动节点的字体为宋体，确保中文字符正确显示
        engineConfiguration.setActivityFontName("宋体");
        // 设置流程图中标签的字体为宋体，确保中文字符正确显示
        engineConfiguration.setLabelFontName("宋体");
        // 设置流程图中注解的字体为宋体，确保中文字符正确显示
        engineConfiguration.setAnnotationFontName("宋体");
        // 设置ID生成器为数据库ID生成器，确保ID的唯一性和一致性
        engineConfiguration.setIdGenerator(new DbIdGenerator());
    }

}