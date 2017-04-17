package com.vip.saturn.job.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * @author xiaopeng.he
 */
public final class SaturnEnvProperties {

    protected static Logger log = LoggerFactory.getLogger(SaturnEnvProperties.class);

    private SaturnEnvProperties() {
    }

    /**
     * zk注册中心
     */
    public static String NAME_VIP_SATURN_ZK_CONNECTION = "VIP_SATURN_ZK_CONNECTION";

    /**
     * 指定注册中心地址
     */
    public static String REG_CENTER_VALUE;

    /**
     * 指定注册中心地址配置json文件
     */
    public static String REG_CENTER_JSON_FILE;

    public static String CONTAINER_TYPE = System.getProperty("VIP_SATURN_CONTAINER_TYPE", System.getenv("VIP_SATURN_CONTAINER_TYPE"));
    public static String VIP_SATURN_DCOS_REST_URI = System.getProperty("VIP_SATURN_DCOS_REST_URI", System.getenv("VIP_SATURN_DCOS_REST_URI"));
    public static String VIP_SATURN_DCOS_REGISTRY_URI = System.getProperty("VIP_SATURN_DCOS_REGISTRY_URI", System.getenv("VIP_SATURN_DCOS_REGISTRY_URI"));
    public static String NAME_VIP_SATURN_EXECUTOR_CLEAN = "VIP_SATURN_EXECUTOR_CLEAN";
    public static String NAME_VIP_SATURN_DCOS_TASK = "VIP_SATURN_DCOS_TASK";

    public static String NAME_VIP_SATURN_SYSTEM_CONFIG_PATH = "VIP_SATURN_SYSTEM_CONFIG_PATH";
    public static String VIP_SATURN_SYSTEM_CONFIG_PATH = System.getProperty(NAME_VIP_SATURN_SYSTEM_CONFIG_PATH, System.getenv(NAME_VIP_SATURN_SYSTEM_CONFIG_PATH));
    
    public static String SATURN_CONSOLE_DB_URL = System.getProperty("SATURN_CONSOLE_DB_URL", System.getenv("SATURN_CONSOLE_DB_URL"));
    public static String SATURN_CONSOLE_DB_USERNAME = System.getProperty("SATURN_CONSOLE_DB_USERNAME", System.getenv("SATURN_CONSOLE_DB_USERNAME"));
    public static String SATURN_CONSOLE_DB_PASSWORD = System.getProperty("SATURN_CONSOLE_DB_PASSWORD", System.getenv("SATURN_CONSOLE_DB_PASSWORD"));

    static {
        REG_CENTER_JSON_FILE = System.getProperty("REG_CENTER_JSON_PATH", System.getenv("REG_CENTER_JSON_PATH"));
        if (null != REG_CENTER_JSON_FILE) {
        }
        REG_CENTER_VALUE = System.getProperty("REG_CENTER_PROPERTY", System.getenv("REG_CENTER_PROPERTY"));
        if (null != REG_CENTER_VALUE) {
            REG_CENTER_VALUE = new String(REG_CENTER_VALUE.getBytes(), Charset.forName("UTF-8"));
        }
        if(CONTAINER_TYPE == null) {
            CONTAINER_TYPE = "MARATHON";
        }
    }
}
