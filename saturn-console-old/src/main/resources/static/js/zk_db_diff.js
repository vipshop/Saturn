var dataDiffTable;
$(function() {
    bindDiffButton();
});

function bindDiffButton() {
    $("#diffConfig").unbind('click').click(function() {
        var $btn = $(this);
        $btn.button("loading");
        var zkCluster = $("#zkCluster").val();
        $.get("zk_db_diff/diffByCluster", {zkCluster:zkCluster}, function(data) {
            if (dataDiffTable) {
            	dataDiffTable.destroy();
            }
            $("#diffData-tbl tbody").empty();

            if(data.success) {
                var result = data.obj;
                if (result && result instanceof Array){
                    for(var i in result) {
                        var baseTd = "<td>" + result[i].namespace + "</td><td>"
                            + result[i].jobName + "</td><td>"
                            + result[i].diffType + "</td><td>";
                        if(result[i].diffType == "HAS_DIFFERENCE") {
                            baseTd = baseTd + "<button class='btn btn-warning' onclick='seeDiffInfoDialog(this);' namespace='" + result[i].namespace + "' job='" + result[i].jobName  + "' >查看</button>";
                        }
                            baseTd = baseTd + "</td>";
                        $("#diffData-tbl tbody").append("<tr>" + baseTd + "</tr>");
                    }
                }
                dataDiffTable = $("#diffData-tbl").DataTable({"destroy": true,"oLanguage": language});
            } else {
                showFailureDialogWithMsg("failure-dialog", data.message);
            }
        }).always(function() { $btn.button('reset'); });
        return false;
    });
}

function seeDiffInfoDialog(obj) {
    	var jobName = $(obj).attr('job');
    	var namespace = $(obj).attr('namespace');
    	$.get("zk_db_diff/diffByJob", {jobName:jobName, namespace:namespace}, function(data) {
    		if(data.success) {
    		    var result = data.obj.configDiffInfos;
                var content = "";
                if (result == null){
                    content = "<p>没有任何不同</p>";
                } else if (result && result instanceof Array){
                    for(var i in result) {
                        content = content + "<p>属性:[" + result[i].key + "]找到不同  zk=" + result[i].zkValue + "   db=" + result[i].dbValue +"</p>";
                    }
                }
    		    $("#diff-info-textare").html(content);
    		} else {
    		    $("#diff-info-textare").html(data.message);
    		}
    		$("#diff-job-name").html(jobName);
			$("#diff-info-dialog").modal("show", obj);
    	});
}
