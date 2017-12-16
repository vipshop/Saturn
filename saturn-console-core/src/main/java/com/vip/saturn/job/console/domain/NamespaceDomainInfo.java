package com.vip.saturn.job.console.domain;

public class NamespaceDomainInfo {

    private String namespace;

    private String content;

    private String zkCluster;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getZkCluster() {
        return zkCluster;
    }

    public void setZkCluster(String zkCluster) {
        this.zkCluster = zkCluster;
    }
}
