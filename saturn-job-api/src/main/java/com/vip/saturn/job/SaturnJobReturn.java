/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * </p>
 **/

package com.vip.saturn.job;

import com.vip.saturn.job.msg.MsgHolder;
import com.vip.saturn.job.msg.SaturnDelayedLevel;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作为作业执行的返回, 每个分片对应一个SaturnJobReturn对象
 * <p>
 * @author dylan.xue
 */

public class SaturnJobReturn implements Serializable {

	private static final long serialVersionUID = 940032321608832191L;

	public static final String MSG_CONSUME_STATUS_PROP_KEY = "consumeStatus";

	public static final String MSG_BATCH_CONSUME_SUCCESS_OFFSETS = "successOffsets";

	public static final String MSG_BATCH_CONSUME_DISCARD_OFFSETS = "discardOffsets";

	public static final String MSG_BATCH_CONSUME_DELAY_OFFSETS = "delayOffsets";

	public static final String MSG_BATCH_CONSUME_DEFAULT_STATUS = "defaultConsumeStatus";

	public static final String MSG_ALL = "MSG_ALL";

	public static final String OFFSET_SEPERATOR = ",";

	/**
	 * please refer to SaturnDelayedLevel
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

	public static SaturnJobReturnBuilder builder() {
		return new SaturnJobReturnBuilder();
	}

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

	/**
	 * only use for single consume
	 */
	public void reconsumeLater() {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(SaturnJobReturn.MSG_CONSUME_STATUS_PROP_KEY, SaturnConsumeStatus.RECONSUME_LATER.name());
	}

	/**
	 * only use for single consume
	 */
	@Deprecated
	public void reconsumeLater(int delayLevel) {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(SaturnJobReturn.MSG_CONSUME_STATUS_PROP_KEY, SaturnConsumeStatus.RECONSUME_LATER.name());
		prop.put(SaturnJobReturn.DELAY_LEVEL_WHEN_RECONSUME_PROP_KEY, String.valueOf(delayLevel));
	}

	/**
	 * only use for single consume
	 */
	public void reconsumeLater(SaturnDelayedLevel delayLevel) {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(SaturnJobReturn.MSG_CONSUME_STATUS_PROP_KEY, SaturnConsumeStatus.RECONSUME_LATER.name());
		prop.put(SaturnJobReturn.DELAY_LEVEL_WHEN_RECONSUME_PROP_KEY, String.valueOf(delayLevel.getValue()));
	}

	/**
	 * only use for single consume
	 */
	public void complete() {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(SaturnJobReturn.MSG_CONSUME_STATUS_PROP_KEY, SaturnConsumeStatus.CONSUME_SUCCESS.name());
	}

	/**
	 * only use for single consume
	 */
	public void discard() {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(SaturnJobReturn.MSG_CONSUME_STATUS_PROP_KEY, SaturnConsumeStatus.CONSUME_DISCARD.name());
	}

	/**
	 * only use for batch consume
	 */
	public void completeAll() {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(MSG_BATCH_CONSUME_SUCCESS_OFFSETS, MSG_ALL);
	}

	/**
	 * only use for batch consume
	 */
	public boolean isCompleteAll() {
		if (prop == null) {
			return false;
		}
		return MSG_ALL.equals(prop.get(MSG_BATCH_CONSUME_SUCCESS_OFFSETS));
	}

	/**
	 * only use for batch consume
	 */
	public void completeSome(List<MsgHolder> msgHolders) {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(MSG_BATCH_CONSUME_SUCCESS_OFFSETS, collectOffsetsToString(msgHolders));
	}

	/**
	 * only use for batch consume
	 */
	public List<String> getCompleteOffsets() {
		if (prop == null) {
			return Collections.emptyList();
		}
		String offsetsStr = prop.get(MSG_BATCH_CONSUME_SUCCESS_OFFSETS);
		return parseOffsetsStr(offsetsStr);
	}

	/**
	 * only use for batch consume
	 */
	public void reconsumeSome(List<MsgHolder> msgHolders) {
		reconsumeSome(msgHolders, null);
	}

	/**
	 * only use for batch consume
	 */
	public void reconsumeSome(List<MsgHolder> msgHolders, SaturnDelayedLevel delayLevel) {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(MSG_BATCH_CONSUME_DELAY_OFFSETS, collectOffsetsToString(msgHolders));
		if (delayLevel != null) {
			prop.put(SaturnJobReturn.DELAY_LEVEL_WHEN_RECONSUME_PROP_KEY, String.valueOf(delayLevel.getValue()));
		}
	}

	/**
	 * only use for batch consume
	 */
	public List<String> getReconsumeOffsets() {
		if (prop == null) {
			return Collections.emptyList();
		}
		String offsetsStr = prop.get(MSG_BATCH_CONSUME_DELAY_OFFSETS);
		return parseOffsetsStr(offsetsStr);
	}

	/**
	 * only use for batch consume
	 */
	public void reconsumeAllLater() {
		reconsumeAllLater(null);
	}

	/**
	 * only use for batch consume
	 */
	public void reconsumeAllLater(SaturnDelayedLevel delayLevel) {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(MSG_BATCH_CONSUME_DELAY_OFFSETS, MSG_ALL);
		if (delayLevel != null) {
			prop.put(SaturnJobReturn.DELAY_LEVEL_WHEN_RECONSUME_PROP_KEY, String.valueOf(delayLevel.getValue()));
		}
	}

	/**
	 * only use for batch consume
	 */
	public boolean isReconsumeAll() {
		if (prop == null) {
			return false;
		}
		return MSG_ALL.equals(prop.get(MSG_BATCH_CONSUME_DELAY_OFFSETS));
	}

