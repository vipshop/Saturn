INSERT INTO `zk_cluster_info`(`zk_cluster_key`, `alias`, `connect_string`) VALUES('cluster1', '集群1', 'localhost:2181');

INSERT INTO `namespace_zkcluster_mapping`(`namespace`, `name`, `zk_cluster_key`) VALUES('mydomain', '业务组', 'cluster1');

INSERT INTO sys_config(property,value) values('CONSOLE_ZK_CLUSTER_MAPPING','CONSOLE-IT:it_cluster;default:cluster1,it_cluster');