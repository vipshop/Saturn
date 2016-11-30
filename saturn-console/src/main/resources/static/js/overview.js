var regName = $("#regNameFromServer").val(), jobsViewDataTable, serversViewDataTable,
	executorsViewDataTable, $loading, opExecutorNames = new Array(),
	opAddJobNames = new Array(),
	jobOperation = '<div id="jobviews-operation-area"><button class="btn btn-success change-jobStatus-batch-job" id="batch-enable-job" title="点击进行批量启用作业">启用作业</button>&nbsp;&nbsp;'+
               	'<button class="btn btn-warning change-jobStatus-batch-job" id="batch-disable-job" title="点击进行批量禁用作业">禁用作业</button>&nbsp;&nbsp;'+
               	'<button class="btn btn-danger" id="batch-remove-job" title="点击进行批量删除作业">删除作业</button>&nbsp;&nbsp;'+
               	'<button class="btn btn-info" id="add-job" title="点击进行添加作业">添加作业</button>&nbsp;&nbsp;' +
               	'<button class="btn btn-info" id="batch-add-job" title="点击进行导入作业">导入作业</button>&nbsp;&nbsp;' +
               	'<a class="btn btn-info" id="export-job" href="executor/exportJob?nns='+regName+'" title="点击进行导出全域作业" target="_self">导出全域作业</a>&nbsp;&nbsp;' +
               	'<button class="btn btn-primary" id="copy-job" title="点击进行复制作业">复制作业</button>&nbsp;&nbsp;</div>',
	executorOperation = '<div id="serverviews-status-showall"><button class="btn btn-danger" id="batch-remove-executor" title="点击进行批量删除Executor">删除Executor</button>&nbsp;&nbsp;</div>';


