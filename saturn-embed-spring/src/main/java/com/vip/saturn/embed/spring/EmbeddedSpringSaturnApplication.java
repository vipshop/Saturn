package com.vip.saturn.embed.spring;

import com.vip.saturn.embed.EmbeddedSaturn;
import com.vip.saturn.job.spring.AbstractSpringSaturnApplication;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author hebelala
 */
public class EmbeddedSpringSaturnApplication extends AbstractSpringSaturnApplication implements ApplicationListener {

	// use spring log style
	protected final Log logger = LogFactory.getLog(getClass());

	private EmbeddedSaturn embeddedSaturn;

	private boolean ignoreExceptions;

	@Override
	public void init() {

	}

	@Override
	public void destroy() {

	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		try {
			if (event instanceof ContextRefreshedEvent) {
				ContextRefreshedEvent contextRefreshedEvent = (ContextRefreshedEvent) event;
				applicationContext = contextRefreshedEvent.getApplicationContext();
				if (embeddedSaturn == null) {
					embeddedSaturn = new EmbeddedSaturn();
					embeddedSaturn.setSaturnApplication(this);
					embeddedSaturn.start();
				}
			} else if (event instanceof ContextClosedEvent) {
				if (embeddedSaturn != null) {
					embeddedSaturn.stopGracefully();
					embeddedSaturn = null;
				}
			}
		} catch (Exception e) {
			logger.warn("exception happened on event: " + event, e);
			if (!ignoreExceptions) {
				throw new RuntimeException(e);
			}
		}
	}

	public boolean isIgnoreExceptions() {
		return ignoreExceptions;
	}

	public void setIgnoreExceptions(boolean ignoreExceptions) {
		this.ignoreExceptions = ignoreExceptions;
	}
}
