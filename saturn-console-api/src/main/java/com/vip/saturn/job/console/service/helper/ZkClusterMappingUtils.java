package com.vip.saturn.job.console.service.helper;

import static com.vip.saturn.job.console.service.helper.SystemConfigProperties.IDC_CONSOLE_DOMAIN_MAPPING;
import static com.vip.saturn.job.console.service.helper.SystemConfigProperties.IDC_CONSOLE_ID_MAPPING;
import static com.vip.saturn.job.console.service.helper.SystemConfigProperties.IDC_ZK_CLUSTER_MAPPING;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.service.SystemConfigService;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 管理以下的映射关系
 *
 * <ul> <li>zk集群key和IDC标识映射关系</li> <li>IDC标识和console域名的映射关系</li> <li>console集群Id和IDC标识的映射关系</li> </ul>
 *
 * @author timmy.hu
 */
public class ZkClusterMappingUtils {

	private static final Logger log = LoggerFactory.getLogger(ZkClusterMappingUtils.class);

	private static final String DEFAULT_CONSOLE_CLUSTER_ID = "default";

	/**
	 * Console控制台集群ID的环境变量名称
	 */
	private static String NAME_VIP_SATURN_CONSOLE_CLUSTER = "VIP_SATURN_CONSOLE_CLUSTER";

	/**
	 * Console控制台集群ID
	 */
	public static String VIP_SATURN_CONSOLE_CLUSTER_ID = StringUtils
			.trim(System.getProperty(NAME_VIP_SATURN_CONSOLE_CLUSTER, System.getenv(NAME_VIP_SATURN_CONSOLE_CLUSTER)));

	/**
	 * <pre>
	 *       key:zk集群key和IDC标识映射关系的配置串
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
	 * 		 key:console集群Id和IDC标识的映射关系的配置串
	 * 	     value:map
	 *       		key:console集群Id
	 *       		value:idc标识
	 * </pre>
	 */
	private static Map<String, Map<String, String>> consoleIdIdcMapsCache;

	private static String consoleClusterId;

	static {
		if (StringUtils.isBlank(VIP_SATURN_CONSOLE_CLUSTER_ID)) {
			log.info("The VIP_SATURN_CONSOLE_CLUSTER is not configured, will use the default value that is {}",
					DEFAULT_CONSOLE_CLUSTER_ID);
			consoleClusterId = DEFAULT_CONSOLE_CLUSTER_ID;
		} else {
			consoleClusterId = VIP_SATURN_CONSOLE_CLUSTER_ID;
		}
	}

	/**
	 * 根据zk集群key获取IDC标识
	 *
	 * @return zk集群所属idc标识，如果没有，返回null；
	 */
	public static String getIdcByZkClusterKey(SystemConfigService systemConfigService, String zkClusterKey)
			throws SaturnJobConsoleException {
		String idcZkClusterMappingStr = getRelaMappingStr(systemConfigService, IDC_ZK_CLUSTER_MAPPING);
		if (idcZkClusterMappingStr == null) {
			return null;
		}
		Map<String, String> zkClusterIdcMap = null;
		if (zkClusterIdcMapsCache != null) {
			zkClusterIdcMap = zkClusterIdcMapsCache.get(idcZkClusterMappingStr);
		}
		if (zkClusterIdcMap == null) {
			zkClusterIdcMap = convert2RelationshipMap(idcZkClusterMappingStr);
			reInitZkClusterIdcMapsCache(idcZkClusterMappingStr, zkClusterIdcMap);
		}
		String result = zkClusterIdcMap.get(zkClusterKey);
		if (result == null) {
			log.info("idc not found by zkClusterKey: {}", zkClusterKey);
		}
		return result;
	}

	/**
	 * 重新设置zk集群和idc的映射关系缓存
	 */
	private static void reInitZkClusterIdcMapsCache(String idcZkClusterMappingStr,
			Map<String, String> zkClusterIdcMap) {
		Map<String, Map<String, String>> tempZkClusterIdcMapsCache = new HashMap<String, Map<String, String>>();
		tempZkClusterIdcMapsCache.put(idcZkClusterMappingStr, zkClusterIdcMap);
		zkClusterIdcMapsCache = tempZkClusterIdcMapsCache;
	}

