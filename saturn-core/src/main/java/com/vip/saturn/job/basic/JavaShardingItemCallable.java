package com.vip.saturn.job.basic;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.SaturnSystemErrorGroup;
import com.vip.saturn.job.SaturnSystemReturnCode;
import com.vip.saturn.job.java.SaturnJavaJob;
import com.vip.saturn.job.utils.SaturnLogOutputStream;

/**
 * @author xiaopeng.he
 */
public class JavaShardingItemCallable extends ShardingItemCallable{
	
	private static Logger log = LoggerFactory.getLogger(JavaShardingItemCallable.class);
	
	protected Thread currentThread;

	public JavaShardingItemCallable(String jobName, Integer item, String itemValue, int timeoutSeconds,
			SaturnExecutionContext shardingContext, AbstractSaturnJob saturnJob) {
		super(jobName, item, itemValue, timeoutSeconds, shardingContext, saturnJob);
	}
	
	/**
	 * 获取执行作业分片的线程
	 * @return
	 */
	public Thread getCurrentThread() {
		return currentThread;
	}

	/**
	 * 设置执行作业分片的线程
	 * @param currentThread
	 */
	public void setCurrentThread(Thread currentThread) {
		this.currentThread = currentThread;
	}
	
	protected AtomicInteger status = new AtomicInteger(0);
	protected static final int INIT = 0;
	protected static final int TIMEOUT = 1;
	protected static final int SUCCESS = 2;
	protected static final int FORCE_STOP = 3;
	protected static final int STOPED = 4;
	
	protected volatile boolean breakForceStop = false;
	
	protected Object contextForJob;

	/**
	 * 复制对象
	 * @param source
	 * @param classLoader
	 * @return
	 * @throws Exception
	 */
	public static Object cloneObject(Object source, ClassLoader classLoader) throws Exception{
		if(source == null){
			return null;
		}
		Class<?> clazz = classLoader.loadClass(source.getClass().getCanonicalName());
		Object target = clazz.newInstance();
		clazz.getMethod("copyFrom", Object.class).invoke(target, source);
		return target;
	}
	
	/**
	 * 生成分片上下文对象
	 * @param jobClassLoader
	 * @return
	 * @throws Exception
	 */
	public Object getContextForJob(ClassLoader jobClassLoader) throws Exception{
		if(contextForJob == null){
			if(shardingContext == null){
				return null;
			}
			SaturnJobExecutionContext context = new SaturnJobExecutionContext();
			context.setJobName(shardingContext.getJobName());
			context.setShardingItemParameters(shardingContext.getShardingItemParameters());
			context.setCustomContext(shardingContext.getCustomContext());
			context.setJobParameter(shardingContext.getJobParameter());
			context.setShardingItems(shardingContext.getShardingItems());
			context.setShardingTotalCount(shardingContext.getShardingTotalCount());
			contextForJob = cloneObject(context, jobClassLoader);
		}
		
		return contextForJob;
	}
	
	/**
	 * 设置该分片的状态为TIMEOUT
	 * @return Mark timeout success or fail
	 */
	public boolean setTimeout() {
		return status.compareAndSet(INIT, TIMEOUT);
	}

	/**
	 * 该分片执行是否TIMEOUT
	 * @return
	 */
	public boolean isTimeout() {
		return status.get() == TIMEOUT;
	}
	
	/**
	 *设置该分片的状态为FORCE_STOP
	 * @return
	 */
	public boolean forceStop() {
		return status.compareAndSet(INIT, FORCE_STOP);
	}
	
	/**
	 * 作业执行是否被中止
	 * @return
	 */
	public boolean isBreakForceStop() {
		return breakForceStop;
	}
	
	/**
	 * 该分片是否FORCE_STOP状态
	 * @return
	 */
	public boolean isForceStop() {
		return status.get() == FORCE_STOP;
	}
	
	/**
	 * 重新初始化
	 */
	public void reset(){
		status.set(INIT);
		breakForceStop = false;
		saturnJobReturn = null;
		businessReturned = false;
	}
	
	/**
	 * 执行前回调
	 */
	public void beforeExecution(){
	}
	
	/**
	 * 执行后回调
	 */
	public void afterExecution(){
	}
	
	/**
	 * 真正执行作业分片逻辑
	 * @return 执行结果
	 */
	public SaturnJobReturn call(){
		reset();
		
		SaturnLogOutputStream.initLogger();
		currentThread = Thread.currentThread();
		SaturnJobReturn temp = null;
		try {
			
			beforeExecution();
			
			temp = saturnJob.doExecution(jobName, item, itemValue, shardingContext,this);
			
			// 在此之后，不能再强制停止本线程
			breakForceStop = true;
		} catch (Throwable t) {
			// 在此之后，不能再强制停止本线程
			breakForceStop = true;
			
			// 不是超时，不是强制停止。 打印错误日志，设置SaturnJobReturn。
			if(status.get() != TIMEOUT && status.get() != FORCE_STOP) {
				log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, t.getMessage()), t);
				temp = new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL, t.getMessage(), SaturnSystemErrorGroup.FAIL);
			}

		} finally {
			if(status.compareAndSet(INIT, SUCCESS)) {
				saturnJobReturn = temp;
			}
			
			String jobLog = SaturnLogOutputStream.clearAndGetLog();

			if(saturnJob !=null && saturnJob.getConfigService().showNormalLog()){
				this.shardingContext.putJobLog(this.item, jobLog);
			}
		}
		
		return saturnJobReturn;
	}
	
	protected void checkAndSetSaturnJobReturn() {
		switch(status.get()) {
			case TIMEOUT:
				saturnJobReturn = new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL, "execute job timeout(" + timeoutSeconds * 1000 + "ms)", SaturnSystemErrorGroup.TIMEOUT);
				break;
			case FORCE_STOP:
				saturnJobReturn = new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL, "the job was forced to stop", SaturnSystemErrorGroup.FAIL);
				break;
			case STOPED:
				saturnJobReturn = new SaturnJobReturn(SaturnSystemReturnCode.SYSTEM_FAIL, "the job was stopped, will not run the business code", SaturnSystemErrorGroup.FAIL);
				break;
			default:
				break;
		}
		if(saturnJobReturn == null) {
			saturnJobReturn = new SaturnJobReturn(SaturnSystemReturnCode.USER_FAIL, "the SaturnJobReturn can not be null", SaturnSystemErrorGroup.FAIL);
		}
	}

	public void beforeTimeout(){
		try {
			((SaturnJavaJob) saturnJob).beforeTimeout(jobName, item, itemValue, shardingContext,this);
		} catch (Throwable t) {
			log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, t.getMessage()), t);
		}
	}
	
	protected void onTimeout() {
		try {
			((SaturnJavaJob) saturnJob).postTimeout(jobName, item, itemValue, shardingContext,this);
		} catch (Throwable t) {
			log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, t.getMessage()), t);
		}
	}
	
	protected void postForceStop() {
		try {
			((SaturnJavaJob) saturnJob).postForceStop(jobName, item, itemValue, shardingContext,this);
		} catch (Throwable t) {
			log.error(String.format(SaturnConstant.ERROR_LOG_FORMAT, jobName, t.getMessage()), t);
		}
	}
	
}
