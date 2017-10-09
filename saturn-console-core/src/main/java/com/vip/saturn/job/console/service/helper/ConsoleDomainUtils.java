/**
 * 
 */
package com.vip.saturn.job.console.service.helper;

import static com.vip.saturn.job.console.service.helper.SystemConfigProperties.CONSOLE_DOMAIN_ZK_CLUSTER_MAPPING;

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
public class ConsoleDomainUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleDomainUtils.class);

	/**
	 * <pre>
	 * 		key: 配置串
	 * 		value: Map<String, String>
	 *              	key:zkClusterKey
	 *                  value:domain
	 * </pre>
	 */
	private static Map<String, Map<String, String>> domainMapsCache;

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
		String allMappingStr = systemConfigService.getValueDirectly(CONSOLE_DOMAIN_ZK_CLUSTER_MAPPING);
		LOGGER.info("the CONSOLE_DOMAIN_ZK_CLUSTER_MAPPING str is:" + allMappingStr);
		if (StringUtils.isBlank(allMappingStr)) {
			throw new SaturnJobConsoleException(
					"the CONSOLE_DOMAIN_ZK_CLUSTER_MAPPING is not configured in sys_config");
		}
		allMappingStr = StringUtils.deleteWhitespace(allMappingStr);
		if (domainMapsCache != null) {
			Map<String, String> domainMap = domainMapsCache.get(zkClusterKey);
			if (domainMap != null) {
				return domainMap.get(zkClusterKey);
			}
		}
		Map<String, String> domainMap = getDomainMap(allMappingStr);
		Map<String, Map<String, String>> tempDomainMapsCache = new HashMap<String, Map<String, String>>();
		tempDomainMapsCache.put(allMappingStr, domainMap);
		domainMapsCache = tempDomainMapsCache;
		return domainMap.get(zkClusterKey);
	}

	private static Map<String, String> getDomainMap(String allMappingStr) throws SaturnJobConsoleException {
		Map<String, String> result = new HashMap<String, String>();
		String[] consoleDomainMappingArray = allMappingStr.split(";");
		for (String consoleDomainMappingStr : consoleDomainMappingArray) {
			String[] consoleDomainAndClusterKeyArray = consoleDomainMappingStr.split(":");
			if (consoleDomainAndClusterKeyArray.length != 2) {
				throw new SaturnJobConsoleException("the CONSOLE_DOMAIN_ZK_CLUSTER_MAPPING("
						+ consoleDomainAndClusterKeyArray + ") format is not correct, should be like domain:/zk1");
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

}
