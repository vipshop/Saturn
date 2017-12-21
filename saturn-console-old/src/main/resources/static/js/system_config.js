var queryValueResultDataTable;
$(function() {

    $("#queryValue-form").submit(function(event) {
        event.preventDefault();
        var property1 = $("#property1").val();
        $.post("system_config/queryValue", {property1:property1}, function(data) {
            if(queryValueResultDataTable) {
                queryValueResultDataTable.destroy();
            }
            var queryValueResultTbl = $("#queryValueResult-tbl");
            var queryValueResultTblBody = $("#queryValueResult-tbl tbody");
            queryValueResultTblBody.empty();
            if(data.success) {
                var system_configs = data.obj;
                if(system_configs && system_configs instanceof Array) {
                    for(var i in system_configs) {
                        var system_config = system_configs[i];
                        var tdProperty = "<td>" + system_config.property + "</td>";
                        var tdValue = "<td>" + system_config.value + "</td>";
                        var tr = "<tr>" + tdProperty + tdValue + "</tr>";
                        queryValueResultTblBody.append(tr);
                    }
                }
                queryValueResultDataTable = queryValueResultTbl.DataTable({
                    "destroy": true,
                    "oLanguage": language,
                    "aoColumnDefs": [{"bSortable": false,"aTargets": [1]}] // set the columns unSort
                });
            } else {
                showFailureDialogWithMsg("failure-dialog", data.message);
            }
        });
    });

    $("#confirm-dialog").on("shown.bs.modal", function (event) {
        $("#confirm-dialog-confirm-btn").unbind('click').click(function() {
            var $btn = $(this).button('loading');
            var property2 = $("#property2").val();
            var value2 = $("#value2").val();
            $.post("system_config/insertOrUpdate", {property2:property2, value2:value2}, function(data) {
                $("#confirm-dialog").modal("hide");
                if(data.success) {
                    showSuccessDialog();
                } else {
                    showFailureDialogWithMsg("failure-dialog", data.message);
                }
            }).always(function() { $btn.button('reset'); });
            return false;
        });
    });

    $("#insertOrUpdate-form").submit(function(event) {
        event.preventDefault();
        var confirmReason = "确认要提交配置吗";
        $("#confirm-dialog .confirm-reason").text(confirmReason);
        $("#confirm-dialog").modal("show");
    });

});
