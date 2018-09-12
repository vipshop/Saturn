package com.vip.saturn.demo.job;

import com.vip.saturn.demo.DemoMain;
import com.vip.saturn.demo.service.DemoService;
import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DemoJob extends AbstractSaturnJavaJob {
	
	@Autowired
    private DemoService demoService;
	
	public static Object getObject() {
        if(DemoMain.ac == null) {
			DemoMain.main(new String[]{});
        }
        DemoJob demoJob = DemoMain.ac.getBean(DemoJob.class);
        return demoJob;
    }

	@Override
	public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam,
			SaturnJobExecutionContext shardingContext) throws InterruptedException {	
		demoService.execute();
		System.out.println("我会出现在运行日志里.running handleJavaJob:" + jobName + "; " + shardItem + ";" + shardParam);
		return new SaturnJobReturn("我是分片" + shardItem + "的处理结果");
	}
}
