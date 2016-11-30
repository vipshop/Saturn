package com.vip.saturn.job.sharding.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorTransactionFinal;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vip.saturn.job.sharding.entity.Executor;
import com.vip.saturn.job.sharding.entity.Shard;
import com.vip.saturn.job.sharding.node.SaturnExecutorsNode;

/**
 *
 * @author xiaopeng.he
 */
public class NamespaceShardingService {
	static Logger log = LoggerFactory.getLogger(NamespaceShardingService.class);

	private static final int LOAD_LEVEL_DEFAULT = 1;

	private CuratorFramework curatorFramework;

	private AtomicInteger shardingCount;

	private AtomicBoolean needAllSharding;

	private ExecutorService executorService;

	private String namespace;

	private String hostValue;

	private NamespaceShardingContentService namespaceShardingContentService;

    public NamespaceShardingService(CuratorFramework curatorFramework, String hostValue) {
    	this.curatorFramework = curatorFramework;
		this.hostValue = hostValue;
    	this.shardingCount = new AtomicInteger(0);
    	this.needAllSharding = new AtomicBoolean(false);
    	this.executorService = newSingleThreadExecutor();
		this.namespace = curatorFramework.getNamespace();
		this.namespaceShardingContentService = new NamespaceShardingContentService(curatorFramework);
    }

    private ExecutorService newSingleThreadExecutor() {
    	return Executors.newSingleThreadExecutor(new ThreadFactory() {
    		@Override
    		public Thread newThread(Runnable r) {
    			return new Thread(r, namespace + "-" + r.getClass().getSimpleName());
    		}
		});
    }

    private abstract class AbstractAsyncShardingTask implements Runnable {

		@Override
		public void run() {
			boolean isAllShardingTask = this instanceof ExecuteAllShardingTask;
			try {
				// 如果当前变为非leader，则直接返回
				if(!isLeadership()) {
					return;
				}

				// 如果需要全量分片，且当前线程不是全量分片线程，则直接返回，没必要做分片
				if(needAllSharding.get() && !isAllShardingTask) {
					log.info("the {} will be ignored, because there will be {}", this.getClass().getSimpleName(), ExecuteAllShardingTask.class.getSimpleName());
					return;
				}

				List<String> allJobs = getAllJobs();
				List<String> allEnableJobs = getAllEnableJobs(allJobs);
				List<Executor> lastOnlineExecutorList = getLastOnlineExecutorList();
				List<Shard> shardList = new ArrayList<>();
				// 摘取
				if(pick(allEnableJobs, shardList, lastOnlineExecutorList)) {
					// 放回
					putBackBalancing(allEnableJobs, shardList, lastOnlineExecutorList);
					// 如果当前变为非leader，则返回
					if (!isLeadership()) {
						return;
					}
					// 持久化分片结果
					namespaceShardingContentService.persistDirectly(lastOnlineExecutorList);
					// fix, notify all enable jobs whatever.
					notifyJobShardingNecessary(allEnableJobs);
					// sharding count ++
					increaseShardingCount();
				}
			} catch (Throwable t) {
				log.error(t.getMessage(), t);
				if(!isAllShardingTask) { // 如果当前不是全量分片，则需要全量分片来拯救异常； 如果当前是全量分片，不再全量分片
					needAllSharding.set(true);
					shardingCount.incrementAndGet();
					executorService.submit(new ExecuteAllShardingTask());
				}
			} finally {
				if(isAllShardingTask) { // 如果是全量分片，不再进行全量分片
					needAllSharding.set(false);
				}
				shardingCount.decrementAndGet();
			}
		}

		private void increaseShardingCount() throws Exception {
			Integer _shardingCount = 1;
			if (null !=  curatorFramework.checkExists().forPath(SaturnExecutorsNode.SHARDING_COUNT_PATH)) {
				byte[] shardingCountData = curatorFramework.getData().forPath(SaturnExecutorsNode.SHARDING_COUNT_PATH);
				if(shardingCountData != null) {
					try {
						_shardingCount = Integer.parseInt(new String(shardingCountData, "UTF-8")) + 1;
					} catch (NumberFormatException e) {
						log.error(e.getMessage(), e);
					}
				}
				curatorFramework.setData().forPath(SaturnExecutorsNode.SHARDING_COUNT_PATH, _shardingCount.toString().getBytes());
			} else {
				curatorFramework.create().forPath(SaturnExecutorsNode.SHARDING_COUNT_PATH, _shardingCount.toString().getBytes());
			}
		}

    	private void notifyJobShardingNecessary(List<String> allEnableJobs) throws Exception {
    		CuratorTransactionFinal curatorTransactionFinal = curatorFramework.inTransaction().check().forPath("/").and();
    		for(int i=0; i<allEnableJobs.size(); i++) {
    			String jobName =allEnableJobs.get(i);
    			if(curatorFramework.checkExists().forPath(SaturnExecutorsNode.getJobLeaderShardingNodePath(jobName)) == null) {
    				curatorFramework.create().creatingParentsIfNeeded().forPath(SaturnExecutorsNode.getJobLeaderShardingNodePath(jobName));
    			}
				byte[] necessaryData = generateShardingNecessaryData();
				if(curatorFramework.checkExists().forPath(SaturnExecutorsNode.getJobLeaderShardingNecessaryNodePath(jobName)) == null) {
					curatorTransactionFinal.create().forPath(SaturnExecutorsNode.getJobLeaderShardingNecessaryNodePath(jobName), necessaryData).and();
				} else {
					curatorTransactionFinal.setData().forPath(SaturnExecutorsNode.getJobLeaderShardingNecessaryNodePath(jobName), necessaryData).and();
				}
			}
    		curatorTransactionFinal.commit();
    	}

