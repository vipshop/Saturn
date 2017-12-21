var releaseVersion_DataTable;
$(function() {

	renderZks();
	
    $("#dashboard-refresh-btn").on("click", function() {
    	showDashboardRefreshConfirmDialog();
    });
});

function renderZks() {
    $.get("loadZks", {}, function(data) {
        var zks = data.clusters, options="";
        if(zks) {
            for(var i in zks) {
                var disabled = "", alias = zks[i].zkAlias, zkClusterKey = zks[i].zkClusterKey, optionMessage = alias;
                if (zks[i].offline) {
                    disabled = " disabled='disabled'";
                    optionMessage += "[offline]"
                }
                options += "<option" +disabled+ " value='"+zkClusterKey+"' title=" + alias + ":" + zkClusterKey +">" + optionMessage + "</option>";
            }
            $("#zkCluster").append(options);
        }
    });
}

function showDashboardRefreshConfirmDialog(obj) {
	var zkClusterKey = $("#zkCluster").val();
	if(zkClusterKey == null || zkClusterKey == '') {
		alert('请先选择要进行dashboard统计信息刷新的zk集群！');
		$("#zkCluster").focus();
		return;
	}
	var zkClusterAlias = $("#zkCluster").find("option:selected").text();
	var confirmBtnText = "确认刷新【"+zkClusterAlias+"】zk集群的dashboard统计信息吗？";
	$("#dashboard-refresh-confirm-dialog .confirm-reason").text(confirmBtnText);
	$("#dashboard-refresh-confirm-dialog").modal("show", obj);
}

$("#dashboard-refresh-confirm-dialog").on('shown.bs.modal', function (event) {
	$("#dashboard-refresh-confirm-dialog-confirm-btn").unbind('click').click(function() {
        var $btn = $(this).button('loading');
        var zkClusterKey = $("#zkCluster").val();
        $.post("/dashboardRefresh", {zkClusterKey:zkClusterKey}, function (data) {
            if (data.success) {
                $("#dashboard-refresh-confirm-dialog").modal("hide");
                $("#success-dialog .modal-body").html("刷新完成，总共花费 "+data.obj+" ms");
                showSuccessDialog();
            } else {
            	$("#confirm-yes-dialog").modal("hide");
                showFailureDialogWithMsg("failure-dialog", data.message);
            }
        }).always(function() {$btn.button('reset');});
	});
});

function showSuccessDialog() {
    $("#success-dialog").modal("show");
    $("#success-dialog").on('hide.bs.modal');
}
