/**
 * 
 */
package com.vip.saturn.job.console.service;

import java.util.List;
import java.util.Map;

import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * @author timmy.hu
 *
 */
public interface JobConfigInitializationService {

	void exportAllToDb(String userName) throws SaturnJobConsoleException;

	List<RegistryCenterConfiguration> getRegistryCenterConfigurations();

	boolean isExporting();

	Map<String, String> getStatus();

}
