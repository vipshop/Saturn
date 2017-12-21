var junkViewDataTable,$loading = $("#loading");

$(function() {

    renderZks();
    
    $("#confirm-dialog").on("shown.bs.modal", function (event) {
    	var button = $(event.relatedTarget);
    	var type = button.data('type');
    	var path = button.data('path');
    	var namespace = button.data('namespace');
    	$("#confirm-dialog-confirm-btn").unbind('click').click(function() {
    		var $btn = $(this).button('loading');
    		$.post("removeJunkData", {type:type,path:path,namespace:namespace}, function (data) {
    			$("#confirm-dialog").modal("hide");
    			if(data.success) {
                    showSuccessDialogWithCallback(function(){location.reload(true);});
                } else {
                    $("#failure-dialog .fail-reason").text(data.message);
                    showFailureDialog("failure-dialog");
                }
    		}).always(function() { $btn.button('reset'); });
    		return false;
    	});
    });

    $("#delete-running-btn").on("click", function(event) {
        var namespace = $("#delete-running-namespace").val();
        var jobName = $("#delete-running-jobName").val();
        var item = $("#delete-running-item").val();
        if(isNullOrEmpty(namespace)) {
            alert("namespace不能为空");
            return;
        }
        if(isNullOrEmpty(jobName)) {
            alert("jobName不能为空");
            return;
        }
        if(isNullOrEmpty(item)) {
            alert("item不能为空");
            return false;
        }
        if(isNaN(parseInt(item))) {
            alert("item必须为数字");
            return;
        }
        if(parseInt(item) < 0) {
            alert("item不能为小于0");
            return;
        }
        var path = namespace + "/$Jobs/" + jobName + "/execution/" + item + "/running";
        $("#delete-running-confirm-dialog .confirm-reason").text("确认要删除路径：" + path + "？");
        $("#delete-running-confirm-dialog").modal("show");
        $("#delete-running-confirm-dialog-confirm-btn").unbind('click').click(function() {
            var $btn = $(this).button('loading');
            $.post("junkData/deleteRunningNode", {namespace: namespace, jobName: jobName, item: item}, function (data) {
                $("#delete-running-confirm-dialog").modal("hide");
                if(data.success) {
                    showSuccessDialog();
                } else {
                    $("#failure-dialog .fail-reason").text(data.message);
                    showFailureDialog("failure-dialog");
                }
            }).always(function() { $btn.button('reset'); });
            return false;
        });
    });
});

function renderJunkData() {
	$loading.show();

    $.get("getJunkdata", {newZkClusterKey:$("#zks").val()}, function(data) {
    	if (junkViewDataTable) {
    		junkViewDataTable.destroy();
    	}
        $("#junkdata-tbl tbody").empty();
        if(data.success) {
            var junkDataList = data.obj;
            for (var i = 0;i < junkDataList.length;i++) {
                var removeButton = "<button operation='removeJunkData' title='点击清理该废弃数据' class='btn btn-danger' data-type='" + junkDataList[i].type + "' data-path='" + junkDataList[i].path + "' data-namespace='" + junkDataList[i].namespace + "' onclick='showRemoveJunkDataConfirmDialog(this);'>清理</button>";
                var baseTd = "<td>" + junkDataList[i].namespace + "</td>"
                    + "<td>" + junkDataList[i].zkAddr + "</td>"
                    + "<td>" + junkDataList[i].path + "</td>"
                    + "<td>" + junkDataList[i].description + "</td>"
                    + "<td>" + removeButton + "</td>";
                $("#junkdata-tbl tbody").append("<tr>" + baseTd + "</tr>");
            }
            junkViewDataTable = $("#junkdata-tbl").DataTable({"destroy": true,"oLanguage": language});
        } else {
              $("#failure-dialog .fail-reason").text(data.message);
              showFailureDialog("failure-dialog");
          }
    }).always(function() { $loading.hide(); });
}

function showRemoveJunkDataConfirmDialog(obj) {
	var confirmReason = "确认要清理该废弃数据吗?";
	$("#confirm-dialog .confirm-reason").text(confirmReason);
	$("#confirm-dialog").modal("show", obj);
}

function renderZks() {
	$.get("loadZks", {}, function(data) {
		var zks = data.clusters, currentZk = data.currentZk, options="";
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

		$("#zks").change(function(){
            renderJunkData();
        });

        renderJunkData();
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