	public String getDelayLevel() {
		if (prop == null) {
			return null;
		}
		return prop.get(SaturnJobReturn.DELAY_LEVEL_WHEN_RECONSUME_PROP_KEY);
	}

	/**
	 * only use for batch consume
	 */
	public void discardSome(List<MsgHolder> msgHolders) {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(MSG_BATCH_CONSUME_DISCARD_OFFSETS, collectOffsetsToString(msgHolders));
	}

	/**
	 * only use for batch consume
	 */
	public List<String> getDiscardOffsets() {
		if (prop == null) {
			return Collections.emptyList();
		}
		String offsetsStr = prop.get(MSG_BATCH_CONSUME_DISCARD_OFFSETS);
		return parseOffsetsStr(offsetsStr);
	}

	/**
	 * only use for batch consume
	 */
	public void setBatchConsumeDefaultStatus(SaturnConsumeStatus consumeStatus) {
		if (prop == null) {
			prop = new ConcurrentHashMap<>();
		}
		prop.put(MSG_BATCH_CONSUME_DEFAULT_STATUS, consumeStatus.name());
	}

	/**
	 * only use for batch consume
	 */
	public String getBatchConsumeDefaultStatus() {
		if (prop == null) {
			return null;
		}
		return prop.get(MSG_BATCH_CONSUME_DEFAULT_STATUS);
	}

	@Override
	public String toString() {
		return "SaturnJobReturn [returnCode=" + returnCode + ", returnMsg=" + returnMsg + ", errorGroup=" + errorGroup
				+ ", prop=" + prop + "]";
	}

	private String collectOffsetsToString(List<MsgHolder> msgHolders) {
		if (msgHolders == null && msgHolders.size() == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < msgHolders.size(); i++) {
			if (i > 0) {
				sb.append(OFFSET_SEPERATOR);
			}
			sb.append(String.valueOf(msgHolders.get(i).getOffset()));
		}
		return sb.toString();
	}

	private List<String> parseOffsetsStr(String offsetsStr) {
		if (offsetsStr == null || offsetsStr.isEmpty()) {
			return Collections.emptyList();
		}
		String[] splits = offsetsStr.split(OFFSET_SEPERATOR);
		return Arrays.asList(splits);
	}


	public static class SaturnJobReturnBuilder {

		private SaturnJobReturn saturnJobReturn;

		private SaturnJobReturnBuilder() {
			this.saturnJobReturn = new SaturnJobReturn();
		}

		public SaturnJobReturn build() {
			return saturnJobReturn;
		}

		public SaturnJobReturnBuilder returnCode(int returnCode) {
			saturnJobReturn.returnCode = returnCode;
			return this;
		}

		public SaturnJobReturnBuilder returnMsg(String returnMsg) {
			saturnJobReturn.returnMsg = returnMsg;
			return this;
		}

		public SaturnJobReturnBuilder errorGroup(int errorGroup) {
			saturnJobReturn.errorGroup = errorGroup;
			return this;
		}

		/**
		 * only use for single consume
		 */
		public SaturnJobReturnBuilder reconsumeLater() {
			saturnJobReturn.reconsumeLater();
			return this;
		}

		/**
		 * only use for single consume
		 */
		public SaturnJobReturnBuilder reconsumeLater(SaturnDelayedLevel delayLevel) {
			saturnJobReturn.reconsumeLater(delayLevel);
			return this;
		}

		/**
		 * only use for single consume
		 */
		public SaturnJobReturnBuilder complete() {
			saturnJobReturn.complete();
			return this;
		}

		/**
		 * only use for single consume
		 */
		public SaturnJobReturnBuilder discard() {
			saturnJobReturn.discard();
			return this;
		}

		/**
		 * only use for batch consume
		 */
		public SaturnJobReturnBuilder completeAll() {
			saturnJobReturn.completeAll();
			return this;
		}

		/**
		 * only use for batch consume
		 */
		public SaturnJobReturnBuilder completeSome(List<MsgHolder> msgHolders) {
			saturnJobReturn.completeSome(msgHolders);
			return this;
		}

		/**
		 * only use for batch consume
		 */
		public SaturnJobReturnBuilder reconsumeSome(List<MsgHolder> msgHolders) {
			saturnJobReturn.reconsumeSome(msgHolders);
			return this;
		}

		/**
		 * only use for batch consume
		 */
		public SaturnJobReturnBuilder reconsumeSome(List<MsgHolder> msgHolders, SaturnDelayedLevel delayLevel) {
			saturnJobReturn.reconsumeSome(msgHolders, delayLevel);
			return this;
		}

		/**
		 * only use for batch consume
		 */
		public SaturnJobReturnBuilder reconsumeAll() {
			saturnJobReturn.reconsumeAllLater();
			return this;
		}

		/**
		 * only use for batch consume
		 */
		public SaturnJobReturnBuilder reconsumeAll(SaturnDelayedLevel delayLevel) {
			saturnJobReturn.reconsumeAllLater(delayLevel);
			return this;
		}

		/**
		 * only use for batch consume
		 */
		public SaturnJobReturnBuilder discardSome(List<MsgHolder> msgHolders) {
			saturnJobReturn.discardSome(msgHolders);
			return this;
		}

		/**
		 * only use for batch consume
		 */
		public SaturnJobReturnBuilder batchConsumeDefaultStatus(SaturnConsumeStatus consumeStatus) {
			saturnJobReturn.setBatchConsumeDefaultStatus(consumeStatus);
			return this;
		}

	}

}
