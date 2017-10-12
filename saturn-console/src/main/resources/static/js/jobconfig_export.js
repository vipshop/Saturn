$(function() {
	initWithPrivilege();
    renderRegCenters();
    renderExportStatus();
    bindExport();
    bindExportConfigConfirmButton();
});


function renderExportStatus() {
    $.get("jobconfig/getExportStatus", {}, function(data) {
    	if(data.success == true) {
    		var status = data.obj;
	        var statusText = "";
	        if(status) {
	        	if(status.exported == true) {
	        		if(status.success == true) {
	        			statusText="(status:export success,namespace:"+status.successNamespaceNum+", job:"+status.successJobNum+")";
	        		} else {
	        			statusText="(status:export fail,please retry!)";
	        		}
	        	} else {
	        		statusText="(status:exporting,completed:namespace["+status.successNamespaceNum+"], job["+status.successJobNum+"])";
	        	}
	        } else {
	        	statusText="(status:ready to export)";
	        }
	        $("#exportMsg").text(statusText);
    	} else {
    		showFailureDialogWithMsg("failure-dialog", data.message);
    	}
    });
}

function initWithPrivilege() {
	if($("#exporting").val() == "true") {
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
        if(data.success == true) {
        	if(data.obj && data.obj instanceof Array) {
        		var list = data.obj;
	            for (var i = 0;i < list.length;i++) {
	                var baseTd = "<td>" + list[i].namespace + "</td><td>"
	                    + list[i].name + "</td><td>"
	                    + list[i].zkAlias + "</td><td>" + list[i].zkClusterKey + "</td>";
	                $("#regCenters tbody").append("<tr>" + baseTd + "</tr>");
	            }
        	}
        } else {
        	showFailureDialogWithMsg("failure-dialog", data.message);
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



