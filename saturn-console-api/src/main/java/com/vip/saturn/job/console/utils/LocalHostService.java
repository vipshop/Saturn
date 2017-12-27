/**
 * Copyright 2016 vip.com. <p> Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. </p>
 */

package com.vip.saturn.job.console.utils;

import com.google.common.base.Strings;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 获取真实本机网络的实现类.
 *
 */
public class LocalHostService {

	private static final Logger log = LoggerFactory.getLogger(LocalHostService.class);
	private static final String IP_REGEX = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
			+ "(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
			+ "(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\." + "(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
	public static volatile String cachedIpAddress;

	/** for inner test */
	private static volatile String cachedHostName;

	/**
	 * ip读取顺序：参数 -> 环境变量 -> bond0 -> eth0。
	 */
	static {
		cachedIpAddress = System.getProperty("VIP_SATURN_RUNNING_IP", System.getenv("VIP_SATURN_RUNNING_IP"));
		cachedHostName = System.getProperty("VIP_SATURN_RUNNING_HOSTNAME",
				System.getenv("VIP_SATURN_RUNNING_HOSTNAME"));
		if (StringUtils.isEmpty(cachedIpAddress)) {
			try {
				InetAddress inetAddress = InetAddress.getLocalHost();
				if (inetAddress.getHostAddress() == null || "127.0.0.1".equals(inetAddress.getHostAddress())) {
					NetworkInterface ni = NetworkInterface.getByName("bond0");
					if (ni == null) {
						ni = NetworkInterface.getByName("eth0");
					}
					if (ni == null) {
						throw new Exception("failed in getting ip from local host, bond0 and eth0");
					}

					Enumeration<InetAddress> ips = ni.getInetAddresses();
					while (ips.hasMoreElements()) {
						InetAddress nextElement = ips.nextElement();
						if ("127.0.0.1".equals(nextElement.getHostAddress()) || nextElement instanceof Inet6Address
								|| nextElement.getHostAddress().contains(":")) {
							continue;
						}
						inetAddress = nextElement;
						break;
					}
				}
				cachedIpAddress = inetAddress.getHostAddress();
			} catch (Throwable e) {
				log.error(
						"[localHostService error][please configure hostname or bond0 or eth0. System is shutting down.]",
						e);
				System.exit(-1);
			}
		} else {
			if (!isIpv4(cachedIpAddress)) {
				log.error("IP address {} is illegal. System is shutting down.", cachedIpAddress);
				System.exit(-1);
			}
		}
		log.info("Done initial localhostip: {}.", cachedIpAddress);
	}

	/**
	 * 获取本机Host名称.
	 *
	 * @return 本机Host名称
	 */
	public static String getHostName() throws UnknownHostException {
		if (!Strings.isNullOrEmpty(cachedHostName)) {
			return cachedHostName;
		} else {
			return getLocalHost().getHostName();
		}
	}

	private static InetAddress getLocalHost() throws UnknownHostException {
		return InetAddress.getLocalHost();
	}

	// for test only!
	@Deprecated
	public static void setCachedIpAddress(String ip) {
		cachedIpAddress = ip;
	}

	// for test only!
	@Deprecated
	public static void setCachedHostName(String hostName) {
		cachedHostName = hostName;
	}

	private static boolean isIpv4(String ipAddress) {
		Pattern pattern = Pattern.compile(IP_REGEX);
		Matcher matcher = pattern.matcher(ipAddress);
		return matcher.matches();

	}
}
