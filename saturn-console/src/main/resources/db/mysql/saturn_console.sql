
SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `job_config`
-- ----------------------------
CREATE TABLE `job_config` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '作业ID主键',
  `job_name` varchar(255) NOT NULL COMMENT '作业名称',
  `namespace` varchar(255) NOT NULL COMMENT '命名空间',
  `zk_list` varchar(255) DEFAULT NULL COMMENT 'zookeeper连接列表',
  `job_class` varchar(255) DEFAULT NULL COMMENT '作业类名',
  `sharding_total_count` int(11) DEFAULT NULL COMMENT '作业分片总数',
  `load_level` int(11) NOT NULL DEFAULT '1' COMMENT '每个分片默认负荷',
  `cron` varchar(255) DEFAULT NULL COMMENT 'cron表达式',
  `pause_period_date` text COMMENT '停止周期日期',
  `pause_period_time` text COMMENT '停止周期时间',
  `sharding_item_parameters` text COMMENT '分片序列号/参数对照表',
  `job_parameter` text COMMENT '作业参数',
  `monitor_execution` tinyint(1) DEFAULT '1' COMMENT '监控异常',
  `process_count_interval_seconds` int(11) DEFAULT NULL COMMENT '处理总数间隔秒数',
  `concurrent_data_process_thread_count` int(11) DEFAULT NULL COMMENT '当前数据处理线程总数',
  `fetch_data_count` int(11) DEFAULT NULL COMMENT '获取到的数据总数',
  `max_time_diff_seconds` int(11) DEFAULT NULL COMMENT '最大时间相差的秒数',
  `monitor_port` int(11) DEFAULT NULL COMMENT '监控端口',
  `failover` tinyint(1) DEFAULT NULL COMMENT '是否为失效的作业',
  `misfire` tinyint(1) DEFAULT NULL COMMENT '是否为被错过的作业(可能需要触发重发)',
  `job_sharding_strategy_class` varchar(255) DEFAULT NULL COMMENT '作业分片策略类',
  `description` text COMMENT '作业描述',
  `timeout_seconds` int(11) DEFAULT NULL COMMENT '超时秒数',
  `show_normal_log` tinyint(1) DEFAULT NULL COMMENT '是否显示正常日志',
  `channel_name` varchar(255) DEFAULT NULL COMMENT '渠道名称',
  `job_type` varchar(255) DEFAULT NULL COMMENT '作业类型',
  `queue_name` text COMMENT '队列名称',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `last_update_by` varchar(255) DEFAULT NULL COMMENT '最近一次的更新人',
  `last_update_time` timestamp NOT NULL DEFAULT '1980-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT '最近一次的更新时间',
  `prefer_list` text COMMENT '预分配列表',
  `local_mode` tinyint(1) DEFAULT NULL COMMENT '是否启用本地模式',
  `use_disprefer_list` tinyint(1) DEFAULT NULL COMMENT '是否使用非preferList',
  `use_serial` tinyint(1) DEFAULT NULL COMMENT '消息作业是否启用串行消费，默认为并行消费',
  `backup1` varchar(255) DEFAULT NULL COMMENT '备用字段2',
  `backup2` varchar(255) DEFAULT NULL COMMENT '备用字段3',
  `backup3` varchar(255) DEFAULT NULL COMMENT '备用字段2',
  `job_degree` tinyint(1) NOT NULL DEFAULT '0' COMMENT '作业重要等级,0:没有定义,1:非线上业务,2:简单业务,3:一般业务,4:重要业务,5:核心业务',
  `enabled_report` tinyint(1) DEFAULT NULL COMMENT '上报执行信息,1:开启上报,0：不开启上报，对于定时作业，默认开启上报；对于消息作业，默认不开启上报',
  `dependencies` varchar(1000) DEFAULT NULL COMMENT '依赖的作业',
  `groups` varchar(255) DEFAULT NULL COMMENT '所属分组',
  `timeout_4_alarm_seconds` int(11) NOT NULL DEFAULT '0' COMMENT '超时（告警）秒数',
  `time_zone` varchar(255) NOT NULL DEFAULT 'Asia/Shanghai' COMMENT '时区',
  `is_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用标志',
  `job_mode` varchar(255) DEFAULT NULL COMMENT '作业模式',
  `custom_context` varchar(8192) DEFAULT NULL COMMENT '自定义语境参数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_namespace_job_name` (`namespace`,`job_name`),
  KEY `idx_namespace` (`namespace`),
  KEY `idx_zk_list` (`zk_list`),
  KEY `idx_job_name` (`job_name`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for `job_config_history`
-- ----------------------------
CREATE TABLE `job_config_history` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '作业ID主键',
  `job_name` varchar(255) NOT NULL COMMENT '作业名称',
  `job_class` varchar(255) DEFAULT NULL COMMENT '作业类名',
  `sharding_total_count` int(11) DEFAULT NULL COMMENT '作业分片总数',
  `load_level` int(11) NOT NULL DEFAULT '1' COMMENT '每个分片默认负荷',
  `time_zone` varchar(255) DEFAULT NULL COMMENT '时区',
  `cron` varchar(255) DEFAULT NULL COMMENT 'cron表达式',
  `pause_period_date` text COMMENT '停止周期日期',
  `pause_period_time` text COMMENT '停止周期时间',
  `sharding_item_parameters` text COMMENT '分片序列号/参数对照表',
  `job_parameter` text COMMENT '作业参数',
  `monitor_execution` tinyint(1) DEFAULT '1' COMMENT '监控异常',
  `process_count_interval_seconds` int(11) DEFAULT NULL COMMENT '处理总数间隔秒数',
  `concurrent_data_process_thread_count` int(11) DEFAULT NULL COMMENT '当前数据处理线程总数',
  `fetch_data_count` int(11) DEFAULT NULL COMMENT '获取到的数据总数',
  `max_time_diff_seconds` int(11) DEFAULT NULL COMMENT '最大时间相差的秒数',
  `monitor_port` int(11) DEFAULT NULL COMMENT '监控端口',
  `failover` tinyint(1) DEFAULT NULL COMMENT '是否为失效的作业',
  `misfire` tinyint(1) DEFAULT NULL COMMENT '是否为被错过的作业(可能需要触发重发)',
  `job_sharding_strategy_class` varchar(255) DEFAULT NULL COMMENT '作业分片策略类',
  `description` text COMMENT '作业描述',
  `timeout_4_alarm_seconds` int(11) DEFAULT NULL COMMENT '超时（告警）秒数',
  `timeout_seconds` int(11) DEFAULT NULL COMMENT '超时（Kill线程/进程）秒数',
  `show_normal_log` tinyint(1) DEFAULT NULL COMMENT '是否显示正常日志',
  `channel_name` varchar(255) DEFAULT NULL COMMENT '渠道名称',
  `job_type` varchar(255) DEFAULT NULL COMMENT '作业类型',
  `queue_name` text COMMENT '队列名称',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `last_update_by` varchar(255) DEFAULT NULL COMMENT '最近一次的更新人',
  `last_update_time` timestamp NULL DEFAULT NULL COMMENT '最近一次的更新时间',
  `namespace` varchar(255) NOT NULL COMMENT '命名空间',
  `zk_list` varchar(255) DEFAULT NULL COMMENT 'zookeeper连接列表',
  `prefer_list` text COMMENT '预分配列表',
  `local_mode` tinyint(1) DEFAULT NULL COMMENT '是否启用本地模式',
  `use_disprefer_list` tinyint(1) DEFAULT NULL COMMENT '是否使用非preferList',
  `use_serial` tinyint(1) DEFAULT NULL COMMENT '消息作业是否启用串行消费，默认为并行消费',
  `job_degree` tinyint(1) DEFAULT NULL COMMENT '作业重要等级,0:没有定义,1:非线上业务,2:简单业务,3:一般业务,4:重要业务,5:核心业务',
  `enabled_report` tinyint(1) DEFAULT NULL COMMENT '上报执行信息，对于定时作业，默认开启上报；对于消息作业，默认不开启上报',
  `groups` varchar(255) DEFAULT NULL COMMENT '所属分组',
  `dependencies` text COMMENT '依赖的作业',
  `is_enabled` tinyint(1) DEFAULT '0' COMMENT '是否启用标志',
  `job_mode` varchar(255) DEFAULT NULL COMMENT '作业模式',
  `custom_context` varchar(8192) DEFAULT NULL COMMENT '自定义语境参数',
  PRIMARY KEY (`id`),
  KEY `job_name_idx` (`job_name`),
  KEY `namespace_idx` (`namespace`),
  KEY `zk_list_idx` (`zk_list`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='作业配置历史表';


-- ----------------------------
-- Table structure for `saturn_statistics`
-- ----------------------------
CREATE TABLE `saturn_statistics` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '统计表主键ID',
  `name` varchar(255) NOT NULL COMMENT '统计名称，例如top10FailJob',
  `zklist` varchar(255) NOT NULL COMMENT '统计所属zk集群',
  `result` longtext NOT NULL COMMENT '统计结果(json结构)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


-- ----------------------------
-- Table structure for `sys_config`
-- ----------------------------
CREATE TABLE `sys_config` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `property` varchar(255) NOT NULL COMMENT '属性名',
  `value` varchar(2000) NOT NULL COMMENT '属性值',
  PRIMARY KEY (`id`),
  KEY `property_idx` (`property`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='系统配置表';

-- ----------------------------
-- Table structure for `zk_cluster_info`
-- ----------------------------
CREATE TABLE `zk_cluster_info` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除：0，未删除；1，删除',
  `create_time` timestamp NOT NULL DEFAULT '1980-01-01 00:00:00' COMMENT '创建时间',
  `created_by` varchar(255) NOT NULL DEFAULT '' COMMENT '创建人',
  `last_update_time` timestamp NOT NULL DEFAULT '1980-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  `last_updated_by` varchar(255) NOT NULL DEFAULT '' COMMENT '最近更新人',
  `zk_cluster_key` varchar(255) NOT NULL DEFAULT '' COMMENT '集群key值，唯一',
  `alias` varchar(255) NOT NULL DEFAULT '' COMMENT '别名',
  `connect_string` varchar(255) NOT NULL DEFAULT '' COMMENT '连接串',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_zk_cluster_info_zk_cluster_key` (`zk_cluster_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='zk集群信息表';


-- ----------------------------
-- Table structure for `namespace_zkcluster_mapping`
-- ----------------------------
CREATE TABLE `namespace_zkcluster_mapping` (
  `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `is_deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '是否删除：0，未删除；1，删除',
  `create_time` timestamp NOT NULL DEFAULT '1980-01-01 00:00:00' COMMENT '创建时间',
  `created_by` varchar(255) NOT NULL DEFAULT '' COMMENT '创建人',
  `last_update_time` timestamp NOT NULL DEFAULT '1980-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',
  `last_updated_by` varchar(255) NOT NULL DEFAULT '' COMMENT '最近更新人',
  `namespace` varchar(255) NOT NULL DEFAULT '' COMMENT '域名',
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '业务组',
  `zk_cluster_key` varchar(255) NOT NULL DEFAULT '' COMMENT '集群key',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_namespace` (`namespace`),
  KEY `idx_zk_cluster_key` (`zk_cluster_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='域名集群映射表';