		private byte[] generateShardingNecessaryData() throws UnsupportedEncodingException {
			return (hostValue + "-" + System.currentTimeMillis()).getBytes("UTF-8");
		}

		/**
		 * 从zk作业配置信息获取该作业的Shards。<br>
		 * 特别的是，如果是本地模式作业（useDispreferList对本地模式无效，一定是false）：
		 * 	1、如果配置了preferList，并且preferList有在线的executor，则选取preferList中在线的executor的size作为shardingTotalCount；
		 * 	3、如果配置了preferList，并且preferList全部下线，则返回空的Shard集合；
		 * 	4、如果没有配置preferList，则选取preferList中在线的executor的size作为shardingTotalCount
		 */
		protected List<Shard> getShardsByJobInfo(String jobName, List<Executor> executorListOnline) throws Exception {
			if(isLocalMode(jobName)) {
				boolean preferListIsConfigured = preferListIsConfigured(jobName);
				List<String> preferListConfigured = getPreferListConfigured(jobName);
				List<String> preferListOnline = getPreferListOnline(preferListConfigured, executorListOnline);
				if(preferListIsConfigured) {
					if(!preferListOnline.isEmpty()) {
						return createShards(jobName, preferListOnline.size());
					} else {
						return new ArrayList<>();
					}
				} else {
					return createShards(jobName, executorListOnline.size());
				}
			} else {
				return createShards(jobName, getShardingTotalCount(jobName));
			}
		}

		protected boolean isLocalMode(String jobName) throws Exception {
			String localNodePath = SaturnExecutorsNode.getJobConfigLocalModeNodePath(jobName);
			if(curatorFramework.checkExists().forPath(localNodePath) != null) {
				byte[] data = curatorFramework.getData().forPath(localNodePath);
				if(data != null) {
					return Boolean.valueOf(new String(data, "UTF-8"));
				}
			}
			return false;
		}

