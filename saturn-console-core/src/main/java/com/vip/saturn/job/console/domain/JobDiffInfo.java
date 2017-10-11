package com.vip.saturn.job.console.domain;

import java.util.List;


/**
 * The class used for store the zk and db diffByNamespace data.
 */
public class JobDiffInfo {

    private String namespace;

    private String jobName;

    private DiffType diffType;

    private List<ConfigDiffInfo> configDiffInfos;

    public JobDiffInfo(String namespace, String jobName, DiffType diffType, List<ConfigDiffInfo> configDiffInfos) {
        this.namespace = namespace;
        this.jobName = jobName;
        this.diffType = diffType;
        this.configDiffInfos = configDiffInfos;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getJobName() {
        return jobName;
    }

    public DiffType getDiffType() {
        return diffType;
    }

    public List<ConfigDiffInfo> getConfigDiffInfos() {
        return configDiffInfos;
    }

    /**
     * 比较结果的不同类型.
     */
    public enum DiffType {
        // 只有ZK有，DB没有
        ZK_ONLY,
        // 只有DB有，ZK没有
        DB_ONLY,
        // ZK和DB的数据有差异
        HAS_DIFFERENCE;
    }


    public static class ConfigDiffInfo {

        private String key;

        private Object dbValue;

        private Object zkValue;

        public ConfigDiffInfo(String key, Object dbValue, Object zkValue) {
            this.key = key;
            this.dbValue = dbValue;
            this.zkValue = zkValue;
        }

        @Override
        public String toString() {
            return "Different item:" +
                    "'" + key + "'" +
                    ", value in db=" + dbValue +
                    "and value in zk=" + zkValue;
        }

        public String getKey() {
            return key;
        }

        public Object getDbValue() {
            return dbValue;
        }

        public Object getZkValue() {
            return zkValue;
        }
    }

}



