package com.vip.saturn.job.console.controller.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 作业告警统计页面
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/{namespace:.+}/{jobName}/alarmStatistics")
public class JobAlarmStatisticsController extends AbstractGUIController {

}
