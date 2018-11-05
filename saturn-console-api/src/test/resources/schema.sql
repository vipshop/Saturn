create table saturn_domain_process_history
(
	id int auto_increment
		primary key,
	zk_cluster varchar(256) null ,
	success_count int null ,
	fail_count int null ,
	record_date varchar(256) null ,
	constraint saturn_domain_process_history_zk_cluster_record_date_uindex
		unique (zk_cluster, record_date)
);