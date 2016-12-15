package com.vip.saturn.demo.test;

import com.vip.saturn.embed.SaturnEmbed;

/**
 * 嵌入式运行saturn executor(！仅用于测试和调试目的)
 * 
 * 运行步骤：
 * 0. 配置hosts
 * 1. 从maven库下载最新的saturn-job-executor.zip(1.0.11以上版本)
 * 2. 解压到本机的某个目录比如d:/saturn
 * 3. 配置环境变量 SATURN_HOME=d:/saturn
 * 4. 配置环境变量 SATURN_APP_NAMESPACE=您的域名(比如g.vip.com，如果没有请先向CMDB申请)
 * 5. 配置环境变量 SATURN_APP_EXECUTOR_NAME=您起的实例的ID(比如executor_001，同一个域下必须唯一)
 */
public class EmbedDemo {
	/**
	 * 运行Saturn前必须设置以下环境变量
	 * SATURN_HOME或者SATURN_ZIP_FILE
	 * SATURN_APP_NAMESPACE
	 * SATURN_APP_EXECUTOR_NAME
	 */
	public static void demo1(String[] args){
		try {
			SaturnEmbed.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 使用-Dsaturn.zipfile会覆盖环境变量SATURN_ZIP_FILE
	 * 使用-Dsaturn.home会覆盖环境变量SATURN_HOME
	 * 使用-Dsaturn.app.namespace会覆盖环境变量SATURN_APP_NAMESPACE
	 * 使用-Dsaturn.app.executorName会覆盖环境变量SATURN_APP_EXECUTOR_NAME
	 */
	public static void demo2(String[] args){
		try {
	
			//System.setProperty("saturn.home","d:/saturn");//saturn.home和saturn.zipfile有一个即可
			System.setProperty("saturn.zipfile","d:/saturn-job-executor-1.1.1-zip.zip");
			System.setProperty("saturn.app.namespace","saturn-it.vip.com");
			System.setProperty("saturn.app.executorName","executor_001");			
			SaturnEmbed.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		//demo1(args);
		demo2(args);
	}
}
