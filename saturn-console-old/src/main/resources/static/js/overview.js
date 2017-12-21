var regName = $("#regNameFromServer").val(), jobsViewDataTable, serversViewDataTable,containersViewDataTable,
	executorsViewDataTable, $loading,
	ns = $("#namespace").val(),
	containerType = $("#containerType").val(),
	opExecutorNames = new Array(),
	opAddJobNames = new Array(),
	jobOperation = '<div id="jobviews-operation-area"><button class="btn btn-success change-jobStatus-batch-job" id="batch-enable-job" title="点击进行批量启用作业">启用作业</button>&nbsp;&nbsp;'+
               	'<button class="btn btn-warning change-jobStatus-batch-job" id="batch-disable-job" title="点击进行批量禁用作业">禁用作业</button>&nbsp;&nbsp;'+
               	'<button class="btn btn-danger" id="batch-remove-job" title="点击进行批量删除作业">删除作业</button>&nbsp;&nbsp;'+
               	'<button class="btn btn-info" id="add-job" title="点击进行添加作业">添加作业</button>&nbsp;&nbsp;' +
               	'<button class="btn btn-primary" id="copy-job" title="点击进行复制作业">复制作业</button>&nbsp;&nbsp;</div>' +
//               	'<button class="btn btn-success batch-migrate-job" id="batch-migrate-job" title="点击进行批量迁移作业到容器资源">迁移容器</button>&nbsp;&nbsp;'+
                '<button class="btn btn-success batch-set-prefer-executors" id="batch-set-prefer-executors" title="点击进行批量选择优先Executors">优先Executors</button>&nbsp;&nbsp;'+
               	'<button class="btn btn-info" id="batch-add-job" title="点击进行导入作业">导入作业</button>&nbsp;&nbsp;' +
               	'<a class="btn btn-info" id="export-job" href="executor/exportJob?nns='+regName+'" title="点击进行导出全域作业" target="_self">导出全域作业</a>&nbsp;&nbsp;' +
               	'<label>分组 <select id="groupSelect" title="选择作业分组" class="form-control"><option value="">全部分组</option></select></label>';
	executorOperation = '<div id="serverviews-status-showall">' +
		'<button class="btn btn-info" id="shard-all-at-once" title="点击进行一键重排？（即把当前域下所有分片按照作业负荷以及优先Executor等作业配置重新进行排序。注：重排可能导致分片分布剧烈动荡，请谨慎操作！另外在操作本功能时，请尽量避免同时操作Executor上下线或启用禁用作业，以免影响到重排的均衡效果！）">一键重排</button>&nbsp;&nbsp;'+
		'<button class="btn btn-danger" id="batch-remove-executor" title="点击进行批量删除Executor">删除Executor</button>&nbsp;&nbsp;</div>';

