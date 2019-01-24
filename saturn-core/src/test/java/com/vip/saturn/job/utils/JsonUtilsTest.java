package com.vip.saturn.job.utils;

import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.vip.saturn.job.SaturnJobReturn;
import com.vip.saturn.job.executor.ExecutorConfig;
import com.vip.saturn.job.trigger.TriggeredData;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class JsonUtilsTest {

	@Test
	public void testGetGson() {
		assertNotNull(JsonUtils.getGson().fromJson("{}", ExecutorConfig.class));

		try {
			JsonUtils.getGson().fromJson("abc", ExecutorConfig.class);
			fail("cannot happen");
		} catch (Exception e) {
			assertTrue(e instanceof JsonParseException);
		}

		assertNotNull(JsonUtils.getGson().fromJson("{}", new TypeToken<Map<String, String>>() {
		}.getType()));

		try {
			JsonUtils.getGson().fromJson("abc", new TypeToken<Map<String, String>>() {
			}.getType());
			fail("cannot happen");
		} catch (Exception e) {
			assertTrue(e instanceof JsonParseException);
		}

		assertNotNull(JsonUtils.getGson().fromJson("{}", SaturnJobReturn.class));

		try {
			JsonUtils.getGson().fromJson("abc", SaturnJobReturn.class);
			fail("cannot happen");
		} catch (Exception e) {
			assertTrue(e instanceof JsonParseException);
		}

		Map<String, Object> obj = JsonUtils.getGson()
				.fromJson("{\"a\":true, \"b\":1}", new TypeToken<Map<String, Object>>() {
				}.getType());
		assertNotNull(obj);
		assertTrue(obj.containsKey("a"));
		assertEquals(true, obj.get("a"));
		assertTrue(obj.containsKey("b"));
		assertEquals(1.0, obj.get("b"));

		try {
			JsonUtils.getGson().fromJson("abc", new TypeToken<Map<String, Object>>() {
			}.getType());
			fail("cannot happen");
		} catch (Exception e) {
			assertTrue(e instanceof JsonParseException);
		}

		assertNull(JsonUtils.getGson().fromJson("", new TypeToken<Map<String, Object>>() {
		}.getType()));

		String src = null;
		assertNull(JsonUtils.getGson().fromJson(src, new TypeToken<Map<String, Object>>() {
		}.getType()));
	}

	@Test
	public void testGetJsonParser() {
		assertEquals("abc", JsonUtils.getJsonParser().parse("{\"message\":\"abc\"}").getAsJsonObject().get("message")
				.getAsString());
		assertEquals(JsonNull.INSTANCE,
				JsonUtils.getJsonParser().parse("{\"message\":null}").getAsJsonObject().get("message"));
		assertNull(JsonUtils.getJsonParser().parse("{}").getAsJsonObject().get("message"));
	}

	@Test
	public void testToJson1() {
		assertEquals("null", JsonUtils.toJson(null));
		assertEquals("{}", JsonUtils.toJson(new TriggeredData()));

		assertEquals("null", JsonUtils.getGson().toJson(JsonNull.INSTANCE));
		assertEquals("null", JsonUtils.getGson().toJson(null));
	}

	@Test
	public void testToJson2() {
		Map<String, String> src = null;
		assertEquals("null", JsonUtils.toJson(src, new TypeToken<Map<String, String>>() {
		}.getType()));

		src = new HashMap<>();
		src.put("cron", "9 9 9 9 9 ? 2099");
		assertEquals("{\"cron\":\"9 9 9 9 9 ? 2099\"}", JsonUtils.toJson(src, new TypeToken<Map<String, String>>() {
		}.getType()));
	}

	@Test
	public void testFromJson1() {
		String customContextStr = null;
		assertNull(JsonUtils.fromJson(customContextStr, new TypeToken<Map<String, String>>() {
		}.getType()));

		customContextStr = "abc";
		assertNull(JsonUtils.fromJson(customContextStr, new TypeToken<Map<String, String>>() {
		}.getType()));

		customContextStr = "{\"key\":\"value\"}";
		Map<String, String> expected = new HashMap<>();
		expected.put("key", "value");
		assertEquals(expected, JsonUtils.fromJson(customContextStr, new TypeToken<Map<String, String>>() {
		}.getType()));
	}

	@Test
	public void testFromJson2() {
		String triggeredDataStr = null;
		assertNull(JsonUtils.fromJson(triggeredDataStr, TriggeredData.class));

		triggeredDataStr = "null";
		assertNull(JsonUtils.fromJson(triggeredDataStr, TriggeredData.class));

		triggeredDataStr = "{}";
		assertNotNull(JsonUtils.fromJson(triggeredDataStr, TriggeredData.class));

		triggeredDataStr = "abc";
		assertNull(JsonUtils.fromJson(triggeredDataStr, TriggeredData.class));
	}

	@Test
	public void testEscapeHtmlChar() {
		String str = "<>'=";
		assertEquals("\"" + str + "\"", JsonUtils.toJson(str));
	}

	@Test
	public void testDateFormat() {
		assertTrue(JsonUtils.toJson(new Date()).matches("\"\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\""));
	}

}