		protected int getShardingTotalCount(String jobName) throws Exception {
			int shardingTotalCount = 0;
			String jobConfigShardingTotalCountNodePath = SaturnExecutorsNode.getJobConfigShardingTotalCountNodePath(jobName);
			if(curatorFramework.checkExists().forPath(jobConfigShardingTotalCountNodePath) != null) {
				byte[] shardingTotalCountData = curatorFramework.getData().forPath(jobConfigShardingTotalCountNodePath);
				if (shardingTotalCountData != null) {
					try {
						shardingTotalCount = Integer.parseInt(new String(shardingTotalCountData, "UTF-8"));
					} catch (NumberFormatException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
			return shardingTotalCount;
		}

		protected int getLoadLevel(String jobName) throws Exception {
			int loadLevel = LOAD_LEVEL_DEFAULT;
			String jobConfigLoadLevelNodePath = SaturnExecutorsNode.getJobConfigLoadLevelNodePath(jobName);
			if(curatorFramework.checkExists().forPath(jobConfigLoadLevelNodePath) != null) {
				byte[] loadLevelData = curatorFramework.getData().forPath(jobConfigLoadLevelNodePath);
				if (loadLevelData != null) {
					try {
						loadLevel = Integer.parseInt(new String(loadLevelData, "UTF-8"));
					} catch (NumberFormatException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
			return loadLevel;
		}

		private  List<Shard> createShards(String jobName, int shardingTotalCount) throws Exception {
			List<Shard> shardList = new ArrayList<>();
			int loadLevel = getLoadLevel(jobName);
			for(int item = 0; item<shardingTotalCount; item++) {
				try {
					Shard shard = new Shard();
					shard.setJobName(jobName);
					shard.setItem(item);
					shard.setLoadLevel(loadLevel);
					shardList.add(shard);
				} catch (NumberFormatException e) {
					log.error(e.getMessage(), e);
				}
			}
			return shardList;
		}

		/**
		 * 获取Executor集合，默认从sharding/content获取
		 */
		protected List<Executor> getLastOnlineExecutorList() throws Exception {
			return namespaceShardingContentService.getExecutorList();
		}

    	/**
    	 * 摘取
    	 * @param allJob 该域下所有作业
    	 * @param shardList 默认为空集合
    	 * @param lastOnlineExecutorList 默认为当前存储的数据，如果不想使用存储数据，请重写{@link #getLastOnlineExecutorList()}}方法
    	 * @return true摘取成功；false摘取失败，不需要继续下面的逻辑
    	 */
    	protected abstract boolean pick(List<String> allJob, List<Shard> shardList, List<Executor> lastOnlineExecutorList) throws Exception;

    	/**
    	 * 按照loadLevel降序排序，如果loadLevel相同，按照作业名降序排序
    	 */
    	protected void sortShardList(List<Shard> shardList) {
    		Collections.sort(shardList, new Comparator<Shard>() {
				@Override
				public int compare(Shard o1, Shard o2) {
					int loadLevelSub = o2.getLoadLevel() - o1.getLoadLevel();
					return loadLevelSub == 0 ? o2.getJobName().compareTo(o1.getJobName()) : loadLevelSub;
				}
			});
    	}

		private List<Executor> getNotDockerExecutors(List<Executor> lastOnlineExecutorList) throws Exception {
			List<Executor> notDockerExecutors = new ArrayList<>();
			for(int i=0; i<lastOnlineExecutorList.size(); i++) {
				Executor executor = lastOnlineExecutorList.get(i);
				String executorName = executor.getExecutorName();
				if(curatorFramework.checkExists().forPath(SaturnExecutorsNode.getExecutorTaskNodePath(executorName)) == null) {
					notDockerExecutors.add(executor);
				}
			}
			return notDockerExecutors;
		}

    	protected void putBackBalancing(List<String> allEnableJobs, List<Shard> shardList, List<Executor> lastOnlineExecutorList) throws Exception {
			if(lastOnlineExecutorList.isEmpty()) {
				log.warn("Unnecessary to put shards back to executors balanced because of no executor");
				return;
			}

        	sortShardList(shardList);

			// 获取非容器executor
			List<Executor> notDockerExecutors = getNotDockerExecutors(lastOnlineExecutorList);

			// 整体算法放回算法：拿取Shard，放进负荷最小的executor

			// 1、放回localMode的Shard
			// 如果配置了preferList，则选取preferList中的executor。 如果preferList中的executor都挂了，则不转移；否则，选取没有接管该作业的executor列表的loadLevel最小的一个。
			// 如果没有配置preferList，则选取没有接管该作业的executor列表的loadLevel最小的一个。
			Iterator<Shard> shardIterator = shardList.iterator();
			while(shardIterator.hasNext()) {
				Shard shard = shardIterator.next();
				if(isLocalMode(shard.getJobName())) {
					if(preferListIsConfigured(shard.getJobName())) {
						List<String> preferListConfigured = getPreferListConfigured(shard.getJobName());
						if (!preferListConfigured.isEmpty()) {
							List<Executor> preferExecutorList = new ArrayList<>();
							for (int i = 0; i < lastOnlineExecutorList.size(); i++) {
								Executor executor = lastOnlineExecutorList.get(i);
								if (preferListConfigured.contains(executor.getExecutorName())) {
									preferExecutorList.add(executor);
								}
							}
							if (!preferExecutorList.isEmpty()) {
								Executor executor = getExecutorWithMinLoadLevelAndNoThisJob(preferExecutorList, shard.getJobName());
								putShardIntoExecutor(shard, executor);
							}
						}
					} else {
						Executor executor = getExecutorWithMinLoadLevelAndNoThisJob(notDockerExecutors, shard.getJobName());
						putShardIntoExecutor(shard, executor);
					}
					shardIterator.remove();
				}
			}

			// 2、放回配置了preferList的Shard
			Iterator<Shard> shardIterator2 = shardList.iterator();
			List<Shard> noTransferSharding = new ArrayList<>();// 全部preferList都offline的情况且勾选了“只使用优先Executor”，记录不需要迁移的Shard
			while(shardIterator2.hasNext()) {
				Shard shard = shardIterator2.next();
				if(preferListIsConfigured(shard.getJobName())) { // fix, preferList为空不能作为判断是否配置preferList的依据，比如说配置了容器资源，但是全部下线了。
					List<String> preferList = getPreferListConfigured(shard.getJobName());
					List<Executor> preferExecutorList = new ArrayList<>();
					for(int i=0; i<lastOnlineExecutorList.size(); i++) {
						Executor executor = lastOnlineExecutorList.get(i);
						if (preferList.contains(executor.getExecutorName())) {
							preferExecutorList.add(executor);
						}
					}
					// 如果preferList的Executor都offline，则放回到全部online的Executor中某一个。如果是这种情况，则后续再操作，避免不均衡的情况
					// 如果存在preferExecutor，择优放回
					if(!preferExecutorList.isEmpty()) {
						Executor executor = getExecutorWithMinLoadLevel(preferExecutorList);
						putShardIntoExecutor(shard, executor);
						shardIterator2.remove();
					} else{ // 如果不存在preferExecutor
						// 如果“只使用preferExecutor”，则标记，添加到noTransferSharding；否则，等到后续（在第3步）进行放回操作，避免不均衡的情况
						if(!useDispreferList(shard.getJobName())) {
							noTransferSharding.add(shard);
						}
					}
				}
			}

			// 3、放回非preferList的Shard
			Iterator<Shard> shardIterator3 = shardList.iterator();
			while(shardIterator3.hasNext()) {
				Shard shard = shardIterator3.next();
				if(noTransferSharding.contains(shard)){ // 如果是“只使用preferExecutor”，则不需要迁移，直接丢弃
					continue;
				}
				Executor executor = getExecutorWithMinLoadLevel(notDockerExecutors);
				putShardIntoExecutor(shard, executor);
				shardIterator3.remove();
			}
        }

    	/**
    	 * 是否使用非preferList<br>
    	 * 1、存在结点，并且该结点值为false，返回false；<br>
    	 * 2、其他情况，返回true
    	 */
    	protected boolean useDispreferList(String jobName) throws Exception {
    		String jobConfigUseDispreferListNodePath = SaturnExecutorsNode.getJobConfigUseDispreferListNodePath(jobName);
    		if(curatorFramework.checkExists().forPath(jobConfigUseDispreferListNodePath) != null) {
				byte[] useDispreferListData = curatorFramework.getData().forPath(jobConfigUseDispreferListNodePath);
				if(useDispreferListData != null && !Boolean.valueOf(new String(useDispreferListData, "UTF-8"))) {
					return false;
				}
			}
    		return true;
    	}

		private Executor getExecutorWithMinLoadLevel(List<Executor> executorList) {
			Executor minLoadLevelExecutor = null;
			for(int i=0; i<executorList.size(); i++) {
				Executor executor = executorList.get(i);
				if(minLoadLevelExecutor == null || minLoadLevelExecutor.getTotalLoadLevel() > executor.getTotalLoadLevel()) {
					minLoadLevelExecutor = executor;
				}
			}
			return minLoadLevelExecutor;
		}

		private Executor getExecutorWithMinLoadLevelAndNoThisJob(List<Executor> executorList, String jobName) {
			Executor minLoadLevelExecutor = null;
			for(int i=0; i<executorList.size(); i++) {
				Executor executor = executorList.get(i);
				List<Shard> shardList = executor.getShardList();
				boolean containThisJob = false;
				for(int j=0; j<shardList.size(); j++) {
					Shard shard = shardList.get(j);
					if(shard.getJobName().equals(jobName)) {
						containThisJob = true;
						break;
					}
				}
				if(!containThisJob && (minLoadLevelExecutor == null || minLoadLevelExecutor.getTotalLoadLevel() > executor.getTotalLoadLevel())) {
					minLoadLevelExecutor = executor;
				}
			}
			return minLoadLevelExecutor;
		}

		private void putShardIntoExecutor(Shard shard, Executor executor) {
			if(executor != null) {
				if(isIn(shard, executor.getShardList())) {
					log.error("The shard({}-{}) is running in the executor of {}, cannot be put again", shard.getJobName(), shard.getItem(), executor.getExecutorName());
				} else {
					executor.getShardList().add(shard);
					executor.setTotalLoadLevel(executor.getTotalLoadLevel() + shard.getLoadLevel());
				}
			} else {
				log.warn("No executor to take over the shard: {}-{}", shard.getJobName(), shard.getItem());
			}
		}

    	/**
    	 * 获取该域下的所有作业
    	 */
    	private List<String> getAllJobs() throws Exception {
    		List<String> allJob = new ArrayList<>();
    		if(curatorFramework.checkExists().forPath(SaturnExecutorsNode.$JOBSNODE_PATH) == null) {
    			curatorFramework.create().creatingParentsIfNeeded().forPath(SaturnExecutorsNode.$JOBSNODE_PATH);
    		}
    		List<String> tmp = curatorFramework.getChildren().forPath(SaturnExecutorsNode.$JOBSNODE_PATH);
    		if(tmp != null) {
    			allJob.addAll(tmp);
    		}
			return allJob;
    	}

    	/**
    	 * 获取该域下的所有enable的作业
    	 */
    	protected List<String> getAllEnableJobs(List<String> allJob) throws Exception {
    		List<String> allEnableJob = new ArrayList<>();
			for(int i=0; i<allJob.size(); i++) {
				String job = allJob.get(i);
				if(curatorFramework.checkExists().forPath(SaturnExecutorsNode.getJobConfigEnableNodePath(job)) != null) {
					byte[] enableData = curatorFramework.getData().forPath(SaturnExecutorsNode.getJobConfigEnableNodePath(job));
					if(enableData != null && Boolean.valueOf(new String(enableData, "UTF-8"))) {
						allEnableJob.add(job);
					}
				}
			}
			return allEnableJob;
    	}

    	private boolean isIn(Shard shard, List<Shard> shardList) {
    		for(int i=0; i<shardList.size(); i++) {
    			Shard tmp = shardList.get(i);
				if (tmp.getJobName().equals(shard.getJobName()) && tmp.getItem() == shard.getItem()) {
    				return true;
    			}
    		}
    		return false;
    	}

		protected boolean preferListIsConfigured(String jobName) throws Exception {
			if(curatorFramework.checkExists().forPath(SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName)) != null) {
				byte[] preferListData = curatorFramework.getData().forPath(SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName));
				if(preferListData != null) {
					return new String(preferListData, "UTF-8").trim().length() > 0;
				}
			}
			return false;
		}

		/**
		 * 获取配置态的preferList，即使配置的executor不存在，也会返回。 特别的是，对于docker task，如果存在，才去解析出executor列表。
		 */
    	protected List<String> getPreferListConfigured(String jobName) throws Exception {
			List<String> preferList = new ArrayList<>();
			if(curatorFramework.checkExists().forPath(SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName)) != null) {
				byte[] preferListData = curatorFramework.getData().forPath(SaturnExecutorsNode.getJobConfigPreferListNodePath(jobName));
				if(preferListData != null) {
					List<String> allExistsExecutors = getAllExistingExecutors();
					String[] split = new String(preferListData, "UTF-8").split(",");
					for(String tmp : split) {
						String tmpTrim = tmp.trim();
						if(!"".equals(tmpTrim)) {
							fillRealPreferListIfIsDockerOrNot(preferList, tmpTrim, allExistsExecutors);
						}
					}
				}
			}
			return preferList;
		}

		private List<String> getAllExistingExecutors() throws Exception {
			List<String> allExistsExecutors = new ArrayList<>();
			if(curatorFramework.checkExists().forPath(SaturnExecutorsNode.getExecutorsNodePath()) != null) {
				List<String> executors = curatorFramework.getChildren().forPath(SaturnExecutorsNode.getExecutorsNodePath());
				if(executors != null) {
					allExistsExecutors.addAll(executors);
				}
			}
			return allExistsExecutors;
		}

		/**
		 *  如果prefer不是docker容器，并且preferList不包含，则直接添加；<br>
		 *  如果prefer是docker容器（以@开头），则prefer为task，获取该task下的所有executor，如果不包含，添加进preferList。
		 */
		private void fillRealPreferListIfIsDockerOrNot(List<String> preferList, String prefer, List<String> allExistsExecutors) throws Exception {
			if(!prefer.startsWith("@")) { // not docker server
				if(!preferList.contains(prefer)) {
					preferList.add(prefer);
				}
			} else { // docker server, get the real executorList by task
				String task = prefer.substring(1);
				for(int i=0; i<allExistsExecutors.size(); i++) {
					String executor = allExistsExecutors.get(i);
					if(curatorFramework.checkExists().forPath(SaturnExecutorsNode.getExecutorTaskNodePath(executor)) != null) {
						byte[] taskData = curatorFramework.getData().forPath(SaturnExecutorsNode.getExecutorTaskNodePath(executor));
						if(taskData != null && task.equals(new String(taskData, "UTF-8"))) {
							if(!preferList.contains(executor)) {
								preferList.add(executor);
							}
						}
					}
				}
			}
		}

		protected List<String> getPreferListOnline(List<String> preferListConfigured, List<Executor> executorListOnline) {
			List<String> preferListOnline = new ArrayList<>();
			if(executorListOnline != null) {
				for(int i=0; i<executorListOnline.size(); i++) {
					String executorName = executorListOnline.get(i).getExecutorName();
					if (preferListConfigured.contains(executorName)) {
						preferListOnline.add(executorName);
					}
				}
			}
			return preferListOnline;
		}

	}

    private class ExecuteAllShardingTask extends AbstractAsyncShardingTask {

		@Override
		public void run() {
			log.info("Execute the {} ", this.getClass().getSimpleName());
			super.run();
		}

		@Override
		protected boolean pick(List<String> allEnableJob, List<Shard> shardList, List<Executor> lastOnlineExecutorList) throws Exception {
			// 从$SaturnExecutors节点下，获取所有正在运行的Executor
			if(curatorFramework.checkExists().forPath(SaturnExecutorsNode.getExecutorsNodePath()) != null) {
				List<String> zkExecutors = curatorFramework.getChildren().forPath(SaturnExecutorsNode.getExecutorsNodePath());
				if(zkExecutors != null) {
					for(int i=0; i<zkExecutors.size(); i++) {
						String zkExecutor = zkExecutors.get(i);
						if(curatorFramework.checkExists().forPath(SaturnExecutorsNode.getExecutorIpNodePath(zkExecutor)) != null) {
							byte[] ipData = curatorFramework.getData().forPath(SaturnExecutorsNode.getExecutorIpNodePath(zkExecutor));
							if(ipData != null) {
								Executor executor = new Executor();
								executor.setExecutorName(zkExecutor);
								executor.setIp(new String(ipData, "UTF-8"));
								executor.setShardList(new ArrayList<Shard>());
								lastOnlineExecutorList.add(executor);
							}
						}
					}
				}
			}
			// 获取该域下所有作业的所有分片
			for(int i=0; i<allEnableJob.size(); i++) {
				String jobName = allEnableJob.get(i);
				shardList.addAll(getShardsByJobInfo(jobName, lastOnlineExecutorList));
			}

			return true;
		}

		@Override
		protected List<Executor> getLastOnlineExecutorList() {
			return new ArrayList<>();
		}

	}

    // 每个域拥有相同的作业
    private class ExecuteOnlineShardingTask extends AbstractAsyncShardingTask {

		private String executorName;

		public ExecuteOnlineShardingTask(String executorName) {
			this.executorName = executorName;
		}

		@Override
		public void run() {
			log.info("Execute the {} with {} online", this.getClass().getSimpleName(), executorName);
			super.run();
		}

		@Override
		protected boolean pick(List<String> allEnableJobs, List<Shard> shardList, List<Executor> lastOnlineExecutorList) throws Exception {//NOSONAR
			// 如果没有Executor在运行，则需要进行全量分片
			if(lastOnlineExecutorList.isEmpty()) {
				log.warn("There are no running executors, need all sharding");
				needAllSharding.set(true);
				shardingCount.incrementAndGet();
				executorService.submit(new ExecuteAllShardingTask());
				return false;
			}

			// 如果当前上线的Executor，已经在运行，则不做处理
			for(int i=0; i< lastOnlineExecutorList.size(); i++) {
				if(lastOnlineExecutorList.get(i).getExecutorName().equals(executorName)) {
					log.warn("The executor is already running, no necessary to sharding");
					return false;
				}
			}

			int totalLoalLevel = getTotalLoadLevel(lastOnlineExecutorList);
			int averageTotalLoal = totalLoalLevel / (lastOnlineExecutorList.size() + 1);

			/**
			 * 遍历所有运行的作业，对于作业
			 * 如果配置了preferList，为了修正错误，遍历executorList，对于executor
			 * 如果executor不属于preferList，则摘掉该executor的该作业的全部分片
			 */
			for(int i=0; i<allEnableJobs.size(); i++) {
				String job = allEnableJobs.get(i);
				List<String> preferList = getPreferListConfigured(job);
				if(!preferList.isEmpty()) { // 配置了preferList
					for(int j=0; j< lastOnlineExecutorList.size(); j++) {
						Executor executor = lastOnlineExecutorList.get(j);
						if(!preferList.contains(executor.getExecutorName())) {
							Iterator<Shard> iterator = executor.getShardList().iterator();
							while(iterator.hasNext()) {
								Shard shard = iterator.next();
								if(shard.getJobName().equals(job)) {
									executor.setTotalLoadLevel(executor.getTotalLoadLevel() - shard.getLoadLevel());
									iterator.remove();
									shardList.add(shard);
								}
							}
						}
					}
				}
			}

			/**
			 *  avg=平均每个executor拥有loadLevel
			 *  pickLoadLevel=currentAllLoadLevel-avg
			 *  1、从当前executor中摘取>=pickLoadLevel，并且如果该Shard具有preferList，则其必须能够运行在上线的Executor，得到列表的最小值
			 *  2、如果当前Shard都小于pickLoadLevel，则取<=pickLoadLevel的最大值
			 *  3、如果是2，再次循环选取
			 *
			 *  注意：先过滤，再选取。不摘取本地模式的作业分片；不摘取配置preferList，并且上线的Executor不属于preferList的作业分片。
			 */
			for(int i=0; i< lastOnlineExecutorList.size(); i++) {
				Executor executor = lastOnlineExecutorList.get(i);
				while(true) {
					int pickLoadLevel = executor.getTotalLoadLevel() - averageTotalLoal;
					if(pickLoadLevel > 0 && executor.getShardList().size() > 0) {
						// 选择最优
						Shard pickShard = null;
						for(int j=0; j<executor.getShardList().size(); j++) {
							Shard shard = executor.getShardList().get(j);
							// 如果当前Shard为本地模式作业分片，则不摘取，继续下一个
							if(isLocalMode(shard.getJobName())) {
								continue;
							}
							// 如果当前Shard的preferList不为空不包含当前上线的Executor，则继续下一个
							List<String> preferList = getPreferListConfigured(shard.getJobName());
							if(!preferList.isEmpty() && !preferList.contains(executorName)) {
								continue;
							}
							if(pickShard == null) { // fix, 修复可能存在的空指针问题
								pickShard = shard;
							} else {
								if(pickShard.getLoadLevel() >= pickLoadLevel) {
									if(shard.getLoadLevel() >= pickLoadLevel && shard.getLoadLevel() < pickShard.getLoadLevel()) {
										pickShard = shard;
									}
								} else{
									if(shard.getLoadLevel() >= pickLoadLevel) {
										pickShard = shard;
									} else {
										if(shard.getLoadLevel() > pickShard.getLoadLevel()) {
											pickShard = shard;
										}
									}
								}
							}
						}
						// 摘取
						if(pickShard != null) {
							executor.setTotalLoadLevel(executor.getTotalLoadLevel() - pickShard.getLoadLevel());
							executor.getShardList().remove(pickShard);
							shardList.add(pickShard);
						} else { // 没有符合摘取条件的，无需再选择摘取
							break;
						}
					} else { // 无需再选择摘取
						break;
					}
				}

			}

			// 添加上线executor至executorList
			byte[] ipData = curatorFramework.getData().forPath(SaturnExecutorsNode.getExecutorIpNodePath(executorName));
			Executor newExecutor = new Executor();
			newExecutor.setExecutorName(executorName);
			newExecutor.setIp(new String(ipData, "UTF-8"));
			newExecutor.setShardList(new ArrayList<Shard>());
			newExecutor.setTotalLoadLevel(0);
			lastOnlineExecutorList.add(newExecutor);

			// 添加新的Shard

			// 添加本地模式的作业的Shard，如果配置了preferList，并且当前上线的executor属于preferList；或者，如果没有配置preferList。
			// 添加非本地模式的作业的Shard。如果当前shardList不包含该作业，并且当前上线的executor可以运行该作业 ->
			// 	1、该作业没有配置preferList；
			//	2、或者配置了preferList，并且preferList包含当前上线的executor；
			//	3、或者配置了preferList，而preferList不包含当前上线的executor，并且preferList都下线了，并且配置了useDispreferList为true。
			for(int i=0; i<allEnableJobs.size(); i++) {
				String jobName = allEnableJobs.get(i);
				if(isLocalMode(jobName)) {
					// 注意这里的executorList包含了新上线的executor
					Shard shard = createLocalModeShard(jobName, lastOnlineExecutorList);
					if(shard != null) {
						shardList.add(shard);
					}
				} else {
					if(!includeJob(shardList, jobName)) {
						boolean preferListIsConfigured = preferListIsConfigured(jobName);
						List<String> preferListConfigured = getPreferListConfigured(jobName);
						List<String> preferListOnline = getPreferListOnline(preferListConfigured, lastOnlineExecutorList); // 注意这里lastOnlineExecutorList包含了新上线的executor
						if(!preferListIsConfigured || preferListConfigured.contains(executorName) || preferListOnline.isEmpty() && useDispreferList(jobName)) {
							shardList.addAll(createGoodShardList(jobName, lastOnlineExecutorList, getShardingTotalCount(jobName)));
						}
					}
				}
			}

			return true;
		}

		/**
		 *	如果配置了preferList，如果当前上线的executor属于preferList，则创建一个合理的Shard。<br>
		 * 	如果没有配置preferList，则创建一个合理的Shard。<br>
		 * 	合理的Shard的创建算法：如果现存0、2、3分片，那么新建分片项为1的Shard；如果现存0、1、2分片，那么新建分片项为3的Shard。
		 */
		private Shard createLocalModeShard(String jobName, List<Executor> executorList) throws Exception {
			Shard shard = null;
			boolean preferListIsConfigured = preferListIsConfigured(jobName);
			List<String> preferListConfigured = getPreferListConfigured(jobName);
			if(preferListIsConfigured && preferListConfigured.contains(executorName) || !preferListIsConfigured) {
				List<Shard> goodShardList = createGoodShardList(jobName, executorList, Math.max(preferListConfigured.size(), executorList.size()));
				if(!goodShardList.isEmpty()) {
					shard = goodShardList.get(0);
				}
			}
			return shard;
		}

		/**
		 * 创建合理的Shard，算法：如果现存0、2、3分片，那么新建分片项为1的Shard；如果现存0、1、2分片，那么新建分片项为3的Shard。如果现存0、2、4，shardCount为5，则新建分片项为1、3。
		 */
		private List<Shard> createGoodShardList(String jobName, List<Executor> executorList, int shardCount) throws Exception {
			List<Shard> goodShardList = new ArrayList<>();
			boolean[] flags = new boolean[shardCount];
			for (int j = 0; j < executorList.size(); j++) {
				Executor executor = executorList.get(j);
				for (int k = 0; k < executor.getShardList().size(); k++) {
					Shard shardTmp = executor.getShardList().get(k);
					if (shardTmp.getJobName().equals(jobName)) {
						flags[shardTmp.getItem()] = true;
					}
				}
			}
			for (int j = 0; j < flags.length; j++) {
				if (!flags[j]) {
					Shard shard = new Shard();
					shard.setJobName(jobName);
					shard.setItem(j);
					shard.setLoadLevel(getLoadLevel(jobName));
					goodShardList.add(shard);
				}
			}
			return goodShardList;
		}

		private boolean includeJob(List<Shard> shardList, String jobName) {
			boolean include = false;
			for(int j=0; j<shardList.size(); j++) {
				Shard shard = shardList.get(j);
				if(shard.getJobName().equals(jobName)) {
					include = true;
					break;
				}
			}
			return include;
		}

		private int getTotalLoadLevel(List<Executor> executorList) {
			int total = 0;
			for(int i=0; i<executorList.size(); i++) {
				total += executorList.get(i).getTotalLoadLevel();
			}
			return total;
		}

	}

    private class ExecuteOfflineShardingTask extends AbstractAsyncShardingTask {

		private String executorName;

		public ExecuteOfflineShardingTask(String executorName) {
			this.executorName = executorName;
		}

		@Override
		public void run() {
			log.info("Execute the {} with {} offline", this.getClass().getSimpleName(), executorName);
			super.run();
		}

		@Override
		protected boolean pick(List<String> allEnableJobs, List<Shard> shardList, List<Executor> lastOnlineExecutorList) throws Exception {
			/**
			 * 摘取下线的exectuor全部Shard
			 */
			boolean wasOffline = true;
			Iterator<Executor> iterator = lastOnlineExecutorList.iterator();
			while(iterator.hasNext()) {
				Executor executor = iterator.next();
				if(executor.getExecutorName().equals(executorName)) {
					wasOffline = false;
					iterator.remove();
					shardList.addAll(executor.getShardList());
					break;
				}
			}

			// 如果该executor实际上已经在此之前下线，则摘取失败
			if(wasOffline) {
				return false;
			}

			// 移除本地模式的作业分片
			Iterator<Shard> shardIterator = shardList.iterator();
			while(shardIterator.hasNext()) {
				Shard shard = shardIterator.next();
				if(isLocalMode(shard.getJobName())) {
					shardIterator.remove();
				}
			}

			return true;
		}

	}

    private class ExecuteJobEnableShardingTask extends AbstractAsyncShardingTask {

		private String jobName;

		public ExecuteJobEnableShardingTask(String jobName) {
			this.jobName = jobName;
		}

		@Override
		public void run() {
			log.info("Execute the {} with {} enable", this.getClass().getSimpleName(), jobName);
			super.run();
		}

		@Override
		protected boolean pick(List<String> allEnableJobs, List<Shard> shardList, List<Executor> lastOnlineExecutorList) throws Exception {
			// 获取该作业的Shard
			shardList.addAll(getShardsByJobInfo(jobName, lastOnlineExecutorList));

			// 移除已经在Executor运行的该作业的所有Shard
			boolean hasRemove = false;
			for(int i=0; i< lastOnlineExecutorList.size(); i++) {
				Executor executor = lastOnlineExecutorList.get(i);
				Iterator<Shard> iterator = executor.getShardList().iterator();
				while(iterator.hasNext()) {
					Shard shard = iterator.next();
					if(jobName.equals(shard.getJobName())) {
						log.warn("The Shard {}-{} is running in the executor {}, need to remove", jobName, shard.getItem(), executor.getExecutorName());
						iterator.remove();
						hasRemove = true;
					}
				}
			}

			// 如果shardList为空，并且没有移除shard，则没必要再进行放回等操作，摘取失败
			if(shardList.isEmpty() && !hasRemove) {
				return false;
			}

			return true;
		}

	}

    private class ExecuteJobDisableShardingTask extends AbstractAsyncShardingTask {

		private String jobName;

		public ExecuteJobDisableShardingTask(String jobName) {
			this.jobName = jobName;
		}

		@Override
		public void run() {
			log.info("Execute the {} with {} disable", this.getClass().getSimpleName(), jobName);
			super.run();
		}

		@Override
		protected boolean pick(List<String> allEnableJobs, List<Shard> shardList, List<Executor> lastOnlineExecutorList) {
			// 摘取所有该作业的Shard
			for(int i=0; i< lastOnlineExecutorList.size(); i++) {
				Executor executor = lastOnlineExecutorList.get(i);
				Iterator<Shard> iterator = executor.getShardList().iterator();
				while(iterator.hasNext()) {
					Shard shard = iterator.next();
					if (shard.getJobName().equals(jobName)) {
						executor.setTotalLoadLevel(executor.getTotalLoadLevel() - shard.getLoadLevel());
						iterator.remove();
						shardList.add(shard);
					}
				}
			}

			// 如果shardList为空，则没必要进行放回等操作，摘取失败
			if(shardList.isEmpty()) {
				return false;
			}

			return true;
		}

		@Override
		protected void putBackBalancing(List<String> allEnableJobs, List<Shard> shardList, List<Executor> lastOnlineExecutorList) {
			// 不做操作
		}

	}

    /**
     * 结点上线处理
     * @param executorName
     * @throws Exception
     */
	public void asyncShardingWhenExecutorOnline(String executorName) throws Exception {
		if(isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteOnlineShardingTask(executorName));
		}
	}

	/**
	 * 结点掉线处理
	 * @param executorName
	 * @throws Exception
	 */
	public void asyncShardingWhenExecutorOffline(String executorName) throws Exception {
		if(isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteOfflineShardingTask(executorName));
		}
	}

