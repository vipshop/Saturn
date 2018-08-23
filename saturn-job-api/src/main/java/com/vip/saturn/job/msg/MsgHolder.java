package com.vip.saturn.job.msg;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class MsgHolder implements Serializable {
	private static final long serialVersionUID = 6371889076371714759L;

	private byte[] payloadBytes;

	/** 消息内容 */
	@Deprecated
	private String payload;

	/** 来自消息服务器的Context信息 */
	private Set<Entry<String, String>> prop;

	/** 消息id */
	private String messageId;

	/** Kafka offset */
	private long offset;

	@Deprecated
	public MsgHolder(String payload, Set<Entry<String, String>> prop, String messageId) {
		this.payload = payload;
		this.prop = prop;
		this.messageId = messageId;
	}

	public MsgHolder(byte[] payloadBytes, Set<Entry<String, String>> prop, String messageId, long offset) {// NOSONAR
		this.payloadBytes = payloadBytes;
		this.prop = prop;
		this.messageId = messageId;
		this.offset = offset;
	}

	public MsgHolder(byte[] payloadBytes, Set<Entry<String, String>> prop, String messageId) {// NOSONAR
		this.payloadBytes = payloadBytes;
		this.prop = prop;
		this.messageId = messageId;
	}

	public MsgHolder() {

	}

	public void copyFrom(Object source) {
		Class<?> clazz = source.getClass();
		try {
			Field field = null;
			Object res = null;

			try {
				field = clazz.getDeclaredField("payloadBytes");
				field.setAccessible(true);
				res = field.get(source);
				if (res != null) {
					this.payloadBytes = (byte[]) res;
				}
			} catch (NoSuchFieldException e) {// NOSONAR
			}

			field = clazz.getDeclaredField("payload");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.payload = (String) res;
			}

			field = clazz.getDeclaredField("prop");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.prop = (Set) res;
			}

			field = clazz.getDeclaredField("messageId");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.messageId = (String) res;
			}

			field = clazz.getDeclaredField("offset");
			field.setAccessible(true);
			res = field.get(source);
			if (res != null) {
				this.offset = (long) res;
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] getPayloadBytes() {
		return payloadBytes;
	}

	/**
	 * 使用当前字符集编码，将原始byte[]类型的payload转成字符串类型。如果有编码要求，建议直接使用{@link #getPayloadBytes()}
	 * @return 返回payload的字符串
	 */
	@Deprecated
	public String getPayload() {
		if (payload == null && payloadBytes != null) {
			payload = new String(payloadBytes);
		}
		return payload;
	}

	public Set<Entry<String, String>> getProp() {
		return prop;
	}

	public String getProp(String key) {
		if (prop != null) {
			Iterator<Entry<String, String>> iterator = prop.iterator();
			while (iterator.hasNext()) {
				Entry<String, String> next = iterator.next();
				if ((key != null && key.equals(next.getKey())) || (key == null && next.getKey() == null)) {
					return next.getValue();
				}
			}
		}
		return null;
	}

	public String getMessageId() {
		return messageId;
	}

	public long getOffset() {
		return offset;
	}
}
