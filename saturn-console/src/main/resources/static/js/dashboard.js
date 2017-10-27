var cleanButtonClass = ($("#authorizeSaturnConsoleDashBoardAllPrivilege").val() == "true")?"show":"hide",
		causeMap = {"NOT_RUN": "过时未跑", "NO_SHARDS": "没有分片", "EXECUTORS_NOT_READY": "没有executor能运行该作业"};
var abnormalJobDataTable, timeout4AlarmJobDataTable, unableFailoverJobDataTable, abnormalContainerDataTable;
$(function() {
	window.parent.setActiveTab("#dashboardTab");
	window.parent.releaseRegName();

    $loading = $("#loading");

	renderZks(function() {
        $("#zks").change(function(){
            renderAll();
        });
        renderAll();
    });

    $("#dashboard-confirm-dialog").on("shown.bs.modal", function (event) {
    	var button = $(event.relatedTarget),
    		nns = button.data("nns"),
    		job = button.data("job"),
    		cleanType = button.data("clean"),
    		url;
    	if (cleanType === "shard") {
    		url = "dashboard/cleanShardingCount";
    	} else if (cleanType === "one-analyse") {
    		url = "dashboard/cleanOneJobAnalyse";
    	} else if (cleanType === "all-analyse") {
    		url = "dashboard/cleanAllJobAnalyse";
    	} else if (cleanType === "one-executor") {
    		url = "dashboard/cleanOneJobExecutorCount";
    	}
    	$("#dashboard-confirm-dialog-confirm-btn").unbind('click').click(function() {
    		var $btn = $(this).bootstrapBtn('loading');
    		$.post(url, {nns : nns, job: job}, function (data) {
    			$("#dashboard-confirm-dialog").modal("hide");
    			if("ok" == data) {
    				$("#success-dialog .success-msg").text("操作完成，请稍后刷新页面获取最新数据。");
    				showSuccessDialogAndNotHide();
    			} else {
                	$("#failure-dialog .fail-reason").text(data);
                	showFailureDialog("failure-dialog");
    			}
    		}).always(function() { $btn.bootstrapBtn('reset'); });
    		return false;
    	});
    });

    $("#setUnnormalJobToRead-confirm-dialog").on("shown.bs.modal", function (event) {
    	$("#setUnnormalJobToRead-confirm-dialog-confirm-btn").unbind('click').click(function() {
    		var button = $(event.relatedTarget);
    		var formData = getFormData();
    		formData.uuid = button.attr('uuid');
    		$.post("dashboard/setUnnormalJobMonitorStatusToRead", formData, function (data0) {
    			$("#setUnnormalJobToRead-confirm-dialog").modal("hide");
    			if(data0 =='ok') {
    				$(event.relatedTarget).attr("disabled","disabled");
    			}
    		});
    	});
    });

    $("#setTimeout4AlarmJobToRead-confirm-dialog").on("shown.bs.modal", function (event) {
    	$("#setTimeout4AlarmJobToRead-confirm-dialog-confirm-btn").unbind('click').click(function() {
    		var button = $(event.relatedTarget);
    		var formData = getFormData();
    		formData.uuid = button.attr('uuid');
    		$.post("dashboard/setTimeout4AlarmJobMonitorStatusToRead", formData, function (data0) {
    			$("#setTimeout4AlarmJobToRead-confirm-dialog").modal("hide");
    			if(data0 =='ok') {
    				$(event.relatedTarget).attr("disabled","disabled");
    			}
    		});
    	});
    });
});

function renderZks(callback) {
	$.get("loadZks", {}, function(data) {
		var zks = data.clusters, currentZk = data.currentZk, options="";
		$("#zks").empty().append("<option>所有集群</option>");
		for(var i in zks) {
			var disabled = "", alias = zks[i].zkAlias;
			
			if (zks[i].offline) {
				disabled = " disabled='disabled' ";
				alias += "[offline]"
			}
			if (currentZk == zks[i].zkClusterKey) {
				options += "<option " +disabled+ " selected='selected' value='"+zks[i].zkClusterKey+"'>" + alias + "</option>";
			} else {
				options += "<option " +disabled+ " value='"+zks[i].zkClusterKey+"'>" + alias + "</option>";
			}
		}
		$("#zks").append(options);

		callback();
	});
}