	/**
	 * 作业启用事件
	 * @param jobName
	 * @throws Exception
	 */
	public void asyncShardingWhenJobEnable(String jobName) throws Exception {
		if(isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteJobEnableShardingTask(jobName));
		}
	}

	/**
	 * 处理作业禁用事件
	 * @param jobName
	 * @throws Exception
	 */
	public void asyncShardingWhenJobDisable(String jobName) throws Exception {
		if(isLeadership()) {
			shardingCount.incrementAndGet();
			executorService.submit(new ExecuteJobDisableShardingTask(jobName));
		}
	}

	/**
	 * 选举
	 * @throws Exception
	 */
	public void leaderElection() throws Exception {
		log.info("{}-{} leadership election", namespace, hostValue);
		LeaderLatch leaderLatch = new LeaderLatch(curatorFramework, SaturnExecutorsNode.LEADER_LATCHNODE_PATH);
		try {
			leaderLatch.start();
			leaderLatch.await();
			if (curatorFramework.checkExists().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH) == null) {
				// 持久化$Jobs节点
				if(curatorFramework.checkExists().forPath(SaturnExecutorsNode.$JOBSNODE_PATH) == null) {
	    			curatorFramework.create().creatingParentsIfNeeded().forPath(SaturnExecutorsNode.$JOBSNODE_PATH);
	    		}
				// 持久化LeaderValue
				curatorFramework.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH, hostValue.getBytes("UTF-8"));
				// 清理、重置变量
				executorService.shutdownNow();
				while(!executorService.isTerminated()) { // 等待全部任务已经退出
					Thread.sleep(200);
				}
				needAllSharding.set(false);
				shardingCount.set(0);
				executorService = newSingleThreadExecutor();
				// 提交全量分片线程
				needAllSharding.set(true);
				shardingCount.incrementAndGet();
				executorService.submit(new ExecuteAllShardingTask());
				log.info("{}-{} become leadership", namespace, hostValue);
			}
		} catch (Exception e) {
			log.error(namespace + "-" + hostValue + " leadership election failed", e);
			throw e;
		} finally {
			try {
				leaderLatch.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	private boolean hasLeadership() throws Exception {
		return curatorFramework.checkExists().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH) != null;
	}

	private boolean isLeadership() throws Exception {
		while (!hasLeadership()) {
			leaderElection();
		}
		return new String(curatorFramework.getData().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH), "UTF-8").equals(hostValue);
	}

	private void deleteLeadership() throws Exception {
		if(isLeadership()) {
			curatorFramework.delete().forPath(SaturnExecutorsNode.LEADER_HOSTNODE_PATH);
		}
	}

	/**
	 * 关闭
	 */
	public void shutdown() {
		try {
			if(curatorFramework.getZookeeperClient().isConnected()){
				deleteLeadership();
			}
		} catch (Exception e) {
			log.error("delete leadership failed", e);
		}
		if(executorService != null) {
			executorService.shutdownNow();
		}
	}

}
