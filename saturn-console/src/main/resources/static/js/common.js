$(function() {
    $("[data-toggle='tooltip']").tooltip();
});

var language = {
	"sLengthMenu": "每页显示 _MENU_ 条记录",
	"sSearch":"过滤",
	"sZeroRecords": "抱歉， 没有找到",
	"sInfo": "从 _START_ 到 _END_ /共 _TOTAL_ 条数据",
	"sInfoEmpty": "没有数据",
	"sInfoFiltered": "(从 _MAX_ 条数据中检索)",
	"oPaginate": {
	"sFirst": "首页",
	"sPrevious": "前一页",
	"sNext": "后一页",
	"sLast": "尾页"
	},
	"sZeroRecords": "没有检索到数据"},
	degreeMap = {"":"没有定义","0":"没有定义","1":"非线上业务","2":"简单业务","3":"一般业务","4":"重要业务","5":"核心业务"}, $loading = $("#loading");

function showSuccessDialogAndNotHide() {
    $("#success-dialog").modal("show");
}

function showSuccessDialog() {
    $("#success-dialog").modal("show");
    setTimeout('$("#success-dialog").modal("hide")', 1000);
}

function showSuccessDialogWithCallback(callback) {
    $("#success-dialog").modal("show");
    setTimeout('$("#success-dialog").modal("hide")', 1000);
    $("#success-dialog").on('hide.bs.modal', function (event) {
    	callback();
    });
}

function showFailureDialog(id) {
    $("#" + id).modal("show");
//    setTimeout("$('#" + id + "').modal('hide')", 4000);
}
function showFailureDialogWithMsg(id, msg) {
    var dom = $("#" + id);
    dom.find(".fail-reason").html(msg);
	dom.modal("show");
//    setTimeout("$('#" + id + "').modal('hide')", 4000);
}
function showFailureDialogWithMsgAndCallback(id, msg, callback) {
    var dom = $("#" + id);
    dom.find(".fail-reason").html(msg);
	dom.modal("show");
//    setTimeout("$('#" + id + "').modal('hide')", 4000);
    dom.on('hide.bs.modal', function (event) {
		callback();
	});
}
function showConfirmDialogWithMsgAndCallback(id, msg, callback) {
    var dom = $("#" + id);
    dom.find(".confirm-reason").html(msg);
    dom.modal("show");
//    setTimeout("$('#" + id + "').modal('hide')", 4000);

    $("#" + id + "-confirm-btn").on('click', function(event) {
        dom.modal('hide');
    });

    dom.on('hide.bs.modal', function (event) {
		callback();
	});
}

function showPromptDialogWithMsgAndCallback(id, msg, callback) {
    var dom = $("#" + id);
    dom.find(".prompt-reason").html(msg);
    dom.modal("show");

    if(callback != null) {
        dom.on('hide.bs.modal', function (event) {
            callback();
        });
	}
}

/**       
 * 对Date的扩展，将 Date 转化为指定格式的String       
 * 月(M)、日(d)、12小时(h)、24小时(H)、分(m)、秒(s)、周(E)、季度(q) 可以用 1-2 个占位符       
 * 年(y)可以用 1-4 个占位符，毫秒(S)只能用 1 个占位符(是 1-3 位的数字)       
 * eg:       
 * (new Date()).format("yyyy-MM-dd hh:mm:ss.S") ==> 2006-07-02 08:09:04.423       
 * (new Date()).format("yyyy-MM-dd E HH:mm:ss") ==> 2009-03-10 二 20:09:04       
 * (new Date()).format("yyyy-MM-dd EE hh:mm:ss") ==> 2009-03-10 周二 08:09:04       
 * (new Date()).format("yyyy-MM-dd EEE hh:mm:ss") ==> 2009-03-10 星期二 08:09:04       
 * (new Date()).format("yyyy-M-d h:m:s.S") ==> 2006-7-2 8:9:4.18       
 */          
Date.prototype.format=function(fmt) {           
    var o = {           
	    "M+" : this.getMonth()+1, //月份           
	    "d+" : this.getDate(), //日           
	    "h+" : this.getHours()%12 == 0 ? 12 : this.getHours()%12, //小时           
	    "H+" : this.getHours(), //小时           
	    "m+" : this.getMinutes(), //分           
	    "s+" : this.getSeconds(), //秒           
	    "q+" : Math.floor((this.getMonth()+3)/3), //季度           
	    "S" : this.getMilliseconds() //毫秒           
    };           
    var week = {           
	    "0" : "/u65e5",           
	    "1" : "/u4e00",           
	    "2" : "/u4e8c",           
	    "3" : "/u4e09",           
	    "4" : "/u56db",           
	    "5" : "/u4e94",           
	    "6" : "/u516d"          
    };           
    if(/(y+)/.test(fmt)){           
        fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));           
    }           
    if(/(E+)/.test(fmt)){           
        fmt=fmt.replace(RegExp.$1, ((RegExp.$1.length>1) ? (RegExp.$1.length>2 ? "/u661f/u671f" : "/u5468") : "")+week[this.getDay()+""]);           
    }           
    for(var k in o){           
        if(new RegExp("("+ k +")").test(fmt)){           
            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));           
        }           
    }           
    return fmt;           
};