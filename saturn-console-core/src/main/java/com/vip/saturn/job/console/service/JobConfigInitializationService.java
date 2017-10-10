/**
 * 
 */
package com.vip.saturn.job.console.service;

import java.util.List;

import com.vip.saturn.job.console.domain.ExportJobConfigPageStatus;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;

/**
 * @author timmy.hu
 *
 */
public interface JobConfigInitializationService {

	ExportJobConfigPageStatus exportAllToDb(String userName) throws SaturnJobConsoleException;

	List<RegistryCenterConfiguration> getRegistryCenterConfigurations() throws SaturnJobConsoleException;

}
