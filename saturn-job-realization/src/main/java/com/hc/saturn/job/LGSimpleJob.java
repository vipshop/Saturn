package com.hc.saturn.job;

import com.alibaba.fastjson.JSONObject;
import com.hc.saturn.util.HttpClientUtils;
import com.vip.saturn.job.AbstractSaturnJavaJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 临港的任务执行类
 * todo: 暂时以项目为划分，后期如果项目多了，可以整理以功能划分
 *
 * @author linj
 */
public class LGSimpleJob extends AbstractSaturnJavaJob {

    /**
     * @param jobName         任务名称
     * @param shardItem       分片 暂时未使用 todo ：实现分片进行测试多线程，需要确定分片使用的场景
     * @param shardParam      分片参数
     * @param shardingContext 自定义其他参数 ：可以用于具体的业务实现 {}
     * @return
     * @throws InterruptedException
     */
    @Override
    public SaturnJobReturn handleJavaJob(final String jobName, final Integer shardItem, final String shardParam, final SaturnJobExecutionContext shardingContext) throws InterruptedException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        System.out.println("《《《:"+sdf.format(new Date()));
        switch (shardItem) {
            case 0:
                System.out.println("《《《:"+sdf.format(new Date()));
                //return new SaturnJobReturn("0");
                return singleTask(jobName, shardItem, shardParam, shardingContext);
            default:
                System.out.println("default");
                return new SaturnJobReturn("default");
        }
        /*
        //todo ：后续需要确定分片使用的场景
       SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        switch (shardItem){
            case 0 :
                System.out.println(sdf.format(new Date())+";"+shardParam);
                return new SaturnJobReturn("shardParam");
            case 1 :
                System.out.println(sdf.format(new Date())+";"+shardParam);
                return new SaturnJobReturn("shardItem:1");
            case 2 :
                System.out.println(sdf.format(new Date())+";"+shardParam);
                return new SaturnJobReturn("shardItem:2");
        }
        return new SaturnJobReturn("null");*/
    }

    /**
     * 单分片任务
     *
     * @param jobName
     * @param shardItem
     * @param shardParam
     * @param shardingContext
     * @return
     */
    public SaturnJobReturn singleTask(final String jobName, final Integer shardItem, final String shardParam, final SaturnJobExecutionContext shardingContext) {
        if (StringUtils.isNotBlank(shardingContext.getJobParameter())) {
            JSONObject jsonObject = JSONObject.parseObject(shardingContext.getJobParameter());
            String url = jsonObject.getString("url");
            String result = HttpClientUtils.doGet(url);
            // TODO: 2018/6/25 linj  这里需要模拟elastic job的日志系统，将采集的每次记录保存
            //JSONObject resultJson = JSONObject.parseObject(result);
            //记录采集的自定义日志
            // url 、status 、 desc  time
            return new SaturnJobReturn(result);
        }
        return new SaturnJobReturn("自定义参数为空");
    }
}
