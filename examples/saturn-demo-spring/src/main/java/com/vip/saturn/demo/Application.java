package com.vip.saturn.demo;

import com.vip.saturn.job.spring.GenericSpringSaturnApplication;

/**
 * 简单的，继承GenericSpringSaturnApplication，并在saturn.properties中声明该类，然后在applicationContext.xml文件中定义作业类即可。
 *
 * <p>如果想自定义启动Spring，可重写GenericSpringSaturnApplication的某些方法，具体可查看其javadoc。
 */
public class Application extends GenericSpringSaturnApplication {

}