function reload(){
    var s = document.getElementById("auto-refresh-seconds");
    if(s.innerHTML == 0){
        location.reload(true);
        return false;
    }
    s.innerHTML = s.innerHTML * 1 - 1;
}

function renderAll() {
    $('.collapse').on('hidden.bs.collapse', function () {
		$(this).parent().find(".fa").removeClass("fa-minus").addClass("fa-plus");
	}).on('shown.bs.collapse', function () {
		$(this).parent().find(".fa").addClass("fa-minus").removeClass("fa-plus");
	});

	$('[href="#count"]').click(function(event) {
		$loading.show();
        renderCountOverview();
    });
    $('[href="#domain"]').click(function(event) {
    	$loading.show();
    	renderDomainOverview();
    });
    $('[href="#jobs"]').click(function(event) {
    	$loading.show();
    	renderJobsOverview();
    });
    $('[href="#servers"]').click(function(event) {
    	$loading.show();
        renderServersOverview();
    });
    $('[href="#warn"]').click(function(event) {
    	$loading.show();
    	renderWarnOverview();
    });

	if ($("#count").is(':visible')) {
    	$loading.show();
        renderCountOverview();
    } else if ($("#domain").is(':visible')) {
    	$loading.show();
    	renderDomainOverview();
    } else if($("#jobs").is(':visible')){
    	$loading.show();
    	renderJobsOverview();
    } else if($("#servers").is(':visible')) {
        $loading.show();
        renderServersOverview();
    } else if($("#warn").is(':visible')) {
    	$loading.show();
        renderWarnOverview();
    }
}

function renderCountOverview(){
	$(".collapse").collapse("show");
	renderCountJob();
	renderAbnormalJob();
}

function renderDomainOverview(){
	$(".collapse").collapse("show");
	renderDomainRank();
	renderDomainProcessCount();
	renderTop10FailDomain();
	renderTop10UnStableDomain();
	renderVersionDomainNumber();
}

function renderJobsOverview(){
	$(".collapse").collapse("show");
	renderJobRank();
	renderTop10FailJob();
	renderTop10ActiveJob();
	renderTop10LoadJob();
}

function renderServersOverview(){
	$(".collapse").collapse("show");
	renderTop10FailExe();
	renderTop10LoadExecutor();
	renderVersionExecutorNumber();
}

function renderWarnOverview(){
	$(".collapse").collapse("show");
	renderAbnormalJob();
	renderTimeout4AlarmJob();
	renderUnableFailoverJob();
	renderAbnormalContainer();
}

function getFormData() {
    var formData = {};
    if($("#zks").get(0).selectedIndex == 0) {
        formData = {allZkCluster: true};
    } else {
        var zkClusterKey = $("#zks").val();
        formData = {allZkCluster: false, zkClusterKey: zkClusterKey};
    }
    return formData;
}

function renderCountJob() {
	$.post("dashboard/count", getFormData(), function (data) {
		var jobCount = data["jobCount"];
        var domainCount =data["domainCount"];
		var executorInDockerCount = data["executorInDockerCount"];
		var executorNotInDockerCount = data["executorNotInDockerCount"];
		if(jobCount >= 0) {
            $("#jobCount").html(jobCount);
        } else {
            $("#jobCount").html('COUNTING');
        }
        if(domainCount >= 0) {
            $("#domainCount").html(domainCount);
        } else {
            $("#domainCount").html('COUNTING');
        }
		if(executorInDockerCount >= 0 && executorNotInDockerCount >= 0){
			$("#executorCount").html(executorNotInDockerCount +'+'+ executorInDockerCount);
		}else if(executorInDockerCount >= 0){
			$("#executorCount").html(executorInDockerCount +'+COUNTING');
		}else if(executorNotInDockerCount >= 0){
			$("#executorCount").html('COUNTING+'+ executorNotInDockerCount);
		}
	}).always(function() { $loading.hide(); });
}

