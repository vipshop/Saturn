package com.vip.saturn.job.console.controller.gui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 作业的告警统计
 *
 * @author hebelala
 */
@Controller
@RequestMapping("/console/{namespace:.+}/jobs/{jobName}/alarmStatistics")
public class JobAlarmStatisticsController extends AbstractGUIController {

}
