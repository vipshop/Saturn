/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

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