function renderTop10FailDomain() {
	$.post("dashboard/top10FailDomain", getFormData(), function (data0) {
		var domains = [], seriesData = [];
		if(data0) {
            var data = JSON.parse(data0);
            for (var d in data) {
                if (!data[d].failureRateOfAllTime || data[d].failureRateOfAllTime == 0) {
                    continue;
                }
                domains.push(data[d].domainName);
                data[d].y = data[d].failureRateOfAllTime;
                data[d].url = "overview?name=" + data[d].nns;
                seriesData.push(data[d]);
            }
		}
		var series = [{showInLegend: false,data:seriesData}];
		$('#top10-fail-domain').highcharts({
		    chart: {
		        type: 'column'
		    },
		    credits : {
				enabled : false
			},
		    title: {
		        text: ''
		    },
		    xAxis: {
		        categories: domains,
		        labels: {
	                rotation: -20
		        }
		    },
		    yAxis: {
		        min: 0,
		        title: {
		            text: '失败率(小数)'
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
		        useHTML:true,
		        hideDelay: 1000,
		        formatter: function() {
                    return '<b>' + this.point.category + '</b><br/>错误率: : ' + this.point.y
                    	+ '<br/>执行总数: ' + this.point.processCountOfAllTime + '<br/>失败总数: ' + this.point.errorCountOfAllTime + '<br/><button class="' + cleanButtonClass + '" data-clean="all-analyse" data-nns="' + this.point.nns + '" data-target="#dashboard-confirm-dialog" onclick="cleanAllJobAnalyse(this);">清除zk</button>';
                }
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
		        },
		        series: {
		            cursor: 'pointer',
		            events: {
		                click: function(e) {
		                	location.href = e.point.url;
			    	    }
			    	}
		        }
		    },
		    series: series
		});
	}).always(function() { $loading.hide(); });
}
function renderTop10UnStableDomain() {
	$.post("dashboard/top10UnstableDomain", getFormData(), function (data0) {
		var domains = [], seriesData = [];
		if(data0) {
            var data = JSON.parse(data0);
            for (var d in data) {
                if (!data[d].shardingCount || data[d].shardingCount == 0) {
                    continue;
                }
                domains.push(data[d].domainName);
                data[d].y = data[d].shardingCount;
                data[d].url = "overview?name=" + data[d].nns;
                seriesData.push(data[d]);
            }
		}
		var series = [{showInLegend: false,data:seriesData}];
		$('#top10-unstable-domain').highcharts({
		    chart: {
		        type: 'column'
		    },
		    credits : {
				enabled : false
			},
		    title: {
		        text: ''
		    },
		    xAxis: {
		        categories: domains,
		        labels: {
	                rotation: -20
		        }
		    },
		    yAxis: {
		        min: 0,
		        title: {
		            text: '分片次数'
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
		        useHTML:true,
		        hideDelay: 1000,
		        formatter: function() {
                    return '<b>' + this.point.category + '</b><br/>分片次数: ' + this.point.y + '<br/><button class="' + cleanButtonClass + '" data-clean="shard" data-nns="' + this.point.nns + '" data-target="#dashboard-confirm-dialog" onclick="cleanShardingCount(this);">清除zk</button>';
                }
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
		        },
		        series: {
		            cursor: 'pointer',
		            events: {
		                click: function(e) {
		                	location.href = e.point.url;
			    	    }
			    	}
		        }
		    },
		    series: series
		});
	}).always(function() { $loading.hide(); });
}
function renderTop10FailExe() {
	$.post("dashboard/top10FailExe", getFormData(), function (data0) {
		var exes = [], seriesData = [];
		if(data0) {
            var data = JSON.parse(data0);
            for (var d in data) {
                if (!data[d].failureRateOfTheDay || data[d].failureRateOfTheDay == 0) {
                    continue;
                }
                exes.push(data[d].executorName);
                data[d].y = data[d].failureRateOfTheDay;
                data[d].url = "overview?name=" + data[d].nns;
                seriesData.push(data[d]);
            }
		}
		var series = [{showInLegend: false,data:seriesData}];
		$('#top10-fail-exe').highcharts({
		    chart: {
		        type: 'column'
		    },
		    credits : {
				enabled : false
			},
		    title: {
		        text: ''
		    },
		    xAxis: {
		        categories: exes,
		        labels: {
	                rotation: -20
		        }
		    },
		    yAxis: {
		        min: 0,
		        title: {
		            text: '失败率(小数)'
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
		        useHTML:true,
		        hideDelay: 1000,
		        formatter: function() {
                    return '<b>' + this.point.category + '</b><br/>所属域: ' + this.point.domain
                    	+ '<br/>IP: ' + this.point.ip
                    	+ '<br/>总执行数: ' + this.point.processCountOfTheDay + '<br/>总失败数: ' + this.point.failureCountOfTheDay + '<br/>';
                }
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
		        },
		        series: {
		            cursor: 'pointer',
		            events: {
		                click: function(e) {
		                	location.href = e.point.url;
			    	    }
			    	}
		        }
		    },
		    series: series
		});
	}).always(function() { $loading.hide(); });
}

