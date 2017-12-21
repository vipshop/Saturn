package com.vip.saturn.job.console.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.vip.saturn.job.console.domain.RequestResult;
import com.vip.saturn.job.console.domain.ZkCluster;

@Controller
@RequestMapping("/home")
public class HomePageController extends AbstractController {

	@RequestMapping(value = "/getNamespaces", method = RequestMethod.GET)
	public ResponseEntity<Object> getNamespaces(final HttpServletRequest request, final String keyword) {
		RequestResult requestResult = new RequestResult();
		try {
			Collection<ZkCluster> zkClusterList = registryCenterService.getZkClusterList();
			
			Map<String, Object> obj = new HashMap<>();
			obj.put("namespaces", zkClusterList);
			requestResult.setObj(obj);
			requestResult.setSuccess(true);
		} catch (Exception e) {
			requestResult.setSuccess(false);
			requestResult.setMessage(e.toString());
		}
		
		return new ResponseEntity<Object>(requestResult, HttpStatus.OK);
	}
	
}
