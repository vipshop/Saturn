package com.vip.saturn.it.job;

import com.vip.saturn.job.AbstractSaturnMsgJob;
import com.vip.saturn.job.SaturnJobExecutionContext;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.msg.MsgHolder;
import com.vipshop.code.vdp.common.event.VdpEvent;
import com.vipshop.code.vdp.serializer.impl.VdpSerializerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xiaopeng.he on 2016/8/29.
 */
public class VdpMsgJob extends AbstractSaturnMsgJob {

    public static final Logger log = LoggerFactory.getLogger(VdpMsgJob.class);

    public static AtomicInteger receiveCount = new AtomicInteger();

    public static AtomicInteger failCount = new AtomicInteger();

    @Override
    public SaturnJobReturn handleMsgJob(String jobName, Integer shardItem, String shardParam, MsgHolder msgHolder, SaturnJobExecutionContext shardingContext) throws InterruptedException {
        receiveCount.incrementAndGet();
        try {
            VdpEvent deserialize = new VdpSerializerImpl().deserialize(msgHolder.getPayloadBytes());
            log.info("{} deserialize payload result: {}", jobName, deserialize.toString());
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
            failCount.incrementAndGet();
        }
        return new SaturnJobReturn();
    }

}
