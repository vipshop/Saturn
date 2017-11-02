package com.vip.saturn.job.sharding.exception;

/**
 * @author hebelala
 */
public class ShardingException extends Exception {

    static final long serialVersionUID = 1L;

    public ShardingException() {
        super();
    }

    public ShardingException(String message) {
        super(message);
    }

    public ShardingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShardingException(Throwable cause) {
        super(cause);
    }

    protected ShardingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
