package com.vip.saturn.demo.embed.tomcat.job;

import com.vip.saturn.demo.embed.tomcat.service.DemoService;
import com.vip.saturn.demo.embed.tomcat.utils.SpringContextUtils;
import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;
import org.springframework.beans.factory.annotation.Autowired;

public class DemoJob extends AbstractSaturnJavaJob {

    @Autowired
    private DemoService demoService;

    @Override
    public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam, SaturnJobExecutionContext shardingContext) {
        demoService.execute();
        System.out.println("running, " + jobName + "; " + shardItem + ";" + shardParam);
        return new SaturnJobReturn("我是分片" + shardItem + "的处理结果");
    }

    /**
     * 这是个静态方法，在executor初始化时会调用，并生成供saturn使用的实现类对象
     */
    public static Object getObject() {
        DemoJob instance = (DemoJob) SpringContextUtils.getBean("demoJob");
        return instance;
    }

}
