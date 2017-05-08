package com.vip.saturn.job.console.service.impl.helper;

import com.vip.saturn.job.console.domain.RegistryCenterClient;
import com.vip.saturn.job.console.domain.RegistryCenterConfiguration;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;
import com.vip.saturn.job.console.service.RegistryCenterService;
import com.vip.saturn.job.console.utils.JobNodePath;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hebelala
 */
public class ReuseUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReuseUtils.class);

    public static <T> T reuse(String namespace, final String jobName, RegistryCenterService registryCenterService, CuratorRepository curatorRepository, final ReuseCallBack<T> callBack) throws SaturnJobConsoleException {
        return reuse(namespace, registryCenterService, curatorRepository, new ReuseCallBack<T>() {
            @Override
            public T call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
                if (!curatorFrameworkOp.checkExists(JobNodePath.getJobNodePath(jobName))) {
                    throw new SaturnJobConsoleException("The jobName does not exists");
                }
                return callBack.call(curatorFrameworkOp);
            }
        });
    }

    public static <T> T reuse(String namespace, RegistryCenterService registryCenterService, CuratorRepository curatorRepository, ReuseCallBack<T> callBack) throws SaturnJobConsoleException {
        try {
            RegistryCenterConfiguration registryCenterConfiguration = registryCenterService.findConfigByNamespace(namespace);
            if (registryCenterConfiguration == null) {
                throw new SaturnJobConsoleException("The namespace does not exists");
            }
            RegistryCenterClient registryCenterClient = registryCenterService.connectByNamespace(namespace);
            if (registryCenterClient != null && registryCenterClient.isConnected()) {
                CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
                CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.newCuratorFrameworkOp(curatorClient);
                return callBack.call(curatorFrameworkOp);
            } else {
                throw new SaturnJobConsoleException("Connect zookeeper failed");
            }
        } catch (SaturnJobConsoleException e) {
            throw e;
        } catch (Throwable t) {
            LOGGER.error(t.getMessage(), t);
            throw new SaturnJobConsoleException(t);
        }
    }

    public static void reuse(String namespace, final String jobName, RegistryCenterService registryCenterService, CuratorRepository curatorRepository, final ReuseCallBackWithoutReturn callBack) throws SaturnJobConsoleException {
        reuse(namespace, registryCenterService, curatorRepository, new ReuseCallBackWithoutReturn() {
            @Override
            public void call(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) throws SaturnJobConsoleException {
                if (!curatorFrameworkOp.checkExists(JobNodePath.getJobNodePath(jobName))) {
                    throw new SaturnJobConsoleException("The jobName does not exists");
                }
                callBack.call(curatorFrameworkOp);
            }
        });
    }

    public static void reuse(String namespace, RegistryCenterService registryCenterService, CuratorRepository curatorRepository, ReuseCallBackWithoutReturn callBack) throws SaturnJobConsoleException {
        try {
            RegistryCenterConfiguration registryCenterConfiguration = registryCenterService.findConfigByNamespace(namespace);
            if (registryCenterConfiguration == null) {
                throw new SaturnJobConsoleException("The namespace does not exists");
            }
            RegistryCenterClient registryCenterClient = registryCenterService.connectByNamespace(namespace);
            if (registryCenterClient != null && registryCenterClient.isConnected()) {
                CuratorFramework curatorClient = registryCenterClient.getCuratorClient();
                CuratorRepository.CuratorFrameworkOp curatorFrameworkOp = curatorRepository.newCuratorFrameworkOp(curatorClient);

                callBack.call(curatorFrameworkOp);
            } else {
                throw new SaturnJobConsoleException("Connect zookeeper failed");
            }
        } catch (SaturnJobConsoleException e) {
            throw e;
        } catch (Throwable t) {
            LOGGER.error(t.getMessage(), t);
            throw new SaturnJobConsoleException(t);
        }
    }

}