$(function() {

	$loading = $("#loading");
	bindSubmitExecutorAddJobsForm();
	
	// 下次触发时间排序
	jQuery.fn.dataTableExt.oSort['zn-datetime-asc']  = function(a,b) {
		// a = a.replace(/-/g,"/");
		var dateA = new Date(a);
		if (dateA.getSeconds() == a.substring(a.length - 2)) {
			// b = b.replace(/-/g,"/");
			var dateB = new Date(b);
			if (dateB.getSeconds() == b.substring(b.length - 2)) {
				return dateA > dateB;
			}
			return false;
		}
		return true;
	};
	
	jQuery.fn.dataTableExt.oSort['zn-datetime-desc'] = function(a,b) {
		// a = a.replace(/-/g,"/");
		var dateA = new Date(a);
		if (dateA.getSeconds() == a.substring(a.length - 2)) {
			// b = b.replace(/-/g,"/");
			var dateB = new Date(b);
			if (dateB.getSeconds() == b.substring(b.length - 2)) {
				return dateA < dateB;
			}
			return true;
		}
		return false;
	};
	// logstash排序
	jQuery.fn.dataTableExt.oSort['zn-datetime-withmillionseconds-desc'] = function(a,b) {
		// a = a.replace(/-/g,"/");
		var dateA = new Date(a);
		if (dateA.getMilliseconds() == a.substring(a.length - 3)) {
			// b = b.replace(/-/g,"/");
			var dateB = new Date(b);
			if (dateB.getMilliseconds() == b.substring(b.length - 3)) {
				return dateA < dateB;
			}
			return true;
		}
		return false;
	};
	
	jQuery.fn.dataTableExt.oSort['zn-datetime-withmillionseconds-asc']  = function(a,b) {
		// a = a.replace(/-/g,"/");
		var dateA = new Date(a);
		if (dateA.getMilliseconds() == a.substring(a.length - 3)) {
			// b = b.replace(/-/g,"/");
			var dateB = new Date(b);
			if (dateB.getMilliseconds() == b.substring(b.length - 3)) {
				return dateA > dateB;
			}
			return false;
		}
		return true;
	};
	regName = $("#regNameFromServer").val();
	window.parent.setRegName(regName, $("#namespace").val());
    window.parent.reloadTreeAndExpandJob(regName);
    $("#jobviews-status-showall-btn").click(function(){
    	filterJobs("","");
    });
    $("#executosviews-status-showall-btn").click(function(){
    	filterExecutors("");
    });
    $('[href="#jobs"]').click(function(event) {
    	$loading.show();
    	renderJobsOverview();
    });
    $('[href="#servers"]').click(function(event) {
    	$loading.show();
        renderServersOverview();
    });

    $('#jobType').change(function(event) {
    	setJobTypeConfig($(this).val());
    });
    
    if(location.hash) {
    	$('a[href=' + location.hash + ']').tab('show');
    }
	$(document.body).on("click", "a[role]", function(event) {
		location.hash = this.getAttribute("href");
	});

	$(window).on('popstate', function() {
		var anchor = location.hash || $('a[role="presentation"]').first().attr("href");
		$('a[href=' + anchor + ']').tab('show');
	});
	jQuery.fn.dataTableExt.oSort['number-fate-desc'] = function(s1,s2) {  
	    s1 = s1.replace('%','');  
	    s2 = s2.replace('%','');  
	    return s2-s1;  
	}; 
	
	$("#change-jobStatus-confirm-dialog").on('shown.bs.modal', function (event) {
    	var button = $(event.relatedTarget); // Button that triggered the modal
    	var jobName = button.data('jobname');
    	var jobstate = button.data('jobstate');
    	$("#change-jobStatus-confirm-dialog-confirm-btn").unbind('click').click(function() {
    		var $btn = $(this).button('loading');
    		$.post("job/toggleJobEnabledState", {jobName : jobName, state: !jobstate,nns:regName}, function (data) {
    			$("#change-jobStatus-confirm-dialog").modal("hide");
    			if (data == "ok") {
		  			showSuccessDialogWithCallback(function() {location.reload(true);});
		  		} else {
		  			showFailureDialogWithMsg("executor-failure-dialog", data);
		  		}
		       }).always(function() { $btn.button('reset'); });
    		return false;
    	});
	});
	
	$("#change-jobStatus-batch-confirm-dialog").on('shown.bs.modal', function (event) {
    	var jobNameState = event.relatedTarget; // Button that triggered the modal
    	if(!jobNameState){
    		return;
    	}
    	var jobNames = jobNameState.jobNames;
    	var jobstate = jobNameState.isJobEnabled;
    	$("#change-jobStatus-batch-confirm-dialog-confirm-btn").unbind('click').click(function() {
    		var $btn = $(this).button('loading');
    		$.post("job/batchToggleJobEnabledState", {jobNames : jobNames, state: jobstate,nns:regName}, function (data) {
    			$("#change-jobStatus-batch-confirm-dialog").modal("hide");
    			if (data == "ok") {
		  			showSuccessDialogWithCallback(function() {location.reload(true);});
		  		} else {
		  			showFailureDialogWithMsg("executor-failure-dialog", data);
		  		}
		       }).always(function() { $btn.button('reset'); });
    		return false;
    	});
	});
	
	$("#remove-job-confirm-dialog").on('shown.bs.modal', function (event) {
    	var button = $(event.relatedTarget); // Button that triggered the modal
    	var jobName = button.data('jobname');
    	$("#remove-job-confirm-dialog-confirm-btn").unbind('click').click(function() {
    		var $btn = $(this).button('loading');
    		$.post("job/remove/job", {jobName : jobName,nns:regName}, function (data) {
		    	$("#remove-job-confirm-dialog").modal("hide");
		    	if (data == "ok") {
		    		showSuccessDialogWithCallback(function(){
			        	window.parent.location.reload(true);
		        	});
		  		} else {
		  			showFailureDialogWithMsg("executor-failure-dialog", data);
		  		}
		       }).always(function() { $btn.button('reset'); });
    		return false;
    	});
	});
	
	$("#remove-job-batch-confirm-dialog").on('shown.bs.modal', function (event) {
    	var jobNames = event.relatedTarget;
    	$("#remove-job-batch-confirm-dialog-confirm-btn").unbind('click').click(function() {
    		var $btn = $(this).button('loading');
    		$.post("job/batchRemove/jobs", {jobNames : jobNames,nns:regName}, function (data) {
		    	$("#remove-job-batch-confirm-dialog").modal("hide");
		    	if (data == "ok") {
		    		showSuccessDialogWithCallback(function(){
			        	window.parent.location.reload(true);
		        	});
		  		} else {
		  			showFailureDialogWithMsgAndCallback("executor-failure-dialog", data,function(){
			        	window.parent.location.reload(true);
		        	});
		  		}
		       }).always(function() { $btn.button('reset'); });
    		return false;
    	});
	});
    
    $("#remove-executor-confirm-dialog").on("shown.bs.modal", function (event) {
    	var button = $(event.relatedTarget);
    	var executor = button.data('executor');
    	if(!executor){// 批量删除和单击删除公用一个confirm-dialog，如果取到的executor undefined，说明点击的是批量删除按钮，直接取到executor集合的值
    		executor = event.relatedTarget;
    	}
    	$("#remove-executor-confirm-dialog-confirm-btn").unbind('click').click(function() {
    		var $btn = $(this).button('loading');
    		$.post("job/remove/executor", {executor : executor,nns:regName}, function (data) {
    			$("#remove-executor-confirm-dialog").modal("hide");
    			if(data == "ok") {
    				showSuccessDialogWithCallback(function(){location.reload(true);});
    			} else {
    				$("#executor-failure-dialog .fail-reason").text(data);
	            	showFailureDialog("executor-failure-dialog");
    			}
    		}).always(function() { $btn.button('reset'); });
    		return false;
    	});
    });
    
    $(document).on("click", "#add-job", function(event) {
    	$("#isCopyJob").val(false);
    	$("#loadLevel").val('');
    	$("#preferList").val('');
    	$("#useDispreferList").val('');
    	$("#localMode").val('');
    	$("#processCountIntervalSeconds").val('');
    	$("#timeoutSeconds").val('');
		$("#pausePeriodDate").val('');
		$("#pausePeriodTime").val('');
		$("#showNormalLog").val('');
		$("#jobType").removeAttr("disabled");
    	$("#addJobTitle").html("添加作业");
		$("#add-job-dialog").modal("show");
		$("#jobName").focus();
	});

	$(document).on("click", "#batch-add-job", function(event) {
		$("#batch-add-job-dialog").modal("show");
	});

	$(document).on("click", "#copy-job", function(event) {
		var chooseInputSize = 0;
		var jobJsonStr;
		$(".batchInput").each(function(){
	    	if($(this).is(":checked")){
    			++chooseInputSize;
    			jobJsonStr = $(this).next("textarea:first").val();
    		}
		});
		if(chooseInputSize != 1) {// 只能选中1个作业进行copy
			$("#executor-failure-dialog .fail-reason").text("请勾选1个作业进行复制！");
			showFailureDialog("executor-failure-dialog");
			return;
		}
		var job = eval("("+jobJsonStr+")");
		$("#isCopyJob").val(true);
		$("#jobType").val(job.jobType);
		$("#originJobName").val(job.jobName);
		$("#jobName").val("CopyOf"+job.jobName);
		$("#jobClass").val(job.jobClass);
		$("#cron").val(job.cron);
		$("#shardingTotalCount").val(job.shardingTotalCount);
		$("#jobParameter").val(job.jobParameter);
		$("#shardingItemParameters").val(job.shardingItemParameters);
		$("#description").val(job.description);
		$("#queueName").val(job.queueName);
		$("#channelName").val(job.channelName);
		$("#loadLevel").val(job.loadLevel);
		$("#preferList").val(job.preferList);
		$("#useDispreferList").val(job.useDispreferList);
		$("#localMode").val(job.localMode);
		$("#processCountIntervalSeconds").val(job.processCountIntervalSeconds);
		$("#timeoutSeconds").val(job.timeoutSeconds);
		$("#pausePeriodDate").val(job.pausePeriodDate);
		$("#pausePeriodTime").val(job.pausePeriodTime);
		$("#showNormalLog").val(job.showNormalLog);
		$("#jobType").attr("disabled","disabled");
		$("#addJobTitle").html("复制作业");
		setJobTypeConfig(job.jobType);
		$("#add-job-dialog").modal("show");
		$("#jobName").focus();
	});
    
    $(document).on("click", "#batch-remove-job", function(event) {
    	var jobNames = "";
		$(".batchInput").each(function(){
		    var status = $(this).attr("status");
		    if(status === "STOPPED"){// 只有已授权并且状态是STOPPED的作业才可以删除
		    	if($(this).is(":checked")){
	    			jobNames += $(this).attr("jobName") + ",";
	    		}
		    }
		});
    	if(isNullOrEmpty(jobNames)) {
			$("#executor-failure-dialog .fail-reason").text("没有可以“删除”的作业，请勾选！");
			showFailureDialog("executor-failure-dialog");
			return;
		}
    	jobNames = jobNames.substring(0,jobNames.length-1);
		var confirmReason = "确定要删除作业（" + jobNames + "）吗?";
    	$("#remove-job-batch-confirm-dialog .confirm-reason").text(confirmReason);
    	$("#remove-job-batch-confirm-dialog").modal("show",jobNames);
	});
    
    $(document).on("click", "#batch-remove-executor", function(event) {
    	var executorNames = "";
		$(".batchDelExecutorInput").each(function(){
		    var removeBtnClass = $(this).attr("removeBtnClass");
		    if(removeBtnClass == ""){// (removeBtnClass=="")说明该executor是可以删除的
		    	if($(this).is(":checked")){
		    		executorNames += $(this).attr("executorName") + ",";
	    		}
		    }
		});
    	if(isNullOrEmpty(executorNames)) {
			$("#executor-failure-dialog .fail-reason").text("没有可以“删除”的Executor，请勾选！");
			showFailureDialog("executor-failure-dialog");
			return;
		}
    	executorNames = executorNames.substring(0,executorNames.length-1);
		var confirmReason = "确定要删除Executor：（" + executorNames + "）吗?";
    	$("#remove-executor-confirm-dialog .confirm-reason").text(confirmReason);
    	$("#remove-executor-confirm-dialog").modal("show",executorNames);
	});
    
    $(document).on("click", ".change-jobStatus-batch-job", function(event) {
    	var targetButtonId = event.target.id;
    	var isEnableButton = false;
    	var tipsMsg = "禁用";
    	if(targetButtonId == "batch-enable-job"){
    		isEnableButton = true;
    		tipsMsg = "启用";
    	}
		var jobNames = "";
		$(".batchInput").each(function(){
			var checkCurJobState;
		    var isJobEnabled = $(this).attr("isJobEnabled");
		    if(isEnableButton){//点击的是启用作业按钮
		    	checkCurJobState = (isJobEnabled == "false");
		    }else {
		    	checkCurJobState = (isJobEnabled == "true");
		    }
		    if(checkCurJobState){
		    	if($(this).is(":checked")){
	    			jobNames += $(this).attr("jobName") + ",";
	    		}
		    }
		});
    	if(isNullOrEmpty(jobNames)) {
			$("#executor-failure-dialog .fail-reason").text("没有可以“"+tipsMsg+"”的作业，请勾选！");
			showFailureDialog("executor-failure-dialog");
			return;
		}
    	jobNames = jobNames.substring(0,jobNames.length-1);
		var confirmReason = "确定要"+tipsMsg+"作业（" + jobNames + "）吗?";
		var jobObj = new Object();
		jobObj.jobNames = jobNames;
		jobObj.isJobEnabled = isEnableButton;
    	$("#change-jobStatus-batch-confirm-dialog .confirm-reason").text(confirmReason);
    	$("#change-jobStatus-batch-confirm-dialog").modal("show",jobObj);
	});
    
    $("#add-job-dialog-confirm-btn").on('click', function(event) {
    	var $btn = $(this).button('loading'),jobType = $("#jobType").val(), jobName = $("#jobName").val(),originJobName = $("#originJobName").val(),jobClass = $("#jobClass").val(), queueName = $("#queueName").val(),channelName = $("#channelName").val(),
    			cron = $("#cron").val(),shardingTotalCount = $("#shardingTotalCount").val(),shardingItemParameters = $("#shardingItemParameters").val(),jobParameter = $("#jobParameter").val().trim(),description = $("#description").val(),
    			loadLevel = $("#loadLevel").val(),preferList = $("#preferList").val(),useDispreferList = $("#useDispreferList").val(),localMode = $("#localMode").val(),processCountIntervalSeconds = $("#processCountIntervalSeconds").val(),
    			timeoutSeconds = $("#timeoutSeconds").val(),pausePeriodDate = $("#pausePeriodDate").val(),pausePeriodTime = $("#pausePeriodTime").val(),showNormalLog = $("#showNormalLog").val(),isCopyJob = $("#isCopyJob").val();
    	if(isNullOrEmpty(jobType)){
    		alert("作业类型不能为空");
			$btn.button('reset');
			return false;
    	}
		if(isNullOrEmpty(jobName)) {
			alert("作业名不能为空");
			$btn.button('reset');
			return false;
		}
		if(/^[0-9a-zA-Z_]+$/.test(jobName) == false) {
			alert("作业名只允许包含：数字0-9、小写字符a-z、大写字符A-Z、下划线_");
			$btn.button('reset');
			return false;
		}
		if(limitJobNameLength(jobName) == false) {
			alert("作业名不能超过80字符，请检查！");
			$btn.button('reset');
			return false;
		}
		if(jobType != "SHELL_JOB" && jobType != "VSHELL"){//shell作业不需要添加作业类名
			if(isNullOrEmpty(jobClass)) {
				alert("作业Class类名不能为空");
				$btn.button('reset');
				return false;
			}
			if(jobClass.length > 100) {
				alert("作业Class类名不能超过100字符，请检查！");
				$btn.button('reset');
				return false;
			}
		}
		if(isNullOrEmpty(shardingTotalCount)) {
			alert("作业分片总数不能为空且必须是数字");
			$btn.button('reset');
			return false;
		}
		if(shardingItemParameters && shardingItemParameters.length < 2 || shardingItemParameters.indexOf("=") == -1){
			alert("作业分片参数有误");
        	$("#shardingItemParameters").focus();
        	$btn.button('reset');
        	return false;
        }
		var shardingItemParametersArr = shardingItemParameters.split(","),realShardingItemParameters = [];
    	// 去除空白元素
    	for (var i in shardingItemParametersArr) {
    		if (shardingItemParametersArr[i].trim() != "") {
    			realShardingItemParameters.push(shardingItemParametersArr[i]);
    		}
    	}
    	shardingItemParametersArr = realShardingItemParameters;
        if (realShardingItemParameters.length < shardingTotalCount) {
        	alert("分片参数不能小于分片总数");
        	$("#shardingItemParameters").focus();
        	$btn.button('reset');
        	return false;
        }
		if(shardingItemParameters){
			if(!localMode){// 复制作业时本地模式可能为true
				if(shardingItemParameters.substring(0,2) == "*="){// 如果是非本地模式，此时如果参数为*=，则报错
					alert("作业分片参数有误，作业默认不是本地模式，不能用“*=”，如需要修改成本地模式，请到设置页进行修改");
					$("#shardingItemParameters").focus();
					$btn.button('reset');
					return;
				}
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
		$.post("executor/checkAndAddJobs",{jobName: jobName,jobClass:jobClass,channelName:channelName,queueName:queueName,
			jobType:jobType,cron:cron,shardingTotalCount:shardingTotalCount,jobParameter:jobParameter,
			shardingItemParameters:shardingItemParameters,description:description,loadLevel:loadLevel,preferList:preferList,
			useDispreferList:useDispreferList,localMode:localMode,processCountIntervalSeconds:processCountIntervalSeconds,
			timeoutSeconds:timeoutSeconds,pausePeriodDate:pausePeriodDate,isCopyJob:isCopyJob,originJobName:originJobName,
			pausePeriodTime:pausePeriodTime,showNormalLog:showNormalLog,nns:regName}, function(data) {
			if(data.success == true) {
				$("#add-job-dialog").modal("hide");
				showSuccessDialogWithCallback(function(){
		        	window.parent.location.reload(true);
	        	});
	        } else {
	        	alert("操作失败："+data.message);
	        }
		}).always(function() { $btn.button('reset'); });
		return false;
    });

	$("#batch-add-job-dialog-confirm-btn").on('click', function(event) {
		var $btn = $(this).button('loading');
		$("#fileupload").ajaxSubmit({
				success: function(result) {
					$("#batch-add-job-dialog").modal("hide");
					if(result.success == true) {
						showPromptDialogWithMsgAndCallback("batch-add-job-prompt-dialog", result.message, function() {
							window.parent.location.reload(true);
						});
					} else {
						showPromptDialogWithMsgAndCallback("batch-add-job-prompt-dialog",result.message, function() {
							window.parent.location.reload(true);
						});
					}
				},
				error: function(result) {
					showPromptDialogWithMsgAndCallback("batch-add-job-prompt-dialog",result.responseText, function() {
						window.parent.location.reload(true);
					});
				}
			});
		$btn.button('reset');
		return false;
	});


    $("#totalCheckbox").on('click', function(event) {
    	if($(this).is(":checked")){
    		$('.batchInput').prop('checked',true);// jquery1.9以后的版本，用attr有点问题，要用prop
    	}else{
    		$('.batchInput').prop('checked',false);
    	}
    });
    
    $("#totalExecutorCheckbox").on('click', function(event) {
    	if($(this).is(":checked")){
    		$('.batchDelExecutorInput').prop('checked',true);// jquery1.9以后的版本，用attr有点问题，要用prop
    	}else{
    		$('.batchDelExecutorInput').prop('checked',false);
    	}
    });
    $("#custom-search-btn").click(function() {
    	selectedJobName = $jobNamesSelect.val();
    	var st = $("#start-time").val();
    	var et = $("#end-time").val();
    	$logstashTable.ajax.url("logstash?jn="+selectedJobName+"&st="+st+"&et="+et).load();
    }); 
    if ($("#jobs").is(':visible')) {
    	$loading.show();
        renderJobsOverview();
    } else if ($("#servers").is(':visible')) {
    	$loading.show();
        renderServersOverview();
    }
    
    $("[data-toggle='tooltip']").tooltip();
});
	/**
	 * 点击“作业总览”表格的复选框checkbox触发的事件
	 * 
	 * @param obj
	 * @returns {Boolean}
	 */
	function clickBatchInputCheckBox(obj){
		var chooseCheckBox = $(obj).is(":checked");
		if(!chooseCheckBox){
			$("#totalCheckbox").prop('checked',false);// 只要有一个没选中，全选复选框就置为false
			return false;
		}
		var checkChoose = true;
    	$(".batchInput").each(function(){
	    	if(!$(this).is(":checked")){
	    		checkChoose = false;
    		}
		});
    	if(checkChoose){
    		$("#totalCheckbox").prop('checked',true);// 只有全部都选中，全选复选框才置为true
    	}else{
    		$("#totalCheckbox").prop('checked',false);// 只要有一个没选中，全选复选框就置为false
    	}
	}
	
	/**
	 * 点击“Executor总览”表格的复选框checkbox触发的事件
	 * 
	 * @param obj
	 * @returns {Boolean}
	 */
	function clickBatchDelExecutorInputCheckBox(obj){
		var chooseCheckBox = $(obj).is(":checked");
		if(!chooseCheckBox){
			$("#totalExecutorCheckbox").prop('checked',false);// 只要有一个没选中，全选复选框就置为false
			return false;
		}
		var checkChoose = true;
    	$(".batchDelExecutorInput").each(function(){
	    	if(!$(this).is(":checked")){
	    		checkChoose = false;
    		}
		});
    	if(checkChoose){
    		$("#totalExecutorCheckbox").prop('checked',true);// 只有全部都选中，全选复选框才置为true
    	}else{
    		$("#totalExecutorCheckbox").prop('checked',false);// 只要有一个没选中，全选复选框就置为false
    	}
	}
	
	function showChangeJobConfirmDialog(obj) {
		var jobstate = $(obj).data('jobstate');
		var confirmBtnText = "确认启用作业吗?";
		if (jobstate == true) {
			confirmBtnText = "确认禁用作业吗?";
		}
		$("#change-jobStatus-confirm-dialog .confirm-reason").text(confirmBtnText);
		$("#change-jobStatus-confirm-dialog").modal("show", obj);
	}

    function showRemoveExecutorConfirmDialog(obj) {
    	var confirmReason = "确认要删除Executor：（" + $(obj).data('executor') + "）吗?";
    	$("#remove-executor-confirm-dialog .confirm-reason").text(confirmReason);
    	$("#remove-executor-confirm-dialog").modal("show", obj);
    }

    function renderJobsOverview() {
    	jobNameSelectValues = [];
    	var readys = runnings = stoppings = stoppeds = 0;
        $.get("job/jobs", {nns:regName}, function (data) {
        	if (jobsViewDataTable) {
        		jobsViewDataTable.destroy();
        	}
        	$("#totalCheckbox").prop('checked',false);// 刷新标签页时，表头的checkbox也要取消勾选状态
            $("#jobs-overview-tbl tbody").empty();
            
            var list = data;
            for (var i = 0;i < list.length;i++) {
            	var jobName = list[i].jobName;
            	jobNameSelectValues.push(jobName);
                var status = list[i].status;
                var nextFireTime = (null == list[i].nextFireTime ? "-" : list[i].nextFireTime);
                var lastBeginTime = list[i].lastBeginTime;
                var loadLevel = list[i].loadLevel;
                var shardingTotalCount = list[i].shardingTotalCount;
                if(list[i].localMode == true){
                	shardingTotalCount = "N/A";
                }
                var preferList = list[i].preferList;
                var cron = list[i].cron, iconClass = "";
                var isJobEnabled = list[i].isJobEnabled;
                if (list[i].jobType === "MSG_JOB") {
                	cron = "java消息作业cron无效";
                	iconClass = "fa fa-reorder";
                	isShellExecutor = false;
                } else if (list[i].jobType === "SHELL_JOB") {
                	iconClass = "devicon devicon-linux-plain";
                } else if (list[i].jobType === "VSHELL") {
                	cron = "shell消息作业cron无效";
                	iconClass = "devicon devicon-linux-plain";
                	isShellExecutor = false;
                } else {
                	iconClass = "devicon devicon-java-plain";
                	isShellExecutor = false;
                }
                var jobJsonStr = JSON.stringify(list[i]);
                var baseTd = "<td><input class='batchInput' jobName='"+jobName+"' isJobEnabled='"+isJobEnabled+"' status='"+status+"' type='checkbox' onclick='clickBatchInputCheckBox(this);'/><textarea hidden='hidden'>"+jobJsonStr+"</textarea></td>"
                	+ "<td>" + "<a href='job_detail?jobName=" + jobName + "&nns=" 
                	+ regName +"'><span class=\"fancytree-custom-icon "+ iconClass +"\"> " 
                	+ jobName + "</span></a>" + "</td>" 
                	+ "<td>" + list[i].jobRate + "</td>"
                	+ "<td>" + status + "</td>" 
                	+ "<td>" + loadLevel + "</td>" 
                	+ "<td>" + shardingTotalCount + "</td>" 
                	+ "<td id='showPreferList_"+i+"' style='width: 600px; word-wrap:break-word;word-break:break-all;'>" + preferList + "</td>"
                	+ "<td style='width: 600px; word-wrap:break-word;word-break:break-all;'>" + list[i].shardingList + "</td>"
                	+ "<td>" + cron + "</td>" 
                	+ "<td>" + lastBeginTime + " - " + list[i].lastCompleteTime + "</td>" 
                	+ "<td>" + nextFireTime + "</td>" 
                	+ "<td id='showDescription_"+i+"'></td>";
                var trClass = "";
                var operationBtn = "启用";
                var operationBtnClass = "btn btn-success";
                if(isJobEnabled == true){
                	operationBtn = "禁用";
                	operationBtnClass = "btn btn-warning";
                }
                var operationTd = "<button operation='change-jobStatus' class='"+operationBtnClass+"' data-jobstate='" + isJobEnabled + "' data-jobname='" + jobName + "' data-target='#change-jobStatus-confirm-dialog' onclick='showChangeJobConfirmDialog(this);'>"+operationBtn+"</button> ";
                if ("READY" === status) {
                    trClass = "info";
                    baseTd += "<td>"+operationTd+"</td>";
                    readys ++;
                } else if ("STOPPED" === status) {
                    trClass = "warning";
                    baseTd += "<td>"+operationTd+"<button operation='remove-job' class='btn btn-danger' data-jobname='" + jobName + "'data-toggle='modal' data-target='#remove-job-confirm-dialog'" + ">删除</button></td>";
                    stoppeds ++;
                } else if ("RUNNING" === status) {
                    trClass = "success";
                    baseTd += "<td>"+operationTd+"</td>";
                    runnings ++;
                } else if ("STOPPING" === status) {
                    trClass = "danger";
                    baseTd += "<td>"+operationTd+"</td>";
                    stoppings ++;
                } 
                $("#jobs-overview-tbl tbody").append("<tr id='tr-" + jobName + "' class='" + trClass + "'>" + baseTd + "</tr>");
                $("#showPreferList_"+i).text(preferList);// 使用text来赋值，防止&gt;等被转义成>
                $("#showDescription_"+i).text(list[i].description);// 使用text来赋值，防止&gt;等被转义成>
            }
            jobsViewDataTable = $("#jobs-overview-tbl").DataTable({"destroy": true, 
            	"oLanguage": language,
                "search": {"regex":true,"smart":false},
                "initComplete": function() {
                	$("#jobs-overview-tbl_filter input").attr("title","搜索作业名，支持正则").keyup(function(){
                		var term = $(this).val(),
                        regex =  term ;
                		jobsViewDataTable.columns(1).search(regex, true, false).draw();
                	});
                },
            	"columnDefs": [{ "type": "zn-datetime", targets: 10 }],
            	"order": [[5, "desc" ]], 
            	"aoColumns": [ 
            	    { "asSorting": []},
    	            { "asSorting": [ "desc", "asc"]  },
    	            { "asSorting": [ "desc", "asc"]  },
  					{ "asSorting": [ "desc", "asc"]  },  
					{ "asSorting": [ "desc", "asc"]  },  
					{ "asSorting": [ "desc", "asc"]  },  
					{ "asSorting": [ "desc", "asc"]  },  
					{ "asSorting": [ "desc", "asc"]  },
					{ "asSorting": []  },
					{ "asSorting": []  },
					{ "asSorting": [ "desc", "asc"]  },
					{ "asSorting": [ "desc", "asc"]  },
					{ "asSorting": []} 
				]});
            $("#jobs-overview-tbl_filter label").before(jobOperation);
        	$("#add-job").attr("title","点击进行添加作业");
        	$("#add-job").removeAttr("disabled");
			$("#batch-add-job").removeAttr("disabled");
        	$(".hide-when-is-script-job").show();
    		$("#jobType").empty();
        	$("#jobType").append('<option value="JAVA_JOB">&nbsp;Java定时作业</option>');
        	$("#jobType").append('<option value="SHELL_JOB">&nbsp;Shell定时作业</option>');
            $("#jobs-overview-tbl").parent().css("overflow","auto");// 给表格table增加overflow样式，防止表格元素太长而撑大表格总宽度
            $("#jobs-count").text(readys+runnings+stoppings+stoppeds);
            $('#jobs-chart').highcharts({
                chart: {
                    type: 'pie',
                    options3d: {
                        enabled: true,
                        alpha: 45
                    }
                },
			    credits : {
					enabled : false
				},
                title: {text:""},
                plotOptions: {
                    pie: {
                        innerSize: 100,
                        depth: 30
                    }
                },
                series: [{
                    name: '该状态下的作业数量',
                    data: [
                        ['READY', readys],
                        ['STOPPING', stoppings],
                        ['RUNNING', runnings],
                        ['STOPPED', stoppeds]
                    ]
                }]
            });
              // init series.
            var series = [];
            var datas = [];
            var jobNameCategories = [];
            for (var i = 0;i < list.length;i++) {
            	var oneData = {};
            	var jobName = list[i].jobName;
            	var jobRate = parseFloat(list[i].jobRate.substring(0,list[i].jobRate.length-1));
            	if(!jobRate){
            		continue;
            	}
				oneData['name'] = jobName;
				oneData['y'] = jobRate;
				datas.push(oneData);
				jobNameCategories.push(jobName);
            }
            if(datas && datas.length > 0){
            	var serie = {};
            	serie['data'] = datas;
            	serie['colorByPoint'] = true;
            	series.push(serie);
            }
            $('#jobs-rate-3dcolumn').highcharts({
                chart: {
                    type: 'column'
                },
			    credits : {
					enabled : false
				},
				legend: {// 不显示series的name值属性
					enabled: false
				},
                title: {
                    text: ''
                },
                plotOptions: {
                    column: {
                        depth: 25
                    }
                },
                xAxis: {
                    categories: jobNameCategories
                },
                yAxis: {
                    min: 0,
                    title: {
                        text: '作业成功率(%)'
                    },
                    labels: {//格式化纵坐标的显示风格
                        formatter: function() {
                          return this.value + "%";
                        }
                    },
                    stackLabels: {
                        enabled: true,
                        style: {
                            fontWeight: 'bold',
                            color: (Highcharts.theme && Highcharts.theme.textColor) || 'gray'
                        }
                    }
                },
                tooltip: {
                    headerFormat: '<b>作业名：{point.x}</b><br/>',
                    pointFormat: '成功率：{point.y}%'
                },
                plotOptions: {
                    column: {
                        stacking: 'normal',
                        dataLabels: {
                            enabled: false,
                            color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white',
                            style: {
                                textShadow: '0 0 3px black'
                            }
                        },
                        cursor: 'pointer',
                        events: {
                            click: function(e) {
                            	location.href = "job_detail?jobName="+e.point.name+"&nns="+regName; //点击成功率柱状图上的作业跳转到相应的作业设置页
                            }
                        }
                    }
                },
                series: series
            });
        }).always(function() { $loading.hide(); });
    }
    
    function filterJobsRegex(regex) {
    	jobsViewDataTable.search(regex,true,false).draw();
    }
    function filterJobs(status,name) {
    	jobsViewDataTable.search(status).draw();
    }
    function filterServers (status) {
    	serversViewDataTable.search(status).draw();
    }
    function filterExecutors (status) {
    	executorsViewDataTable.search(status).draw();
    }
    function setJobTypeConfig(value){
    	if(value == "MSG_JOB" || value == "VSHELL"){
    		$(".show-when-is-msg-job").show();
    		$(".hide-when-is-msg-job").hide();
    	}else{
    		$(".show-when-is-msg-job").hide();
    		$(".hide-when-is-msg-job").show();
    	}
    	
    	if(value == "SHELL_JOB" || value == "VSHELL"){
    		$(".hide-when-is-script-job").hide();
    	}else{
    		$(".hide-when-is-script-job").show();
    	}
    }
    
    function renderServersOverview() {
    	var onlines = offlines = 0;
        $.get("server/servers", {nns:regName}, function (data) {
        	if (serversViewDataTable) {
        		serversViewDataTable.destroy();
        	}
        	$("#totalExecutorCheckbox").prop('checked',false);// 刷新标签页时，表头的checkbox也要取消勾选状态
            $("#servers-overview-tbl tbody").empty();
            var loadLevels= [], exeNames = [], serverInfos = data["serverInfos"], lv = data["jobShardLoadLevels"];
            if(serverInfos){
            	for (var i = 0;i < serverInfos.length;i++) {
            		var status = serverInfos[i].status,jobStatus = serverInfos[i].jobStatus,sharding = serverInfos[i].sharding, lastBeginTime = serverInfos[i].lastBeginTime,executorName = serverInfos[i].executorName,trClass = "",removeBtnClass = "",removeBtnTitle="";
            		loadLevels.push(serverInfos[i].totalLoadLevel),hasSharding = serverInfos[i].hasSharding;
            		if ("ONLINE" === status) {
            			trClass = "success";
            			onlines ++;
            			removeBtnClass = "disabled";
            			removeBtnTitle="无法删除ONLINE的Executor";
            			exeNames.push(serverInfos[i].executorName);
            		} else {
            			trClass = "warning";
            			offlines ++;
            			lastBeginTime = "";
            			removeBtnTitle="点击进行删除该Executor";
            			if(hasSharding){
            				removeBtnClass = "disabled";
            				removeBtnTitle="无法删除分片不为空(有残留分片)的Executor";
            			}
            		}
            		var removeButton = "<button operation='removeExecutor' title='"+removeBtnTitle+"' class='btn btn-danger "+removeBtnClass+"' data-executor='" + executorName + "' onclick='showRemoveExecutorConfirmDialog(this);' "+removeBtnClass+">删除</button>";
            		var baseTd = "<td><input class='batchDelExecutorInput' executorName='"+executorName+"' removeBtnClass='"+removeBtnClass+"' type='checkbox' onclick='clickBatchDelExecutorInputCheckBox(this);'/></td>"
            		+ "<td>" + executorName + "</td>" 
            		+ "<td>" + serverInfos[i].serverIp + "</td>" 
            		+ "<td>" + serverInfos[i].totalLoadLevel + "</td>" 
            		+ "<td>" + sharding + "</td>" 
            		+ "<td>" + status + "</td>"
            		+ "<td>" + lastBeginTime + "</td>"
            		+ "<td>" + serverInfos[i].version + "</td>"
            		+ "<td>" + removeButton + "</td>";
            		$("#servers-overview-tbl tbody").append("<tr class='" + trClass + "'>" + baseTd + "</tr>");
            	}
            }
            
            
            $("#executors-count").text(onlines+offlines);
            $("#executors-online-count").text(onlines);
            $("#executors-offline-count").text(offlines);
            serversViewDataTable = $("#servers-overview-tbl").DataTable({"destroy": true,
            	"oLanguage": language,
            	"order": [[5, "desc"],[2, "asc"]], 
    			"aoColumns": [ 
    			              	{ "asSorting": []}, 
    			              	{ "asSorting": [ "desc", "asc"]  },
    		    	            { "asSorting": [ "desc", "asc"]  },
    		  					{ "asSorting": [ "desc", "asc"]  },  
    							{ "asSorting": [ "desc", "asc"]  },  
    							{ "asSorting": [ "desc", "asc"]  },  
    							{ "asSorting": [ "desc", "asc"]  }, 
    							{ "asSorting": [ "desc", "asc"]  },
    							{ "asSorting": []}
    						]
    			});
            $("#servers-overview-tbl_filter label").before(executorOperation);
            $("#serverviews-status-showall").show().css("display","inline");// 显示批量删除executor按钮
            $('#executors-donuts').highcharts({
                chart: {
                    type: 'pie',
                    options3d: {
                        enabled: true,
                        alpha: 45
                    }
                },
			    credits : {
					enabled : false
				},
                title: {text:""},
                plotOptions: {
                    pie: {
                        innerSize: 100,
                        depth: 30
                    }
                },
                series: [{
                    name: '该状态下的executor数量',
                    data: [
                        ['ONLINE', onlines],
                        ['OFFLINE', offlines]
                    ]
                }]
            });
            
            // init series.
            var series = [];
            for(var p in lv) {
				  var oneData = {}, datas = [], noShard = true;
				  for(var d in lv[p]) {
					  if (0 != lv[p][d]) {
						  noShard = false;
					  }
					  datas.push(lv[p][d]);
				  } 
				  if (!noShard) {
					  oneData['name'] = p;
					  oneData['data'] = datas;
					  series.push(oneData);
            	  }
            }
            $('#executors-3dcolumn').highcharts({
                chart: {
                    type: 'column'
                },
			    credits : {
					enabled : false
				},
                title: {
                    text: ''
                },
                plotOptions: {
                    column: {
                        depth: 25
                    }
                },
                xAxis: {
                    categories: exeNames
                },
                yAxis: {
                    min: 0,
                    title: {
                        text: '总负荷分布'
                    },
                    stackLabels: {
                        enabled: true,
                        style: {
                            fontWeight: 'bold',
                            color: (Highcharts.theme && Highcharts.theme.textColor) || 'gray'
                        }
                    }
                },
                tooltip: {
                    headerFormat: '<b>{point.x}</b><br/>',
                    pointFormat: '作业{series.name}, 负荷{point.y}<br/>总负荷: {point.stackTotal}'
                },
                plotOptions: {
                    column: {
                        stacking: 'normal',
                        dataLabels: {
                            enabled: false,
                            color: (Highcharts.theme && Highcharts.theme.dataLabelsColor) || 'white',
                            style: {
                                textShadow: '0 0 3px black'
                            }
                        }
                    }
                },
                series: series
            });
        }).always(function() { $loading.hide(); });
    }
    
    function bindSubmitExecutorAddJobsForm() {
    	$("#executors-add-jobs-form").submit(function(event) {
    		event.preventDefault();
    	});
    }

    function isNullOrEmpty(value) {
    	if(!value || value == null) {
    		return true;
    	}
    	if(value instanceof Array) {
    		return value.length == 0;
    	}
    	if(typeof value == "string") {
    		return value.trim() == "";
    	}
    	return false;
    }
    
    function null2Empty(value) {
    	if(!value || value == null) {
    		return "";
    	}
    	return value;
    }
    
    function contains(array, value) {
    	if(array && array != null && array instanceof Array) {
    		for(var i=0; i<array.length; i++) {
    			if(value && value != null && array[i].trim() == value.trim()) {
    				return true;
    			}
    		}
    	}
    	return false;
    }

    function limitJobNameLength(value) {
    	var arr = value.split(",");
    	for(var i=0; i<arr.length; i++) {
    		if(arr[i].trim().length > 80) {
    			return false;
    		}
    	}
    	return true;
    }
	function addDay(today, day2Add) {
		var newD = today._d.getTime() + (day2Add * (1000*60*60*24));
		return new Date(newD);
	}
    