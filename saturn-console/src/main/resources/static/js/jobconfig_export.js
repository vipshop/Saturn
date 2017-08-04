$(function() {
	
	initWithPrivilege();
    renderRegCenters();
    renderExportStatus();
    bindExport();
    bindExportConfigConfirmButton();
});


function renderExportStatus() {
    $.get("jobconfig/getExportStatus", {}, function(data) {
        var statusText = "";
        if(data.exportStatus.hadExported == 'false') {
        	statusText="(status:ready to export)";
        } else if(data.exportStatus.success == 'true' && data.exportStatus.exporting == 'false') {
        	statusText="(status:export success,namespace:"+data.exportStatus.successNamespaceNum+", job:"+data.exportStatus.successJobNum+")";
        }  else if(data.exportStatus.exporting == 'true') {
        	statusText="(status:exporting,completed:namespace["+data.exportStatus.successNamespaceNum+"], job["+data.exportStatus.successJobNum+"])";
        } else if(data.exportStatus.success == 'false') {
        	statusText="(status:export fail,please retry!)";
        }
        $("#exportMsg").text(statusText);
    });
}

function initWithPrivilege() {
	if($("#authorizeSaturnConsoleDashBoardAllPrivilege").val() == "true") {
		$("#exportAllConfig").removeAttr("hidden");
	} else {
		$("#exportAllConfig").attr("hidden");
	}
	
	if($("#isExporting").val() == "true") {
		$("#exportAllConfig").attr("disabled",true);
		$("#exportAllConfig").text("复制中...");
	} else {
		$("#exportAllConfig").attr("disabled",false);
		$("#exportAllConfig").text("复制全量配置");
	}
}

function renderRegCenters() {
    $.get("jobconfig/getExportRegList", {}, function(data) {
        $("#regCenters tbody").empty();
        if(data.configs && data.configs instanceof Array) {
            for (var i = 0;i < data.configs.length;i++) {
                var baseTd = "<td>" + data.configs[i].namespace + "</td><td>"
                    + data.configs[i].name + "</td><td>"
                    + data.configs[i].zkAlias + "</td><td>" + data.configs[i].zkClusterKey + "</td>"
                    +"<td>"+data.configs[i].msg+"</td>";
                $("#regCenters tbody").append("<tr>" + baseTd + "</tr>");
            }
        }
        $("#regCenters").DataTable({"oLanguage": language, "displayLength":100});
    });
}



function bindExportConfigConfirmButton() {
    $("#exportConfig-confirm-dialog").on("shown.bs.modal", function (event) {
         $("#exportConfig-confirm-dialog-confirm-btn").unbind('click').click(function() {
     		$("#exportAllConfig").attr("disabled",true);
     		$("#exportAllConfig").text("复制中...");
    		var $btn = $(this);
    		$btn.button("loading");
    		$.post("jobconfig/exportAllConfigToDb", function(data) {
    			$("#exportConfig-confirm-dialog").modal("hide");
    			if(data.success == true) {
    				showSuccessDialog();
    				location.reload();
    			} else {
    				showFailureDialogWithMsg("failure-dialog", data.message);
    			}
    		}).always(function() {  });
    		return false;;
         });
     });
}

function bindExport() {
	$("#exportAllConfig").click(function() {
		$("#exportConfig-confirm-dialog .confirm-reason").text("全量复制将先清空作业表信息，再进行全量复制！确认要全量复制作业配置信息吗？");
		$("#exportConfig-confirm-dialog").modal("show", this);
    });
}


function bindRefreshCmdbButton() {
	$("#exportAllConfig").click(function() {
		$("#exportAllConfig").attr("disabled",true);
		var $btn = $(this);
		$btn.button("loading");
		$.post("jobconfig/exportAllConfigToDb", function(data) {
			if(data.success == true) {
				showSuccessDialog();
			} else {
				showFailureDialogWithMsg("failure-dialog", data.message);
			}
		}).always(function() {  });
		return false;;
    });
}


