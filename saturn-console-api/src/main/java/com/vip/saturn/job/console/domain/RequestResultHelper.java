package com.vip.saturn.job.console.domain;

/**
 * @author hebelala
 */
public class RequestResultHelper {

	private static final int SUCCESS = 0;
	private static final int FAILURE = 1;
	private static final int REDIRECT = 2;

	public static RequestResult success() {
		RequestResult requestResult = new RequestResult();
		requestResult.setStatus(SUCCESS);
		return requestResult;
	}

	public static RequestResult success(Object obj) {
		RequestResult requestResult = success();
		requestResult.setObj(obj);
		return requestResult;
	}

	public static RequestResult failure(String message) {
		RequestResult requestResult = new RequestResult();
		requestResult.setStatus(FAILURE);
		requestResult.setMessage(message);
		return requestResult;
	}

	public static RequestResult redirect(String url) {
		RequestResult requestResult = new RequestResult();
		requestResult.setStatus(REDIRECT);
		requestResult.setObj(url);
		return requestResult;
	}

	public static boolean isSuccess(RequestResult requestResult) {
		return requestResult != null && requestResult.getStatus() == SUCCESS;
	}

	public static boolean isFailure(RequestResult requestResult) {
		return requestResult != null && requestResult.getStatus() == FAILURE;
	}

}
