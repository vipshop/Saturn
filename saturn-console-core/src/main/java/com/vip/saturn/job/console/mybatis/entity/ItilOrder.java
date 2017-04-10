package com.vip.saturn.job.console.mybatis.entity;

import java.util.Date;

/**
 * @author xiaopeng.he
 */
public class ItilOrder {

    private static final long serialVersionUID = 1l;

    private Long id;
    private String serialno;
    private String applier;
    private Date apply_time;
    private String domain;
    private String change_type;
    private String event_serialnos;
    private String status;
    private String url;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getSerialno() {
		return serialno;
	}
	public void setSerialno(String serialno) {
		this.serialno = serialno;
	}
	public String getApplier() {
		return applier;
	}
	public void setApplier(String applier) {
		this.applier = applier;
	}
	public Date getApply_time() {
		return apply_time;
	}
	public void setApply_time(Date apply_time) {
		this.apply_time = apply_time;
	}
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public String getChange_type() {
		return change_type;
	}
	public void setChange_type(String change_type) {
		this.change_type = change_type;
	}
	public String getEvent_serialnos() {
		return event_serialnos;
	}
	public void setEvent_serialnos(String event_serialnos) {
		this.event_serialnos = event_serialnos;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

}
