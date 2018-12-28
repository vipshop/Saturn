package com.vip.saturn.demo;

import com.vip.saturn.job.springboot.GenericSpringBootSaturnApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 简单的，继承GenericSpringBootSaturnApplication，并使用@SpringBootApplication注解该类，而且在saturn.properties中声明该类。
 *
 * <p>如果想自定义启动SpringBoot，可重写GenericSpringBootSaturnApplication的某些方法，具体可查看其javadoc。
 */
@SpringBootApplication
public class Application extends GenericSpringBootSaturnApplication {

}