function renderTop10FailJob() {
	$.post("dashboard/top10FailJob", getFormData(), function (data0) {
		var jobs = [], seriesData = [];
		if(data0) {
            var data = JSON.parse(data0);
            for (var d in data) {
                if (!data[d].failureRateOfAllTime || data[d].failureRateOfAllTime == 0) {
                    continue;
                }
                jobs.push(data[d].jobName);
                data[d].y = data[d].failureRateOfAllTime;
                data[d].url = "job_detail?nns="+data[d].nns+"&jobName="+data[d].jobName;
                seriesData.push(data[d]);
            }
        }
		var series = [{showInLegend: false,data:seriesData}];
		$('#top10-fail-job').highcharts({
		    chart: {
		        type: 'column'
		    },
		    credits : {
				enabled : false
			},
		    title: {
		        text: ''
		    },
		    xAxis: {
		        categories: jobs,
		        labels: {
	                rotation: -20
		        }
		    },
		    yAxis: {
		        min: 0,
		        title: {
		            text: '失败率(小数)'
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
		        useHTML:true,
		        hideDelay: 1000,
		        formatter: function() {
                    return '<b>' + this.point.category + '</b><br/>所属域: ' + this.point.domainName
                    	+ '<br/>总执行数: ' + this.point.processCountOfAllTime + '<br/>总失败数: ' + this.point.errorCountOfAllTime + '<br/><button class="' + cleanButtonClass + '" data-clean="one-analyse" data-job=' + this.point.jobName + ' data-nns="' + this.point.nns + '" data-target="#dashboard-confirm-dialog" onclick="cleanOneJobAnalyse(this);">清除zk</button>';
                }
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
		        },
		        series: {
		            cursor: 'pointer',
		            events: {
		                click: function(e) {
		                	location.href = e.point.url;
			    	    }
			    	}
		        }
		    },
		    series: series
		});
	}).always(function() { $loading.hide(); });
}
function renderTop10ActiveJob() {
	$.post("dashboard/top10ActiveJob", getFormData(), function (data0) {
		var jobs = [], seriesData = [];
		if(data0) {
            var data = JSON.parse(data0);
            for (var d in data) {
                if (!data[d].processCountOfTheDay || data[d].processCountOfTheDay == 0) {
                    continue;
                }
                jobs.push(data[d].jobName);
                data[d].y = data[d].processCountOfTheDay;
                data[d].url = "job_detail?nns="+data[d].nns+"&jobName="+data[d].jobName;
                seriesData.push(data[d]);
            }
		}
		var series = [{showInLegend: false,data:seriesData}];
		$('#top10-active-job').highcharts({
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
		        categories: jobs,
		        labels: {
	                rotation: -20
		        }
		    },
		    yAxis: {
		        min: 0,
		        title: {
		            text: '当天执行次数'
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
		        useHTML:true,
		        hideDelay: 1000,
		        formatter: function() {
                    return '<b>' + this.point.jobName + '</b><br/>所属域: : ' + this.point.domainName
                    	+ '<br/>当天执行总数: ' + this.point.processCountOfTheDay + '<br/>当天失败数: ' + this.point.failureCountOfTheDay + '<br/><button class="' + cleanButtonClass + '" data-clean="one-executor" data-job=' + this.point.jobName + ' data-nns="' + this.point.nns + '" data-target="#dashboard-confirm-dialog" onclick="cleanOneJobExecutorCount(this);">清除zk</button>';
                }
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
		        },
		        series: {
		            cursor: 'pointer',
		            events: {
		                click: function(e) {
		                	location.href = e.point.url;
			    	    }
			    	}
		        }
		    },
		    series: series
		});
	}).always(function() { $loading.hide(); });
}
function renderTop10LoadJob() {
	$.post("dashboard/top10LoadJob", getFormData(), function (data0) {
		var jobs = [], seriesData = [];
		if(data0) {
            var data = JSON.parse(data0);
            for (var d in data) {
                if (!data[d].totalLoadLevel || data[d].totalLoadLevel == 0) {
                    continue;
                }
                jobs.push(data[d].jobName);
                data[d].y = data[d].totalLoadLevel;
                data[d].url = "job_detail?nns="+data[d].nns+"&jobName="+data[d].jobName;
                seriesData.push(data[d]);
            }
		}
		var series = [{showInLegend: false,data:seriesData}];
		$('#top10-load-job').highcharts({
		    chart: {
		        type: 'column'
		    },
		    credits : {
				enabled : false
			},
		    title: {
		        text: ''
		    },
		    xAxis: {
		        categories: jobs,
		        labels: {
	                rotation: -20
		        }
		    },
		    yAxis: {
		        min: 0,
		        title: {
		            text: '作业总负荷'
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
		        pointFormat: '所属域: {point.domainName}<br/>失败率: {point.failureRateOfAllTime}<br/>总负荷: {point.y}'
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
		        },
		        series: {
		            cursor: 'pointer',
		            events: {
		                click: function(e) {
		                	location.href = e.point.url;
			    	    }
			    	}
		        }
		    },
		    series: series
		});
	}).always(function() { $loading.hide(); });
}
function renderTop10LoadExecutor() {
	$.post("dashboard/top10LoadExecutor", getFormData(), function (data0) {
		var exes = [], seriesData = [];
		if(data0) {
            var data = JSON.parse(data0);
            for (var d in data) {
                if (!data[d].loadLevel || data[d].loadLevel == 0) {
                    continue;
                }
                exes.push(data[d].executorName);
                data[d].y = data[d].loadLevel;
                seriesData.push(data[d]);
            }
		}
		var series = [{showInLegend: false,data:seriesData}];
		$('#top10-load-executor').highcharts({
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
		        categories: exes,
		        labels: {
	                rotation: -20
		        }
		    },
		    yAxis: {
		        min: 0,
		        title: {
		            text: 'Executor总负荷'
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
		        pointFormat: '所属域: {point.domain}<br/>总负荷: {point.y}<br/>作业与分片: {point.jobAndShardings}'
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
function renderDomainProcessCount() {
	$.post("dashboard/domainProcessCount", getFormData(), function (data0) {
		var error = 0, count = 0, success = 0;
		if(data0) {
		    var data = JSON.parse(data0);
		    error = data.error;
		    count = data.count;
		    success = parseInt(count - error);
		}
		$("#domains-total").html(count);
		$('#domain-process-count-pie').highcharts({
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
                 name: '全域当天执行数据',
                 data: [
                     ['成功次数', success],
                     ['失败次数', error]
                 ]
            }]
         });
	}).always(function() { $loading.hide(); });
}

function renderDomainRank() {
	$.post("dashboard/loadDomainRank", getFormData(), function (data) {
		var rankArray = [];
		if(data) {
            for (var degree in data) {
                var oneDegree = []
                oneDegree.push(degreeMap[degree]+ ": " + data[degree], data[degree]);
                rankArray.push(oneDegree)
            }
		}
		$('#domain-rank-pie').highcharts({
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
                 name: '该等级域数量',
                 data: rankArray
            }]
         });
	}).always(function() { $loading.hide(); });
}

function renderJobRank() {
	$.post("dashboard/loadJobRank", getFormData(), function (data) {
		var rankArray = [];
		if(data) {
            for (var degree in data) {
                var oneDegree = []
                oneDegree.push(degreeMap[degree]+ ": " + data[degree], data[degree]);
                rankArray.push(oneDegree)
            }
		}
		$('#job-rank-pie').highcharts({
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
                 name: '该等级作业数量',
                 data: rankArray
            }]
         });
	}).always(function() { $loading.hide(); });
}

