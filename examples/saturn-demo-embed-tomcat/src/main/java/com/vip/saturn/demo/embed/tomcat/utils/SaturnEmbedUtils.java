package com.vip.saturn.demo.embed.tomcat.utils;

import com.vip.saturn.embed.SaturnEmbed;

public class SaturnEmbedUtils {

    /**
     * 方案1：<br>
     * 运行Saturn前必须设置以下环境变量：SATURN_HOME、SATURN_APP_NAMESPACE、SATURN_APP_EXECUTOR_NAME
     */
    public void start1() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            SaturnEmbed.start();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    /**
     * 方案2：使用java system property<br>
     * 使用-Dsaturn.home会覆盖环境变量SATURN_HOME<br>
     * 使用-Dsaturn.app.namespace会覆盖环境变量SATURN_APP_NAMESPACE<br>
     * 使用-Dsaturn.app.executorName会覆盖环境变量SATURN_APP_EXECUTOR_NAME<br>
     */
    public void start2() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            System.setProperty("saturn.home", "E:/saturn-job-executor-2.1.2");
            System.setProperty("saturn.app.namespace", "yfb-saturn-java.vip.vip.com");
            System.setProperty("saturn.app.executorName", "executor_001");
//            System.setProperty("saturn.stdout", "true"); // 注意，开启saturn-executor的日志输出到控制台，只用于开发环境
            SaturnEmbed.start();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

}
