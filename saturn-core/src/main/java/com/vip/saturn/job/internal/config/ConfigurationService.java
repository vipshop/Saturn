/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.vip.saturn.job.internal.config;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.map.type.TypeFactory;
import org.quartz.CronExpression;

import com.google.common.base.Strings;
import com.vip.saturn.job.basic.AbstractSaturnService;
import com.vip.saturn.job.basic.JobScheduler;
import com.vip.saturn.job.exception.SaturnJobException;
import com.vip.saturn.job.exception.ShardingItemParametersException;
import com.vip.saturn.job.utils.JsonUtils;

/**
 * 弹性化分布式作业配置服务.
 * 
 * 
 */
public class ConfigurationService extends AbstractSaturnService{
	
	private static final String DOUBLE_QUOTE = "\"";
	
	//参考http://stackoverflow.com/questions/17963969/java-regex-pattern-split-commna
	private static final String PATTERN = ",(?=(([^\"]*\"){2})*[^\"]*$)";
	
    private MapType customContextType = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, String.class);
    
    public ConfigurationService(JobScheduler jobScheduler) {
        super(jobScheduler);
    }
    
	
    /**
     * 获取作业分片总数.
     * 
     * @return 作业分片总数
     */
    public int getShardingTotalCount() {
        return jobConfiguration.getShardingTotalCount();
    }

	public boolean isLocalMode() {
		return jobConfiguration.isLocalMode();
	}

    /**
     *	获取分片序列号和个性化参数对照表.<br>
	 * 	如果是本地模式的作业，则获取到[-1=xx]
     * 
     * @return 分片序列号和个性化参数对照表
     */
    public Map<Integer, String> getShardingItemParameters() {
		Map<Integer, String> result = new HashMap<>();
		String value = jobConfiguration.getShardingItemParameters();
		if (Strings.isNullOrEmpty(value)) {
			return result;
		}
		//解释命令行参数
		String[] shardingItemParameters = value.split(PATTERN);
		Map<String, String> result0 = new HashMap<>(shardingItemParameters.length);
		for (String each : shardingItemParameters) {
			String item = "";
			String exec = "";

			int index = each.indexOf("=");
			if (index > -1) {
				item = each.substring(0, index).trim();
				exec = each.substring(index + 1, each.length()).trim();
				//去掉前后的双引号"
				if (exec.startsWith(DOUBLE_QUOTE)) {
					exec = exec.substring(1);
				}

				if (exec.endsWith(DOUBLE_QUOTE)) {
					exec = exec.substring(0, exec.length() - 1);
				}
			} else {
				throw new ShardingItemParametersException("Sharding item parameters '%s' format error", value);
			}
			result0.put(item, exec);
		}
		if(isLocalMode()) {
			if(result0.containsKey("*")) {
				result.put(-1, result0.get("*"));
			} else {
				throw new ShardingItemParametersException("Sharding item parameters '%s' format error with local mode job, should be *=xx", value);
			}
		} else {
			Iterator<Map.Entry<String, String>> iterator = result0.entrySet().iterator();
			while(iterator.hasNext()) {
				Map.Entry<String, String> next = iterator.next();
				String item = next.getKey();
				String exec = next.getValue();
				try {
					result.put(Integer.parseInt(item), exec);
				} catch (final NumberFormatException ex) {
					throw new ShardingItemParametersException("Sharding item parameters key '%s' is not an integer.", item);
				}
			}
		}
		return result;
    }
    
    /**
     * 获取作业自定义参数.
     * 
     * @return 作业自定义参数
     */
    public String getJobParameter() {
        return jobConfiguration.getJobParameter();
    }
    
    /**
     * 获取作业启动时间的cron表达式.
     * 
     * @return 作业启动时间的cron表达式
     */
    public String getCron() {
        return jobConfiguration.getCron();
    }
    
    /**
     * 更新作业的cron表达式
     * @param jobName 作业名
     * @param cron cron表达式
     * @param customContext 自定义上下文
     * @throws SaturnJobException 可能抛的异常有：type为0，表示cron表达式无效；type为1，表示作业名在这个namespace下不存在；type为3，表示customContext内容超出1M。
     */
    public void updateJobCron(String jobName, String cron, Map<String, String> customContext) throws SaturnJobException {
    	String cron0 = cron;
    	if(cron0 != null && !cron0.trim().isEmpty()) {
			try {
				cron0 = cron0.trim();
				CronExpression.validateExpression(cron0);
			} catch (ParseException e) {
				throw new SaturnJobException(SaturnJobException.CRON_VALID, "The cron expression is valid: " + cron);
			}
    	} else {
    		cron0 = "";
    	}
    	if(getJobNodeStorage().isJobExisted(jobName)) {
	    	String oldCustomContextStr = getJobNodeStorage().getJobNodeDataDirectly(jobName, ConfigurationNode.CUSTOME_CONTEXT);
	    	Map<String, String> oldCustomContextMap = toCustomContext(oldCustomContextStr);
	    	if(customContext != null && !customContext.isEmpty()) {
	    		oldCustomContextMap.putAll(customContext);
	    		String newCustomContextStr = toCustomContext(oldCustomContextMap);
	    		if(newCustomContextStr.getBytes().length > 1024 * 1024) {
	    			throw new SaturnJobException(SaturnJobException.OUT_OF_ZK_LIMIT_MEMORY, "The all customContext is out of zk limit memory(1M)");
	    		}
	    		getJobNodeStorage().replaceJobNode(jobName, ConfigurationNode.CUSTOME_CONTEXT, newCustomContextStr);
	    	}
	    	String oldCron = getJobNodeStorage().getJobNodeDataDirectly(jobName, ConfigurationNode.CRON);
	    	if(cron0 != null && oldCron != null && !cron0.equals(oldCron.trim())) {
	    		getJobNodeStorage().updateJobNode(jobName, ConfigurationNode.CRON, cron0);
	    	}
    	} else {
    		throw new SaturnJobException(SaturnJobException.JOB_NOT_FOUND, "The job is not found: " + jobName);
    	}
    }
    
    /**
     * 获取统计作业处理数据数量的间隔时间.
     * 
     * @return 统计作业处理数据数量的间隔时间
     */
    public int getProcessCountIntervalSeconds() {
    	return jobConfiguration.getProcessCountIntervalSeconds();
    }
    
    /**
     * 本机当前时间是否在作业暂停时间段范围内。
     * <p>特别的，无论pausePeriodDate，还是pausePeriodTime，如果解析发生异常，则忽略该节点，视为没有配置该日期或时分段。
     * 
     * @return 本机当前时间是否在作业暂停时间段范围内.
     */
    public boolean isInPausePeriod() {
		Date now = new Date(System.currentTimeMillis());
		return isInPausePeriod(now);
    }
    
    /**
     * 该时间是否在作业暂停时间段范围内。
     * <p>特别的，无论pausePeriodDate，还是pausePeriodTime，如果解析发生异常，则忽略该节点，视为没有配置该日期或时分段。
     * 
     * @param date 时间
     * 
     * @return 该时间是否在作业暂停时间段范围内。
     */
	public boolean isInPausePeriod(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int M = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH begin from 0.
		int d = calendar.get(Calendar.DAY_OF_MONTH);
		int h = calendar.get(Calendar.HOUR_OF_DAY);
		int m = calendar.get(Calendar.MINUTE);

		boolean dateIn = false;
		String pausePeriodDate = jobConfiguration.getPausePeriodDate();
		boolean pausePeriodDateIsEmpty = (pausePeriodDate==null || pausePeriodDate.trim().isEmpty());
		if(!pausePeriodDateIsEmpty){
			String[] periodsDate = pausePeriodDate.split(",");
			if (periodsDate != null) {
				for (String period : periodsDate) {
					String[] tmp = period.trim().split("-");
					if (tmp != null && tmp.length == 2) {
						String left = tmp[0].trim();
						String right = tmp[1].trim();
						String[] MdLeft = left.split("/");
						String[] MdRight = right.split("/");
						if (MdLeft != null && MdLeft.length == 2 && MdRight != null && MdRight.length == 2) {
							try {
								int MLeft = Integer.parseInt(MdLeft[0]);
								int dLeft = Integer.parseInt(MdLeft[1]);
								int MRight = Integer.parseInt(MdRight[0]);
								int dRight = Integer.parseInt(MdRight[1]);
								dateIn = (M > MLeft || M == MLeft && d >= dLeft) && (M < MRight || M == MRight && d <= dRight);//NOSONAR
								if (dateIn) {
									break;
								}
							} catch (NumberFormatException e) {
								dateIn = false;
								break;
							}
						} else {
							dateIn = false;
							break;
						}
					} else {
						dateIn = false;
						break;
					}
				}
			}
		}
		boolean timeIn = false;
		String pausePeriodTime = jobConfiguration.getPausePeriodTime();
		boolean pausePeriodTimeIsEmpty = (pausePeriodTime==null||pausePeriodTime.trim().isEmpty());
		if(!pausePeriodTimeIsEmpty){
			String[] periodsTime = pausePeriodTime.split(",");
			if (periodsTime != null) {
				for (String period : periodsTime) {
					String[] tmp = period.trim().split("-");
					if (tmp != null && tmp.length == 2) {
						String left = tmp[0].trim();
						String right = tmp[1].trim();
						String[] hmLeft = left.split(":");
						String[] hmRight = right.split(":");
						if (hmLeft != null && hmLeft.length == 2 && hmRight != null && hmRight.length == 2) {
							try {
								int hLeft = Integer.parseInt(hmLeft[0]);
								int mLeft = Integer.parseInt(hmLeft[1]);
								int hRight = Integer.parseInt(hmRight[0]);
								int mRight = Integer.parseInt(hmRight[1]);
								timeIn = (h > hLeft || h == hLeft && m >= mLeft) && (h < hRight || h == hRight && m <= mRight);//NOSONAR
								if (timeIn) {
									break;
								}
							} catch (NumberFormatException e) {
								timeIn = false;
								break;
							}
						} else {
							timeIn = false;
							break;
						}
					} else {
						timeIn = false;
						break;
					}
				}
			}
		}
		
		
		if(pausePeriodDateIsEmpty) {
			if(pausePeriodTimeIsEmpty) {
				return false;
			} else {
				return timeIn;
			}
		} else {
			if(pausePeriodTimeIsEmpty) {
				return dateIn;
			} else {
				return dateIn && timeIn;
			}
		}
	}
	
    /**
     * 获取是否开启失效转移.
     * 
     * @return 是否开启失效转移
     */
    public boolean isFailover() {
        return jobConfiguration.isFailover();
    }
    
    /**
     * 获取是否开启作业.
     * 
     * @return 作业是否开启
     */
    public boolean isJobEnabled() {
        return jobConfiguration.isEnabled();
    }
    
    
    /**
     * 获取超时时间
     * 
     * @return 超时时间
     */
    public int getTimeoutSeconds(){    	
		return jobConfiguration.getTimeoutSeconds();
    }
    
    /**
     * 获取是否显示正常日志
     * @return 是否显示正常日志
     */
    public boolean showNormalLog() {
    	return jobConfiguration.isShowNormalLog();
    }
    
    /**
     * 获取自定义上下文
     * @return 获取自定义上下文
     */
    public Map<String, String> getCustomContext() {
    	String jobNodeData = getJobNodeStorage().getJobNodeData(ConfigurationNode.CUSTOME_CONTEXT);
    	return toCustomContext(jobNodeData);
    }
    
    /**
     * 将str转为map
     * @param customContextStr str字符串
     * @return 自定义上下文map
     */
    private Map<String, String> toCustomContext(String customContextStr) {
    	Map<String, String> customContext = null;
    	if(customContextStr != null) {
    		customContext = JsonUtils.fromJSON(customContextStr, customContextType);
    	}
    	if(customContext == null) {
    		customContext = new HashMap<>();
    	}
    	return customContext;
    }
    
    /**
     * 将map转为str字符串
     * @param customContextMap 自定义上下文map
     * @return 自定义上下文str
     */
    private String toCustomContext(Map<String, String> customContextMap) {
    	String result = JsonUtils.toJSON(customContextMap);
    	if(result == null) {
    		result = "";
    	}
    	return result.trim();
    }
    
    
    public String getRawJobType(){
   	 return jobConfiguration.getJobType();
   }
    
    /**
     * 作业接收的queue名字
     */
	public String getQueueName() {
		return jobConfiguration.getQueueName();
	}

	
    /**
     * 执行作业发送的channel名字
     */
	public String getChannelName() {
		return jobConfiguration.getChannelName();
	}
	
	
	public List<String> getPreferList(){
		List<String> executorList = new ArrayList<String>();
		
		String prefer = jobConfiguration.getPreferList();
		if(prefer == null || prefer.isEmpty()){
			return executorList;
		}
		String[] executors = prefer.split(",");
		if(executors.length == 0){
			return executorList;
		}
		for(String executor:executors){
			executorList.add(executor);
		}
		
		return executorList;
	}
	
	public boolean isUseDispreferList() {
		return jobConfiguration.isUseDispreferList();
	}
}
