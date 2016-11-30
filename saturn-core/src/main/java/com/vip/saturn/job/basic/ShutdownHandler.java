package com.vip.saturn.job.basic;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Saturn优雅退出: 退出时清理信息
 * @author dylan.xue
 */
@SuppressWarnings("restriction")
public class ShutdownHandler implements SignalHandler {
	static Logger log = LoggerFactory.getLogger(ShutdownHandler.class);

	private static List<Runnable> listeners = new ArrayList<Runnable>();
	
	private static ShutdownHandler handler;
	
	static{
		handler = new ShutdownHandler(true);
		Signal.handle(new Signal("TERM"), handler); // 相当于kill -15
		Signal.handle(new Signal("INT"), handler); // 相当于Ctrl+C	
	}
	
	public ShutdownHandler(boolean _exit) {
		exit = _exit;
	}
	private boolean exit = true;
	
	public static void addShutdownCallback(Runnable c){
		listeners.add(c);
	}
	
	
	@Override
	public void handle(Signal sn) {
		
		if (listeners != null) {
			for (Runnable callable : listeners) {
				try {
					if (callable != null) {
						callable.run();
					}
				} catch (Exception e) {
					log.error("msg=" + e.getMessage(), e);
				}
			}
		}
		

		// stop loggerContext
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.stop();
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			log.error("msg=" + e.getMessage(),e);
		}

		// 退出
		log.info("msg=Saturn is shutdown...");
		if(exit){
			System.exit(-1);
		}
	}
	
}
