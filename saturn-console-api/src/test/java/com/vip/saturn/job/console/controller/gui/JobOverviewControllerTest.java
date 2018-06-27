package com.vip.saturn.job.console.controller.gui;

import com.vip.saturn.job.console.domain.JobConfig;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JobOverviewControllerTest {

	@Ignore
    @Test(expected = SaturnJobConsoleException.class)
    public void test_assertJobConfigIsValid_invalidShardingParameter() throws Throwable {
        String invalidParameter = "0=1,a=xx";
        JobConfig jobConfig = new JobConfig();
        jobConfig.setShardingItemParameters(invalidParameter);
        JobOverviewController jobOverviewController = new JobOverviewController();
        Method method = JobOverviewController.class.getDeclaredMethod("assertJobConfigIsValid", JobConfig.class);
        method.setAccessible(true);
        try {
            method.invoke(jobOverviewController, new Object[]{jobConfig});
        } catch (InvocationTargetException ex) {
            if ((ex.getCause().getClass().equals(SaturnJobConsoleException.class))) {
                throw ex.getCause();
            }
        }
    }
}
