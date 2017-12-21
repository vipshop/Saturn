var namespace_info_table_DataTable, zk_cluster_info_table_DataTable, namespace_zkcluster_manager_table_DataTable;
var namespace_zkcluster_manager_operation;
$(function() {
    window.parent.setActiveTab("#regTab");
	window.parent.releaseRegName();

	$("#namespace_zkcluster_manager_operation_div").remove();
	namespace_zkcluster_manager_operation = '<div id="namespace_zkcluster_manager_operation_div" style="display: inline;">' +
                                                '<font color="red" size="2">【请务必同时只能一个人并且只使用一个浏览器来操作】</font>&nbsp;' +
                                                '<button class="btn btn-success" id="batch-move-namespace">批量迁移</button>&nbsp;' +
                                                '<button class="btn btn-warning" id="init-namespace-zkcluster-mapping">初始化映射表</button>&nbsp;' +
                                                '</div>';

	$("#refresh_registry_center").on("click", function() {
		var $btn = $(this).button("loading");
		$.get("registry_center/refreshRegCenter", function(data) {
			if(data.success == true) {
				window.parent.reloadTreeData();
                window.parent.contentFrame.location.href="registry_center_page";
			} else {
				showFailureDialogWithMsg("failure-dialog", data.message);
			}
		}).always(function() { $btn.button('reset'); });
	});
	
	$('[href="#namespace_info_tabpanel"]').on("click", function() {
		refreshNamespaceInfo();
    });
	$('[href="#zk_cluster_info_tabpanel"]').on("click", function() {
		refreshZkClusterInfo();
	});
	$('[href="#namespace_zkcluster_manager_tabpanel"]').on("click", function() {
		refreshNamespaceZkClusterMappingList();
    });
	if ($("#namespace_info_tabpanel").is(':visible')) {
		refreshNamespaceInfo();
	} else if ($("#zk_cluster_info_tabpanel").is(':visible')) {
		refreshZkClusterInfo();
	} else if ($("#namespace_zkcluster_manager_tabpanel").is(':visible')) {
		refreshNamespaceZkClusterMappingList();
	}

	$("#namespace_zkcluster_manager_table_select_all").on('click', function(event) {
	    if($(this).is(":checked")){
            $('.batchInput').prop('checked',true);// jquery1.9以后的版本，用attr有点问题，要用prop
        }else{
            $('.batchInput').prop('checked',false);
        }
    });

    $("#move-namespace-batch-dialog").on("shown.bs.modal", function (event) {
        $("#move-namespace-batch-dialog-confirm-btn").unbind('click').click(function() {
            var namespaces = $("#move-namespace-batch-dialog-namespace").html();
            var zkClusterKeyNew = "";
            if($("#move-namespace-batch-dialog-manual").attr("status") == "auto") {
                zkClusterKeyNew = $("#move-namespace-batch-dialog-zkCluster").val();
            } else {
                zkClusterKeyNew = $("#move-namespace-batch-dialog-zkCluster-input").val();
            }
            var updateDBOnly = $("#move-namespace-batch-dialog-updateDBOnly").is(":checked");
            $("#confirm-yes-dialog-title").html('域名：<pre>' + namespaces + '</pre>目标集群：<pre>' + zkClusterKeyNew + '</pre>只更新映射表：<pre>' + updateDBOnly + '</pre>确认批量迁移，请输入yes！');
            $("#giveMeYes").val("");
            $("#confirm-yes-dialog-confirm-btn").unbind('click').on("click", function() {
                var yes = $("#giveMeYes").val();
                if (yes === "yes") {
                    $("#move-namespace-batch-dialog").modal("hide");
                    $("#confirm-yes-dialog").modal("hide");
                    $("#move-namespace-batch-status-dialog-zkCluster").html(zkClusterKeyNew);
                    var split = namespaces.split(",");
                    if(split && split instanceof Array) {
                        $.post("registry_center/moveNamespaceBatch", {namespaces: namespaces, zkClusterKeyNew: zkClusterKeyNew, updateDBOnly: updateDBOnly}, function(data) {
                            if(data.success) {
                                var size = split.length;
                                $("#move-namespace-batch-status-dialog-data").html("成功：" + 0 + "<br/>失败：" + 0 + "<br/>忽略：" + 0 + "<br/>未做：" + size + "<br/>总共：" + size);
                                $("#move-namespace-batch-status-dialog-tips").css("display", "inline");
                                $("#move-namespace-batch-status-dialog-x").css("display", "none");
                                $("#move-namespace-batch-status-dialog").modal({backdrop:"static", keyboard: false});
                                $("#move-namespace-batch-status-dialog").modal("show");
                                getMoveNamespaceBatchStatus();
                            } else {
                                showFailureDialogWithMsg("failure-dialog", data.message);
                            }
                        });
                    } else {
                        alert("请选择要迁移的域");
                    }
                } else {
                    alert("确认请输入yes");
                }
            });
            $("#confirm-yes-dialog").modal({backdrop:"static", keyboard: false});
            $("#confirm-yes-dialog").modal("show");
        });
    });

    $("#move-namespace-batch-dialog-manual").on('click', function(event) {
        if($("#move-namespace-batch-dialog-manual").attr("status") == "auto") {
            $("#move-namespace-batch-dialog-zkCluster").attr("style", "display:none");
            $("#move-namespace-batch-dialog-zkCluster-input").removeAttr("style");
            $("#move-namespace-batch-dialog-manual").attr("status", "manual");
            $("#move-namespace-batch-dialog-manual").html("在线的集群");
        } else {
            $("#move-namespace-batch-dialog-zkCluster").removeAttr("style");
            $("#move-namespace-batch-dialog-zkCluster-input").attr("style", "display:none");
            $("#move-namespace-batch-dialog-manual").attr("status", "auto");
            $("#move-namespace-batch-dialog-manual").html("手动输入");
        }
    });

    $("#confirm-yes-dialog").on("shown.bs.modal", function (event) {
        $("#giveMeYes").focus();
    });

    $("#move-namespace-batch-status-dialog-x").on("click", function() {
        $("#move-namespace-batch-status-dialog").modal("hide");
        $.post("registry_center/clearMoveNamespaceBatchStatus", {}, function(data) {});
        refreshNamespaceZkClusterMappingList();
    });

    refreshNamespaceInfo();
});

