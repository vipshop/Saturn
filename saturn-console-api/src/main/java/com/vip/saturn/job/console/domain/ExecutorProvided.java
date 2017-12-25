package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
 */
public class ExecutorProvided {

    private String executorName;
    private ExecutorProvidedType type;
    private boolean noTraffic;

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    public ExecutorProvidedType getType() {
        return type;
    }

    public void setType(ExecutorProvidedType type) {
        this.type = type;
    }

    public boolean isNoTraffic() {
        return noTraffic;
    }

    public void setNoTraffic(boolean noTraffic) {
        this.noTraffic = noTraffic;
    }
}
