$(function() {
	window.parent.setActiveTab("#regTab");
    renderRegCenters();
    bindRefreshRegCenterButton();
});

function renderRegCenters() {
    $.get("registry_center", {}, function(data) {
        $("#regCenters tbody").empty();
        var activedNameAndNamespace = data.actived;
        for (var i = 0;i < data.configs.length;i++) {
            var baseTd = "<td><a href='overview?name=" + data.configs[i].name + "/" + data.configs[i].namespace + "'>" + data.configs[i].namespace + "</a></td><td>" 
            	+ data.configs[i].name + "</td><td>" + data.configs[i].zkAddressList + "</td>";
            if (data.configs[i].nameAndNamespace === activedNameAndNamespace) {
                $("#activated-reg-center").text(data.configs[i].name);
            	window.parent.setRegName(data.configs[i].name, data.configs[i].namespace);
            	window.parent.reloadTreeAndExpandJob(data.configs[i].name);
            } 
            $("#regCenters tbody").append("<tr>" + baseTd + "</tr>");
        }
        $("#regCenters").DataTable({"oLanguage": language, "displayLength":100});
    });
}

function bindRefreshRegCenterButton() {
	$("#refresh-reg").click(function() {
		var $btn = $(this);
		$btn.button("loading");
		$.get("registry_center/refreshRegCenter", function(data) {
			if(data.success == true) {
				window.parent.reloadTreeData();
				window.parent.location.reload(true);
			} else {
				showFailureDialogWithMsg("failure-dialog", data.message);
			}
		}).always(function() { $btn.button('reset'); });
		return false;;
    });
}

