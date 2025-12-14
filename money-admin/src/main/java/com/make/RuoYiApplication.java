package com.make;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 信息系统启动程序
 *
 * @author ruoyi
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableScheduling
public class RuoYiApplication {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(RuoYiApplication.class);


    /**
     * 主函数，启动Spring Boot应用
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(RuoYiApplication.class, args);
        log.info("(♥◠‿◠)ﾉﾞ  TECHNOLOGY 启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
