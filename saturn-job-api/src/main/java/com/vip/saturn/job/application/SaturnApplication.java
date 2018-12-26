package com.vip.saturn.job.application;

/**
 * @author hebelala
 */
public interface SaturnApplication {

	/**
	 * 初始化，该方法只在启动Executor时被执行
	 */
	void init();

	/**
	 * 销毁，该方法在优雅退出时将被执行
	 */
	void destroy();

	/**
	 * 根据作业类，获取作业类实例
	 * @param jobClass 作业类
	 * @param <J> 作业类泛型
	 * @return 返回作业类实例，如果返回null，那么系统仍然会尝试使用作业类的默认构造方法、或者getObject静态方法来获取实例
	 */
	<J> J getJobInstance(Class<J> jobClass);

}
