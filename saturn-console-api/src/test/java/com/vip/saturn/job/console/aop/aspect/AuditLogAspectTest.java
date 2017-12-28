package com.vip.saturn.job.console.aop.aspect;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import com.vip.saturn.job.console.aop.annotation.Audit;
import com.vip.saturn.job.console.aop.annotation.AuditType;
import com.vip.saturn.job.console.utils.AuditInfoContext;
import com.vip.saturn.job.console.utils.DummyAppender;
import com.vip.saturn.job.console.utils.SessionAttributeKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	public void testRESTRequestAuditLog() {
		prepareRequest("192.168.1.1", "/home/path", null);

		testClass.method1();

		assertEquals("log size should be 1", 1, dummyAppender.getEvents().size());
		assertEquals("log content is not equal",
				"[INFO] REST API:[/home/path] is called by IP:[192.168.1.1], result is success. Context info:{jobName=jobA, namespace=www.abc.com}.",
				dummyAppender.getLastEvent().toString());
	}

	@Test
	public void testWEBRequestAuditLog() {
		prepareRequest("192.168.1.2", "/home/path2", "usera");

		testClass.method2();

		assertEquals("log size should be 1", 1, dummyAppender.getEvents().size());
		assertEquals("log content is not equal",
				"[INFO] GUI API:[/home/path2] is called by User:[usera] with IP:[192.168.1.2], result is success. Context info:{jobName=jobB, namespace=www.abc.com}.",
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

		assertEquals("log size should be 1", 1, dummyAppender.getEvents().size());
		assertEquals("log content is not equal",
				"[INFO] GUI API:[/home/path3] is called by User:[userb] with IP:[192.168.1.3], result is failed. Context info:{jobName=jobC, namespace=www.abc.com}.",
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
		ch.qos.logback.classic.Logger auditlog = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger("AUDITLOG");
		if (dummyAppender != null) {
			dummyAppender.clear();
		}

		auditlog.addAppender(dummyAppender);
	}

}

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = {"com.vip.saturn.job.console.aop.aspect"})
class SpringContextAOP {

}

@Component
class TestAspectClass {

	@Audit(type = AuditType.REST)
	public void method1() {
		AuditInfoContext.putNamespace("www.abc.com");
		AuditInfoContext.putJobName("jobA");
	}

	@Audit(type = AuditType.WEB)
	public void method2() {
		AuditInfoContext.putNamespace("www.abc.com");
		AuditInfoContext.putJobName("jobB");
	}

	@Audit
	public void method3() {
		AuditInfoContext.putNamespace("www.abc.com");
		AuditInfoContext.putJobName("jobC");

		throw new RuntimeException("unexpected");
	}
}

