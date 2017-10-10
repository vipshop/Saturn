/**
 * 
 */
package com.vip.saturn.job.console.service.helper;

import static com.vip.saturn.job.console.service.helper.SystemConfigProperties.IDC_CONSOLE_DOMAIN_MAPPING;
import static com.vip.saturn.job.console.service.helper.SystemConfigProperties.IDC_CONSOLE_ID_MAPPING;
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

	public static String NAME_VIP_SATURN_CONSOLE_CLUSTER = "VIP_SATURN_CONSOLE_CLUSTER";

	public static String VIP_SATURN_CONSOLE_CLUSTER_ID = StringUtils
			.trim(System.getProperty(NAME_VIP_SATURN_CONSOLE_CLUSTER, System.getenv(NAME_VIP_SATURN_CONSOLE_CLUSTER)));

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
	 * <pre>
	 * 		 key:IDC标识和console集群Id的映射关系的配置串
	 * 	     value:map
	 *       		key:idc标识
	 *       		value: console集群Id
	 * </pre>
	 */
	private static Map<String, Map<String, String>> idcConsoleIdMapsCache;

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
		String result = null;
		String idcZkClusterMappingStr = getIdcZkClusterMappingStr(systemConfigService);
		if (zkClusterIdcMapsCache != null) {
			Map<String, String> zkClusterIdcMap = zkClusterIdcMapsCache.get(idcZkClusterMappingStr);
			if (zkClusterIdcMap != null) {
				result = zkClusterIdcMap.get(zkClusterKey);
				if (result == null) {
					throw new SaturnJobConsoleException("idc not found");
				} else {
					return result;
				}
			}
		}
		Map<String, String> zkClusterConsoleDomainMap = getZkClusterIdcMap(idcZkClusterMappingStr);
		Map<String, Map<String, String>> tempZkClusterIdcMapsCache = new HashMap<String, Map<String, String>>();
		tempZkClusterIdcMapsCache.put(idcZkClusterMappingStr, zkClusterConsoleDomainMap);
		zkClusterIdcMapsCache = tempZkClusterIdcMapsCache;
		result = zkClusterConsoleDomainMap.get(zkClusterKey);
		if (result == null) {
			throw new SaturnJobConsoleException("idc not found");
		} else {
			return result;
		}
	}

	/**
	 * 根据consoleId获取IDC标识
	 * 
	 * @param systemConfigService
	 * @param zkClusterKey
	 * @return
	 * @throws SaturnJobConsoleException
	 */
	public static String getIdcByConsoleId(SystemConfigService systemConfigService, String consoleId)
			throws SaturnJobConsoleException {
		String result = null;
		String idcConsoleIdMappingStr = getIdcConsoleIdMappingStr(systemConfigService);
		if (idcConsoleIdMapsCache != null) {
			Map<String, String> idcConsoleIdMap = idcConsoleIdMapsCache.get(idcConsoleIdMappingStr);
			if (idcConsoleIdMap != null) {
				result = idcConsoleIdMap.get(consoleId);
				if (result == null) {
					throw new SaturnJobConsoleException("idc not found");
				} else {
					return result;
				}
			}
		}
		Map<String, String> idcConsoleIdMap = getIdcConsoleIdMap(idcConsoleIdMappingStr);
		Map<String, Map<String, String>> tempIdcConsoleIdMapsCache = new HashMap<String, Map<String, String>>();
		tempIdcConsoleIdMapsCache.put(idcConsoleIdMappingStr, idcConsoleIdMap);
		idcConsoleIdMapsCache = tempIdcConsoleIdMapsCache;
		result = idcConsoleIdMap.get(consoleId);
		if (result == null) {
			throw new SaturnJobConsoleException("idc not found");
		} else {
			return result;
		}
	}

	public static boolean isCurrentConsoleInTheSameIdc(SystemConfigService systemConfigService, String zkClusterKey) {
		try {
			if (StringUtils.isBlank(VIP_SATURN_CONSOLE_CLUSTER_ID)) {
				LOGGER.warn("没有配置VIP_SATURN_CONSOLE_CLUSTER环境变量或者系统属性");
				return false;
			}
			String zkCluseterIdc = getIdcByZkClusterKey(systemConfigService, zkClusterKey);
			if (zkCluseterIdc == null) {
				LOGGER.warn("根据zkClusterKey:" + zkClusterKey + "，没有找到其所属的IDC信息");
				return false;
			}
			String consoleIdc = getIdcByConsoleId(systemConfigService, VIP_SATURN_CONSOLE_CLUSTER_ID);
			return zkCluseterIdc.equals(consoleIdc);
		} catch (SaturnJobConsoleException e) {
			LOGGER.error("error occur when judge current console is in the same idc with the zk cluster", e);
			return false;
		}
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
		String result = null;
		String idcConsoleDomainMappingStr = getIdcConsoleDomainMappingStr(systemConfigService);
		if (idcConsoleDomainMapsCache != null) {
			Map<String, String> idcConsoleDomainMap = idcConsoleDomainMapsCache.get(idcConsoleDomainMappingStr);
			if (idcConsoleDomainMap != null) {
				result = idcConsoleDomainMap.get(idc);
				if (result == null) {
					throw new SaturnJobConsoleException("console domain not found");
				} else {
					return result;
				}
			}
		}
		Map<String, String> idcConsoleDomainMap = getIdcConsoleDomainMap(idcConsoleDomainMappingStr);
		Map<String, Map<String, String>> tempIdcConsoleDomainMapsCache = new HashMap<String, Map<String, String>>();
		tempIdcConsoleDomainMapsCache.put(idcConsoleDomainMappingStr, idcConsoleDomainMap);
		idcConsoleDomainMapsCache = tempIdcConsoleDomainMapsCache;
		result = idcConsoleDomainMap.get(idc);
		if (result == null) {
			throw new SaturnJobConsoleException("console domain not found");
		} else {
			return result;
		}
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
		String result = null;
		if (idc != null) {
			result = getConsoleDomainByIdc(systemConfigService, idc);
		}
		if (result == null) {
			throw new SaturnJobConsoleException("console domain not found");
		} else {
			return result;
		}
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

	private static String getIdcConsoleIdMappingStr(SystemConfigService systemConfigService)
			throws SaturnJobConsoleException {
		String idcConsoleIdMappingStr = systemConfigService.getValueDirectly(IDC_CONSOLE_ID_MAPPING);
		LOGGER.info("the IDC_CONSOLE_ID_MAPPING str is:" + idcConsoleIdMappingStr);
		if (StringUtils.isBlank(idcConsoleIdMappingStr)) {
			throw new SaturnJobConsoleException("the IDC_CONSOLE_ID_MAPPING is not configured in sys_config");
		}
		return StringUtils.deleteWhitespace(idcConsoleIdMappingStr);
	}

	private static Map<String, String> getZkClusterIdcMap(String allMappingStr) throws SaturnJobConsoleException {
		Map<String, String> result = new HashMap<String, String>();
		String[] idcMappingArray = allMappingStr.split(";");
		for (String idcMappingStr : idcMappingArray) {
			int index = idcMappingStr.indexOf(":");
			if (index < 0) {
				throw new SaturnJobConsoleException("the IDC_ZK_CLUSTER_MAPPING(" + idcMappingStr
						+ ") format is not correct, should be like gd6:/zk1");
			}
			String domain = idcMappingStr.substring(0, index);
			String zkClusterKeys = idcMappingStr.substring(index + 1);
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
			int index = consoleDomainMappingStr.indexOf(":");
			if (index < 0) {
				throw new SaturnJobConsoleException("the IDC_CONSOLE_DOMAIN_MAPPING(" + consoleDomainMappingStr
						+ ") format is not correct, should be like CONSOLE-GD9:gd9");
			}
			String idc = consoleDomainMappingStr.substring(0, index);
			String domain = consoleDomainMappingStr.substring(index + 1);
			result.put(idc, domain);
		}
		return result;
	}

	private static Map<String, String> getIdcConsoleIdMap(String allMappingStr) throws SaturnJobConsoleException {
		Map<String, String> result = new HashMap<String, String>();
		String[] consoleIdMappingArray = allMappingStr.split(";");
		for (String consoleIdMappingStr : consoleIdMappingArray) {
			int index = consoleIdMappingStr.indexOf(":");
			if (index < 0) {
				throw new SaturnJobConsoleException("the IDC_CONSOLE_ID_MAPPING(" + consoleIdMappingStr
						+ ") format is not correct, should be like CONSOLE-GD9:gd9");
			}
			String idc = consoleIdMappingStr.substring(0, index);
			String consoleIds = consoleIdMappingStr.substring(index + 1);
			String[] consoleIdArray = consoleIds.trim().split(",");
			for (String consoleId : consoleIdArray) {
				result.put(consoleId, idc);
			}
		}
		return result;
	}

}