function refreshNamespaceInfo() {
    $loading.show();
	$.get("registry_center/getNamespaceInfo", {}, function(data) {
		if(namespace_info_table_DataTable) {
			namespace_info_table_DataTable.destroy();
		}
        $("#namespace_info_table tbody").empty();
        if(data.success) {
    		var list = data.obj;
	        if(list && list instanceof Array) {
	            for (var i = 0;i < list.length;i++) {
            		var cluster = list[i];
            		var key = cluster.zkClusterKey;
            		var regCenterConfList = cluster.regCenterConfList;
            		if(regCenterConfList && regCenterConfList instanceof Array) {
            			for(var j=0; j < regCenterConfList.length; j++) {
	        				var regCenterConf = regCenterConfList[j];
			                var baseTd = "<td><a href='overview?name=" + regCenterConf.name + "/" + regCenterConf.namespace + "'>" + regCenterConf.namespace + "</a></td><td>"
			                    + regCenterConf.name + "</td><td>"
			                    + regCenterConf.version + "</td><td>"
			                    + key + "</td>";
			                $("#namespace_info_table tbody").append("<tr>" + baseTd + "</tr>");
            			}
            		}
	            }
	        }
	        namespace_info_table_DataTable = $("#namespace_info_table").DataTable({"oLanguage": language, "displayLength":100});
        } else {
            showFailureDialogWithMsg("failure-dialog", data.message);
        }
    }).always(function() { $loading.hide(); });
}

