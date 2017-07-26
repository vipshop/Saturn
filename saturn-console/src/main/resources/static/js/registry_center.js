var namespace_info_table_DataTable, namespace_zkcluster_manager_table_DataTable;
$(function() {
	
	$("#refresh_registry_center").on("click", function() {
		var $btn = $(this).button("loading");
		$.get("registry_center/refreshRegCenter", function(data) {
			if(data.success == true) {
				window.parent.location.reload(true);
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
		refreshNamespaceZkCluster();
    });
	if ($("#namespace_info_tabpanel").is(':visible')) {
		refreshNamespaceInfo();
	} else if ($("#zk_cluster_info_tabpanel").is(':visible')) {
		refreshZkClusterInfo();
	} else if ($("#namespace_zkcluster_manager_tabpanel").is(':visible')) {
		refreshNamespaceZkCluster();
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
            var bootstrapKeyNew = "";
            if($("#move-namespace-batch-dialog-manual").attr("status") == "auto") {
                bootstrapKeyNew = $("#move-namespace-batch-dialog-zkCluster").val();
            } else {
                bootstrapKeyNew = $("#move-namespace-batch-dialog-zkCluster-input").val();
            }
            var updateDBOnly = $("#move-namespace-batch-dialog-updateDBOnly").is(":checked");
            $("#confirm-yes-dialog-title").html('域名：<pre>' + namespaces + '</pre>目标集群：<pre>' + bootstrapKeyNew + '</pre>只更新映射表：<pre>' + updateDBOnly + '</pre>确认批量迁移，请输入yes！');
            $("#giveMeYes").val("");
            $("#confirm-yes-dialog-confirm-btn").unbind('click').on("click", function() {
                var yes = $("#giveMeYes").val();
                if (yes === "yes") {
                    $("#move-namespace-batch-dialog").modal("hide");
                    $("#confirm-yes-dialog").modal("hide");
                    $("#move-namespace-batch-status-dialog-zkCluster").html(bootstrapKeyNew);
                    var split = namespaces.split(",");
                    if(split && split instanceof Array) {
                        var id = parseInt(Math.random() * 10000000);
                        $.post("registry_center/moveNamespaceBatch", {namespaces: namespaces, bootstrapKeyNew: bootstrapKeyNew, updateDBOnly: updateDBOnly, id: id}, function(data) {
                            if(data.success) {
                                var size = split.length;
                                $("#move-namespace-batch-status-dialog-data").html("成功：" + 0 + "<br/>失败：" + 0 + "<br/>忽略：" + 0 + "<br/>未做：" + size + "<br/>总共：" + size);
                                $("#move-namespace-batch-status-dialog-message").html("");
                                $("#move-namespace-batch-status-dialog-tips").css("display", "inline");
                                $("#move-namespace-batch-status-dialog-x").css("display", "none");
                                $("#move-namespace-batch-status-dialog").modal({backdrop:"static", keyboard: false});
                                $("#move-namespace-batch-status-dialog").modal("show");
                                getMoveNamespaceBatchStatus(id);
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
        var id = parseInt($("#move-namespace-batch-status-dialog-x").attr("id2"));
        if(!isNaN(id)) {
            $.post("registry_center/clearMoveNamespaceBatchStatus", {id: id}, function(data) {});
        }
        refreshNamespaceZkCluster();
    });
});

function refreshNamespaceInfo() {
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
            		var key = cluster.key;
            		var regCenterConfList = cluster.regCenterConfList;
            		if(regCenterConfList && regCenterConfList instanceof Array) {
            			for(var j=0; j < regCenterConfList.length; j++) {
	        				var regCenterConf = regCenterConfList[j];
			                var baseTd = "<td><a href='overview?name=" + regCenterConf.name + "/" + regCenterConf.namespace + "'>" + regCenterConf.namespace + "</a></td><td>"
			                    + regCenterConf.name + "</td><td>" + regCenterConf.version + "</td><td>" + key + "</td>";
			                $("#namespace_info_table tbody").append("<tr>" + baseTd + "</tr>");
            			}
            		}
	            }
	        }
	        namespace_info_table_DataTable = $("#namespace_info_table").DataTable({"oLanguage": language, "displayLength":100});
        } else {
            showFailureDialogWithMsg("failure-dialog", data.message);
        }
    });
}

function refreshNamespaceZkCluster() {
	$.get("registry_center/getNamespaceZkclusterMappingList", {}, function(data) {
		if(namespace_zkcluster_manager_table_DataTable) {
			namespace_zkcluster_manager_table_DataTable.destroy();
		}
        $("#namespace_zkcluster_manager_table tbody").empty();
        if(data.success) {
    		var list = data.obj;
	        if(list && list instanceof Array) {
	            for (var i = 0;i < list.length;i++) {
            		var cluster = list[i];
            		var key = cluster.key;
            		var regCenterConfList = cluster.regCenterConfList;
            		if(regCenterConfList && regCenterConfList instanceof Array) {
            			for(var j=0; j < regCenterConfList.length; j++) {
            				var regCenterConf = regCenterConfList[j];
			                var baseTd = "<td><a href='overview?name=" + regCenterConf.name + "/" + regCenterConf.namespace + "'>" + regCenterConf.namespace + "</a></td><td>"
			                    + regCenterConf.name + "</td><td>" + regCenterConf.version + "</td><td>" + key + "</td>";
			                $("#namespace_zkcluster_manager_table tbody").append("<tr>" + baseTd + "</tr>");
            			}
            		}
	            }
	        }
	        namespace_zkcluster_manager_table_DataTable = $("#namespace_zkcluster_manager_table").DataTable({"oLanguage": language, "displayLength":100});
        } else {
            showFailureDialogWithMsg("failure-dialog", data.message);
        }
    });
}

function refreshZkClusterInfo() {
	
}
