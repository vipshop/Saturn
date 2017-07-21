package demo;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;
/**
 * @author chembo.huang
 *
 */
public class DemoJavaJob extends AbstractSaturnJavaJob {
	
	@Override
    public SaturnJobReturn handleJavaJob(String jobName, Integer shardItem, String shardParam, SaturnJobExecutionContext shardingContext) {
        System.out.println("u can find this log in LOG of shards-tab of job-detail page, jobName:" + jobName + "; shardItem:" + shardItem);
        return new SaturnJobReturn();
    }
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
		CuratorFramework client = CuratorFrameworkFactory.builder().connectString("localhost:2181")
				.namespace("mydomain").sessionTimeoutMs(10000).retryPolicy(retryPolicy).build();
		client.start();
		addJavaJob(client, "demoJavaJob");
		System.out.println("done add a java-job.");
	}

	protected static void addJavaJob(CuratorFramework client, String jobName) throws Exception {
		String jobConfigNode = "/$Jobs/" + jobName + "/config/";
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "jobType", "JAVA_JOB".getBytes());
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "cron", "*/5 * * * * ?".getBytes());
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "shardingTotalCount", "5".getBytes());
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "shardingItemParameters","0=0,1=1,2=2,3=3,4=4".getBytes());
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "enabled", "false".getBytes());
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "failover", "true".getBytes());
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "timeoutSeconds", "0".getBytes());
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "maxTimeDiffSeconds", "-1".getBytes());
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "processCountIntervalSeconds","5".getBytes());
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "showNormalLog", "true".getBytes());
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "loadLevel", "1".getBytes());
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "monitorExecution", "true".getBytes());
		client.create().creatingParentsIfNeeded().forPath(jobConfigNode + "jobClass","demo.DemoJavaJob".getBytes());
		Thread.sleep(3000);
		client.setData().forPath(jobConfigNode + "enabled", "true".getBytes());
	}

}
