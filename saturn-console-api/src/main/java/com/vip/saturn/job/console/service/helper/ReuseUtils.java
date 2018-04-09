package com.vip.saturn.job.console.service.helper;

import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.JobNodePath;
import com.vip.saturn.job.console.utils.ThreadLocalCuratorClient;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vip.saturn.job.console.exception.SaturnJobConsoleException.ERROR_CODE_NOT_EXISTED;

/**
 * @author hebelala
 */
public class ReuseUtils {

	private static final String JOB_NOT_EXIST_TEMPLATE = "The job {%s} does not exists.";

	private static final String NAMESPACE_NOT_EXIST_TEMPLATE = "The namespace {%s} does not exists.";

	private static final Logger log = LoggerFactory.getLogger(ReuseUtils.class);

	public static <T> T reuse(String namespace, final String jobName, RegistryCenterService registryCenterService,
			CuratorRepository curatorRepository, final ReuseCallBack<T> callBack) throws SaturnJobConsoleException {
		return reuse(namespace, registryCenterService, curatorRepository, new ReuseCallBack<T>() {
			@Override
			public T call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
				if (!curatorFrameworkOp.checkExists(JobNodePath.getJobNodePath(jobName))) {
					throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, String.format(JOB_NOT_EXIST_TEMPLATE, jobName));
				}
				return callBack.call(curatorFrameworkOp);
			}
		});
	}

	public static <T> T reuse(String namespace, RegistryCenterService registryCenterService,
			CuratorRepository curatorRepository, ReuseCallBack<T> callBack) throws SaturnJobConsoleException {
		try {
			RegistryCenterConfiguration registryCenterConfiguration = registryCenterService
					.findConfigByNamespace(namespace);
			if (registryCenterConfiguration == null) {
				throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, String.format(NAMESPACE_NOT_EXIST_TEMPLATE, namespace));
			}
			RegistryCenterClient registryCenterClient = registryCenterService.connectByNamespace(namespace);
			if (registryCenterClient != null && registryCenterClient.isConnected()) {
				CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
				CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository
						.newCuratorFrameworkOp(curatorClient);
				ThreadLocalCuratorClient.setCuratorClient(curatorClient);
				return callBack.call(curatorFrameworkOp);
			} else {
				throw new SaturnJobConsoleException("Connect zookeeper failed");
			}
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			throw new SaturnJobConsoleException(t);
		} finally {
			ThreadLocalCuratorClient.clear();
		}
	}

	public static void reuse(String namespace, final String jobName, RegistryCenterService registryCenterService,
			CuratorRepository curatorRepository, final ReuseCallBackWithoutReturn callBack)
			throws SaturnJobConsoleException {
		reuse(namespace, registryCenterService, curatorRepository, new ReuseCallBackWithoutReturn() {
			@Override
			public void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
				if (!curatorFrameworkOp.checkExists(JobNodePath.getJobNodePath(jobName))) {
					throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, String.format(JOB_NOT_EXIST_TEMPLATE, jobName));
				}
				callBack.call(curatorFrameworkOp);
			}
		});
	}

	public static void reuse(String namespace, RegistryCenterService registryCenterService,
			CuratorRepository curatorRepository, ReuseCallBackWithoutReturn callBack) throws SaturnJobConsoleException {
		try {
			RegistryCenterConfiguration registryCenterConfiguration = registryCenterService
					.findConfigByNamespace(namespace);
			if (registryCenterConfiguration == null) {
				throw new SaturnJobConsoleException(ERROR_CODE_NOT_EXISTED, String.format(NAMESPACE_NOT_EXIST_TEMPLATE, namespace));
			}
			RegistryCenterClient registryCenterClient = registryCenterService.connectByNamespace(namespace);
			if (registryCenterClient != null && registryCenterClient.isConnected()) {
				CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
				CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository
						.newCuratorFrameworkOp(curatorClient);
				ThreadLocalCuratorClient.setCuratorClient(curatorClient);
				callBack.call(curatorFrameworkOp);
			} else {
				throw new SaturnJobConsoleException("Connect zookeeper failed");
			}
		} catch (SaturnJobConsoleException e) {
			throw e;
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			throw new SaturnJobConsoleException(t);
		} finally {
			ThreadLocalCuratorClient.clear();
		}
	}

}
