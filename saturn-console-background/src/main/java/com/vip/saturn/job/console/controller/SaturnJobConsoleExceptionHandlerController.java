package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.RestApiErrorResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Handler for SaturnJobConsoleException.
 *
 * Created by jeff.zhu on 26/05/2017.
 */
@ControllerAdvice
public class SaturnJobConsoleExceptionHandlerController extends AbstractController {

	public final static String NOT_EXISTED_PREFIX = "does not exists";

	@ExceptionHandler
	public ResponseEntity<Object> handleSaturnJobConsoleException(SaturnJobConsoleException e) {
		String message = e.getMessage();
		if (StringUtils.isBlank(message)) {
			message = e.toString();
		}

		if (message.contains(NOT_EXISTED_PREFIX)) {
			return constructErrorResponse(message, HttpStatus.NOT_FOUND);
		}

		return constructErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler
	public ResponseEntity<Object> handleSaturnJobConsoleHttpException(SaturnJobConsoleHttpException e) {
		HttpHeaders httpHeaders = new HttpHeaders();

		SaturnJobConsoleHttpException saturnJobConsoleHttpException = (SaturnJobConsoleHttpException) e;
		int statusCode = saturnJobConsoleHttpException.getStatusCode();

		if (statusCode == HttpStatus.CREATED.value()) {
			return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
		}

		return constructErrorResponse(e.getMessage(), HttpStatus.valueOf(statusCode));
	}

	private ResponseEntity<Object> constructErrorResponse(String errorMsg, HttpStatus status) {
		HttpHeaders httpHeaders = new HttpHeaders();

		RestApiErrorResult restApiErrorResult = new RestApiErrorResult();
		restApiErrorResult.setMessage(errorMsg);

		return new ResponseEntity<Object>(restApiErrorResult, httpHeaders, status);
	}
}
