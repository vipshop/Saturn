-- the first line is kept, see SaturnAutoBasic.prepareForItSql()
INSERT INTO `namespace_zkcluster_mapping`(`namespace`, `name`, `zk_cluster_key`) VALUES('it-saturn', 'it', 'it_cluster');
INSERT INTO job_config(job_name, namespace) VALUES ('toBeupdatedITJob', 'it-saturn');
INSERT INTO job_config(job_name, namespace) VALUES ('updateCronITJob', 'it-saturn');
INSERT INTO job_config(job_name, namespace) VALUES ('updateConfigITJob', 'it-saturn');
INSERT INTO job_config(job_name, namespace) VALUES ('updatePauseDate', 'it-saturn');
INSERT INTO job_config(job_name, namespace) VALUES ('updateShowNormalLog', 'it-saturn');