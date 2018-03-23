package com.vip.saturn.job.console.aop.aspect;

import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.utils.AuditInfoContext;
import com.vip.saturn.job.console.utils.DummyAppender;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringContextAOP.class)
public class AuditLogAspectTest {

	@Autowired
	private TestAspectClass testClass;

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpSession httpSession;

	private DummyAppender dummyAppender = new DummyAppender();

	@Before
	public void before() {
		initAppender();
	}

	@After
	public void after() {
		detachAppender();
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	public void testRESTRequestAuditLog() {
		prepareRequest("192.168.1.1", "/home/path", null);

		testClass.method1();

		assertTrue(AuditInfoContext.currentAuditInfo().isEmpty());
		assertEquals("log size should be 1", 1, dummyAppender.getEvents().size());
		assertEquals("log content is not equal",
				"[INFO] REST API:[method1] path:[/home/path] is called by IP:[192.168.1.1], result is success. Context info:{namespace=www.abc.com, jobName=jobA, jobNames=[jobB, jobC]}.",
				dummyAppender.getLastEvent().toString());
	}

	@Test
	public void testWEBRequestAuditLog() {
		prepareRequest("192.168.1.2", "/home/path2", "usera");

		testClass.method2();

		assertTrue(AuditInfoContext.currentAuditInfo().isEmpty());
		assertEquals("log size should be 1", 1, dummyAppender.getEvents().size());
		assertEquals("log content is not equal",
				"[INFO] GUI API:[method2] path:[/home/path2] is called by User:[usera] with IP:[192.168.1.2], result is success. Context info:{namespace=www.abc.com, jobName=jobA, jobNames=[jobB, jobC]}.",
				dummyAppender.getLastEvent().toString());
	}

	@Test
	public void testWebRequestAuditLogButFail() {
		prepareRequest("192.168.1.3", "/home/path3", "userb");

		try {
			testClass.method3();
		} catch (RuntimeException e) {
			// do nothing
		}

		assertTrue(AuditInfoContext.currentAuditInfo().isEmpty());
		assertEquals("log size should be 1", 1, dummyAppender.getEvents().size());
		assertEquals("log content is not equal",
				"[INFO] GUI API:[method3] path:[/home/path3] is called by User:[userb] with IP:[192.168.1.3], result is failed. Context info:{namespace=www.abc.com, jobName=jobA, jobNames=[jobB, jobC]}.",
				dummyAppender.getLastEvent().toString());
	}

	private void prepareRequest(String ip, String uri, String username) {
		given(request.getHeader("X-FORWARDED-FOR")).willReturn(ip);
		given(request.getRequestURI()).willReturn(uri);
		given(httpSession.getAttribute(SessionAttributeKeys.LOGIN_USER_NAME)).willReturn(username);
		given(request.getSession()).willReturn(httpSession);

		ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(request);
		RequestContextHolder.setRequestAttributes(servletRequestAttributes);
	}

	private void initAppender() {
		ch.qos.logback.classic.Logger auditlog = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("AUDITLOG");
		if (dummyAppender != null) {
			dummyAppender.clear();
		}

		auditlog.addAppender(dummyAppender);
	}

	private void detachAppender() {
		ch.qos.logback.classic.Logger auditlog = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("AUDITLOG");
		if (dummyAppender != null) {
			dummyAppender.clear();
		}

		auditlog.detachAppender(dummyAppender);
	}

}

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {"com.vip.saturn.job.console.aop.aspect"})
class SpringContextAOP {

}

@Component
class TestAspectClass {

	@Audit(type = AuditType.REST, name = "method1")
	public void method1() {
		AuditInfoContext.putNamespace("www.abc.com");
		AuditInfoContext.putJobName("jobA");
		AuditInfoContext.putJobNames(Arrays.asList("jobB", "jobC"));
	}

	@Audit(type = AuditType.WEB)
	public void method2() {
		AuditInfoContext.putNamespace("www.abc.com");
		AuditInfoContext.putJobName("jobA");
		AuditInfoContext.putJobNames(Arrays.asList("jobB", "jobC"));
	}

	@Audit
	public void method3() {
		AuditInfoContext.putNamespace("www.abc.com");
		AuditInfoContext.putJobName("jobA");
		AuditInfoContext.putJobNames(Arrays.asList("jobB", "jobC"));

		throw new RuntimeException("unexpected");
	}
}

