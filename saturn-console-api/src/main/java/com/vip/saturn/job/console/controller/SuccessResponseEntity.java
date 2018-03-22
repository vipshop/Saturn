package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.RequestResultHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Success response entity of controller.
 *
 * @author kfchu
 */
public class SuccessResponseEntity extends ResponseEntity<RequestResult> {

	public SuccessResponseEntity() {
		super(RequestResultHelper.success(), HttpStatus.OK);
	}

	public SuccessResponseEntity(Object responseObj) {
		super(RequestResultHelper.success(responseObj), HttpStatus.OK);
	}

}