	/**
	 * 根据console集群Id获取IDC标识
	 *
	 * @return console集群所属idc标识，如果没有，返回null；
	 */
	public static String getIdcByConsoleId(SystemConfigService systemConfigService, String consoleId)
			throws SaturnJobConsoleException {
		String idcConsoleIdMappingStr = getRelaMappingStr(systemConfigService, IDC_CONSOLE_ID_MAPPING);
		if (idcConsoleIdMappingStr == null) {
			return null;
		}
		Map<String, String> idcConsoleIdMap = null;
		if (consoleIdIdcMapsCache != null) {
			idcConsoleIdMap = consoleIdIdcMapsCache.get(idcConsoleIdMappingStr);
		}
		if (idcConsoleIdMap == null) {
			idcConsoleIdMap = convert2RelationshipMap(idcConsoleIdMappingStr);
			reInitConsoleIdIdcMapsCache(idcConsoleIdMappingStr, idcConsoleIdMap);
		}
		String result = idcConsoleIdMap.get(consoleId);
		if (result == null) {
			log.info("idc not found by consoleId: {}", consoleId);
		}
		return result;
	}

	/**
	 * 重新初始化console集群ID和IDC之间的映射关系缓存
	 */
	private static void reInitConsoleIdIdcMapsCache(String idcConsoleIdMappingStr,
			Map<String, String> idcConsoleIdMap) {
		Map<String, Map<String, String>> tempConsoleIdIdcMapsCache = new HashMap<String, Map<String, String>>();
		tempConsoleIdIdcMapsCache.put(idcConsoleIdMappingStr, idcConsoleIdMap);
		consoleIdIdcMapsCache = tempConsoleIdIdcMapsCache;
	}

	/**
	 * 根据zk集群key，判断该zk集群所属的idc，是否和当前console在同一个idc
	 *
	 * @return zk cluster所属idc标识，如果没有，返回null；
	 */
	public static boolean isCurrentConsoleInTheSameIdc(SystemConfigService systemConfigService, String zkClusterKey) {
		try {
			String zkCluseterIdc = getIdcByZkClusterKey(systemConfigService, zkClusterKey);
			if (zkCluseterIdc == null) {
				return true;
			}
			String consoleIdc = getIdcByConsoleId(systemConfigService, consoleClusterId);
			return StringUtils.isBlank(consoleIdc) ? true : zkCluseterIdc.equalsIgnoreCase(consoleIdc);
		} catch (SaturnJobConsoleException e) {
			log.error("error occur when judge current console is in the same idc with the zk cluster", e);
			return true;
		}
	}

	/**
	 * 根据IDC标识，获取console的域名
	 */
	public static String getConsoleDomainByIdc(SystemConfigService systemConfigService, String idc)
			throws SaturnJobConsoleException {
		String idcConsoleDomainMappingStr = getRelaMappingStr(systemConfigService, IDC_CONSOLE_DOMAIN_MAPPING);
		if (idcConsoleDomainMappingStr == null) {
			return null;
		}
		Map<String, String> idcConsoleDomainMap = null;
		if (idcConsoleDomainMapsCache != null) {
			idcConsoleDomainMap = idcConsoleDomainMapsCache.get(idcConsoleDomainMappingStr);
		}
		if (idcConsoleDomainMap == null) {
			idcConsoleDomainMap = toIdcConsoleDomainMap(idcConsoleDomainMappingStr);
			reInitIdcConsoleDomainMapsCache(idcConsoleDomainMappingStr, idcConsoleDomainMap);
		}
		String result = idcConsoleDomainMap.get(idc);
		if (result == null) {
			log.info("console domain not found by idc: {}", idc);
		}
		return result;
	}

