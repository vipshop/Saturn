$(function() {
	window.parent.setActiveTab("#regTab");
    renderRegCenters();
    bindConnectButtons();
    bindRefreshRegCenterButton();
    $("[data-toggle='tooltip']").tooltip();
});

function renderRegCenters() {
    $.get("registry_center", {}, function(data) {
        $("#regCenters tbody").empty();
        var activedNameAndNamespace = data.actived;
        for (var i = 0;i < data.configs.length;i++) {
            var baseTd = "<td><a href='overview?name=" + data.configs[i].name + "/" + data.configs[i].namespace + "'>" + data.configs[i].namespace + "</a></td><td>" 
            	+ data.configs[i].name + "</td><td>" + data.configs[i].zkAddressList + "</td>";
            var operationTd;
            if (data.configs[i].nameAndNamespace === activedNameAndNamespace) {
                $("#activated-reg-center").text(data.configs[i].name);
                operationTd = "<td><button disabled operation='connect' class='btn' regName='" + data.configs[i].name + "/" + data.configs[i].namespace + "'>已连</button></td>";
            	window.parent.setRegName(data.configs[i].name, data.configs[i].namespace);
            	window.parent.reloadTreeAndExpandJob(data.configs[i].name);
            } else {
                operationTd = "<td><button operation='connect' class='btn btn-primary' regName='" + data.configs[i].name + "/" + data.configs[i].namespace +"' data-loading-text='切换中...'>连接</button></td>";
            }
            $("#regCenters tbody").append("<tr>" + baseTd + operationTd + "</tr>");
        }
        $("#regCenters").DataTable({"oLanguage": language, "displayLength":100});
    });
}

function bindRefreshRegCenterButton() {
	$("#refresh-cmdb").click(function() {
		var $btn = $(this);
		$btn.button("loading");
		$.get("registry_center/refreshRegCenter", function(data) {
			if(data.success == true) {
				window.parent.reloadTreeData();
				location.reload();
			} else {
				$("#refresh-cmdb-failure-dialog .fail-reason").text(data.message);
            	showFailureDialog("refresh-cmdb-failure-dialog");
			}
		}).always(function() { $btn.button('reset'); });
		return false;;
    });
}

function bindConnectButtons() {
    $(document).on("click", "button[operation='connect']", function(event) {
    	var $btn = $(this).button("loading");
        var regName = $(event.currentTarget).attr("regName");
        var currentConnectBtn = $(event.currentTarget);
        $.post("registry_center/connect", {nameAndNamespace : regName}, function (data) {
            if (data.isSuccesssful) {
                $("#activated-reg-center").text(regName);
                var connectBtns = $('button[operation="connect"]');
                connectBtns.text("连接");
                connectBtns.addClass("btn-primary");
                connectBtns.attr("disabled", false);
                currentConnectBtn.attr("disabled", true);
                currentConnectBtn.removeClass("btn-primary");
                currentConnectBtn.text("已连");
                showSuccessDialog();
                window.parent.expandJobsAndSetRegCenter(regName);
            	window.parent.setRegName(regName, data.namespace);
            } else {
                showFailureDialog("connect-reg-center-failure-dialog");
            }
        }).always(function() { $btn.button('reset'); });
		return false;;
    });
}