function renderAbnormalJob() {
	var unnormalJobTmp = $("#unnormal-job-template").html();
	$.post("dashboard/unnormalJob", getFormData(), function (data0) {
		var data = new Array();
		if(data0) {
		    data = JSON.parse(data0);
		}
		$("#abnormalJobCount").html(data.length);
		if(abnormalJobDataTable) {
		    abnormalJobDataTable.destroy();
		}
		var $tbody = $("#unnormal-job-table tbody"), trContent = "", unnormalJobTmp = $("#unnormal-job-template").html();
		$tbody.empty();
        for (var i in data) {
        	trContent += Mustache.render(unnormalJobTmp,{uuid:data[i].uuid,cause:causeMap[data[i].cause],degree:degreeMap[data[i].degree],jobDegree:degreeMap[data[i].jobDegree], jobName:data[i].jobName, domainName:data[i].domainName, nns:data[i].nns+"&", nextFireTime:data[i].timeZone + " " + data[i].nextFireTimeWithTimeZoneFormat});
        }
        $tbody.append(trContent);
        for (var i in data) {
            if(data[i].read && data[i].read == true) {
                $('#button-'+data[i].jobName).attr("disabled","disabled");
            }
        }
        abnormalJobDataTable = $("#unnormal-job-table").DataTable({"destroy": true,"oLanguage": language});
	}).always(function() { $loading.hide(); });
}