function refreshNamespaceZkClusterMappingList() {
    $loading.show();
	$.get("registry_center/getNamespaceZkclusterMappingList", {}, function(data) {
		if(namespace_zkcluster_manager_table_DataTable) {
			namespace_zkcluster_manager_table_DataTable.destroy();
		}
		$("#namespace_zkcluster_manager_table_select_all").prop('checked',false);
        $("#namespace_zkcluster_manager_table tbody").empty();
        if(data.success) {
    		var list = data.obj;
	        if(list && list instanceof Array) {
	            for (var i = 0;i < list.length;i++) {
            		var mapping = list[i];
                    var baseTd = "<td><input class='batchInput' type='checkbox' onclick='clickNamespace_zkcluster_manager_table_select_all(this);' namespace='" + mapping.namespace + "'/></td>"
                        + "<td>" + mapping.namespace + "</td>"
                        + "<td>" + mapping.zkClusterKey + "</td>"
                        + "<td>" + mapping.createTime + "</td>"
                        + "<td>" + mapping.createdBy + "</td>"
                        + "<td>" + mapping.lastUpdateTime + "</td>"
                        + "<td>" + mapping.lastUpdatedBy + "</td>";
                    $("#namespace_zkcluster_manager_table tbody").append("<tr>" + baseTd + "</tr>");
	            }
	        }
        } else {
            showFailureDialogWithMsg("failure-dialog", data.message);
        }
        namespace_zkcluster_manager_table_DataTable = $("#namespace_zkcluster_manager_table").DataTable({
            "sDom":"<'row'<'col-sm-2'l><'col-sm-10'f>><'row'<'col-sm-12'tr>><'row'<'col-sm-5'i><'col-sm-7'p>>", // see dataTables.bootstrap.min.js
            "destroy": true,
            "oLanguage": language,
            "aoColumnDefs": [{"bSortable":false,"aTargets":[0]}], // set the 0 column unSort
            "aaSorting": [[1, "asc"]], // set sort from the second column
            "displayLength":100,
            "aLengthMenu": [[10, 25, 50, 100, -1], [10, 25, 50, 100, "所有"]],
            "initComplete": function () {
                $("#namespace_zkcluster_manager_table_filter label").before(namespace_zkcluster_manager_operation);

                var column = this.api().column(2);
                var opDiv = $("#namespace_zkcluster_manager_operation_div");
                opDiv.find("select").remove();
                opDiv.find('span [name="oneBlank"]').remove();
                var select = $('<select class="form-control"><option value="">全部集群</option></select>')
                    .appendTo(opDiv)
                    .on('change', function () {
                        var val = $.fn.dataTable.util.escapeRegex($(this).val());
                        column.search( val ? '^'+val+'$' : '', true, false ).draw();
                    } );
                column.data().unique().sort().each( function ( d, j ) {
                    select.append( '<option value="'+d+'">'+d+'</option>' )
                } );
                $('<span name="oneBlank"> </span>').appendTo(opDiv);
            }
        });

        $("#init-namespace-zkcluster-mapping").on("click", function() {
            $("#confirm-yes-dialog-title").html('确认初始化映射表，请输入yes！');
            $("#giveMeYes").val("");
            $("#confirm-yes-dialog-confirm-btn").unbind('click').on("click", function() {
                var yes = $("#giveMeYes").val();
                if (yes === "yes") {
                    var $btn = $(this).button('loading');
                    $.post("registry_center/initNamespaceZkClusterMapping", {}, function(data) {
                        $("#confirm-yes-dialog").modal("hide");
                        if(data.success) {
                            showSuccessDialog();
                            refreshNamespaceZkClusterMappingList();
                        } else {
                            showFailureDialogWithMsg("failure-dialog", data.message);
                        }
                    }).always(function() {$btn.button('reset');});
                } else {
                    alert("确认请输入yes");
                }
            });
            $("#confirm-yes-dialog").modal({backdrop:"static", keyboard: false});
            $("#confirm-yes-dialog").modal("show");
        });

        $("#batch-move-namespace").on("click", function() {
            var namespaces = "";
            $(".batchInput").each(function() {
                if($(this).is(":checked")) {
                    if(namespaces == "") {
                        namespaces = $(this).attr("namespace") ;
                    } else {
                        namespaces = namespaces + "," + $(this).attr("namespace") ;
                    }
                }
            });
            if(namespaces == "") {
                showFailureDialogWithMsg("failure-dialog", "请选择要迁移的域");
            } else {
                $.get("registry_center/getZkClusterListWithOnline", {}, function(data) {
                    if(data.success) {
                        $("#move-namespace-batch-dialog-namespace").html(namespaces);
                        $.get("registry_center/getZkClusterListWithOnline", {}, function(data2) {
                            if(data2.success) {
                                var zkClusterListWithOnlineFromCfg = data2.obj;
                                var options = "";
                                if(zkClusterListWithOnlineFromCfg && zkClusterListWithOnlineFromCfg instanceof Array) {
                                    for(var i in zkClusterListWithOnlineFromCfg) {
                                        var zkClusterWithOnlineFromCfg = zkClusterListWithOnlineFromCfg[i];
                                        options += "<option value='"+ zkClusterWithOnlineFromCfg +"'>" + zkClusterWithOnlineFromCfg + "</option>";
                                    }
                                }
                                $("#move-namespace-batch-dialog-zkCluster").empty();
                                $("#move-namespace-batch-dialog-zkCluster").append(options);
                                $("#move-namespace-batch-dialog").modal({backdrop:"static", keyboard: false});
                                $("#move-namespace-batch-dialog").modal("show");
                            } else {
                                showFailureDialogWithMsg("failure-dialog", data.message);
                            }
                        });
                    } else {
                        showFailureDialogWithMsg("failure-dialog", data.message);
                    }
                });
            }
        });
    }).always(function() { $loading.hide(); });
}

