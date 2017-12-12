package com.vip.saturn.job.console.controller;

import com.vip.saturn.job.console.domain.RestApiErrorResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller to handle exception.
 * <p>
 * Created by jeff.zhu on 22/05/2017.
 */
@ControllerAdvice
public class ExceptionHandlerController extends AbstractController {

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
}