function setUnnormalJobMonitorStatusToRead(obj) {
    var domainName = $(obj).attr("domainName");
    var jobName = $(obj).attr("jobName");
	$("#setUnnormalJobToRead-confirm-dialog .confirm-reason").text("确定此异常作业（域名" + domainName + "，作业名" + jobName + "）不再发送告警信息吗？");
	$("#setUnnormalJobToRead-confirm-dialog").modal("show", obj);
}

function renderTimeout4AlarmJob() {
	$.post("dashboard/allTimeout4AlarmJob", getFormData(), function (data0) {
		var data = new Array();
        if(data0) {
            data = JSON.parse(data0);
        }
        if(timeout4AlarmJobDataTable) {
            timeout4AlarmJobDataTable.destroy();
        }
		var $tbody = $("#timeout4Alarm-job-table tbody"), trContent = "", timeout4AlarmJobTmp = $("#timeout4Alarm-job-template").html();
		$tbody.empty();
        for (var i in data) {
        	trContent += Mustache.render(timeout4AlarmJobTmp,{uuid:data[i].uuid,degree:degreeMap[data[i].degree],jobDegree:degreeMap[data[i].jobDegree], jobName:data[i].jobName, domainName:data[i].domainName, nns:data[i].nns+"&", timeout4AlarmSeconds:data[i].timeout4AlarmSeconds, timeoutItems:data[i].timeoutItems});
        }
        $tbody.append(trContent);
        for (var i in data) {
            if(data[i].read && data[i].read == true) {
                $('#button-'+data[i].jobName+'-timeout4AlarmJob').attr("disabled","disabled");
            }
        }
        timeout4AlarmJobDataTable = $("#timeout4Alarm-job-table").DataTable({"destroy": true, "oLanguage": language});
	}).always(function() { $loading.hide(); });
}