function clickNamespace_zkcluster_manager_table_select_all(obj){
	var chooseCheckBox = $(obj).is(":checked");
	if(!chooseCheckBox){
		$("#namespace_zkcluster_manager_table_select_all").prop('checked',false);// 只要有一个没选中，全选复选框就置为false
		return false;
	}
	var checkChoose = true;
	$(".batchInput").each(function(){
    	if(!$(this).is(":checked")){
    		checkChoose = false;
		}
	});
	if(checkChoose){
		$("#namespace_zkcluster_manager_table_select_all").prop('checked',true);// 只有全部都选中，全选复选框才置为true
	}else{
		$("#namespace_zkcluster_manager_table_select_all").prop('checked',false);// 只要有一个没选中，全选复选框就置为false
	}
}

function refreshZkClusterInfo() {
    $loading.show();
    $.get("registry_center/getAllZkClusterInfo", {}, function(data) {
        if(data.success) {
            if(zk_cluster_info_table_DataTable) {
                zk_cluster_info_table_DataTable.destroy();
            }
            $("#zk_cluster_info_table tbody").empty();
            var list = data.obj;
            if(list && list instanceof Array) {
                for(var i=0; i<list.length; i++) {
                    var zkClusterInfo = list[i];
                    var baseTd = "<td>" + zkClusterInfo.zkClusterKey + "</td>" +
                                "<td>" + zkClusterInfo.alias + "</td>" +
                                "<td>" + zkClusterInfo.connectString + "</td>";
                    $("#zk_cluster_info_table tbody").append("<tr>" + baseTd + "</tr>");
                }
            }
            zk_cluster_info_table_DataTable = $("#zk_cluster_info_table").DataTable({"oLanguage": language, "displayLength":100});
        } else {
            showFailureDialogWithMsg("failure-dialog", data.message);
        }
    }).always(function() { $loading.hide(); });
}

function getMoveNamespaceBatchStatus() {
    $.get("registry_center/getMoveNamespaceBatchStatus", {}, function(data) {
        if(data.success) {
            var status = data.obj;
            $("#move-namespace-batch-status-dialog-data").html("成功：" + status.successCount + "<br/>失败：" + status.failCount + "<br/>忽略：" + status.ignoreCount + "<br/>未做：" + status.unDoCount + "<br/>总共：" + status.totalCount);
            if(!status.finished) {
                $("#move-namespace-batch-status-dialog-moving").html(status.moving);
                getMoveNamespaceBatchStatus();
            } else {
                $("#move-namespace-batch-status-dialog-moving").html("无");
                $("#move-namespace-batch-status-dialog-tips").css("display", "none");
                $("#move-namespace-batch-status-dialog-x").css("display", "inline");
            }
        } else {
            showFailureDialogWithMsg("failure-dialog", data.message);
            $("#move-namespace-batch-status-dialog-tips").css("display", "none");
            $("#move-namespace-batch-status-dialog-x").css("display", "inline");
        }
    });
}
