var junkViewDataTable,$loading = $("#loading");

$(function() {
    renderZks();
    
	$("#zks").change(function(){
		var newSelected = $("#zks").val();
		$.post("registry_center/selectZk", {newZkBsKey : newSelected}, function (data) {
			renderJunkData(newSelected);
        }).always(function() {});
	});
    
    $("#confirm-dialog").on("shown.bs.modal", function (event) {
    	var button = $(event.relatedTarget);
    	var type = button.data('type');
    	var path = button.data('path');
    	var zkAddr = button.data('zkaddr');
    	$("#confirm-dialog-confirm-btn").unbind('click').click(function() {
    		var $btn = $(this).button('loading');
    		$.post("removeJunkData", {type:type,path:path,zkAddr:zkAddr}, function (data) {
    			$("#confirm-dialog").modal("hide");
    			if(data == "ok") {
    				showSuccessDialogWithCallback(function(){location.reload(true);});
    			} else {
    				$("#failure-dialog .fail-reason").text(data);
	            	showFailureDialog("failure-dialog");
    			}
    		}).always(function() { $btn.button('reset'); });
    		return false;
    	});
    });
});

function renderJunkData(zkAddr) {
	$loading.show();
    $.get("getJunkdata", {zkAddr:zkAddr}, function(data) {
    	if (junkViewDataTable) {
    		junkViewDataTable.destroy();
    	}
        $("#junkdata-tbl tbody").empty();
    	var junkDataList = data;
        for (var i = 0;i < junkDataList.length;i++) {
        	var removeButton = "<button operation='removeJunkData' title='点击清理该废弃数据' class='btn btn-danger' data-type='" + junkDataList[i].type + "' data-path='" + junkDataList[i].path + "' data-zkaddr='" + junkDataList[i].zkAddr + "' onclick='showRemoveJunkDataConfirmDialog(this);'>清理</button>";
            var baseTd = "<td>" + junkDataList[i].namespace + "</td>" 
            	+ "<td>" + junkDataList[i].zkAddr + "</td>" 
                + "<td>" + junkDataList[i].path + "</td>" 
                + "<td>" + junkDataList[i].description + "</td>"
            	+ "<td>" + removeButton + "</td>";
            $("#junkdata-tbl tbody").append("<tr>" + baseTd + "</tr>");
        }
        junkViewDataTable = $("#junkdata-tbl").DataTable({"destroy": true,"oLanguage": language});
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
			if (currentZk == zks[i].zkAddr) {
				options += "<option " +disabled+ " selected='selected' value='"+zks[i].zkAddr+"'>" + alias + "</option>";
			} else {
				options += "<option " +disabled+ " value='"+zks[i].zkAddr+"'>" + alias + "</option>";
			}
		}
		$("#zks").append(options);
		renderJunkData($("#zks").val());
	});
}
