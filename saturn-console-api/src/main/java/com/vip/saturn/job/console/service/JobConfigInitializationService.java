package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.ExportJobConfigPageStatus;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import java.util.List;

/**
 * @author timmy.hu
 */
public interface JobConfigInitializationService {

	void exportAllToDb(String userName) throws SaturnJobConsoleException;

	ExportJobConfigPageStatus getStatus() throws SaturnJobConsoleException;

	List<RegistryCenterConfiguration> getRegistryCenterConfigurations() throws SaturnJobConsoleException;

}
