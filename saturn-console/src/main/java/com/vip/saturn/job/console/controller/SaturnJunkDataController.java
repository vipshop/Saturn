/**
 * Copyright 2016 vip.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */   
package com.vip.saturn.job.console.controller;   

import java.util.Collection;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Strings;
import com.vip.saturn.job.console.domain.SaturnJunkData;
import com.vip.saturn.job.console.service.SaturnJunkDataService;

/** 
 * @author yangjuanying  
 */
@Controller
@RequestMapping("/")
public class SaturnJunkDataController extends AbstractController {

	private final static Logger LOGGER = LoggerFactory.getLogger(SaturnJunkDataController.class);
	
	@Resource
	private SaturnJunkDataService saturnJunkDataService;
	
	@RequestMapping(value = "junkdata",method = RequestMethod.GET)
	public String junkdata(HttpServletRequest request,HttpSession session,ModelMap model) {
		return "junkdata";
    }
    
    @ResponseBody
    @RequestMapping(value = "getJunkdata",method = RequestMethod.GET)
	public Collection<SaturnJunkData> getJunkData(HttpServletRequest request,HttpSession session,ModelMap model,String zkAddr) {
		return saturnJunkDataService.getJunkData(zkAddr);
    }
    
    @ResponseBody
    @RequestMapping(value = "removeJunkData",method = RequestMethod.POST)
	public String removeJunkData(HttpServletRequest request,SaturnJunkData saturnJunkData,HttpSession session) {
		return removeOneJunkData(saturnJunkData,session);
    }
    
    private String removeOneJunkData(SaturnJunkData saturnJunkData,HttpSession session) {
		try {
			LOGGER.info("[removed junk data {}.]", saturnJunkData);
			if (Strings.isNullOrEmpty(saturnJunkData.getPath()) || saturnJunkData.getType() == null) {
				LOGGER.info("[{} junk data param error, can't be removed.]", saturnJunkData);
				return "传入参数有误，清理废弃数据("+saturnJunkData.getPath()+")失败";
			}
			return saturnJunkDataService.removeSaturnJunkData(saturnJunkData);
		} catch (Exception e) {
			LOGGER.error("removeOneExecutor exception:", e);
			return "清理废弃数据("+saturnJunkData.getPath()+")出现异常："+ e.getMessage();
		}
	}
}
  