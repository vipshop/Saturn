package com.vip.saturn.job.console.utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ser.std.NullSerializer;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Json Utils
 *
 * @author linzhaoming
 */
public class JsonUtils {
	static Logger log = LoggerFactory.getLogger(JsonUtils.class);

	private static final ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
		mapper.getSerializerProvider().setNullKeySerializer(NullSerializer.instance);
	}

	private static ObjectMapper getObjectMapper() {
		return mapper;
	}

	/**
	 * 转换对象为JSON
	 *
	 * @param obj 待转换对象
	 * @return JSON字符串
	 */
	public static String toJSON(Object obj) {
		try {
			String value = getObjectMapper().writeValueAsString(obj);
			return value;
		} catch (Exception e) {
			log.error("msg=Fail at toJSON: ", e);
		}
		return "";
	}

	/**
	 * 从JSON转换为对象
	 *
	 * @param <T> 转换的Java类型
	 * @param jsonStr JSON字符串
	 * @param type 指定类型
	 * @return 转换后的对象
	 */
	public static <T> T fromJSON(String jsonStr, Class<T> type) {
		T result = null;
		try {
			result = getObjectMapper().readValue(jsonStr, type);
		} catch (Exception e) {
			log.error("msg=Fail at fromJSON: ", e);
		}
		return result;
	}

	/**
	 * 从JSON转换为对象
	 *
	 * @param jsonStr JSON字符串
	 * @param javaType 指定类型
	 * @return 转换后的对象
	 */
	public static <T> T fromJSON(String jsonStr, JavaType javaType) {
		T result = null;
		try {
			result = getObjectMapper().readValue(jsonStr, javaType);
		} catch (Exception e) {
			log.error("msg=Fail at fromJSON: ", e);
		}
		return result;
	}

	public static <T> T fromJSON(String jsonStr, TypeReference<T> type) {
		T result = null;
		try {
			result = getObjectMapper().readValue(jsonStr, type);
		} catch (Exception e) {
			log.error("msg=Fail at fromJSON: ", e);
		}
		return result;
	}

	public static <T> JavaType constructListParametricType(Class<T> contentClass) throws ClassNotFoundException {
		return mapper.getTypeFactory().constructParametricType(List.class, Class.forName(contentClass.getName()));
	}

}
