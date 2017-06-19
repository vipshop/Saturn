$(function() {
	window.parent.setActiveTab("#regTab");
	renderZks();
	$("#zks").change(function(){
		var newSelected = $("#zks").val();
		$.post("registry_center/selectZk", {newZkBsKey : newSelected}, function (data) {
			window.location.reload();
        }).always(function() {});
		return false;
	});
    renderRegCenters();
    bindRefreshRegCenterButton();
});

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
	});
}

function renderRegCenters() {
    $.get("registry_center", {}, function(data) {
        $("#regCenters tbody").empty();
        var activedNameAndNamespace = data.actived;
        if(data.configs && data.configs instanceof Array) {
            for (var i = 0;i < data.configs.length;i++) {
                var baseTd = "<td><a href='overview?name=" + data.configs[i].name + "/" + data.configs[i].namespace + "'>" + data.configs[i].namespace + "</a></td><td>"
                    + data.configs[i].name + "</td><td>" + data.configs[i].version + "</td><td>" + data.configs[i].zkAddressList + "</td>";
                $("#regCenters tbody").append("<tr>" + baseTd + "</tr>");
            }
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

