package com.vip.saturn.job;

import com.vip.saturn.job.msg.MsgHolder;
import com.vip.saturn.job.msg.SaturnDelayedLevel;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.vip.saturn.job.SaturnJobReturn.*;

public class SaturnJobReturnTest {

	@Test
	public void testComplete() {
		SaturnJobReturn saturnJobReturn = SaturnJobReturn.builder().complete().build();
		Assert.assertEquals(SaturnConsumeStatus.CONSUME_SUCCESS.name(),
				saturnJobReturn.getProp().get(MSG_CONSUME_STATUS_PROP_KEY));
	}

	@Test
	public void testDiscard() {
		SaturnJobReturn saturnJobReturn = SaturnJobReturn.builder().discard().build();
		Assert.assertEquals(SaturnConsumeStatus.CONSUME_DISCARD.name(),
				saturnJobReturn.getProp().get(MSG_CONSUME_STATUS_PROP_KEY));
	}

	@Test
	public void testReconsume() {
		SaturnJobReturn saturnJobReturn = SaturnJobReturn.builder().returnCode(SaturnSystemReturnCode.USER_FAIL)
				.errorGroup(SaturnSystemErrorGroup.FAIL).reconsumeLater(SaturnDelayedLevel.DELAYED_LEVEL_1H).build();
		Assert.assertEquals(SaturnConsumeStatus.RECONSUME_LATER.name(),
				saturnJobReturn.getProp().get(MSG_CONSUME_STATUS_PROP_KEY));
		Assert.assertEquals(SaturnDelayedLevel.DELAYED_LEVEL_1H.getValue(),
				Integer.parseInt(saturnJobReturn.getDelayLevel()));
		Assert.assertEquals(SaturnSystemReturnCode.USER_FAIL, saturnJobReturn.getReturnCode());
		Assert.assertEquals(SaturnSystemErrorGroup.FAIL, saturnJobReturn.getErrorGroup());
	}

	@Test
	public void testCompleteAll() {
		SaturnJobReturn saturnJobReturn = SaturnJobReturn.builder().completeAll().build();
		Assert.assertTrue(saturnJobReturn.isCompleteAll());
		Assert.assertEquals(SaturnSystemReturnCode.SUCCESS, saturnJobReturn.getReturnCode());
		Assert.assertEquals(SaturnSystemErrorGroup.SUCCESS, saturnJobReturn.getErrorGroup());
	}

	@Test
	public void testReconsumeAll() {
		SaturnJobReturn saturnJobReturn = SaturnJobReturn.builder().reconsumeAll(SaturnDelayedLevel.DELAYED_LEVEL_10S)
				.build();
		Assert.assertTrue(saturnJobReturn.isReconsumeAll());
		Assert.assertEquals(SaturnDelayedLevel.DELAYED_LEVEL_10S.getValue(),
				Integer.parseInt(saturnJobReturn.getDelayLevel()));
		Assert.assertEquals(SaturnSystemReturnCode.SUCCESS, saturnJobReturn.getReturnCode());
		Assert.assertEquals(SaturnSystemErrorGroup.SUCCESS, saturnJobReturn.getErrorGroup());
	}

	@Test
	public void testCompleteSome() {
		List<MsgHolder> completeMsgHolders = new ArrayList<>();
		for (long i = 1; i <= 5; i++) {
			completeMsgHolders.add(new MsgHolder(null, null, "", i));
		}
		List<MsgHolder> reconsumeMsgHolders = new ArrayList<>();
		for (long i = 11; i <= 15; i++) {
			reconsumeMsgHolders.add(new MsgHolder(null, null, "", i));
		}
		List<MsgHolder> discardMsgHolders = new ArrayList<>();
		for (long i = 21; i <= 25; i++) {
			discardMsgHolders.add(new MsgHolder(null, null, "", i));
		}
		SaturnJobReturn saturnJobReturn = SaturnJobReturn.builder()
				.batchConsumeDefaultStatus(SaturnConsumeStatus.CONSUME_SUCCESS).completeSome(completeMsgHolders)
				.reconsumeSome(reconsumeMsgHolders).discardSome(discardMsgHolders).build();

		Assert.assertEquals(SaturnSystemReturnCode.SUCCESS, saturnJobReturn.getReturnCode());
		Assert.assertEquals(SaturnSystemErrorGroup.SUCCESS, saturnJobReturn.getErrorGroup());
		Assert.assertEquals(SaturnConsumeStatus.CONSUME_SUCCESS.name(), saturnJobReturn.getBatchConsumeDefaultStatus());
		Assert.assertEquals(5, saturnJobReturn.getCompleteOffsets().size());
		Assert.assertEquals("1,2,3,4,5", saturnJobReturn.getProp().get(MSG_BATCH_CONSUME_SUCCESS_OFFSETS));
		Assert.assertEquals(5, saturnJobReturn.getReconsumeOffsets().size());
		Assert.assertEquals("11,12,13,14,15", saturnJobReturn.getProp().get(MSG_BATCH_CONSUME_DELAY_OFFSETS));
		Assert.assertEquals(5, saturnJobReturn.getDiscardOffsets().size());
		Assert.assertEquals("21,22,23,24,25", saturnJobReturn.getProp().get(MSG_BATCH_CONSUME_DISCARD_OFFSETS));

		// 测试增量
		completeMsgHolders.clear();
		for (long i = 6; i <= 9; i++) {
			completeMsgHolders.add(new MsgHolder(null, null, "", i));
		}
		reconsumeMsgHolders.clear();
		for (long i = 16; i <= 19; i++) {
			reconsumeMsgHolders.add(new MsgHolder(null, null, "", i));
		}
		discardMsgHolders.clear();
		for (long i = 26; i <= 29; i++) {
			discardMsgHolders.add(new MsgHolder(null, null, "", i));
		}
		saturnJobReturn.completeSome(completeMsgHolders);
		saturnJobReturn.reconsumeSome(reconsumeMsgHolders);
		saturnJobReturn.discardSome(discardMsgHolders);

		Assert.assertEquals(9, saturnJobReturn.getCompleteOffsets().size());
		Assert.assertEquals("1,2,3,4,5,6,7,8,9", saturnJobReturn.getProp().get(MSG_BATCH_CONSUME_SUCCESS_OFFSETS));
		Assert.assertEquals(9, saturnJobReturn.getReconsumeOffsets().size());
		Assert.assertEquals("11,12,13,14,15,16,17,18,19",
				saturnJobReturn.getProp().get(MSG_BATCH_CONSUME_DELAY_OFFSETS));
		Assert.assertEquals(9, saturnJobReturn.getDiscardOffsets().size());
		Assert.assertEquals("21,22,23,24,25,26,27,28,29",
				saturnJobReturn.getProp().get(MSG_BATCH_CONSUME_DISCARD_OFFSETS));

	}
}
