package com.vip.saturn.job.console.controller.rest;

import com.vip.saturn.job.console.controller.AbstractController;
import com.vip.saturn.job.console.domain.RestApiErrorResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Handler for RESTful API Exception.
 * <p>
 * Created by jeff.zhu on 26/05/2017.
 */
@ControllerAdvice
public class RestApiExceptionHandlerController {

	private final static String NOT_EXISTED_PREFIX = "does not exists";

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

	@ExceptionHandler
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public RestApiErrorResult handleMessageNotReadableException(HttpMessageNotReadableException e) {
		return new RestApiErrorResult("The http message is not readable. Please check your request.");
	}

	@ExceptionHandler
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public RestApiErrorResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		return new RestApiErrorResult("Method or argument not valid");
	}

	@ExceptionHandler
	@ResponseBody
	@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
	public RestApiErrorResult handleMessageNotReadableException(HttpMediaTypeNotSupportedException e) {
		return new RestApiErrorResult("Media type not supported.");
	}

	private ResponseEntity<Object> constructErrorResponse(String errorMsg, HttpStatus status) {
		HttpHeaders httpHeaders = new HttpHeaders();

		RestApiErrorResult restApiErrorResult = new RestApiErrorResult();
		restApiErrorResult.setMessage(errorMsg);

		return new ResponseEntity<Object>(restApiErrorResult, httpHeaders, status);
	}
}