	/**
	 * 重新设置Idc和console域名映射关系的缓存
	 */
	private static void reInitIdcConsoleDomainMapsCache(String idcConsoleDomainMappingStr,
			Map<String, String> idcConsoleDomainMap) {
		Map<String, Map<String, String>> tempIdcConsoleDomainMapsCache = new HashMap<String, Map<String, String>>();
		tempIdcConsoleDomainMapsCache.put(idcConsoleDomainMappingStr, idcConsoleDomainMap);
		idcConsoleDomainMapsCache = tempIdcConsoleDomainMapsCache;
	}

	/**
	 * 根据zk集群key，获取该zk集群所属机房的console域名
	 *
	 * @return zk集群所属机房的console域名，如果没有返回null；
	 */
	public static String getConsoleDomainByZkClusterKey(SystemConfigService systemConfigService, String zkClusterKey)
			throws SaturnJobConsoleException {
		String idc = getIdcByZkClusterKey(systemConfigService, zkClusterKey);
		String result = null;
		if (idc != null) {
			result = getConsoleDomainByIdc(systemConfigService, idc);
		}
		if (result == null) {
			log.info("console domain not found by zkClusterKey: {}", zkClusterKey);
		}
		return result;
	}

	/**
	 * 根据映射关系名称，获取该映射关系值
	 */
	private static String getRelaMappingStr(SystemConfigService systemConfigService, String mappingName) {
		String mappingValue = systemConfigService.getValueDirectly(mappingName);
		log.debug("the mapping key is:{}, the mapping value is: {}", mappingName, mappingValue);
		if (StringUtils.isBlank(mappingValue)) {
			log.info("the mapping:{} is not configured in sys_config ", mappingName);
			return null;
		}
		return StringUtils.deleteWhitespace(mappingValue);
	}

	/**
	 * 根据映射关系的字符串，解析出其中的map对应关系
	 *
	 * @param relaMappings 例如输入格式如下: idc1:domain1;idc2:domain2;
	 * @return <pre>
	 *       解析出来的map对应关系 针对以上的输入，返回map如下：
	 *      idc1:domain1
	 *      idc2:domain2
	 * </pre>
	 */
	private static Map<String, String> toIdcConsoleDomainMap(String relaMappings) throws SaturnJobConsoleException {
		Map<String, String> result = new HashMap<String, String>();
		String[] consoleDomainMappingArray = relaMappings.split(";");
		for (String consoleDomainMappingStr : consoleDomainMappingArray) {
			int colonIndex = consoleDomainMappingStr.indexOf(":");
			if (colonIndex < 0) {
				throw new SaturnJobConsoleException("the format(" + consoleDomainMappingStr
						+ ") is not correct, should like key1:value1;key2:value2");
			}
			String idc = consoleDomainMappingStr.substring(0, colonIndex);
			String domain = consoleDomainMappingStr.substring(colonIndex + 1);
			result.put(idc, domain);
		}
		return result;
	}

	/**
	 * 根据映射关系的字符串，解析出其中的map对应关系
	 *
	 * @param relaMappings 例如输入格式如下: idc1:/zk1,/zk2;idc2:/zk3;
	 * @return <pre>
	 *       解析出来的map对应关系 针对以上的输入，返回map如下：
	 *      /zk1:idc1
	 *      /zk2:idc1
	 *      /zk3:idc2
	 * </pre>
	 */
	private static Map<String, String> convert2RelationshipMap(String relaMappings) throws SaturnJobConsoleException {
		Map<String, String> result = new HashMap<>();
		String[] relaMappingArray = relaMappings.split(";");
		for (String relaMapping : relaMappingArray) {
			int colonIndex = relaMapping.indexOf(":");
			if (colonIndex <= 0) {
				throw new SaturnJobConsoleException(
						"the format(" + relaMapping + ") is not correct, should like key1:value1,value2");
			}
			String value = relaMapping.substring(0, colonIndex);
			String keys = relaMapping.substring(colonIndex + 1);
			String[] keyArray = keys.trim().split(",");
			for (String key : keyArray) {
				result.put(key, value);
			}
		}
		return result;
	}

}