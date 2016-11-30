package com.vip.saturn.job.console.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.vip.saturn.job.console.repository.zookeeper.CuratorRepository;

/**
 * Created by xiaopeng.he on 2016/6/29.
 */
public class CommonUtils {

    /**
     * 获取所有作业
     */
    public static List<String> getJobNames(CuratorRepository.CuratorFrameworkOp curatorFrameworkOp) {
        List<String> jobNames = new ArrayList<>();
        List<String> tmp = curatorFrameworkOp.getChildren("/" + JobNodePath.$JOBS_NODE_NAME);
        if(tmp != null) {
            jobNames.addAll(tmp);
        }
        Collections.sort(jobNames);
        return jobNames;
    }

}
