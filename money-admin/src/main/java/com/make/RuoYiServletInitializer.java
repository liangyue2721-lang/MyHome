package com.make;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * web容器中进行部署
 *
 * @author ruoyi
 */
public class RuoYiServletInitializer extends SpringBootServletInitializer
{
    /**
     * 配置Spring应用构建器
     * 
     * @param application Spring应用构建器
     * @return 配置好的应用构建器
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application)
    {
        return application.sources(RuoYiApplication.class);
    }
}