function setTimeout4AlarmJobMonitorStatusToRead(obj) {
    var domainName = $(obj).attr("domainName");
    var jobName = $(obj).attr("jobName");
	$("#setTimeout4AlarmJobToRead-confirm-dialog .confirm-reason").text("确定此超时作业（域名" + domainName + "，作业名" + jobName + "）不再发送告警信息吗？");
	$("#setTimeout4AlarmJobToRead-confirm-dialog").modal("show", obj);
}

function renderUnableFailoverJob() {
	var unableFailoverJobTmp = $("#unable-failover-job-template").html();
	$.post("dashboard/unableFailoverJob", getFormData(), function (data0) {
		var data = new Array();
        if(data0) {
            data = JSON.parse(data0);
        }
        if(unableFailoverJobDataTable) {
            unableFailoverJobDataTable.destroy();
        }
		var $tbody = $("#unable-failover-job-table tbody"), trContent = "", unableFailoverJobTmp = $("#unable-failover-job-template").html();
		$tbody.empty();
        for (var i in data) {
        	trContent += Mustache.render(unableFailoverJobTmp,{degree:degreeMap[data[i].degree], jobName:data[i].jobName, domainName:data[i].domainName, nns:data[i].nns+"&", jobDegree:degreeMap[data[i].jobDegree]});
        }
        $tbody.append(trContent);
        unableFailoverJobDataTable = $("#unable-failover-job-table").DataTable({"destroy": true,"oLanguage": language});
	}).always(function() { $loading.hide(); });
}

function renderAbnormalContainer() {
	$.post("dashboard/abnormalContainer", getFormData(), function (data0) {
		var data = new Array();
        if(data0) {
            data = JSON.parse(data0);
        }
        if(abnormalContainerDataTable) {
            abnormalContainerDataTable.destroy();
        }
		var $tbody = $("#abnormal-container-table tbody"), trContent = "", abnormalContainerTmp = $("#abnormal-container-template").html();
		$tbody.empty();
        for (var i in data) {
            if(data[i].cause == "CONTAINER_INSTANCE_MISMATCH") {
        	    trContent += Mustache.render(abnormalContainerTmp,{taskId:data[i].taskId, nns:data[i].nns + "&", domainName:data[i].domainName, degree:degreeMap[data[i].degree], cause:"运行实例数不匹配，期望" + data[i].configInstances + "个，实际" + data[i].runningInstances + "个"});
        	}
        }
        $tbody.append(trContent);
        abnormalContainerDataTable = $("#abnormal-container-table").DataTable({"destroy": true,"oLanguage": language});
	}).always(function() { $loading.hide(); });
}

function renderVersionDomainNumber() {
	$.post("dashboard/versionDomainNumber", getFormData(), function (data) {
		var rankArray = [];
		if(data) {
            for (var version in data) {
                var element = []
                if(version == "-1") {
                    element.push("未知版本:" + data[version], data[version]);
                } else {
                    element.push(version + ":" + data[version], data[version]);
                }
                rankArray.push(element)
            }
        }
        $('#version-domain-number').highcharts({
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
                 name: '该版本域数量',
                 data: rankArray
            }]
         });
	}).always(function() { $loading.hide(); });
}

function renderVersionExecutorNumber() {
	$.post("dashboard/versionExecutorNumber", getFormData(), function (data) {
		var rankArray = [];
		if(data) {
            for (var version in data) {
                var element = []
                if(version == "-1") {
                    element.push("未知版本:" + data[version], data[version]);
                } else {
                    element.push(version + ":" + data[version], data[version]);
                }
                rankArray.push(element)
            }
        }
        $('#version-executor-number').highcharts({
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
                 name: '该版本Executor数量',
                 data: rankArray
            }]
         });
	}).always(function() { $loading.hide(); });
}

