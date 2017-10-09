/**
 * 
 */
package com.vip.saturn.job.console.service.helper;

import static com.vip.saturn.job.console.service.helper.SystemConfigProperties.IDC_CONSOLE_DOMAIN_MAPPING;
import static com.vip.saturn.job.console.service.helper.SystemConfigProperties.IDC_ZK_CLUSTER_MAPPING;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.SystemConfigService;

/**
 * @author timmy.hu
 */
public class ZkClusterMappingUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZkClusterMappingUtils.class);

	/**
	 * <pre>
	 *       key:IDC标识和zk集群key映射关系的配置串
	 *       value:map
	 *       		key:zk集群key
	 *       		value: IDC标识
	 * </pre>
	 */
	private static Map<String, Map<String, String>> zkClusterIdcMapsCache;

	/**
	 * <pre>
	 * 		 key:IDC标识和console域名的映射关系的配置串
	 * 	     value:map
	 *       		key:idc标识
	 *       		value: console域名
	 * </pre>
	 */
	private static Map<String, Map<String, String>> idcConsoleDomainMapsCache;

	/**
	 * 根据zk集群key获取IDC标识
	 * 
	 * @param systemConfigService
	 * @param zkClusterKey
	 * @return
	 * @throws SaturnJobConsoleException
	 */
	public static String getIdcByZkClusterKey(SystemConfigService systemConfigService, String zkClusterKey)
			throws SaturnJobConsoleException {
		String idcZkClusterMappingStr = getIdcZkClusterMappingStr(systemConfigService);
		if (zkClusterIdcMapsCache != null) {
			Map<String, String> zkClusterIdcMap = zkClusterIdcMapsCache.get(idcZkClusterMappingStr);
			if (zkClusterIdcMap != null) {
				return zkClusterIdcMap.get(zkClusterKey);
			}
		}
		Map<String, String> zkClusterConsoleDomainMap = getZkClusterIdcMap(idcZkClusterMappingStr);
		Map<String, Map<String, String>> tempZkClusterIdcMapsCache = new HashMap<String, Map<String, String>>();
		tempZkClusterIdcMapsCache.put(idcZkClusterMappingStr, zkClusterConsoleDomainMap);
		zkClusterIdcMapsCache = tempZkClusterIdcMapsCache;
		return zkClusterConsoleDomainMap.get(zkClusterKey);
	}

	/**
	 * 根据IDC标识，获取console的域名
	 * 
	 * @param systemConfigService
	 * @param idc
	 * @return
	 * @throws SaturnJobConsoleException
	 */
	public static String getConsoleDomainByIdc(SystemConfigService systemConfigService, String idc)
			throws SaturnJobConsoleException {
		String idcConsoleDomainMappingStr = getIdcConsoleDomainMappingStr(systemConfigService);
		if (idcConsoleDomainMapsCache != null) {
			Map<String, String> idcConsoleDomainMap = idcConsoleDomainMapsCache.get(idcConsoleDomainMappingStr);
			if (idcConsoleDomainMap != null) {
				return idcConsoleDomainMap.get(idc);
			}
		}
		Map<String, String> idcConsoleDomainMap = getIdcConsoleDomainMap(idcConsoleDomainMappingStr);
		Map<String, Map<String, String>> tempIdcConsoleDomainMapsCache = new HashMap<String, Map<String, String>>();
		tempIdcConsoleDomainMapsCache.put(idcConsoleDomainMappingStr, idcConsoleDomainMap);
		idcConsoleDomainMapsCache = tempIdcConsoleDomainMapsCache;
		return idcConsoleDomainMap.get(idc);
	}

	/**
	 * 根据zk集群key，获取该zk集群所属机房的console域名
	 * 
	 * @param systemConfigService
	 * @param zkClusterKey
	 * @return
	 * @throws SaturnJobConsoleException
	 */
	public static String getConsoleDomainByZkClusterKey(SystemConfigService systemConfigService, String zkClusterKey)
			throws SaturnJobConsoleException {
		String idc = getIdcByZkClusterKey(systemConfigService, zkClusterKey);
		if (idc == null) {
			return null;
		}
		return getConsoleDomainByIdc(systemConfigService, idc);
	}

	private static String getIdcZkClusterMappingStr(SystemConfigService systemConfigService)
			throws SaturnJobConsoleException {
		String idcZkClusterMappingStr = systemConfigService.getValueDirectly(IDC_ZK_CLUSTER_MAPPING);
		LOGGER.info("the IDC_ZK_CLUSTER_MAPPING str is:" + idcZkClusterMappingStr);
		if (StringUtils.isBlank(idcZkClusterMappingStr)) {
			throw new SaturnJobConsoleException("the IDC_ZK_CLUSTER_MAPPING is not configured in sys_config");
		}
		return StringUtils.deleteWhitespace(idcZkClusterMappingStr);
	}

	private static String getIdcConsoleDomainMappingStr(SystemConfigService systemConfigService)
			throws SaturnJobConsoleException {
		String idcConsoleDomainMappingStr = systemConfigService.getValueDirectly(IDC_CONSOLE_DOMAIN_MAPPING);
		LOGGER.info("the IDC_CONSOLE_DOMAIN_MAPPING str is:" + idcConsoleDomainMappingStr);
		if (StringUtils.isBlank(idcConsoleDomainMappingStr)) {
			throw new SaturnJobConsoleException("the IDC_CONSOLE_DOMAIN_MAPPING is not configured in sys_config");
		}
		return StringUtils.deleteWhitespace(idcConsoleDomainMappingStr);
	}

	private static Map<String, String> getZkClusterIdcMap(String allMappingStr) throws SaturnJobConsoleException {
		Map<String, String> result = new HashMap<String, String>();
		String[] consoleDomainMappingArray = allMappingStr.split(";");
		for (String consoleDomainMappingStr : consoleDomainMappingArray) {
			String[] consoleDomainAndClusterKeyArray = consoleDomainMappingStr.split(":");
			if (consoleDomainAndClusterKeyArray.length != 2) {
				throw new SaturnJobConsoleException("the IDC_ZK_CLUSTER_MAPPING(" + consoleDomainAndClusterKeyArray
						+ ") format is not correct, should be like gd6:/zk1");
			}
			String domain = consoleDomainAndClusterKeyArray[0];
			String zkClusterKeys = consoleDomainAndClusterKeyArray[1];
			String[] zkClusterKeyArray = zkClusterKeys.trim().split(",");
			for (String zkClusterKey : zkClusterKeyArray) {
				result.put(zkClusterKey, domain);
			}
		}
		return result;
	}

	private static Map<String, String> getIdcConsoleDomainMap(String allMappingStr) throws SaturnJobConsoleException {
		Map<String, String> result = new HashMap<String, String>();
		String[] consoleDomainMappingArray = allMappingStr.split(";");
		for (String consoleDomainMappingStr : consoleDomainMappingArray) {
			String[] consoleDomainAndClusterKeyArray = consoleDomainMappingStr.split(":");
			if (consoleDomainAndClusterKeyArray.length != 2) {
				throw new SaturnJobConsoleException("the IDC_CONSOLE_DOMAIN_MAPPING(" + consoleDomainAndClusterKeyArray
						+ ") format is not correct, should be like gd6:saturn.vip.vip.com");
			}
			String idc = consoleDomainAndClusterKeyArray[0];
			String domain = consoleDomainAndClusterKeyArray[1];
			result.put(idc, domain);
		}
		return result;
	}

}
