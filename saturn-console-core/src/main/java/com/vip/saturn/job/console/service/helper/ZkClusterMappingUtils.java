package com.vip.saturn.job.console.service.helper;

import com.vip.saturn.job.console.service.SystemConfigService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vip.saturn.job.console.service.helper.SystemConfigProperties.*;

/**
 * 管理以下的映射关系
 * <p>
 * <ul> <li>zk集群key和IDC标识映射关系</li> <li>IDC标识和console域名的映射关系</li> <li>console集群Id和IDC标识的映射关系</li> </ul>
 *
 * @author timmy.hu
 */
public class ZkClusterMappingUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZkClusterMappingUtils.class);

	private static final String DEFAULT_CONSOLE_CLUSTER_ID = "default";

	/**
	 * Console控制台集群ID的环境变量名称
	 */
	private static final String NAME_VIP_SATURN_CONSOLE_CLUSTER = "VIP_SATURN_CONSOLE_CLUSTER";

	/**
	 * Console控制台集群ID
	 */
	public static String VIP_SATURN_CONSOLE_CLUSTER_ID = StringUtils
			.trim(System.getProperty(NAME_VIP_SATURN_CONSOLE_CLUSTER, System.getenv(NAME_VIP_SATURN_CONSOLE_CLUSTER)));

	static {
		if (StringUtils.isBlank(VIP_SATURN_CONSOLE_CLUSTER_ID)) {
			LOGGER.warn("The {} is not configured, will use the default value that is {}",
					NAME_VIP_SATURN_CONSOLE_CLUSTER,
					DEFAULT_CONSOLE_CLUSTER_ID);
			VIP_SATURN_CONSOLE_CLUSTER_ID = DEFAULT_CONSOLE_CLUSTER_ID;
		}
	}

	/**
	 * 根据zk集群key获取IDC标识
	 */
	public static String getIdcByZkClusterKey(SystemConfigService systemConfigService, String zkClusterKey) {
		return getValue(systemConfigService, IDC_ZK_CLUSTER_MAPPING, zkClusterKey);
	}

	private static String getValue(SystemConfigService systemConfigService, String mappingName, String key) {
		String mappingValue = StringUtils
				.deleteWhitespace(systemConfigService.getValueDirectly(mappingName));
		if (StringUtils.isBlank(mappingValue)) {
			LOGGER.warn("The {} is not configured in sys_config", mappingName);
			return null;
		}
		String[] split = mappingValue.split(";");
		for (String temp : split) {
			int idx = temp.indexOf(':');
			if (idx < 0) {
				LOGGER.warn(
						"The {}' value {}, whose format is not correct, should like key:value1,value2;key2:value3,value4",
						mappingName, mappingValue);
				return null;
			}
			String tempKey = temp.substring(0, idx);
			String tempValues = temp.substring(idx + 1);
			String[] valuesArray = tempValues.split(",");
			for (String value : valuesArray) {
				if (value.equals(key)) {
					return tempKey;
				}
			}
		}
		return null;
	}

	/**
	 * 根据consoleId获取IDC标识
	 */
	public static String getIdcByConsoleId(SystemConfigService systemConfigService, String consoleId) {
		return getValue(systemConfigService, IDC_CONSOLE_ID_MAPPING, consoleId);
	}

	/**
	 * 根据zk集群key，判断该zk集群所属的idc，是否和当前console在同一个idc
	 */
	public static boolean isCurrentConsoleInTheSameIdc(SystemConfigService systemConfigService, String zkClusterKey) {
		try {
			String zkClusterIdc = getIdcByZkClusterKey(systemConfigService, zkClusterKey);
			if (StringUtils.isBlank(zkClusterIdc)) {
				LOGGER.warn("The mapping idc is not found for the zkClusterKey that is {}", zkClusterKey);
				return true;
			}
			String consoleIdc = getIdcByConsoleId(systemConfigService, VIP_SATURN_CONSOLE_CLUSTER_ID);
			return zkClusterIdc.equalsIgnoreCase(consoleIdc);
		} catch (Exception e) {
			LOGGER.error("isCurrentConsoleInTheSameIdc error, will return true", e);
			return true;
		}
	}

	/**
	 * 根据IDC标识，获取console的域名
	 */
	public static String getConsoleDomainByIdc(SystemConfigService systemConfigService, String idc) {
		String mappingValue = StringUtils
				.deleteWhitespace(systemConfigService.getValueDirectly(IDC_CONSOLE_DOMAIN_MAPPING));
		if (StringUtils.isBlank(mappingValue)) {
			LOGGER.warn("The {} is not configured in sys_config", IDC_CONSOLE_DOMAIN_MAPPING);
			return null;
		}
		String[] split = mappingValue.split(";");
		for (String temp : split) {
			int idx = temp.indexOf(':');
			if (idx < 0) {
				LOGGER.warn("The {}' value {}, whose format is not correct, should like key:value;key2:value2",
						IDC_CONSOLE_DOMAIN_MAPPING, mappingValue);
				return null;
			}
			String tempIdc = temp.substring(0, idx);
			String tempDomain = temp.substring(idx + 1);
			if (tempIdc.equals(idc)) {
				return tempDomain;
			}
		}
		return null;
	}

	/**
	 * 根据zk集群key，获取该zk集群所属机房的console域名
	 */
	public static String getConsoleDomainByZkClusterKey(SystemConfigService systemConfigService, String zkClusterKey) {
		String idc = getIdcByZkClusterKey(systemConfigService, zkClusterKey);
		return StringUtils.isNotBlank(idc) ? getConsoleDomainByIdc(systemConfigService, idc) : null;
	}

}
