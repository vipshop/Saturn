package com.vip.saturn.job;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作为作业执行的返回, 每个分片对应一个SaturnJobReturn对象
 * <p>
 * @author dylan.xue
 */

public class SaturnJobReturn implements Serializable {

	private static final long serialVersionUID = 940032321608832191L;

	public static final String MSG_CONSUME_STATUS_PROP_KEY = "consumeStatus";

	/**
	 * 支持16个延时等级的投递，默认情况按照重试次数依次使用不同延时来进行消息再投递；用户亦可修改每次重试的延时。 16个延时级别为： 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m
	 * 30m 1h; 用户可修改每次延迟的时间间隔; 其中delayLevel为1对应5s,16对应1h
	 */
	public static final String DELAY_LEVEL_WHEN_RECONSUME_PROP_KEY = "delayLevel";

	/**
	 * Job执行返回值, 默认0。
	 */
	private int returnCode = SaturnSystemReturnCode.SUCCESS;

	/**
	 * Job执行返回字符串信息
	 */
	private String returnMsg;

	/**
	 * 异常组，默认200
	 */
	private int errorGroup = SaturnSystemErrorGroup.SUCCESS;

	/**
	 * 返回的属性，消息服务的作业会将该属性设置到发送的Channel中
	 */
	private Map<String, String> prop;

	/**
	 * returnCode默认0（成功），errorGroup默认200（成功）。
	 * @see SaturnSystemReturnCode
	 * @see SaturnSystemErrorGroup
	 */
	public SaturnJobReturn() {
	}

	/**
	 * returnCode默认0（成功），errorGroup默认200（成功）。
	 *
	 * @param returnMsg 作业执行返回字符串信息
	 * @see SaturnSystemReturnCode
	 * @see SaturnSystemErrorGroup
	 */
	public SaturnJobReturn(String returnMsg) {
		this.returnMsg = returnMsg;
	}

	public SaturnJobReturn(int returnCode, String returnMsg, int errorGroup) {
		this.returnCode = returnCode;
		this.returnMsg = returnMsg;
		this.errorGroup = errorGroup;
	}

	public void copyFrom(Object source) {
		Class<?> clazz = source.getClass();
		try {
			Field field = null;
			Object res = null;

			field = clazz.getDeclaredField("returnCode");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.returnCode = (int) res;
			}

			field = clazz.getDeclaredField("returnMsg");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.returnMsg = (String) res;
			}

			field = clazz.getDeclaredField("errorGroup");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.errorGroup = (int) res;
			}

			field = clazz.getDeclaredField("prop");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.prop = (Map) res;
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}

	public String getReturnMsg() {
		return returnMsg;
	}

	public void setReturnMsg(String returnMsg) {
		this.returnMsg = returnMsg;
	}

	public int getErrorGroup() {
		return errorGroup;
	}

	public void setErrorGroup(int errorGroup) {
		this.errorGroup = errorGroup;
	}

	public Map<String, String> getProp() {
		return prop;
	}

	public void setProp(Map<String, String> prop) {
		this.prop = prop;
	}

	public void reconsumeLater() {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(SaturnJobReturn.MSG_CONSUME_STATUS_PROP_KEY, SaturnConsumeStatus.RECONSUME_LATER.name());
	}

	public void reconsumeLater(int delayLevel) {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(SaturnJobReturn.MSG_CONSUME_STATUS_PROP_KEY, SaturnConsumeStatus.RECONSUME_LATER.name());
		prop.put(SaturnJobReturn.DELAY_LEVEL_WHEN_RECONSUME_PROP_KEY, String.valueOf(delayLevel));
	}

	@Override
	public String toString() {
		return "SaturnJobReturn [returnCode=" + returnCode + ", returnMsg=" + returnMsg + ", errorGroup=" + errorGroup
				+ ", prop=" + prop + "]";
	}

}
