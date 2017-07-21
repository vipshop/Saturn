-- the first line is kept, see SaturnAutoBasic.prepareForItSql()
INSERT INTO `namespace_zk_cluster_mapping`(`namespace`, `name`, `cluster_key`) VALUES('it-saturn-java.vip.vip.com', 'it', 'it_cluster');
INSERT INTO job_config(job_name, namespace) VALUES ('toBeupdatedITJob', 'it-saturn-java.vip.vip.com');
INSERT INTO job_config(job_name, namespace) VALUES ('updateCronITJob', 'it-saturn-java.vip.vip.com');
INSERT INTO job_config(job_name, namespace) VALUES ('updateConfigITJob', 'it-saturn-java.vip.vip.com');
INSERT INTO job_config(job_name, namespace) VALUES ('updatePauseDate', 'it-saturn-java.vip.vip.com');
INSERT INTO job_config(job_name, namespace) VALUES ('updateShowNormalLog', 'it-saturn-java.vip.vip.com');