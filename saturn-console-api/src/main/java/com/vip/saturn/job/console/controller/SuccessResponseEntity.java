package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.RequestResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Success response entity of controller.
 *
 * @author kfchu
 */
public class SuccessResponseEntity extends ResponseEntity<RequestResult> {

	public SuccessResponseEntity() {
		super(new RequestResult(true), HttpStatus.OK);
	}

	public SuccessResponseEntity(Object responseObj) {
		super(new RequestResult(true, responseObj), HttpStatus.OK);
	}

}
