/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 */

package com.vip.saturn.job.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Strings;

/**
 * 获取真实本机网络的实现类.
 * 
 * 
 */
public class LocalHostService {

	private static final String IP_REGEX = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
			+ "(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
			+ "(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\." + "(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";
	public static volatile String cachedIpAddress;

	/** for inner test */
	private static volatile String cachedHostName;

	private static final String ERROR_HOSTNAME = "GET_HOSTNAME_ERROR";

	/**
	 * ip读取顺序：参数 -> 环境变量 -> bond0 -> eth0。
	 */
	static {
		cachedIpAddress = System.getProperty("VIP_SATURN_RUNNING_IP", System.getenv("VIP_SATURN_RUNNING_IP"));
		cachedHostName = System.getProperty("VIP_SATURN_RUNNING_HOSTNAME",
				System.getenv("VIP_SATURN_RUNNING_HOSTNAME"));
		if (StringUtils.isEmpty(cachedIpAddress)) {
			obtainCacheIpAddress();
		} else {
			if (!isIpv4(cachedIpAddress)) {
				System.err.println("IP address " + cachedIpAddress + " is illegal. System is shutting down.");// NOSONAR
				System.exit(-1);
			}
		}
		System.out.println("Done initial localhostip: " + cachedIpAddress);// NOSONAR
	}

	private static void obtainCacheIpAddress() {
		try {
			InetAddress inetAddress = InetAddress.getLocalHost();
			if (inetAddress.getHostAddress() == null || "127.0.0.1".equals(inetAddress.getHostAddress())) {
				NetworkInterface ni = NetworkInterface.getByName("bond0");
				if (ni == null) {
					ni = NetworkInterface.getByName("eth0");
				}
				if (ni == null) {
					throw new Exception(
							"wrong with get ip cause by could not read any info from local host, bond0 and eth0");
				}

				Enumeration<InetAddress> ips = ni.getInetAddresses();
				while (ips.hasMoreElements()) {
					InetAddress nextElement = ips.nextElement();
					if (!"127.0.0.1".equals(nextElement.getHostAddress()) && !(nextElement instanceof Inet6Address)
							&& !nextElement.getHostAddress().contains(":")) {
						inetAddress = nextElement;
						break;
					}
				}
			}
			cachedIpAddress = inetAddress.getHostAddress();
		} catch (Throwable e) {// NOSONAR
			System.err.println("getCachedAddressException:" + e.toString());// NOSONAR
			System.exit(-1);
		}
	}

	/**
	 * 获取本机Host名称.
	 * 
	 * @return 本机Host名称
	 */
	public static String getHostName() {
		if (!Strings.isNullOrEmpty(cachedHostName)) {
			return cachedHostName;
		} else {
			try {
				cachedHostName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {// NOSONAR
				e.printStackTrace();// NOSONAR
				return ERROR_HOSTNAME;
			}
			return cachedHostName;
		}
	}

	private static boolean isIpv4(String ipAddress) {
		Pattern pattern = Pattern.compile(IP_REGEX);
		Matcher matcher = pattern.matcher(ipAddress);
		return matcher.matches();

	}
}
