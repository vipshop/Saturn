package com.vip.saturn.job.console.controller;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.mybatis.service.RegistryCenterConfigService;

/**
 * 
 * @author hebelala
 *
 */
@Controller
@RequestMapping("/registryCenterConfig")
public class RegistryCenterConfigController {
	
	public final static String BAD_REQ_MSG_PREFIX = "Invalid request.";
	
	public final static String MISSING_REQUEST_MSG = BAD_REQ_MSG_PREFIX + " Missing parameter: {%s}";
	
	@Resource
	private RegistryCenterConfigService registryCenterConfigService;
	
	@RequestMapping(value = "/connectString", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> getConnectString(String namespace) throws SaturnJobConsoleException {
		HttpHeaders headers = new HttpHeaders();
        try {
        	if (StringUtils.isBlank(namespace)) {
                throw new SaturnJobConsoleHttpException(HttpStatus.BAD_REQUEST.value(), String.format(MISSING_REQUEST_MSG, "namespace"));
            }
        	String connectString = registryCenterConfigService.getConnectString(namespace);
        	if(connectString == null) {
        		throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "The connectString is not configured for " + namespace);
        	}
        	return new ResponseEntity<Object>(connectString, headers, HttpStatus.OK);
        } catch (SaturnJobConsoleException e) {
            throw e;
        } catch (Exception e) {
            throw new SaturnJobConsoleHttpException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), e);
        }
    }

}
