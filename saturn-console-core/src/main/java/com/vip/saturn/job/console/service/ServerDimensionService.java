/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.console.service;

import java.util.Map;

import com.vip.saturn.job.console.domain.ServerStatus;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

public interface ServerDimensionService {

	Map<String, Object> getAllServersBriefInfo();

	ServerStatus getExecutorStatus(String executor);

	void removeOffLineExecutor(String executor);

	boolean isRunning(String jobName, String executor);

	boolean isReady(String jobName, String executor);
	
	void trafficExtraction(String executorName) throws SaturnJobConsoleException;
	
	void traficRecovery(String executorName) throws SaturnJobConsoleException;

}
