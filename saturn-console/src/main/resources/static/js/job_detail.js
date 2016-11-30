$(function() {
	var jobName = $("#job-name").text(), $loading = $(".loading"), jobTypeStr = $("#jobTypeFromController").val(),
				$historyConfig = $("#history-config"),$settingsForm = $("#job-settings-form"), $historyConfigTable, 
				detailTmp = $("#detail-block-template").html(), $operationDiv = $("#operation-div"), 
				isJobEnabledVal = $("#isJobEnabledVal").val(), 
				jobStatus = $("#jobStatus").val(), $jobstatusSpan = $("#job-status-span"),confirmDialogMsg = undefined,
				confirmOps,jobConfigShardingTotalCount, regName = $("#regNameFromServer").val();
	$("[data-toggle='tooltip']").tooltip();
	window.parent.setRegName(regName, $("#namespace").val());
	window.parent.reloadTreeAndExpandJob(regName);
	var preferSelectSumo = $('.preferListSelect').SumoSelect({selectAll:true,csvDispCount:10}).sumo;
	
   
    
    setJobTypeClass();
    
    setJobStatusAndIsEnabled();
    
    $('[href="#settings"]').click(function(event) {
    	$loading.show();
        renderSettings(null);
    });
    $('[href="#servers"]').click(function(event) {
    	$loading.show();
        renderServers();
    });
    $('[href="#execution_info"]').click(function(event) {
    	$loading.show();
        renderExecution();
    });
   
    bindSubmitJobSettingsForm();
    bindRunAtOnceButton();
    bindStopAtOnceButton();
    bindLogViewButton();
    bindHistoryConfigButton();
    bindCurrentConfigButton();
    bindRecoverButton();
    bindReloadHistoryConfigButton();
    bindCheckAndForecastCronButton();
    
    if(location.hash) {
    	$('a[href=' + location.hash + ']').tab('show');
    }
    
    showSpecificTabAndRenderServerStatus();
    
	$(document.body).on("click", "a[role]", function(event) {
		location.hash = this.getAttribute("href");
	});

	$(window).on('popstate', function() {
		var anchor = location.hash || $('a[role="presentation"]').first().attr("href");
		$('a[href=' + anchor + ']').tab('show');
	});
	
	/** remove executor only can be done in overview page **/
	$("#remove-znode-confirm-dialog").on('shown.bs.modal', function (event) {
		  var button = $(event.relatedTarget);
		  var executor = button.data('executor');
		  $("#remove-znode-confirm-dialog-confirm-btn").unbind('click').click(function() {
			  var $btn = $(this).button('loading');
			  $.post("job/remove/executor", {jobName: jobName, executor: executor,nns:regName}, function (data) {
		        	$("#remove-znode-confirm-dialog").modal("hide");
		        	if (data == "ok") {
		        		renderServers();
		        		showSuccessDialog();
			  		} else {
			  			showFailureDialogWithMsg("update-failure-dialog", data);
			  		}
			  }).always(function() { $btn.button('reset'); });
			  return false;
		  });
	});
	
	function cleanConfirmInfo() {
		confirmOps = ""; 
		confirmDialogMsg = undefined; 
	}
	$("#toggleEnabled-confirm-dialog").on('shown.bs.modal', function (event) {
		  var button = $(event.relatedTarget);
		  var msg = button.data('msg') || confirmDialogMsg;
		  $("#toggleEnabled-confirm-dialog .confirm-reason").html(msg);
		  $("#toggleEnabled-confirm-dialog-confirm-btn").unbind('click').click(function() {
			  var $btn = $(this).button('loading');
				  $.post("job/toggleJobEnabledState", {jobName : jobName, state: !(isJobEnabledVal == 'true'),nns:regName},function (data) {
		        		$("#toggleEnabled-confirm-dialog").modal("hide");
				  		if (data == "ok") {
				  			showSuccessDialogWithCallback(function() {location.reload(true);});
				  		} else {
				  			showFailureDialogWithMsg("update-failure-dialog", data);
				  		}
				  }).always(function() {cleanConfirmInfo(); $btn.button('reset'); });
			  return false;
		  });
	});
	
	$("#remove-job-confirm-dialog").on('shown.bs.modal', function (event) {
		  var button = $(event.relatedTarget);
		  var msg = button.data('msg') || confirmDialogMsg;
		  $("#remove-job-confirm-dialog .confirm-reason").html(msg);
		  $("#remove-job-confirm-dialog-confirm-btn").unbind('click').click(function() {
			  var $btn = $(this).button('loading');
			  $.post("job/remove/job", {jobName : jobName,nns:regName}, function (data) {
		        	$("#remove-job-confirm-dialog").modal("hide");
		        	if (data == "ok") {
		        		showSuccessDialogWithCallback(function() {window.parent.reloadJobsAfterRemove();window.location = "overview";});
			  		} else {
			  			showFailureDialogWithMsg("update-failure-dialog", data);
			  		}
			  }).always(function() {cleanConfirmInfo(); $btn.button('reset'); });
			  return false;
		  });
	});
	
	$("#stop-at-once-confirm-dialog").on('shown.bs.modal', function (event) {
		  $("#stop-at-once-confirm-dialog-confirm-btn").unbind('click').click(function() {
			  $.post("job/stopAllOneTime", {jobName : jobName,nns:regName}, function (data) {
				    $("#stop-at-once-confirm-dialog").modal("hide");
	            	$loading.hide();
	            	if (data == "ok") {
	            		showSuccessDialogWithCallback(function(){location.reload(true);});
	            	} else {
			  			showFailureDialogWithMsg("update-failure-dialog", data);
	            	}
		      });
		  });
	});
	
	function setJobStatusAndIsEnabled (){
	    switch(jobStatus){
	    case "READY":
	    	$jobstatusSpan.addClass("label-info");
	    	$("#remove-job").addClass("disabled").attr("disabled","disabled").attr("title","ready状态下不可以删除作业");
	    	break;
	    case "STOPPED":
	    	$jobstatusSpan.addClass("label-default");
	    	$("#remove-job").removeAttr("disabled");
	    	$("#remove-job").removeClass("hide");
	    	$("#run-at-once-btn").addClass("disabled").attr("disabled","disabled").attr("title","stopped状态下不可点");
	    	break;
	    case "RUNNING":
	    	$jobstatusSpan.addClass("label-success");
	    	$("#run-at-once-btn").addClass("disabled").attr("disabled","disabled").attr("title","running状态下不可点");
	    	$("#remove-job").addClass("disabled").attr("disabled","disabled").attr("title","running状态下不可以删除作业");
	    	break;
	    case "STOPPING":
	    	$jobstatusSpan.addClass("label-warning");
	    	$("#run-at-once-btn").addClass("disabled").attr("disabled","disabled").attr("title","stopping状态下不可点");
	    	$("#remove-job").addClass("disabled").attr("disabled","disabled").attr("title","stopping状态下不可以删除作业");
	    	break;
	    }
	    if (isJobEnabledVal == 'true') {
	    	$("#isJobEnabled").text("禁用");
	    	$("#isJobEnabled").addClass("btn-warning");
	    } else {
	    	$("#isJobEnabled").text("启用");
	    	$("#isJobEnabled").addClass("btn-success");
	    }
	}
	
	$("#isJobEnabled").on('click', function (event) {
		confirmOps = "toggleEnabled";
		if (isJobEnabledVal == 'true') {
    		confirmDialogMsg = "确认禁用作业吗?";
		} else {
			confirmDialogMsg = "确认启用作业吗?"
		}
		$("#toggleEnabled-confirm-dialog").modal("show");
	});
	$("#localMode").on('click', function (event) {
		if($(this).is(":checked")){
			$("#shardingTotalCount").attr("disabled","disabled").addClass("disabled");
			$("#shardingTotalCount").attr("type","text");
			$("#shardingTotalCount").val("N/A");
        	$("#onlyUsePreferList").hide();
			$("#onlyUsePreferListLabel").hide();
		}else{
			$("#shardingTotalCount").attr("type","number");
			$("#shardingTotalCount").removeAttr("disabled");
			$("#shardingTotalCount").val(jobConfigShardingTotalCount);
        	$("#onlyUsePreferList").show();
			$("#onlyUsePreferListLabel").show();
		}
	});
	/** [作业设置] Tab*/
	function renderSettings(historyId) {
	    $.get("job/settings", {jobName : jobName,nns:regName}, function (data) {
	    	$operationDiv.empty();
    		$operationDiv.append("&nbsp;<button id=\"update-btn\" type=\"submit\" class=\"btn btn-primary\">更新</button>");;
    	    if (isJobEnabledVal == 'true') {
    	    	$("#update-btn").attr("disabled","disabled").addClass("disabled").attr("title","启用状态下的job不能编辑提交");
    	    }
	    	
	    	var jobConfig = data, reinitDate = false, reinitTime = false;

	    	if (jobConfig.jobType === "SHELL_JOB") {
	    		$(".hide-when-is-script-job").hide();
	    		$(".hide-when-is-script-job-but-show-when-is-vshell-job").hide();
	        }
	    	if (jobConfig.jobType === "VSHELL"){
	    		$(".hide-when-is-script-job").hide();
	    		$(".hide-when-is-script-job-but-show-when-is-vshell-job").show();
	    	}
	    	
	    	if (jobConfig.jobType != "MSG_JOB" && jobConfig.jobType != "VSHELL") {
	        	$(".show-when-is-msg-job").hide();
	        } else {
	        	$(".hide-when-is-msg-job").hide();
	        }
	    	jobConfigShardingTotalCount = jobConfig.shardingTotalCount;
	        $("#processCountIntervalSeconds").val(jobConfig.processCountIntervalSeconds);
	        $("#loadLevel").val(jobConfig.loadLevel);
	        if(jobConfig.localMode == true){
	        	$("#localMode").prop("checked",true);
	        	$("#shardingTotalCount").attr("disabled",true).addClass("disabled");
	        	$("#shardingTotalCount").attr("type","text");
	        	$("#shardingTotalCount").val("N/A");
				$("#onlyUsePreferList").hide();
				$("#onlyUsePreferListLabel").hide();
	        }else{
	        	$("#localMode").prop("checked",false);
	        	$("#shardingTotalCount").attr("type","number");
	        	$("#shardingTotalCount").removeAttr("disabled");
	        	$("#shardingTotalCount").val(jobConfig.shardingTotalCount);
				$("#onlyUsePreferList").show();
				$("#onlyUsePreferListLabel").show();
	        }
	        if(jobConfig.useSerial == true){
	        	$("#useSerial").prop("checked",true);
	        }else{
	        	$("#useSerial").prop("checked",false);
	        }
	        var useDispreferList = jobConfig.useDispreferList;
	        if(typeof useDispreferList == "boolean" && useDispreferList == false){
	        	$("#onlyUsePreferList").prop("checked",true);
	        }else{
	        	$("#onlyUsePreferList").prop("checked",false);
	        }
	        $("#cron").val(jobConfig.cron);
	        if(jobConfig.disabled == false){// 启用状态的作业不可编辑（不可调整prefer list)
	        	$(".preferListSelect").attr("disabled", true).addClass("disabled");
	        }else{
	        	$(".preferListSelect").removeAttr("disabled");
	        }
        	var preferListCandidate = jobConfig.preferListCandidate;
        	var preferList = jobConfig.preferList;
        	var hasPreferList = (typeof preferList == "string");
        	if(typeof preferListCandidate == "string"){
        		$(".preferListSelect").empty();// 清空options
        		preferSelectSumo.ul.empty();// 清空select下拉显示的ul
        		preferSelectSumo.caption.html("请选择优先Executor");// 重置复选框显示问题
        		preferSelectSumo.caption.addClass('placeholder');
        		var deletePreferExecutor = "";
        		var preferListCandidateArr = preferListCandidate.split(",");
                for (var i = 0; i < preferListCandidateArr.length; i++) {
                    var preferExecutorCandidate = preferListCandidateArr[i];
                    if(!preferExecutorCandidate){
                    	continue;
                    }
                    if(preferExecutorCandidate.indexOf("已删除") != -1){
                    	deletePreferExecutor += preferExecutorCandidate + ", ";
                    	continue;
                    }
                    var preferExecutorCandidateArr =  preferExecutorCandidate.split("(");
                    var preferExecutorShow = preferExecutorCandidateArr[0]; 
                    preferSelectSumo.add(preferExecutorShow,preferExecutorCandidate,i);// 往preferListSelect添加option
                    if(hasPreferList && preferList.indexOf(preferExecutorShow) != -1){
                    	preferSelectSumo.selectItem(i);// 勾选option
                    }
                }
                preferSelectSumo.selAllState();// 调整selectAll和select的勾选状态
                if(deletePreferExecutor){
                	var deletePreferExecutorStr = deletePreferExecutor.substring(0,deletePreferExecutor.length-2);// 去掉末尾的2个字符逗号和空格", "
                	var preferSelect = preferSelectSumo.E.find(':selected').not(':disabled').text();
                	if(preferSelect){
                		preferSelectSumo.caption.html(preferSelect + ", " + deletePreferExecutorStr);
                	}else{
                		preferSelectSumo.caption.html(deletePreferExecutorStr);
                		preferSelectSumo.caption.removeClass('placeholder');
                	}
                }
        	}
        	
	        $("#shardingItemParameters").val(jobConfig.shardingItemParameters);
	        $("#jobParameter").val(jobConfig.jobParameter);
	        $("#timeoutSeconds").val(jobConfig.timeoutSeconds);
	        $("#showNormalLog").prop("checked", jobConfig.showNormalLog);
	        $("#description").val(jobConfig.description);
	        $("#queueName").val(jobConfig.queueName);
        	$("#channelName").val(jobConfig.channelName);

        	if(jobConfig.pausePeriodDate){
        		$('#pausePeriodDate').tagsInput({width:'auto', defaultText:""});
        		$('#pausePeriodDate').importTags(jobConfig.pausePeriodDate);
        	}else{
        		$('#pausePeriodDate_tagsinput .tag').remove();
        		$('#pausePeriodDate').tagsInput({width:'auto', defaultText:"增加一个日期段"});
        	}
	        if (reinitDate) {
	        	var w = '<span title="当前配置值为：'+ pausePeriodDate +'" style="display: inline;" class="glyphicon glyphicon-warning-sign diff-conf-warning"></span>';
        		$("#pausePeriodDate_tagsinput").append(w).addClass("warnging-tag");
	        } 
	        if(jobConfig.pausePeriodTime){
	        	$("#pausePeriodTime").tagsInput({"min-width":'100px',width:'auto', defaultText:""});
        		$('#pausePeriodTime').importTags(jobConfig.pausePeriodTime);
        	}else{
        		$('#pausePeriodTime_tagsinput .tag').remove();
        		$("#pausePeriodTime").tagsInput({"min-width":'100px',width:'auto', defaultText:"增加一个时间段"});
        	}
	        if (reinitTime) {
	        	var w = '<span title="当前配置值为：'+ pausePeriodTime +'" style="display: inline;" class="glyphicon glyphicon-warning-sign diff-conf-warning"></span>';
        		$("#pausePeriodTime_tagsinput").append(w).addClass("warnging-tag");
	        } 
	        $("#jobClass").val(jobConfig.jobClass);
	    }).always(function() { $loading.hide(); });
	}

	function bindSubmitJobSettingsForm() {
	    $("#job-settings-form").submit(function(event) {
	        event.preventDefault();
	        var shardingTotalCount = $("#shardingTotalCount").val();
	        var shardingItemParameters = $("#shardingItemParameters").val();
	        var localMode = $("#localMode").prop("checked");
	        if(shardingItemParameters && shardingItemParameters.length < 2 || shardingItemParameters.indexOf("=") == -1){
	        	showFailureDialogWithMsg("update-failure-dialog", "作业分片参数有误");
	        	$("#shardingItemParameters").focus();
	        	return;
	        }
	        if(localMode != true){
	        	var shardingItemParametersArr = shardingItemParameters.split(","),realShardingItemParameters = [];
	        	// 去除空白元素
	        	for (var i in shardingItemParametersArr) {
	        		if (shardingItemParametersArr[i].trim() != "") {
	        			realShardingItemParameters.push(shardingItemParametersArr[i]);
	        		}
	        	}
    			shardingItemParametersArr = realShardingItemParameters;
		        if (realShardingItemParameters.length < shardingTotalCount) {
		        	showFailureDialogWithMsg("update-failure-dialog", "分片参数不能小于分片总数。");
		        	$("#shardingItemParameters").focus();
		        	return;
		        }
		        if(shardingItemParameters){
		        	if(shardingItemParameters.substring(0,2) == "*="){
		        		showFailureDialogWithMsg("update-failure-dialog", "作业分片参数有误");
		        		$("#shardingItemParameters").focus();
		        		return;
		        	}
		        	var shardingItemStr = "";
		        	for(var i=0;i<shardingItemParametersArr.length;i++){
		        		var kvPare = shardingItemParametersArr[i].split("=");
		        		if (kvPare.length < 2) {
		        			alert("作业分片参数有误，必需是key=value的形式");
				        	return;
		        		}
		        		var shardingItem = kvPare[0];
		        		if(shardingItemStr.indexOf(shardingItem) != -1){
		        			showFailureDialogWithMsg("update-failure-dialog", "作业分片参数有误，分片号不能相同");
			        		$("#shardingItemParameters").focus();
		        			return;
		        		}
						shardingItemStr += shardingItem + ",";
		        	}
		        }
		    }else{
		    	if(shardingItemParameters && shardingItemParameters.substring(0,2) != "*="){
		        	showFailureDialogWithMsg("update-failure-dialog", "作业分片参数有误，对于本地模式的作业，只需要输入如：*=a 即可。");
		        	$("#shardingItemParameters").focus();
		        	return;
		        }
		    	shardingTotalCount = 1;// 本地模式给作业分片总数设为1，方便恢复非本地模式默认作业分片总数的显示以及防止Integer.parseInt报错，实际该项无论设为何值都不影响sharding分片
		    }
	        var useSerial = $("#useSerial").prop("checked");
	        if(useSerial == true && shardingTotalCount != "1"){
	        	showFailureDialogWithMsg("update-failure-dialog", "串行消费只支持单分片的场景，如果启用串行消费，请修改作业分片数为1");
        		$("#shardingTotalCount").focus();
        		return;
	        }
	        var pausePeriodDate = $("#pausePeriodDate").val().replace(/(^\s*)|(\s*$)/g, "");
	        var re = /^(\s)*$|^(\d{1,2}\/\d{1,2}-\d{1,2}\/\d{1,2}(,\d{1,2}\/\d{1,2}-\d{1,2}\/\d{1,2})*)$/
	        if(re.test(pausePeriodDate) == false) {
	        	showFailureDialogWithMsg("update-failure-dialog", "修改暂停日期时间段失败，请严格遵守输入格式");
	        	return;
	        }
	        var pausePeriodTime = $("#pausePeriodTime").val().replace(/(^\s*)|(\s*$)/g, "");
	        var re = /^(\s)*$|^(\d{1,2}:\d{1,2}-\d{1,2}:\d{1,2}(,\d{1,2}:\d{1,2}-\d{1,2}:\d{1,2})*)$/
	        if(re.test(pausePeriodTime) == false) {
	        	showFailureDialogWithMsg("update-failure-dialog", "修改暂停小时分钟时间段失败，请严格遵守输入格式");
	        	return;
	        }
	        var loadLevel = $("#loadLevel").val();
	        var cron = $("#cron").val();
	        var jobClass = $("#jobClass").val();
	        var jobParameter = $("#jobParameter").val().trim();
	        var processCountIntervalSeconds = $("#processCountIntervalSeconds").val();
	        var failover = true;
	        if(localMode == true){
	        	failover = false;
	        }
	        var onlyUsePreferList = $("#onlyUsePreferList").prop("checked");
	        var description = $("#description").val();
	        var timeoutSeconds = $("#timeoutSeconds").val();
	        var showNormalLog = $("#showNormalLog").prop("checked");
	        var queueName = $("#queueName").val();
	        var channelName = $("#channelName").val();
	        var preferList = preferSelectSumo.getSelStr();
	        $.post("job/settings", {channelName: channelName, loadLevel:loadLevel, localMode:localMode,useSerial:useSerial,useDispreferList:!onlyUsePreferList, queueName: queueName, 
	        	showNormalLog: showNormalLog, jobName: jobName, jobClass : jobClass, shardingTotalCount: shardingTotalCount, 
	        	jobParameter: jobParameter, cron: cron, pausePeriodDate: pausePeriodDate, pausePeriodTime: pausePeriodTime, processCountIntervalSeconds: processCountIntervalSeconds, 
	        	failover: failover, shardingItemParameters: shardingItemParameters, description: description,
	        	timeoutSeconds:timeoutSeconds,preferList:preferList,nns:regName}, function(data) {
		            if(data.success == true) {
		            	showSuccessDialog();
		            	setTimeout("window.parent.location.reload(true)", 1000)
		            } else {
		            	showFailureDialogWithMsg("update-failure-dialog", data.message);
		            }
	        });
	    });
	}
	
	function ifNullReturnEmpty(str) {
		if (!str || str === 'null') {
			return '';
		}
		return str;
	}
	/** [作业服务器] Tab*/
	function renderServers(){
	    $.get("job/servers", {jobName : jobName,nns:regName}, function (data) {
	    	var $rightArea = $("#servers").find(".pull-right");
	    	$rightArea.empty();
	    	
	        $("#servers tbody").empty();
	        var allCrashed = true, allStopped = true, statusLabel,
	        	enabledAllRunOnTimeWhenThereGotReadyServer = false,enabledAllStopOnTimeWhenThereGotStoppedJob = false,list = data;
	        for (var i = 0;i < list.length;i++) {
	        	if(i == 0 && list[i].jobStatus != ""){// it needs only read the first jobStatus
		    		jobStatus = list[i].jobStatus;
		    	}
	        	var sharding = list[i].sharding, status = list[i].status, leader = list[i].leader;
	            // only the ready-status job can see the run-one-time button.
	            if (status == "ONLINE" && jobStatus === "READY") {
	            	enabledAllRunOnTimeWhenThereGotReadyServer = true;
	            }
	            if (jobStatus === "STOPPING") {
	            	enabledAllStopOnTimeWhenThereGotStoppedJob = true;
	            }
	        	if ("OFFLINE" === status) {
	        		statusLabel = "label-warning";
	        	} else {
	        		statusLabel = "label-success";
	        	}
	            var baseTd = "<td>" + list[i].executorName + "</td><td>" 
	            	+ list[i].ip + "</td><td><span class='label "
	            	+ statusLabel + "'>" + status + "</span></td><td title='统计周期可配置(参考作业配置)'>" 
		            + list[i].processSuccessCount + "</td><td title='统计周期可配置(参考作业配置)'>" + list[i].processFailureCount 
		            + "</td><td>" + list[i].percentage 
		            + "</td><td>" + sharding + "</td><td>" + list[i].version + "</td>";
	            
	            var trClass = "";
	            if ("ONLINE" === status) {
	                trClass = "success";
	                if (jobStatus === "STOPPED" || sharding ==  "") {// 过滤未得到分片的executor，作业状态是STOPPED也要过滤(由于重分片代码逻辑或延时问题，可能造成STOPPED状态下也可能有分片，正常情况下是不应该有分片的)
	                	trClass = "hide";
	                }
	            } else {
	                trClass = "warning hide";// 过滤offline的executor
	            }
	            
	            $("#servers tbody").append("<tr class='" + trClass + "'>" + baseTd + "</tr>");
		    	if (jobTypeStr == "MSG_JOB" ) {
		        	$(".hide-when-is-msg-job").hide();
		    	}
	        }
        	if (jobTypeStr == "MSG_JOB" ) {
        		$("#run-at-once-btn").remove();
        		if(isJobEnabledVal == 'true') {
        			$("#stop-at-once-btn").attr("disabled", true).addClass("disabled").attr("title", "作业不处于禁用状态，不能强行终止作业.");
        		}
        	} else {
				// 无ready机器时禁用全部执行一次按钮
				if (!enabledAllRunOnTimeWhenThereGotReadyServer) {
					$("#run-at-once-btn").attr("disabled", true).addClass("disabled").attr("title", "没有online executor或job不处于ready状态.");
				}
				if (!enabledAllStopOnTimeWhenThereGotStoppedJob) {
					$("#stop-at-once-btn").attr("disabled", true).addClass("disabled").attr("title", "作业不处于stopping状态，不能强行终止作业.");
				}
	        }
	    }).always(function() { $loading.hide(); });
	}

	/** [立即执行] 按钮*/
	function bindRunAtOnceButton() {
	    $(document).on("click", "#run-at-once-btn", function(event) {
	        $.post("job/runAllOneTime", {jobName : jobName,nns:regName}, function (data) {
            	$loading.hide();
            	if (data == "ok") {
            		showSuccessDialogWithCallback(function(){location.reload(true);});
            	} else {
		  			showFailureDialogWithMsg("update-failure-dialog", data);
            	}
	        });
	    });
	}
	
	/** [立刻终止] 按钮*/
	function bindStopAtOnceButton() {
	    $(document).on("click", "#stop-at-once-btn", function(event) {
			$("#stop-at-once-confirm-dialog").modal("show");
	    });
	}
	
	/** [查看日志] 按钮 */
	function bindLogViewButton() {
	    $(document).on("click", ".logBtn", function(event) {
	        $.get("job/logs", {jobName : jobName, item : $(this).attr("jobItem"),nns:regName}, function (data) {
	        	$('#myModal').modal();
	        	if(typeof data == "object"){
	        		$("#logMsg").val(data.logMsg);
	        	}
	        });
	    });
	}

	/** [作业运行状态] Tab*/
	function renderExecution() {
	    $.get("job/execution", {jobName : jobName,nns:regName}, function (data) {
	        $("#execution tbody").empty();
	        for (var i = 0;i < data.length;i++) {
	            var status = data[i].status;
	            var jobMsg = null == data[i].jobMsg? "-" : data[i].jobMsg;
	            var runningIp = data[i].runningIp;
	            var falioverExecutor = null == data[i].failoverExecutor ? "-" : data[i].failoverExecutor;
	            var lastBeginTime = null == data[i].lastBeginTime ? "-" : data[i].lastBeginTime;
	            var executionTime;
	            if(!data[i].lastCompleteTime || status === "RUNNING"){
	            	executionTime = lastBeginTime + " - ";
	            }else{
	            	executionTime = lastBeginTime+ " - " + data[i].lastCompleteTime;
	            }
	            var nextFireTime = null == data[i].nextFireTime ? "-" : data[i].nextFireTime;
	            var pausePeriodEffected = new Boolean(data[i].pausePeriodEffected);
	            var trClass = "";
	            if ("RUNNING" === status) {
	                trClass = "success";
	            } else if ("COMPLETED" === status) {
	                trClass = "info";
	            } else if ("PENDING" === status) {
	                trClass = "warning";
	            } else if ("FAILED" === status) {
	                trClass = "danger";
	            }
	            var nextFireTimeStr = (pausePeriodEffected == true ? "<font color='red' title='被暂停时间段所影响'>": "") + nextFireTime + (pausePeriodEffected == true ? "</font>" : "");
	            if( data[i].timeConsumed >= 60) {
	            	trClass = "danger";
	            	var baseTd = "<td><span class='glyphicon glyphicon-exclamation-sign red-color' >" + data[i].item + "</span></td>" 
	            		+ "<td>" + status + "</td>" 
	            		+ "<td>" + jobMsg + "</td>" 
	            		+ "<td>" + runningIp + "</td>" 
	            		+ "<td>" + executionTime + "</td>" 
	            		+ "<td class='hide-when-is-msg-job'>" + nextFireTimeStr + "</td>" 
	            		+ "<td><button class='logBtn btn btn-info' + jobItem='" + data[i].item + "' title='查看作业分片日志'>查看" + "</button></td>";
	            	$("#execution tbody").append("<tr  title='该任务已运行" + data[i].timeConsumed + "秒。' class='" + trClass + "'>" + baseTd + "</tr>");
	            } else {
	            	var baseTd = "<td>" + data[i].item + "</td>" 
	            		+ "<td>" + status + "</td>" 
	            		+ "<td>" + jobMsg + "</td>" 
	            		+ "<td>" + runningIp + "</td>"
	            		+ "<td>" + executionTime + "</td>" 
	            		+ "<td class='hide-when-is-msg-job'>"+ nextFireTimeStr + "</td>" 
	                	+ "<td><button class='logBtn btn btn-info' + jobItem='" + data[i].item + "' title='查看作业分片日志'>查看" + "</button></td>";
	            	$("#execution tbody").append("<tr class='" + trClass + "'>" + baseTd + "</tr>");
	            }
	            if (jobTypeStr == "MSG_JOB" ) {
		        	$(".hide-when-is-msg-job").hide();
		    	}
	        }
	    }).always(function() { $loading.hide(); });
	}
	/** history config 按钮*/
	function bindHistoryConfigButton() {
	    $(document).on("click", "#show-history-config", function(event) {
	    	$settingsForm.hide();
	    	$historyConfig.show();
	    	// 恢复当前配置，防止当前配置显示的是恢复属性
	    	$("#update-btn").text("更新");
	    	$(".diff-conf-warning").hide();
	    	$("input").removeClass("waning-border");
	    	$("textarea").removeClass("waning-border");
	    	renderHistoryTable();
	    });
	}
	
	function renderHistoryTable() {
    	 $historyConfigTable = $("#history-config-table").DataTable({ 
    		"destroy": true,
         	"oLanguage": language,
            "iDisplayLength": 10,//每页显示10条
            "bProcessing": true,
            "ajax": {
                "url": "job/loadHistoryConfig",
                "type": "POST",
                "deferRender": true,
                "data": {jobName:jobName},
                "dataSrc": "data"
            },
            "rowCallback": function( row, data, displayIndex, displayIndexFull ) {
            	$(row).attr("title","单击查看详细配置").attr("dataIndex",displayIndex);
            },
            "fnCreatedRow": function(nRow, aData, iDataIndex) {
            	if (jobTypeStr == "MSG_JOB") {
            		$('td:eq(3)', nRow).html((!aData.useDispreferList).toString());
            		$('td:eq(6)', nRow).html(aData.queueName);
            		$('td:eq(7)', nRow).html(aData.channelName);
                	$('td:eq(8)', nRow).html(aData.lastUpdateBy);
                	$('td:eq(9)', nRow).html(new Date(aData.lastUpdateTime).format("yyyy-MM-dd HH:mm:ss"));
                	$('td:eq(10)', nRow).html("<button class='recover-btn btn btn-primary' data-id='"+aData.id+"'>恢复</button>");
            	}else{
            		$('td:eq(3)', nRow).html((!aData.useDispreferList).toString());
                	$('td:eq(7)', nRow).html(aData.lastUpdateBy);
                	$('td:eq(8)', nRow).html(new Date(aData.lastUpdateTime).format("yyyy-MM-dd HH:mm:ss"));
                	$('td:eq(9)', nRow).html("<button class='recover-btn btn btn-primary' data-id='"+aData.id+"'>恢复</button>");
                	$('td:eq(10)', nRow).remove();
            	}
            },
            "columns": [
                        { "data": "rownum" },
                        { "data": "loadLevel" },
                        { "data": "localMode" },
                        { "data": "useDispreferList" },
		                { "data": "shardingTotalCount" },
		                { "data": "timeoutSeconds" },
		                { "data": "cron" },
		                { "data": "queueName" },
		                { "data": "channelName" },
		                { "data": "lastUpdateBy" },
		                { "data": "lastUpdateTime" }
            ],
            "initComplete": function(settings, json) {
            	afterRenderHistoryData();
            },
            "drawCallback": function( settings, action, redraw ) {
            	afterRenderHistoryData();
            },
            "search": {"sSearch": "", "bSmart": true}
         });
	}
	
	function afterRenderHistoryData() {
    	$(".show-when-is-msg-job-in-history").hide();
    	$(".hide-when-is-msg-job-in-history").show();
		$(".hide-when-is-script-job-in-history").show();
		if (jobTypeStr == "MSG_JOB" || jobTypeStr == "VSHELL") {
        	$(".show-when-is-msg-job-in-history").show();
        	$(".hide-when-is-msg-job-in-history").hide();
    	} else if (jobTypeStr == "SHELL_JOB" || jobTypeStr == "VSHELL") {
    		$(".hide-when-is-script-job-in-history").hide();
    	}
	}
	
	$('#history-config-table').on('click', ' tbody tr',
	    function() {
	        var nTr = $(this);
	        var instance = $historyConfigTable.context[0].oInstance;
	        if (instance.fnIsOpen(nTr)) //判断是否已打开
	        {
	            /* This row is already open - close it */
	            //$(this).addClass("fa-plus").removeClass("fa-minus");
	            instance.fnClose(nTr);
	        } else {
	            /* Open this row */
	            //$(this).addClass("fa-minus").removeClass("fa-plus");
	        	var index = $(this).attr("dataIndex");
	        	var aData = instance.fnGetData(this);
	        	if (aData) {
		            	var detailNode = Mustache.render(detailTmp,{
		            		jobClass:aData.jobClass,
		            		localMode:aData.localMode,
		            		onlyUsePreferList:(!aData.useDispreferList).toString(),
		            		processCountIntervalSeconds:aData.processCountIntervalSeconds,
			            	pausePeriodDate:aData.pausePeriodDate,
			            	pausePeriodTime:aData.pausePeriodTime,
			            	showNormalLog:aData.showNormalLog,
			            	jobParameter:aData.jobParameter,
			            	shardingItemParameters:aData.shardingItemParameters,
			            	description:aData.description,
			            	createBy:aData.createBy,
			            	createTime:new Date(aData.createTime).format("yyyy-MM-dd HH:mm:ss"),
			            	namespace:aData.namespace,
			            	zkList:aData.zkList
		            });
		            instance.fnOpen(nTr, detailNode, 'details');
		            afterRenderHistoryData();
	        	}
	        }
    });

	/** checkAndForecastCron按钮 */
	function bindCheckAndForecastCronButton() {
		 $(document).on("click", "#check-and-forecast-cron", function(event) {
			$.post("job/checkAndForecastCron", {cron : $("#cron").val()}, function (data) {
				var msg = "检验结果：";
				if(data.success == true) {
					msg += "成功";
					msg += "<hr>";
					msg += "预测执行时间点：<br><br>";
					msg += data.message;
				} else {
					msg += "失败";
					msg += "<hr>";
					msg += "错误信息：<br><br>";
					msg += data.message;
				}
				showPromptDialogWithMsgAndCallback("check-and-forecast-cron-prompt-dialog", msg, null);
			});
		});
	}

	/** reload history config 按钮*/
	function bindReloadHistoryConfigButton() {
	    $(document).on("click", "#reload-history-config", function(event) {
	    	renderHistoryTable();
	    });
	}
	
	/** current config 按钮*/
	function bindCurrentConfigButton() {
	    $(document).on("click", "#show-current-config", function(event) {
	    	$settingsForm.show();
	    	$historyConfig.hide();
	    });
	}
	/** 恢复 按钮*/
	function bindRecoverButton() {
	    $(document).on("click", ".recover-btn", function(event) {
	    	$settingsForm.show();
	    	$historyConfig.hide();
	    	renderSettings($(this).attr("data-id"));
	    });
	}
	// 高级配置
	$('.collapse').on('hidden.bs.collapse', function () {
		$(this).parent().find(".fa").removeClass("fa-minus").addClass("fa-plus");
	}).on('shown.bs.collapse', function () {
		$(this).parent().find(".fa").addClass("fa-minus").removeClass("fa-plus");
	});
	
	function showSpecificTabAndRenderServerStatus() {
    	$loading.show();
		if ($("#settings").is(':visible')) {
	        renderSettings(null);
		} else if ($("#execution_info").is(':visible')) {
	        renderExecution();
		}
        // job的button功能需要服务器状态支持
    	renderServers();
	}
	
	function setJobTypeClass(){
		if (jobTypeStr === "MSG_JOB") {
        	$("#job-type-class").addClass("fa fa-reorder");
        } else if (jobTypeStr === "SHELL_JOB") {
        	$("#job-type-class").addClass("devicon devicon-linux-plain");
        } else if (jobTypeStr === "VSHELL") {
        	$("#job-type-class").addClass("devicon devicon-linux-plain");
        } else {
        	$("#job-type-class").addClass("devicon devicon-java-plain");
        }
	}

});
