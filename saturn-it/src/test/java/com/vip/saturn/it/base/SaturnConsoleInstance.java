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

package com.vip.saturn.it.base;

import com.vip.saturn.job.console.springboot.SaturnConsoleApp;
import org.springframework.context.ApplicationContext;

/**
 * @author hebelala
 */
public class SaturnConsoleInstance {

	public ApplicationContext applicationContext;
	public int port;
	public String url;

	public SaturnConsoleInstance() {
	}

	public SaturnConsoleInstance(ApplicationContext applicationContext, int port, String url) {
		this.applicationContext = applicationContext;
		this.port = port;
		this.url = url;
	}

	public void stop() {
		SaturnConsoleApp.stop(applicationContext);
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