function showDomainCharts() {
	renderDomainOverview();

	$("#count").removeClass("active");
	$("#domain").addClass("active");
	$("#jobs").removeClass("active");
	$("#servers").removeClass("active");
	$("#warn").removeClass("active");
	$("#count_tab").removeClass("active");
	$("#domain_tab").addClass("active");
	$("#jobs_tab").removeClass("active");
	$("#servers_tab").removeClass("active");
	$("#warn_tab").removeClass("active");

	$(".collapse").collapse("hide");
	setTimeout(function() {
		$("#top10-fail-domain-body").collapse("show");
		$("#top10-unstable-domain-body").collapse("show");
		$("#domain-rank-body").collapse("show");
		$("#domain-process-count-body").collapse("show");
		$("#version-domain-number-body").collapse("show");
	}, 600);
	location.href="#top10-fail-domain-body";
	return false;
}

function showJobCharts() {
	renderJobsOverview();

	$("#count").removeClass("active");
	$("#domain").removeClass("active");
	$("#jobs").addClass("active");
	$("#servers").removeClass("active");
	$("#warn").removeClass("active");
	$("#count_tab").removeClass("active");
	$("#domain_tab").removeClass("active");
	$("#jobs_tab").addClass("active");
	$("#servers_tab").removeClass("active");
	$("#warn_tab").removeClass("active");

	$(".collapse").collapse("hide");
	setTimeout(function() {
		$("#top10-fail-job-body").collapse("show");
		$("#top10-load-job-body").collapse("show");
		$("#top10-active-job-body").collapse("show");
		$("#unnormal-job-body").collapse("show");
		$("#job-rank-body").collapse("show");
		$("#unable-failover-job-body").collapse("show");
	}, 600);
	location.href="#top10-fail-job-body";
	return false;
}

function showExecutorCharts() {
	renderServersOverview();

	$("#count").removeClass("active");
	$("#domain").removeClass("active");
	$("#jobs").removeClass("active");
	$("#servers").addClass("active");
	$("#warn").removeClass("active");
	$("#count_tab").removeClass("active");
	$("#domain_tab").removeClass("active");
	$("#jobs_tab").removeClass("active");
	$("#servers_tab").addClass("active");
	$("#warn_tab").removeClass("active");

	$(".collapse").collapse("hide");
	setTimeout(function() {
		$("#top10-fail-exe-body").collapse("show");
		$("#top10-load-executor-body").collapse("show");
		$("#version-executor-number-body").collapse("show");
	}, 600);
	location.href="#top10-fail-exe-body";
	return false;
}

function showUnnormalJob() {
	renderWarnOverview();

	$("#count").removeClass("active");
	$("#domain").removeClass("active");
	$("#jobs").removeClass("active");
	$("#servers").removeClass("active");
	$("#warn").addClass("active");
	$("#count_tab").removeClass("active");
	$("#domain_tab").removeClass("active");
	$("#jobs_tab").removeClass("active");
	$("#servers_tab").removeClass("active");
	$("#warn_tab").addClass("active");

	$(".collapse").collapse("hide");
	setTimeout(function() {
		$("#unnormal-job-body").collapse("show");
	}, 600);
	location.href="#unnormal-job-body";
	return false;
}

function cleanShardingCount(btn) {
	$("#dashboard-confirm-dialog .confirm-reason").text("确定清零: "+$(btn).data("nns")+"/$SaturnExecutors/sharding/count吗？");
	$("#dashboard-confirm-dialog").modal("show", btn);
}

function cleanOneJobAnalyse(btn) {
	$("#dashboard-confirm-dialog .confirm-reason").text("确定清零: "+$(btn).data("nns")+"/$Jobs/"+$(btn).data("job")+"/analyse/processCount & errorCount");
	$("#dashboard-confirm-dialog").modal("show", btn);
}

function cleanOneJobExecutorCount(btn) {
	$("#dashboard-confirm-dialog .confirm-reason").text("确定清零: "+$(btn).data("nns")+"/$Jobs/"+$(btn).data("job")+"/servers/所有executor/processSuccessCount & processFailureCount");
	$("#dashboard-confirm-dialog").modal("show", btn);
}

function cleanAllJobAnalyse(btn) {
	$("#dashboard-confirm-dialog .confirm-reason").text("确定清零: "+$(btn).data("nns")+"/$Jobs/所有作业/analyse/processCount & errorCount");
	$("#dashboard-confirm-dialog").modal("show", btn);
}