$(function() {

	$loading = $("#loading");
	bindSubmitExecutorAddJobsForm();

	$.fn.money_field = function(opts) {
        var defaults = { width: null, symbol: '$' };
        var opts     = $.extend(defaults, opts);
        return this.each(function() {
          if(opts.width)
            $(this).css('width', opts.width + 'px');
          $(this).wrap("<div class='input-group'>").before("<span class='input-group-addon'>" + opts.symbol + "</span>");
        });
    };
	
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
	window.parent.setRegName($("#namespace").val(), $("#zkAlias").val());
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
    $('[href="#containers"]').click(function(event) {
        $loading.show();
        renderContainersOverview();
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
        var state = !button.data('jobstate');
        $("#change-jobStatus-confirm-dialog-confirm-btn").unbind('click').click(function() {
            var $btn = $(this).button('loading');
            $.post("job/toggleJobEnabledState", {jobName : jobName, state: state, confirmed: false, nns:regName}, function (data) {
                $("#change-jobStatus-confirm-dialog").modal("hide");
                if (data.success) {
                    showSuccessDialogWithCallback(function() {location.reload(true);});
                } else {
                    if(data.obj == "confirmDependencies") {
                        $("#confirmDependencies-confirm-dialog .confirm-reason").html(data.message);
                        $("#confirmDependencies-confirm-dialog").attr("operation", "toggleJobEnabledState");
                        $("#confirmDependencies-confirm-dialog").attr("jobName", jobName);
                        $("#confirmDependencies-confirm-dialog").attr("state", state.toString());
                        $("#confirmDependencies-confirm-dialog").modal("show");
                    } else {
                        showFailureDialogWithMsg("executor-failure-dialog", data.message);
                    }
                }
            }).always(function() { $btn.button('reset');});
            return false;
        });
    });

    $("#confirmDependencies-confirm-dialog-confirm-btn").on('click', function(event) {
        var $btn = $(this).button('loading');
        var operation = $("#confirmDependencies-confirm-dialog").attr("operation");
        if(operation == "toggleJobEnabledState") {
            var jobName = $("#confirmDependencies-confirm-dialog").attr("jobName");
            var state = $("#confirmDependencies-confirm-dialog").attr("state");
            $.post("job/toggleJobEnabledState", {jobName : jobName, state: state, confirmed: true, nns:regName},function (data) {
                $("#confirmDependencies-confirm-dialog").modal("hide");
                if (data.success) {
                    showSuccessDialogWithCallback(function() {location.reload(true);});
                } else {
                    showFailureDialogWithMsg("executor-failure-dialog", data.message);
                }
            }).always(function() { $btn.button('reset'); });
        } else if(operation == "batchToggleJobEnabledState") {
            var jobNames = $("#confirmDependencies-confirm-dialog").attr("jobNames");
            var state = $("#confirmDependencies-confirm-dialog").attr("state");
            $.post("job/batchToggleJobEnabledState", {jobNames : jobNames, state: state, confirmed: true, nns:regName}, function (data) {
                $("#confirmDependencies-confirm-dialog").modal("hide");
                if (data.success) {
                    showSuccessDialogWithCallback(function() {location.reload(true);});
                } else {
                   showFailureDialogWithMsg("executor-failure-dialog", data.message);
                }
            }).always(function() { $btn.button('reset'); });
        }
        return false;
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
    		$.post("job/batchToggleJobEnabledState", {jobNames : jobNames, state: jobstate, confirmed: false, nns:regName}, function (data) {
                $("#change-jobStatus-batch-confirm-dialog").modal("hide");
                if (data.success) {
                    showSuccessDialogWithCallback(function() {location.reload(true);});
                } else {
                    if(data.obj == "confirmDependencies") {
                        $("#confirmDependencies-confirm-dialog .confirm-reason").html(data.message);
                        $("#confirmDependencies-confirm-dialog").attr("operation", "batchToggleJobEnabledState");
                        $("#confirmDependencies-confirm-dialog").attr("jobNames", jobNames);
                        $("#confirmDependencies-confirm-dialog").attr("state", jobstate.toString());
                        $("#confirmDependencies-confirm-dialog").modal("show");
                    } else {
                        showFailureDialogWithMsg("executor-failure-dialog", data.message);
                    }
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
            $.post("job/remove/job", {jobName : jobName, nns:regName}, function (data) {
                $("#remove-job-confirm-dialog").modal("hide");
                if (data.success) {
                    showSuccessDialogWithCallback(function(){ window.parent.location.reload(true); });
                } else {
                    showFailureDialogWithMsg("executor-failure-dialog", data.message);
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
    
	$("#traffic-executor-confirm-dialog").on("shown.bs.modal", function (event) {
        var button = $(event.relatedTarget);
        var executor = button.data('executor');
        var operation = button.data('operation');
        $("#traffic-executor-confirm-dialog-confirm-btn").unbind('click').click(function() {
            var $btn = $(this).button('loading');
            $.post("server/traffic", {executorName: executor, operation: operation, nns: regName}, function (data) {
                $("#traffic-executor-confirm-dialog").modal("hide");
                if(data.success) {
                    showSuccessDialogWithCallback(function(){location.reload(true);});
                } else {
                    $("#executor-failure-dialog .fail-reason").text(data.message);
                    showFailureDialog("executor-failure-dialog");
                }
            }).always(function() { $btn.button('reset'); });
            return false;
        });
    });
	
    $("#remove-executor-confirm-dialog").on("shown.bs.modal", function (event) {
    	// 批量删除和单击删除公用一个confirm-dialog，如果取到relatedTarget为字符串，则为批量删除
        var target = event.relatedTarget;
        if(typeof target == "string") {
            executor = event.relatedTarget;
        } else {
            var button = $(target);
            var executor = button.data('executor');
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

    $("#remove-container-confirm-dialog").on("shown.bs.modal", function (event) {
        var taskId = event.relatedTarget;
        $("#remove-container-confirm-dialog-confirm-btn").unbind('click').click(function() {
            var $btn = $(this).button('loading');
            $.post("container/removeContainer", {taskId : taskId,nns:regName}, function (data) {
                $("#remove-container-confirm-dialog").modal("hide");
                if(data.success == true) {
                    showSuccessDialogWithCallback(function(){location.reload(true);});
                } else {
                    $("#failure-dialog .fail-reason").text(data.message);
                    showFailureDialog("failure-dialog");
                }
            }).always(function() { $btn.button('reset'); });
            return false;
        });
    });

    $(document).on("change", "#groupSelect", function(event) {
    	var term = $(this).val();
        regex =  term ;
        if (regex == "") {
    		jobsViewDataTable.columns(8).search(regex, true, false).draw();
        } else {
    		jobsViewDataTable.columns(8).search("^" + regex + "$", true, false).draw();
        }
    });
    
    $(document).on("click", "#add-job", function(event) {
    	$("#isCopyJob").val(false);
    	$("#loadLevel").val('');
    	$("#preferList").val('');
    	$("#useDispreferList").val('');
    	$("#localMode").val('');
    	$("#processCountIntervalSeconds").val('');
    	$("#timeout4AlarmSeconds").val('');
    	$("#timeoutSeconds").val('');
		$("#pausePeriodDate").val('');
		$("#pausePeriodTime").val('');
		$("#showNormalLog").val('');
		$("#timeZone").val('Asia/Shanghai');
		$("#jobType").removeAttr("disabled");
        reloadPreferListProvided(null, "", function() {
            $("#addJobTitle").html("添加作业");
            $("#add-job-dialog").modal("show").css({height:'100%', 'overflow-y':'scroll'});
            $("#jobName").focus();
        });
	});

	$("#taskId").money_field({ width: null,symbol: ns + "-"});
    $("#cmd").val("/apps/dat/web/working/saturn/bin/saturn-job-executor.sh start -r foreground -env docker -n " + ns);

    $(document).on("click", "#add-preferResource", function(event) {
        if(containerType == "VDOS") {
            $.get("container/getContainerToken",{nns:regName}, function(data) {
                if(data.success) {
                    $("#containers-operation-token").val(data.obj.keyValue.token == null ? "" : data.obj.keyValue.token);
                    $("#add-container-dialog-token").val(data.obj.keyValue.token == null ? "" : data.obj.keyValue.token);
                } else {
                    $("#failure-dialog .fail-reason").text("get token error:" + data.message);
                    showFailureDialog("failure-dialog");
                }
            });
        } else {
            $.get("container/getContainerToken", {nns: regName}, function(data) {
                if(data.success) {
                    $("#containers-operation-userName").val(data.obj.keyValue.userName == null ? "" : data.obj.keyValue.userName);
                    $("#containers-operation-password").val(data.obj.keyValue.password == null ? "" : data.obj.keyValue.password);
                    $("#add-container-dialog-userName").val(data.obj.keyValue.userName == null ? "" : data.obj.keyValue.userName);
                    $("#add-container-dialog-password").val(data.obj.keyValue.password == null ? "" : data.obj.keyValue.password);
                } else {
                    $("#failure-dialog .fail-reason").text("get token error:" + data.message);
                    showFailureDialog("failure-dialog");
                }
            });
        }
        $("#nested").val("true");
        $("#add-container-dialog").modal("show");
    });

	$(document).on("click", "#batch-add-job", function(event) {
		$("#batch-add-job-dialog").modal("show");
	});

	$(document).on("click", "#add-container", function(event) {
        $("#nested").val('');
        $("#add-container-dialog").modal("show");
    });

    $(document).on("change", "#image-repositories", function(event) {
        var imageTagsSelect = $("#image-tags");
        imageTagsSelect.empty();
        var repositoryValSelected = $(this).find("option:selected").val();
        $(this).attr("title", repositoryValSelected);
        if(!isNullOrEmpty(repositoryValSelected)) {
            $.get("container/getRegistryRepositoryTags", {repository: repositoryValSelected,nns:regName}, function(data) {
                if(data.success == true && data.obj) {
                    var obj = $.parseJSON(data.obj);
                    if(obj.tags && obj.tags instanceof Array) {
                        var _options = "";
                        for(var i=0; i<obj.tags.length; i++) {
                            _options = _options + '<option value="' + obj.tags[i] + '" title="' + obj.tags[i] + '">' + obj.tags[i] + '</option>';
                        }
                        imageTagsSelect.append(_options);
                        var imageTagsVal = $("#image-tags").find("option:selected").val();
                        if(imageTagsVal) {
                            $("#image-tags").attr("title", imageTagsVal);
                        }
                    }
                } else {
                    $("#failure-dialog .fail-reason").text("get getRegistryRepositoryTags error: " + data.message);
                    showFailureDialog("failure-dialog");
                }
            });
        }
    });

    $("#volumes_table").attr("trAddCount", "1");
    $("#volumes_table").find(".fa-plus").each(function(i) {
        $(this).on("click", function(event) {
            addVolumesTableTr(this);
        });
    });

    $("#add-container-dialog").on("show.bs.modal", function(event) {
        var imageRepositoriesSelect = $("#image-repositories");
        imageRepositoriesSelect.empty();

        var imageTagsSelect = $("#image-tags");
        imageTagsSelect.empty();

        $.get("container/getRegistryCatalog",{nns:regName}, function(data) {
            if(data.success == true && data.obj) {
                var obj = $.parseJSON(data.obj);
                if(obj.repositories && obj.repositories instanceof Array) {
                    var _options = "";
                    for(var i=0; i<obj.repositories.length; i++) {
                        var _selected = '';
                        if(i == 0) {
                            _selected = 'selected=true';
                        }
                        _options = _options + '<option value="' + obj.repositories[i] + '" title="' + obj.repositories[i] + '" ' + _selected + '>' + obj.repositories[i] + '</option>';
                    }
                    imageRepositoriesSelect.append(_options);
                    imageRepositoriesSelect.trigger("change");
                }
            } else {
                $("#failure-dialog .fail-reason").text("get getRegistryCatalog error: " + data.message);
                showFailureDialog("failure-dialog");
            }
        });
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
		$("#timeZone").val(job.timeZone);
		$("#cron").val(job.cron);
		$("#shardingTotalCount").val(job.shardingTotalCount);
		$("#jobParameter").val(job.jobParameter);
		$("#shardingItemParameters").val(job.shardingItemParameters);
		$("#description").val(job.description);
		$("#queueName").val(job.queueName);
		$("#channelName").val(job.channelName);
		$("#loadLevel").val(job.loadLevel);
		$("#useDispreferList").val(job.useDispreferList);
		$("#localMode").val(job.localMode);
		$("#processCountIntervalSeconds").val(job.processCountIntervalSeconds);
		$("#timeout4AlarmSeconds").val(job.timeout4AlarmSeconds);
		$("#timeoutSeconds").val(job.timeoutSeconds);
		$("#pausePeriodDate").val(job.pausePeriodDate);
		$("#pausePeriodTime").val(job.pausePeriodTime);
		$("#showNormalLog").val(job.showNormalLog);
		$("#jobType").attr("disabled","disabled");
		$("#addJobTitle").html("复制作业");
		setJobTypeConfig(job.jobType);
        reloadPreferListProvided(job.jobName, job.preferList, function() {
            $("#addJobTitle").html("复制作业");
            $("#add-job-dialog").modal("show").css({height:'100%', 'overflow-y':'scroll'});
            $("#jobName").focus();
        });
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
    
    $(document).on("click", "#shard-all-at-once", function(event) {
    	$("#shard-all-at-once-confirm-dialog").modal("show");
	});
    
    $(document).on("click", ".batch-migrate-job", function(event) {
    	var jobNames = "";
		$(".batchInput").each(function(){
			if($(this).is(":checked")){
    			jobNames += $(this).attr("jobName") + ",";
    		}
		});
    	if(isNullOrEmpty(jobNames)) {
			$("#failure-dialog .fail-reason").text("没有可以“迁移”的作业，请勾选！");
			showFailureDialog("failure-dialog");
			return;
		}
    	
    	jobNames = jobNames.substring(0, jobNames.length-1);
    	
    	var migrateJobTasksSelectDiv = $("#batch-migrate-job-tasks-select");
        migrateJobTasksSelectDiv.empty();
        $.get("job/batchTasksMigrateEnabled", {nns:regName}, function(data) {
            if(data.success == true) {
                var jobMigrateInfo = data.obj;
                $("#batch-migrate-jobName").html(jobNames);
                $("#batch-migrate-tasks-old").html(jobMigrateInfo.tasksOld.toString());
                var tasksMigrateEnabled = data.obj.tasksMigrateEnabled;
                if(tasksMigrateEnabled instanceof Array) {
                    for(var i=0; i<tasksMigrateEnabled.length; i++) {
                        var taskOption = "<option value='" + tasksMigrateEnabled[i] + "'>" + tasksMigrateEnabled[i] + "</option>";
                        migrateJobTasksSelectDiv.append(taskOption);
                    }
                }
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        });
        var jobObj = new Object();
        $("#batch-migrate-job-dialog").modal("show", jobObj);
	});

	$(document).on("click", ".batch-set-prefer-executors", function(event) {
        var jobNames = "";
        $(".batchInput").each(function(){
            if($(this).is(":checked")){
                jobNames += $(this).attr("jobName") + ",";
            }
        });
        if(isNullOrEmpty(jobNames)) {
            $("#failure-dialog .fail-reason").text("请勾选至少一个作业！");
            showFailureDialog("failure-dialog");
            return;
        }

        jobNames = jobNames.substring(0, jobNames.length-1);

        var migrateJobTasksSelectDiv = $("#batch-set-prefer-executors-select");
        migrateJobTasksSelectDiv.empty();
        $.get("job/batchSetPreferExecutorsEnabled", {nns:regName}, function(data) {
            if(data.success == true) {
                $("#batch-set-prefer-executors-jobName").html(jobNames);
                $("#batch-preferList").empty();
                var preferListProvided = data.obj;
                for (var i = 0; i < preferListProvided.length; i++) {
                    var temp = preferListProvided[i];
                    var executorName = temp.executorName;
                    var optionTitle = executorName;
                    if (temp.type == "ONLINE") {
                        if (temp.noTraffic == true) {
                            optionTitle = optionTitle + "(无流量)";
                        }
                        $("#batch-preferList").append("<option value='" + executorName + "'>" + optionTitle + "</option>");
                    } else if (temp.type == "DOCKER") {
                        var tips = "容器";
                        if (temp.noTraffic == true) {
                            tips = tips + "，" + "无流量";
                        }
                        optionTitle = optionTitle + "(" + tips + ")";
                        $("#batch-preferList").append("<option value='@" + executorName + "'>" + optionTitle + "</option>");
                    }
                }
                $("#batch-preferList").selectpicker('refresh');
                onPreferListSelectChanged($("#batch-preferList"));
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        });
        var jobObj = new Object();
        $("#batch-set-prefer-executors-dialog").modal("show", jobObj);
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

    $(document).on("click", "#containers-operation-save-token", function(event) {
        var token = $("#containers-operation-token").val();
        if(isNullOrEmpty(token)) {
            alert("token不能为空");
            return;
        }
        $loading.show();
        $.post("container/saveOrUpdateContainerToken", {"keyValue['token']" : token,nns:regName}, function(data) {
            if(data.success == true) {
                $("#add-container-dialog-token").val(token);
                showSuccessDialogWithCallback(function() {location.reload(true);});
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() { $loading.hide(); });
    });

    $(document).on("click", "#add-container-dialog-save-token", function(event) {
        var token = $("#add-container-dialog-token").val();
        if(isNullOrEmpty(token)) {
            alert("token不能为空");
            return;
        }
        $loading.show();
        $.post("container/saveOrUpdateContainerToken", {"keyValue['token']" : token,nns:regName}, function(data) {
            if(data.success == true) {
                $("#containers-operation-token").val(token);
                showSuccessDialog();
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() { $loading.hide(); });
    });

    $(document).on("click", "#containers-operation-save-user-pwd", function(event) {
        var userName = $("#containers-operation-userName").val();
        if(isNullOrEmpty(userName)) {
            alert("userName不能为空");
            return;
        }
        var password = $("#containers-operation-password").val();
        if(isNullOrEmpty(password)) {
            alert("password不能为空");
            return;
        }
        $loading.show();
        $.post("container/saveOrUpdateContainerToken", {"keyValue['userName']": userName, "keyValue['password']": password, nns:regName}, function(data) {
            if(data.success == true) {
                $("#add-container-dialog-userName").val(userName);
                $("#add-container-dialog-password").val(password);
                showSuccessDialogWithCallback(function() {location.reload(true);});
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() { $loading.hide(); });
    });

    $(document).on("click", "#add-container-dialog-save-user-pwd", function(event) {
        var userName = $("#add-container-dialog-userName").val();
        if(isNullOrEmpty(userName)) {
            alert("userName不能为空");
            return;
        }
        var password = $("#add-container-dialog-password").val();
        if(isNullOrEmpty(password)) {
            alert("password不能为空");
            return;
        }
        $loading.show();
        $.post("container/saveOrUpdateContainerToken", {"keyValue['userName']": userName, "keyValue['password']":password, nns:regName}, function(data) {
            if(data.success == true) {
                $("#containers-operation-userName").val(userName);
                $("#containers-operation-password").val(password);
                showSuccessDialog();
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() { $loading.hide(); });
    });

    $("#migrate-job-dialog-confirm-btn").on('click', function(event) {
        var $btn = $(this).button('loading');
        var jobName = $("#migrate-jobName").html();
        var newTask = $("#migrate-job-tasks-select").find("option:selected").val();
        $.post("job/migrateJobNewTask", {jobName : jobName, newTask : newTask,nns:regName}, function(data) {
            if(data.success == true) {
                showSuccessDialogWithCallback(function() {location.reload(true);});
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() { $btn.button('reset'); });
        return false;
    });
    
    $("#batch-migrate-job-dialog-confirm-btn").on('click', function(event) {
        var $btn = $(this).button('loading');
        var jobName = $("#batch-migrate-jobName").html();
        var newTask = $("#batch-migrate-job-tasks-select").find("option:selected").val();
        $.post("job/batchMigrateJobNewTask", {jobNames : jobName, newTask : newTask,nns:regName}, function(data) {
            if(data.success == true) {
                showSuccessDialogWithCallback(function() {location.reload(true);});
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() { $btn.button('reset'); });
        return false;
    });    

    $("#batch-set-prefer-executors-confirm-btn").on('click', function(event) {
        var $btn = $(this).button('loading');
        var jobName = $("#batch-set-prefer-executors-jobName").html();
        if($("#batch-preferList").val() != null) {
            var newPreferExecutors = $("#batch-preferList").val().toString();
        }
        if(newPreferExecutors && newPreferExecutors.slice(-1) == ",") {
            $("#failure-dialog .fail-reason").text("'无优先Executor'只能单选");
            showFailureDialog("failure-dialog");
            $btn.button('reset');
            return false;
        }
        $.post("job/batchSetPreferExecutors", {jobNames : jobName, newPreferExecutors : newPreferExecutors,nns:regName}, function(data) {
            if(data.success == true) {
                showSuccessDialogWithCallback(function() {location.reload(true);});
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() { $btn.button('reset'); });
        return false;
    });

    $("#add-container-dialog-confirm-btn").on('click', function(event) {
        var $btn = $(this).button('loading'),
        taskIdPre = $("#taskId").prev(".input-group-addon:first").text(), 
        taskId = $("#taskId").val(), 
        instances = $("#containerInstance").val(),
        cpus = $("#cpus").val(),
        mem = $("#memory").val(),
        cmd = $("#cmd").val(),
        privileged = $("#privileged-div input[name='privileged']:checked").val(),
        force_pull_image = $("#force_pull_image-div input[name='force_pull_image']:checked").val();
        var token;
        var userName;
        var password;
        var postJson = {};
        if(containerType == "VDOS") {
            token = $("#add-container-dialog-token").val();
            if(isNullOrEmpty(token)) {
                alert("token不能为空");
                $btn.button('reset');
                return;
            }
            postJson["containerToken.keyValue['token']"] = token;
        } else {
            userName = $("#add-container-dialog-userName").val();
            if(isNullOrEmpty(userName)) {
                alert("userName不能为空");
                $btn.button('reset');
                return;
            }
            postJson["containerToken.keyValue['userName']"] = userName;
            password = $("#add-container-dialog-password").val();
            if(isNullOrEmpty(password)) {
                alert("password不能为空");
                $btn.button('reset');
                return;
            }
            postJson["containerToken.keyValue['password']"] = password;
        }

        var imageRepositoriesVal = $("#image-repositories").find("option:selected").val();
        if(isNullOrEmpty(imageRepositoriesVal)) {
            alert("repository不能为空");
            $btn.button('reset');
            return;
        }
        var imageTagsVal = $("#image-tags").find("option:selected").val();
        if(isNullOrEmpty(imageTagsVal)) {
            alert("tags不能为空");
            $btn.button('reset');
            return;
        }
        var image = imageRepositoriesVal + ":" + imageTagsVal;
        if(isNullOrEmpty(taskId)) {
            alert("资源标识不能为空");
            $btn.button('reset');
            return;
        }
        if(isNullOrEmpty(cpus)) {
            alert("CPU数不能为空");
            $btn.button('reset');
            return;
        }
        if(isNullOrEmpty(mem)) {
            alert("内存不能为空");
            $btn.button('reset');
            return;
        }
        if(isNullOrEmpty(instances)) {
            alert("实例数不能为空");
            $btn.button('reset');
            return;
        }
        if(isNullOrEmpty(image)) {
            alert("镜像名不能为空");
            $btn.button('reset');
            return;
        }
        if(isNaN(parseInt(cpus))) {
            alert("CPU数必须为数字");
            $btn.button('reset');
            return;
        }
        if(parseInt(cpus) <= 0) {
            alert("CPU数必须大于0");
            $btn.button('reset');
            return;
        }
        if(isNaN(parseInt(mem))) {
            alert("内存数必须为数字");
            $btn.button('reset');
            return;
        }
        if(parseInt(mem) < 1024) {
            alert("内存数不能小于1024");
            $btn.button('reset');
            return;
        }
        if(isNaN(parseInt(instances))) {
            alert("实例数数必须为数字");
            $btn.button('reset');
            return;
        }
        if(parseInt(instances) <= 0) {
            alert("实例数必须为正数");
            $btn.button('reset');
            return;
        }

        postJson["taskId"] = taskIdPre + taskId;
        postJson["cmd"] = cmd;
        postJson["cpus"] = cpus;
        postJson["mem"] = mem;
        postJson["instances"] = instances;

        $("#constraints_table tbody tr").each(function(i) {
            postJson["constraints["+i+"][0]"] = $(this).find("input[name='attribute']").val();
            postJson["constraints["+i+"][1]"] = $(this).find("input[name='operator']").val();
            postJson["constraints["+i+"][2]"] = $(this).find("input[name='value']").val();
        });

        $("#env_table tbody tr").each(function(i) {
            postJson["env['" + $(this).find("input[name='key']").val() + "']"] = $(this).find("input[name='value']").val();
        });

        postJson["privileged"] = privileged;
        postJson["forcePullImage"] = force_pull_image;

        $("#parameters_table tbody tr").each(function(i) {
            postJson["parameters["+i+"]['key']"] = $(this).find("input[name='key']").val();
            postJson["parameters["+i+"]['value']"] = $(this).find("input[name='value']").val();
        });

        $("#volumes_table tbody tr").each(function(i) {
            postJson["volumes["+i+"]['containerPath']"] = $(this).find("input[name='containerPath']").val();
            postJson["volumes["+i+"]['hostPath']"] = $(this).find("input[name='hostPath']").val();
            postJson["volumes["+i+"]['mode']"] = $(this).find("input[type='radio']:checked").val();
        });

        postJson["image"] = image;
        postJson["nns"] = regName;

        $.post("container/addContainer", postJson,
        	function(data) {
                if(data.success == true) {
                    $("#add-container-dialog-confirm-btn").modal("hide");
                    if($("#nested").val() == "true") {
                        reloadPreferListProvided(null, "", function() {
                            $("#add-container-dialog").modal("hide");
                        });
                        showSuccessDialog();
                    }else{
                        showSuccessDialogWithCallback(function() {location.reload(true);});
                    }
                } else {
                    $("#failure-dialog .fail-reason").text(data.message);
                    showFailureDialog("failure-dialog");
                }
            }
        ).always(function() { $btn.button('reset'); });
    });

    $("#alter-instance-dialog-confirm-btn").on('click', function(event) {
        var $btn = $(this).button('loading'),taskId = $("#showTask").html(),instances = $("#alterInstance").val();
        if(isNaN(parseInt(instances))) {
            alert("实例数必须为数字");
            $btn.button('reset');
            return;
        }
        if(parseInt(instances) < 0) {
            alert("实例数不能为负数");
            $btn.button('reset');
            return;
        }
        $.post("container/updateContainerInstances",{taskId:taskId,instances:instances,nns:regName}, function(data) {
            if(data.success == true) {
                $("#alter-instance-dialog-confirm-btn").modal("hide");
                showSuccessDialogWithCallback(function() {location.reload(true);});
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() { $btn.button('reset'); });
    });

    $("#add-scale-job-dialog-check-and-forecast-cron").on('click', function(event) {
        var timeZone = $("#add-scale-job-dialog-timeZone").val();
        var cron = $("#add-scale-job-dialog-cron").val();
        checkAndForecastCron(timeZone, cron);
    });

    $("#see-scale-job-dialog-check-and-forecast-cron").on('click', function(event) {
        var timeZone = $("#see-scale-job-dialog-timeZone").val();
        var cron = $("#see-scale-job-dialog-cron").val();
        checkAndForecastCron(timeZone, cron);
    });

    $("#add-scale-job-dialog-confirm-btn").on('click', function(event) {
        var $btn = $(this).button('loading');
        var taskId = $("#add-scale-job-dialog-showTask").text();
        var jobDesc = $("#add-scale-job-dialog-jobDesc").val();
        var instances = $("#add-scale-job-dialog-instances").val();
        var timeZone = $("#add-scale-job-dialog-timeZone").val();
        var cron = $("#add-scale-job-dialog-cron").val();
        if(isNullOrEmpty(taskId)) {
            alert("taskId不能为空");
            $btn.button('reset');
            return false;
        }
        if(isNullOrEmpty(jobDesc)) {
            alert("计划名称不能为空");
            $btn.button('reset');
            return false;
        }
        if(isNullOrEmpty(instances)) {
            alert("实例数不能为空");
            $btn.button('reset');
            return false;
        }
        if(isNaN(parseInt(instances))) {
            alert("实例数必须为数字");
            $btn.button('reset');
            return;
        }
        if(parseInt(instances) < 1) {
            alert("实例数不能为小于1");
            $btn.button('reset');
            return;
        }
        if(isNullOrEmpty(timeZone)) {
            alert("timeZone不能为空");
            $btn.button('reset');
            return false;
        }
        if(isNullOrEmpty(cron)) {
            alert("Cron不能为空");
            $btn.button('reset');
            return false;
        }
        $.post("container/addContainerScaleJob", {nns:regName, taskId:taskId, jobDesc:jobDesc, instances:instances, timeZone:timeZone, cron:cron}, function(data) {
            if(data.success == true) {
                $("#add-scale-job-dialog").modal("hide");
                showSuccessDialogWithCallback(function(){
                    location.reload(true);
                });
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() {$btn.button('reset');});
    });

    $("#see-scale-job-dialog-enable-confirm-btn").on('click', function(event) {
        var $btn = $(this).button('loading');
        var taskId = $("#see-scale-job-dialog-showTask").text();
        var jobName = $("#see-scale-job-dialog-jobName").val();
        var jobDesc = $("#see-scale-job-dialog-jobDesc").val();
        var enable = $("#see-scale-job-dialog-enable-span").attr("enable");
        $.post("container/enableContainerScaleJob", {nns:regName, jobName:jobName, enable:enable}, function(data) {
            if(data.success == true) {
                $.get("container/getContainerScaleJobVo", {nns:regName, taskId:taskId, jobName:jobName}, function(data) {
                    if(data.success == true) {
                        var iconId=$("#see-scale-job-dialog-iconId").val();
                        if(data.obj.enabled == "false") {
                            $("#see-scale-job-dialog-enable-span").attr("enable", "true");
                            $("#see-scale-job-dialog-enable-span").html("启用");
                            $("#see-scale-job-dialog-enable-confirm-btn").removeClass("btn-warning");
                            $("#see-scale-job-dialog-enable-confirm-btn").addClass("btn-success");
                            $("#" + iconId).attr("src", "image/icon-mini-suspended-color.png");
                            $("#" + iconId).attr("title", "点击启用该伸缩计划");
                            $("#" + iconId).attr("enable", "true");
                        } else {
                            $("#see-scale-job-dialog-enable-span").attr("enable", "false");
                            $("#see-scale-job-dialog-enable-span").html("禁用");
                            $("#see-scale-job-dialog-enable-confirm-btn").removeClass("btn-success");
                            $("#see-scale-job-dialog-enable-confirm-btn").addClass("btn-warning");
                            $("#" + iconId).attr("src", "image/icon-mini-running-color.png");
                            $("#" + iconId).attr("title", "点击禁用该伸缩计划");
                            $("#" + iconId).attr("enable", "false");
                        }
                        showSuccessDialog();
                    } else {
                        $("#failure-dialog .fail-reason").text(data.message);
                        showFailureDialog("failure-dialog");
                    }
                });
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() {$btn.button('reset');});
    });

    $("#see-scale-job-dialog-delete-confirm-btn").on('click', function(event) {
        var $btn = $(this).button('loading');
        var taskId = $("#see-scale-job-dialog-showTask").text();
        var jobName = $("#see-scale-job-dialog-jobName").val();
        var jobDesc = $("#see-scale-job-dialog-jobDesc").val();
        $.post("container/deleteContainerScaleJob", {nns:regName, taskId:taskId, jobName:jobName, jobDesc:jobDesc}, function(data) {
            if(data.success == true) {
                $("#see-scale-job-dialog").modal("hide");
                showSuccessDialogWithCallback(function() {
                    location.reload(true);
                });
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() {$btn.button('reset');});
    });

    $("#icon-enable-scale-job-dialog-confirm-btn").on('click', function(event) {
        var $btn = $(this).button('loading');
        var iconId = $("#icon-enable-scale-job-dialog-iconId").val();
        var taskId = $("#icon-enable-scale-job-dialog-taskId").val();
        var jobName = $("#icon-enable-scale-job-dialog-jobName").val();
        var jobDesc = $("#icon-enable-scale-job-dialog-jobDesc").val();
        var enable = $("#icon-enable-scale-job-dialog-enable").val();
        $.post("container/enableContainerScaleJob", {nns:regName, jobName:jobName, enable:enable}, function(data) {
            if(data.success == true) {
                $.get("container/getContainerScaleJobVo", {nns:regName, taskId:taskId, jobName:jobName}, function(data) {
                    $("#icon-enable-scale-job-dialog").modal("hide");
                    if(data.success == true) {
                        if(data.obj.enabled == "false") {
                            $("#" + iconId).attr("enable", "true");
                            $("#" + iconId).attr("src", "image/icon-mini-suspended-color.png");
                            $("#" + iconId).attr("title", "点击启用该伸缩计划");
                        } else {
                            $("#" + iconId).attr("enable", "false");
                            $("#" + iconId).attr("src", "image/icon-mini-running-color.png");
                            $("#" + iconId).attr("title", "点击禁用该伸缩计划");
                        }
                        showSuccessDialog();
                    } else {
                        $("#failure-dialog .fail-reason").text(data.message);
                        showFailureDialog("failure-dialog");
                    }
                });
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() {$btn.button('reset');});
    });

    $("#add-job-dialog-confirm-btn").on('click', function(event) {
    	var $btn = $(this).button('loading'),jobType = $("#jobType").val(), jobName = $("#jobName").val(),originJobName = $("#originJobName").val(),jobClass = $("#jobClass").val(), queueName = $("#queueName").val(),channelName = $("#channelName").val(),
    			timeZone = $("#timeZone").val(), cron = $("#cron").val(),shardingTotalCount = $("#shardingTotalCount").val(),shardingItemParameters = $("#shardingItemParameters").val(),jobParameter = $("#jobParameter").val().trim(),description = $("#description").val(),
    			loadLevel = $("#loadLevel").val(),preferList = "",useDispreferList = $("#useDispreferList").val(),localMode = $("#localMode").val(),processCountIntervalSeconds = $("#processCountIntervalSeconds").val(),
    			timeout4AlarmSeconds = $("#timeout4AlarmSeconds").val(),timeoutSeconds = $("#timeoutSeconds").val(),pausePeriodDate = $("#pausePeriodDate").val(),pausePeriodTime = $("#pausePeriodTime").val(),showNormalLog = $("#showNormalLog").val(),isCopyJob = $("#isCopyJob").val();
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
					return false;
				}
			}
        	var shardingItemStr = "";
        	for(var i=0;i<shardingItemParametersArr.length;i++){
        		var kvPare = shardingItemParametersArr[i].split("=");
        		if (kvPare.length < 2) {
        			alert("作业分片参数有误，必需是key=value的形式");
		        	$("#shardingItemParameters").focus();
                    $btn.button('reset');
                    return false;
        		}
        		var shardingItem = kvPare[0];
        		if(shardingItemStr.indexOf(shardingItem) != -1){
        			alert("作业分片参数有误，分片号不能相同");
	        		$("#shardingItemParameters").focus();
                    $btn.button('reset');
                    return false;
        		}
				shardingItemStr += shardingItem + ",";
        	}
        }
        if($("#preferListProvided").val() != null) {
            preferList = $("#preferListProvided").val().toString();
        }
		$.post("executor/checkAndAddJobs",{jobName: jobName,jobClass:jobClass,channelName:channelName,queueName:queueName,
			jobType:jobType,timeZone:timeZone,cron:cron,shardingTotalCount:shardingTotalCount,jobParameter:jobParameter,
			shardingItemParameters:shardingItemParameters,description:description,loadLevel:loadLevel,preferList:preferList,
			useDispreferList:useDispreferList,localMode:localMode,processCountIntervalSeconds:processCountIntervalSeconds,
			timeout4AlarmSeconds:timeout4AlarmSeconds,timeoutSeconds:timeoutSeconds,pausePeriodDate:pausePeriodDate,isCopyJob:isCopyJob,originJobName:originJobName,
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
    
    $("#shard-all-at-once-confirm-dialog").on("shown.bs.modal", function (event) {
    	$("#shard-all-at-once-confirm-dialog-confirm-btn").unbind('click').click(function() {
    		var $btn = $(this).button('loading');
    		$.post("executor/shardAllAtOnce", {nns:regName}, function (data) {
    			$("#shard-all-at-once-confirm-dialog").modal("hide");
    			if(data.success == true) {
    				showSuccessDialogWithCallback(function(){location.reload(true);});
    			} else {
    				$("#failure-dialog .fail-reason").text(data.object.message);
	            	showFailureDialog("failure-dialog");
    			}
    		}).always(function() { $btn.button('reset'); });
    		return false;
    	});
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
				},
                complete:function(result) {
                	$btn.button('reset');
                }
			});
		return false;
	});

    $(document).on("click", "#check-and-forecast-cron", function(event) {
        var timeZone = $("#timeZone").val();
        var cron = $("#cron").val();
        checkAndForecastCron(timeZone, cron);
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
    	$logstashTable.ajax.url("logstash?jn="+selectedJobName+"&st="+st+"&et="+et+"&nns="+regName+"&ns="+ns).load();
    }); 
    if ($("#jobs").is(':visible')) {
    	$loading.show();
        renderJobsOverview();
    } else if ($("#servers").is(':visible')) {
    	$loading.show();
        renderServersOverview();
    } else if($("#containers").is(':visible')){
        $loading.show();
        renderContainersOverview();
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

	function showTrafficConfirmDialog(obj) {
        var executor = $(obj).data('executor');
        var operation = $(obj).data('operation');
        var confirmReason = null;
        if(operation == "extract") {
            confirmReason = "确认要 摘取 Executor(" + executor + ") 的流量吗?";
        } else {
            confirmReason = "确认要 恢复 Executor(" + executor + ") 的流量吗?";
        }
        $("#traffic-executor-confirm-dialog .confirm-reason").text(confirmReason);
        $("#traffic-executor-confirm-dialog").modal("show", obj);
    }
	
    function showRemoveExecutorConfirmDialog(obj) {
    	var confirmReason = "确认要删除Executor：（" + $(obj).data('executor') + "）吗?";
    	$("#remove-executor-confirm-dialog .confirm-reason").text(confirmReason);
    	$("#remove-executor-confirm-dialog").modal("show", obj);
    }

    function showMigrateJobDialog(obj) {
        var migrateJobTasksSelectDiv = $("#migrate-job-tasks-select");
        migrateJobTasksSelectDiv.empty();
        var jobName = $(obj).attr("data-jobname");
        $.get("job/tasksMigrateEnabled", {nns:regName, jobName:jobName}, function(data) {
            if(data.success == true) {
                var jobMigrateInfo = data.obj;
                $("#migrate-jobName").html(jobMigrateInfo.jobName);
                $("#migrate-tasks-old").html(jobMigrateInfo.tasksOld.toString());
                var tasksMigrateEnabled = data.obj.tasksMigrateEnabled;
                if(tasksMigrateEnabled instanceof Array) {
                    for(var i=0; i<tasksMigrateEnabled.length; i++) {
                        var taskOption = "<option value='" + tasksMigrateEnabled[i] + "'>" + tasksMigrateEnabled[i] + "</option>";
                        migrateJobTasksSelectDiv.append(taskOption);
                    }
                }
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        });
        $("#migrate-job-dialog").modal("show", obj);
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
                	+ "<td>" + status + "</td>" 
                	+ "<td>" + loadLevel + "</td>" 
                	+ "<td>" + shardingTotalCount + "</td>" 
                	+ "<td style='width: 300px; word-wrap:break-word;word-break:break-all;'>" + list[i].shardingList + "</td>"
                	+ "<td>" + cron + "</td>" 
                	+ "<td>" + degreeMap[list[i].jobDegree] + "</td>"
                	+ "<td>" + list[i].groups + "</td>"
                	+ "<td id='showDescription_"+i+"'></td>";
                var trClass = "";
                var operationBtn = "启用";
                var operationBtnClass = "btn btn-success";
                if(isJobEnabled == true){
                	operationBtn = "禁用";
                	operationBtnClass = "btn btn-warning";
                }
                var operationTd = "";
                if(list[i].migrateEnabled && list[i].migrateEnabled == true) {
                    operationTd = "<button class='btn btn-success' data-jobname='" + jobName + "' onclick='showMigrateJobDialog(this);'>迁移</button>";
                }
                operationTd = operationTd + "<button operation='change-jobStatus' class='"+operationBtnClass+"' data-jobstate='" + isJobEnabled + "' data-jobname='" + jobName + "' data-target='#change-jobStatus-confirm-dialog' onclick='showChangeJobConfirmDialog(this);'>"+operationBtn+"</button> ";
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
                $("#showDescription_"+i).text(list[i].description);// 使用text来赋值，防止&gt;等被转义成>
            }
            jobsViewDataTable = $("#jobs-overview-tbl").DataTable({
                "sDom":"<'row'<'col-sm-2'l><'col-sm-10'f>><'row'<'col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>", // see dataTables.bootstrap.min.js
                "destroy": true,
            	"oLanguage": language,
                "search": {"regex":true,"smart":false},
                "initComplete": function() {
                	$("#jobs-overview-tbl_filter input").attr("title","搜索作业名，支持正则").keyup(function(){
                		var term = $(this).val(),
                        regex =  term ;
                		jobsViewDataTable.columns(1).search(regex, true, false).draw();
                	});
                },
                "createdRow": function( row, data, dataIndex ) {
                    var jobName = list[dataIndex].jobName;
                },
            	"aoColumnDefs": [{"bSortable": false,"aTargets": [0, 6, 10]}], // set the columns unSort
                "aaSorting": [[3, "desc" ],[1, "asc" ],[1, "asc" ]], // set init sorting
                "aLengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "所有"]]
			});
            $("#jobs-overview-tbl_filter label").before(jobOperation);
            
            $.get("job/getAllJobGroups", {nns:regName}, function (data) {
        		if(!data){
        			return;
        		}
        		var options = "";
        		for(var i=0;i<data.length;i++) {
        			var groups = data[i];
        			options += "<option value='"+groups+"'>" + groups + "</option>";
        		}
        		$("#groupSelect").append(options);
            }).always(function() {});
            
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

    function renderContainersOverview() {
        $("#containers-operation-token").val("")
        $("#containers-operation-userName").val("");
        $("#containers-operation-password").val("");
        $("#add-container-dialog-token").val("");
        $("#add-container-dialog-userName").val("");
        $("#add-container-dialog-password").val("");
        if(containerType == "VDOS") {
            $("#token-div").removeAttr("hidden");
            $("#user-pwd-div").attr("hidden", "hidden");
            $("#add-container-dialog-token-div").removeAttr("hidden");
            $("#add-container-dialog-user-pwd-div").attr("hidden", "hidden");
            $.get("container/getContainerToken",{nns:regName}, function(data) {
                if(data.success) {
                    $("#containers-operation-token").val(data.obj.keyValue.token == null ? "" : data.obj.keyValue.token);
                    $("#add-container-dialog-token").val(data.obj.keyValue.token == null ? "" : data.obj.keyValue.token);
                } else {
                    $("#failure-dialog .fail-reason").text("get token error:" + data.message);
                    showFailureDialog("failure-dialog");
                }
            });
        } else {
            $("#token-div").attr("hidden", "hidden");
            $("#user-pwd-div").removeAttr("hidden");
            $("#add-container-dialog-token-div").attr("hidden", "hidden");
            $("#add-container-dialog-user-pwd-div").removeAttr("hidden");
            $.get("container/getContainerToken", {nns: regName}, function(data) {
                if(data.success) {
                    $("#containers-operation-userName").val(data.obj.keyValue.userName == null ? "" : data.obj.keyValue.userName);
                    $("#containers-operation-password").val(data.obj.keyValue.password == null ? "" : data.obj.keyValue.password);
                    $("#add-container-dialog-userName").val(data.obj.keyValue.userName == null ? "" : data.obj.keyValue.userName);
                    $("#add-container-dialog-password").val(data.obj.keyValue.password == null ? "" : data.obj.keyValue.password);
                } else {
                    $("#failure-dialog .fail-reason").text("get token error:" + data.message);
                    showFailureDialog("failure-dialog");
                }
            });
        }

        $.get("container/getContainerVos",{nns:regName}, function (data) {
            if(data.success) {
                if (containersViewDataTable) {
                    containersViewDataTable.destroy();
                }
                $("#containers-tbl tbody").empty();
                var containerVos = data.obj;
                if(containerVos) {
                    for (var i = 0, length = containerVos.length;i < length;i++) {
                        var containerVo = containerVos[i];
                        var containerExecutorVos = containerVo.containerExecutorVos;
                        var bindingJobNames = containerVo.bindingJobNames;
                        var containerStatus = containerVo.containerStatus;
                        var containerConfig = containerVo.containerConfig;
                        var createTime = containerVo.createTime;
                        var containerScaleJobVos = containerVo.containerScaleJobVos;
                        var instancesConfigured = containerVo.instancesConfigured;
                        var rowspan = 0;
                        if(containerExecutorVos && containerExecutorVos instanceof Array) {
                            rowspan = containerExecutorVos.length;
                        }
                        if(rowspan == 0) {
                            rowspan = 1;
                        }
                        var taskId = containerVo.taskId;
                        var executorName0 = "";
                        var ip0 = "";
                        var runningJobNames0 = "";
                        if(containerExecutorVos && containerExecutorVos.length > 0) {
                            executorName0 = containerExecutorVos[0].executorName ? containerExecutorVos[0].executorName : "";
                            ip0 = containerExecutorVos[0].ip ? containerExecutorVos[0].ip : "";
                            runningJobNames0 = containerExecutorVos[0].runningJobNames ? containerExecutorVos[0].runningJobNames : "";
                        }
                        var baseTr = "<tr><td rowspan="+rowspan+">";
                        baseTr = baseTr + "<a href='javascript:void(0);' onclick='seeContainerInfoDialog(this)' title='查看资源详细信息' taskId='"+taskId+"'>"+taskId+"</a>";
                        baseTr = baseTr + "</td>"
                        + "<td>" + executorName0 + "</td>"
                        + "<td>" + ip0 + "</td>"
                        + "<td>" + runningJobNames0 + "</td>"
                        + "<td rowspan="+rowspan+">"+ bindingJobNames+ "</td>"
                        + "<td rowspan="+rowspan+">"+ containerStatus + "</td>"
                        + "<td rowspan="+rowspan+" style='width: 350px; word-wrap:break-word;word-break:break-all;'>"+ containerConfig + "</td>"
                        + "<td rowspan="+rowspan+">" + createTime + "</td>";
                        baseTr = baseTr + "<td rowspan="+rowspan+">";
                        if(containerScaleJobVos && containerScaleJobVos instanceof Array) {
                            for(var j=0; j<containerScaleJobVos.length; j++) {
                                var containerScaleJobVo = containerScaleJobVos[j];
                                var jobName = containerScaleJobVo.jobName;
                                var jobDesc = containerScaleJobVo.jobDesc;
                                var instances = containerScaleJobVo.instances;;
                                var cron = containerScaleJobVo.cron;
                                var enabled = containerScaleJobVo.enabled;
                                var enable;
                                if(enabled == "false") {
                                    baseTr = baseTr + "<img src='image/icon-mini-suspended-color.png' title='点击启用该伸缩计划'";
                                    enable = "true";
                                } else {
                                    baseTr = baseTr + "<img src='image/icon-mini-running-color.png' title='点击禁用该伸缩计划'";
                                    enable = "false";
                                }
                                var iconId = "icon-enable-" + jobName;
                                baseTr = baseTr + " id='"+iconId+"' style='cursor:pointer; width:16px; height:16px;' taskId='"+taskId+"' jobName='"+jobName+"' jobDesc='"+jobDesc+"' enable='"+enable+"' onclick='iconEnableScaleJob(this);'/>  ";
                                var jobDescCut = jobDesc.substring(0, 10);
                                if(jobDesc.length > 10) {
                                    jobDescCut = jobDescCut + "...";
                                }
                                baseTr = baseTr + "<a href='javascript:void(0);' iconId='"+iconId+"' taskId='"+taskId+"' jobName='"+jobName+"' onclick='seeScaleJob(this);' title='"+jobDesc+"'>"+jobDescCut+"</a>";
                                if(j<containerScaleJobVos.length-1) {
                                    baseTr = baseTr + "<br/>";
                                }
                            }
                        }
                        baseTr = baseTr + "</td>"
                            + "<td rowspan="+rowspan+"><button class='btn btn-danger' taskId='"+taskId+"' onclick='removeContainer(this);'>销毁</button><br/>"
                            + "<button class='btn btn-success' taskId='"+taskId+"' currentInstance='"+instancesConfigured+"' onclick='elasticInstance(this);'>伸缩</button></br>"
                            + "<button class='btn btn-warning' taskId='"+taskId+"' onclick='addScaleJob(this);'>计划</button></td></tr>";
                        for(var j=1;j < rowspan;j++){
                            baseTr += "<tr>"
                                + "<td>" + containerExecutorVos[j].executorName + "</td>"
                                + "<td>" + containerExecutorVos[j].ip + "</td>"
                                + "<td>" + containerExecutorVos[j].runningJobNames + "</td>"
                                + "</tr>";
                        }
                        $("#containers-tbl tbody").append(baseTr);
                    }
                }
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        }).always(function() { $loading.hide(); });
    }

    function reloadPreferListProvided(jobName, preferList, callback) {
	    var postData = {};
	    if(jobName == null) {
	        postData = {nns: regName};
        } else {
	        postData = {nns: regName, jobName: jobName};
        }
        $.get("job/getAllExecutors", postData, function (data) {
            $("#preferListProvided").empty();
            if(data.success == true) {
                var preferListArr = preferList.split(",");
                var preferListProvided = data.obj;
                expandPreferListSelect(preferListArr, preferListProvided, $("#preferListProvided"));
            }
            if(callback) callback();
        });
    }

    function expandPreferListSelect(preferListArr, preferListProvided, selectObj) {
        selectObj.empty();
        for (var i = 0; i < preferListProvided.length; i++) {
            var temp = preferListProvided[i];
            var selected = false;
            var optionValue = temp.executorName;
            var optionTitle = optionValue;
            var tips = "";
            if (temp.type == "DOCKER") {
                optionValue = "@" + optionValue;
                tips = "容器";
            } else if (temp.type == "OFFLINE") {
                tips = "已离线";
            } else if (temp.type == "DELETED") {
                tips = "已删除";
            }
            if (temp.noTraffic == true) {
                if (tips == "") {
                    tips = "无流量";
                } else {
                    tips = tips + ", " + "无流量";
                }
            }
            if (tips != "") {
                optionTitle = optionTitle + "(" + tips + ")";
            }
            $.each(preferListArr, function (index, preferValue) {
                if (preferValue == optionValue) {
                    selected = true;
                }
            });
            var option = "<option value='" + optionValue + "'";
            if (selected) {
                option = option + " selected";
            }
            option = option + ">" + optionTitle + "</option>";
            selectObj.append(option);
        }
        selectObj.selectpicker('refresh');
        onPreferListSelectChanged(selectObj);
    }

    function onPreferListSelectChanged(selectObj) {
        var containerSelected = false;
        var options = selectObj.find("option");
        if(options) {
            for(var i=0; i<options.length; i++) {
                var option = options[i];
                if(option.selected && option.value && option.value[0] == "@") {
                    containerSelected = true;
                    break;
                }
            }
        }
        if(containerSelected) {
            for(var i=0; i<options.length; i++) {
                var option = options[i];
                if(!(option.selected) && option.value && option.value[0] == "@") {
                    $(option).attr("disabled", "disabled");
                }
            }
        } else {
            for(var i=0; i<options.length; i++) {
                var option = options[i];
                if(option.value && option.value[0] == "@") {
                    $(option).removeAttr("disabled");
                }
            }
        }
        selectObj.selectpicker('refresh');
    }

    $('#preferListProvided').on('changed.bs.select', function (e) {
        onPreferListSelectChanged($('#preferListProvided'));
    });
    
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
            		var serverInfo = serverInfos[i];
            		var status = serverInfo.status,jobStatus = serverInfo.jobStatus,sharding = serverInfo.sharding, lastBeginTime = serverInfo.lastBeginTime,executorName = serverInfo.executorName,trClass = "",removeBtnClass = "",removeBtnTitle="";
            		loadLevels.push(serverInfo.totalLoadLevel),hasSharding = serverInfo.hasSharding, noTraffic = serverInfo.noTraffic;
            		if ("ONLINE" === status) {
            			trClass = "success";
            			onlines ++;
            			removeBtnClass = "disabled";
            			removeBtnTitle="无法删除ONLINE的Executor";
            			exeNames.push(serverInfo.executorName);
            		} else {
            			trClass = "warning";
            			offlines ++;
            			lastBeginTime = "";
            			removeBtnTitle="点击进行删除该Executor";
            		}
            		var trafficButton = "";
            		if(noTraffic == false) { // 可以摘取流量
                        trafficButton = "<button class='btn btn-warning' data-executor='" + executorName + "' data-operation='extract' onclick='showTrafficConfirmDialog(this);'" + ">流量摘取</button>";
                    } else {
                        trafficButton = "<button class='btn btn-info' data-executor='" + executorName + "' data-operation='recover' onclick='showTrafficConfirmDialog(this);'" + ">流量恢复</button>";
                    }
            		var removeButton = "<button operation='removeExecutor' title='"+removeBtnTitle+"' class='btn btn-danger "+removeBtnClass+"' data-executor='" + executorName + "' onclick='showRemoveExecutorConfirmDialog(this);' "+removeBtnClass+">删除</button>";
            		var baseTd = "<td><input class='batchDelExecutorInput' executorName='"+executorName+"' removeBtnClass='"+removeBtnClass+"' type='checkbox' onclick='clickBatchDelExecutorInputCheckBox(this);'/></td>"
            		+ "<td>" + executorName + "</td>" 
            		+ "<td>" + serverInfo.serverIp + "</td>" 
            		+ "<td>" + serverInfo.totalLoadLevel + "</td>" 
            		+ "<td>" + sharding + "</td>" 
            		+ "<td>" + status + "</td>"
            		+ "<td>" + lastBeginTime + "</td>"
            		+ "<td>" + serverInfo.version + "</td>"
            		+ "<td>" + trafficButton + removeButton + "</td>";
            		$("#servers-overview-tbl tbody").append("<tr class='" + trClass + "'>" + baseTd + "</tr>");
            	}
            }
            
            
            $("#executors-count").text(onlines+offlines);
            $("#executors-online-count").text(onlines);
            $("#executors-offline-count").text(offlines);
            serversViewDataTable = $("#servers-overview-tbl").DataTable({
                "sDom":"<'row'<'col-sm-2'l><'col-sm-10'f>><'row'<'col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>", // see dataTables.bootstrap.min.js
                "destroy": true,
            	"oLanguage": language,
            	"aoColumnDefs": [{"bSortable": false,"aTargets": [0, 8]}], // set the columns unSort
                "aaSorting": [[5, "desc" ],[2, "asc" ]], // set init sorting
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

    function seeContainerInfoDialog(obj) {
        var taskId = $(obj).attr('taskId');
        $.get("container/getContainerDetail", {taskId:taskId,nns:regName}, function(data) {
            if(data.success) {
                $("#see-container-info-textare").html(JSON.stringify($.parseJSON(data.obj), undefined, 4));
            } else {
                $("#see-container-info-textare").html(data.message);
            }
            $("#see-container-info-showTask").html(taskId);
            $("#see-container-info-dialog").modal("show", obj);
        });
    }

    function addEnvTableTr(obj, special) {
        var trAddCount = parseInt($("#env_table").attr("trAddCount"));
        trAddCount = trAddCount + 1;
        $("#env_table").attr("trAddCount", trAddCount);
        var trContent = '<tr>' +
            '<td>' +
                '<input type="text" class="form-control" name="key" />' +
            '</td>' +
            '<td>' +
                '<input type="text" class="form-control" name="value" />' +
            '</td>' +
            '<td>' +
                '<button class="fa fa-plus" onclick="addEnvTableTr(this);"></button><br/>' +
                '<button class="fa fa-minus" onclick="delEnvTableTr(this);"></button>' +
            '</td>';
        if(special == 0) {
            $("#env_table tbody").prepend(trContent);
        } else {
            $(obj).closest("tr").after(trContent);
        }
    }

    function delEnvTableTr(obj) {
        $(obj).closest("tr").remove();
    }

    function addConstraintsTableTr(obj, special) {
        var trAddCount = parseInt($("#constraints_table").attr("trAddCount"));
        trAddCount = trAddCount + 1;
        $("#constraints_table").attr("trAddCount", trAddCount);
        var trContent = '<tr>' +
            '<td>' +
                '<input type="text" class="form-control" name="attribute" />' +
            '</td>' +
            '<td>' +
                '<input type="text" class="form-control" name="operator" />' +
            '</td>' +
            '<td>' +
                '<input type="text" class="form-control" name="value" />' +
            '</td>' +
            '<td>' +
                '<button class="fa fa-plus" onclick="addConstraintsTableTr(this);"></button><br/>' +
                '<button class="fa fa-minus" onclick="delConstraintsTableTr(this);"></button>' +
            '</td>';
        if(special == 0) {
            $("#constraints_table tbody").prepend(trContent);
        } else {
            $(obj).closest("tr").after(trContent);
        }
    }

    function delConstraintsTableTr(obj) {
        $(obj).closest("tr").remove();
    }

    function addParametersTableTr(obj, special) {
        var trAddCount = parseInt($("#parameters_table").attr("trAddCount"));
        trAddCount = trAddCount + 1;
        $("#parameters_table").attr("trAddCount", trAddCount);
        var trContent = '<tr>' +
            '<td>' +
                '<input type="text" class="form-control" name="key" />' +
            '</td>' +
            '<td>' +
                '<input type="text" class="form-control" name="value" />' +
            '</td>' +
            '<td>' +
                '<button class="fa fa-plus" onclick="addParametersTableTr(this);"></button><br/>' +
                '<button class="fa fa-minus" onclick="delParametersTableTr(this);"></button>' +
            '</td>';
        if(special == 0) {
            $("#parameters_table tbody").prepend(trContent);
        } else {
            $(obj).closest("tr").after(trContent);
        }
    }

    function delParametersTableTr(obj) {
        $(obj).closest("tr").remove();
    }

    function addVolumesTableTr(obj) {
        var trAddCount = parseInt($("#volumes_table").attr("trAddCount"));
        trAddCount = trAddCount + 1;
        $("#volumes_table").attr("trAddCount", trAddCount);
        var trContent = '<tr>' +
            '<td>' +
                '<input type="text" class="form-control disabled" name="containerPath" />' +
            '</td>' +
            '<td>' +
                '<input type="text" class="form-control disabled" name="hostPath" />' +
            '</td>' +
            '<td>' +
                '<div class="radio">' +
                    '<label>' +
                        '<input type="radio" name="mode' + trAddCount + '" value="RW" checked="checked" />RW' +
                    '</label>' +
                    '<br/>' +
                    '<label>' +
                        '<input type="radio" name="mode' + trAddCount + '" value="RO"/>RO' +
                    '</label>' +
                '</div>' +
            '</td>' +
            '<td>' +
                '<button class="fa fa-plus" onclick="addVolumesTableTr(this);"></button><br/>' +
                '<button class="fa fa-minus" onclick="delVolumesTableTr(this);"></button>' +
            '</td>';
        $(obj).closest("tr").after(trContent);
    }

    function delVolumesTableTr(obj) {
        $(obj).closest("tr").remove();
    }

    function elasticInstance(obj) {
        var taskId = $(obj).attr('taskId');
        var currentInstance = $(obj).attr('currentInstance');
        $("#showTask").html(taskId);
        $("#currentInstance").html(currentInstance);
        $("#alterInstance").val(currentInstance);
        $("#alter-instance-dialog").modal("show");
    }

    function addScaleJob(obj) {
        $.get("container/getTimeZoneIds", {nns:regName}, function(data) {
            if(data.success == true) {
                $("#add-scale-job-dialog-timeZone").empty();
                if(data.obj instanceof Array) {
                    for(var i in data.obj) {
                        var tmp = data.obj[i];
                        var option = "<option value='" + tmp + "'";
                        if(tmp == "Asia/Shanghai") {
                            option = option + " selected"
                        }
                        option = option + ">" + tmp + "</option>";
                        $("#add-scale-job-dialog-timeZone").append(option);
                    }
                }
                $("#add-scale-job-dialog-timeZone").selectpicker('refresh');
                var taskId = $(obj).attr('taskId');
                $("#add-scale-job-dialog-showTask").html(taskId);
                $("#add-scale-job-dialog").modal("show");
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        });
    }

    function seeScaleJob(obj) {
        var iconId = $(obj).attr("iconId");
        var taskId = $(obj).attr('taskId');
        var jobName = $(obj).attr('jobName');
        $.get("container/getContainerScaleJobVo", {nns:regName, taskId:taskId, jobName:jobName}, function(data) {
            if(data.success == true) {
                var scaleJobVo = data.obj;
                $("#see-scale-job-dialog-showTask").html(taskId);
                $("#see-scale-job-dialog-iconId").val(iconId);
                $("#see-scale-job-dialog-jobName").val(scaleJobVo.jobName);
                $("#see-scale-job-dialog-jobDesc").val(scaleJobVo.jobDesc);
                $("#see-scale-job-dialog-instances").val(scaleJobVo.instances);
                $("#see-scale-job-dialog-timeZone").val(scaleJobVo.timeZone);
                $("#see-scale-job-dialog-cron").val(scaleJobVo.cron);
                if(scaleJobVo.enabled == "false") {
                    $("#see-scale-job-dialog-enable-span").attr("enable", "true");
                    $("#see-scale-job-dialog-enable-span").html("启用");
                    $("#see-scale-job-dialog-enable-confirm-btn").removeClass("btn-warning");
                    $("#see-scale-job-dialog-enable-confirm-btn").addClass("btn-success");
                } else {
                    $("#see-scale-job-dialog-enable-span").attr("enable", "false");
                    $("#see-scale-job-dialog-enable-span").html("禁用");
                    $("#see-scale-job-dialog-enable-confirm-btn").removeClass("btn-success");
                    $("#see-scale-job-dialog-enable-confirm-btn").addClass("btn-warning");
                }
                $("#see-scale-job-dialog").modal("show");
            } else {
                $("#failure-dialog .fail-reason").text(data.message);
                showFailureDialog("failure-dialog");
            }
        });
    }

    function iconEnableScaleJob(obj) {
        var iconId = $(obj).attr("id");
        var taskId = $(obj).attr("taskId");
        var jobName = $(obj).attr("jobName");
        var jobDesc = $(obj).attr("jobDesc");
        var enable = $(obj).attr("enable");

        $("#icon-enable-scale-job-dialog-text").empty();
        if(enable == "true") {
            $("#icon-enable-scale-job-dialog-text").html("确认<font color='red'>启用</font>伸缩计划<font color='red'>"+jobDesc + "</font>？");
        } else {
            $("#icon-enable-scale-job-dialog-text").html("确认<font color='red'>禁用</font>伸缩计划<font color='red'>"+jobDesc + "</font>？");
        }
        $("#icon-enable-scale-job-dialog-iconId").val(iconId);
        $("#icon-enable-scale-job-dialog-taskId").val(taskId);
        $("#icon-enable-scale-job-dialog-jobName").val(jobName);
        $("#icon-enable-scale-job-dialog-jobDesc").val(jobDesc);
        $("#icon-enable-scale-job-dialog-enable").val(enable);

        $("#icon-enable-scale-job-dialog").modal("show");
    }

    function checkAndForecastCron(timeZone, cron) {
        $.post("job/checkAndForecastCron", {timeZone: timeZone, cron : cron,nns:regName}, function (data) {
            var msg = "检验结果：";
            if(data.success == true) {
                msg += "成功";
                msg += "<hr>";
                msg += "作业时区：" + timeZone;
                msg += "<hr>";
                msg += "预测执行时间点：<br><br>";
                msg += data.obj.times;
            } else {
                msg += "失败";
                msg += "<hr>";
                msg += "作业时区：" + timeZone;
                msg += "<hr>";
                msg += "错误信息：<br><br>";
                msg += data.message;
            }
            showPromptDialogWithMsgAndCallback("check-and-forecast-cron-prompt-dialog", msg, null);
        });
    }

    function removeContainer(obj) {
        var taskId = $(obj).attr('taskId');
        $("#remove-container-confirm-dialog").modal("show",taskId);
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
    