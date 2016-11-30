$(function() {
	renderDomainTree();
    $("input[name=searchTree]").keyup(function(e){
        var n, match = $(this).val(), opts = {
        	autoExpand: true,
            autoApply: true,  // Re-apply last filter if lazy data is loaded
            counter: true,  // Show a badge with number of matching child nodes near parent icons
            fuzzy: false,  // Match single characters in order, e.g. 'fb' will match 'FooBar'
            hideExpandedCounter: true,  // Hide counter badge, when parent is expanded
            highlight: false,  // Highlight matches by wrapping inside <mark> tags
            mode: "hide"  // Grayout unmatched nodes (pass "hide" to remove unmatched node instead)
        };
        if(e && e.which === $.ui.keyCode.ESCAPE || $.trim(match) === ""){
        	tree.clearFilter();
        	return;
        }
        n = tree.filterNodes(match, opts);
      }).focus();

});
var tree, sideBarOverlay = $("#sidebar-overlay"), rp = $("#activated-reg-center").parent(), $regNameParent = $(rp);
function reloadTreeData() {
	tree.reload();
}
function renderRegistryCenterForDashboardNav() {
    $.get("registry_center", {}, function(data) {
        var activatedRegCenter = $("#activated-reg-center").text();
        var $registryCenterDimension = $("#registry-center-dimension");
        $registryCenterDimension.empty();
        for (var i = 0; i < data.length; i++) {
            var regName = data[i].name;
            var liContent = "<a href='#' reg-name='" + regName + "' data-loading-text='切换中...'>" + regName + "</a>";
            if (activatedRegCenter && activatedRegCenter === regName) {
                $registryCenterDimension.append("<li class='open' id='job-"+data[i].jobName+"'>" + liContent + "</li>");
            } else {
                $registryCenterDimension.append("<li id='job-"+data[i].jobName+"'>"  + liContent + "</li>");
            }
        }
    });
}
function renderDomainTree() {
	$("#tree").fancytree({
		extensions: ["filter"],
	    quicksearch: true,
		// glyph: glyph_opts,
		source : {
			url : "registry_center/loadTree"
		},
		lazyLoad: function(event, data) {
			var fp = data.node.data.fullPath || "";
			data.result = {url: "registry_center/loadTree?fp=" + fp, "type": "get"}; 
				//{url: "registry_center/connectAndLoadJobs", "type": "post", data: {"name": domain}};
        },
        filter: {
        	autoExpand: true,
            autoApply: true,  // Re-apply last filter if lazy data is loaded
            counter: true,  // Show a badge with number of matching child nodes near parent icons
            fuzzy: false,  // Match single characters in order, e.g. 'fb' will match 'FooBar'
            hideExpandedCounter: true,  // Hide counter badge, when parent is expanded
            highlight: false,  // Highlight matches by wrapping inside <mark> tags
            mode: "dimm"  // Grayout unmatched nodes (pass "hide" to remove unmatched node instead)
        },
        activate: function(event, data) {
            var node = data.node;
            if( node.data.href ){
            	window.open(node.data.href, node.data.target);
            }
            setDegreeTitle();
        },
        init: function(event, data) {
        	tree = $("#tree").fancytree("getTree");
        	var regName = $("#activated-reg-center").html();
        	expandJobs(data.tree, regName);
        	setDegreeTitle();
        }
	});
}
function setDegreeTitle() {
	$(".degree-0 > .fancytree-icon").attr("title","业务等级没有定义");
	$(".degree-1 > .fancytree-icon").attr("title","业务等级为非线上业务");
	$(".degree-2 > .fancytree-icon").attr("title","业务等级为简单业务");
	$(".degree-3 > .fancytree-icon").attr("title","业务等级为一般业务");
	$(".degree-4 > .fancytree-icon").attr("title","业务等级为重要业务");
	$(".degree-5 > .fancytree-icon").attr("title","业务等级为核心业务");
}
function expandJobs(tree, regName) {
	if (regName && regName != "") {
		var path = regName.split("/");
		var activedDomain = path[path.length - 1];
		if (tree) {
			var oldActiveNode = tree.getActiveNode();
			if (oldActiveNode) {
				oldActiveNode.setFocus(false);
				oldActiveNode.setExpanded(false);
			}
			var activeNode = tree.findFirst(activedDomain);
			if (activeNode) {
				activeNode.setFocus(true);
				activeNode.setExpanded(true);
			}
		}
	}
}
function collapseTree () {
	if (tree) {
		tree.visit(function(node){
	        node.setExpanded(false);
		});
	}
}
/*function loadJobsByDomain(domain, nodeData) {
	var tree = $("#tree").fancytree("getTree"),
    node = tree.getActiveNode();
	$.post("registry_center/connectAndLoadJobs", {name : domain}, function (data) {
        // locate node element.
		var jobNodes = [];
		for (var i = 0; i < data.length; i++) {
        	 var href = "job_detail?jobName=" + data[i].jobName ;
        	 newData = {"title": data[i].jobName},
        	 node.appendSibling(newData);
        }
    });
}*/

function reloadTreeAndExpandJob() {
	var regName = $("#activated-reg-center").text();
	var node = tree.findFirst(regName);
	if (node) {
		node.resetLazy();
	}
	expandJobs(tree, regName);
}

function focusAndActiveJob(jobName) {
	var jobNode = tree.findFirst(jobName);
	if(jobNode) {
		jobNode.setFocus();
		jobNode.setActive();
	}
}

function expandJobsAndSetRegCenter(regName) {
	expandJobs(tree, regName);
	setRegName(regName);
}

function setRegName(regname, ns) {
	if (regname) {
		$("#activated-reg-center").html(regname);
	}
	if (ns) {
		$regNameParent.attr("title", "命名空间："+ns);
	}
}

function setActiveTab(tabName) {
	clearActiveTab();
	var tab = $(tabName);
	if (tab) {
		tab.addClass("active");
	}
}

function clearActiveTab() {
	$(".navbar-nav li").each(function(){
		  $(this).removeClass("active");
	});
}

function reloadJobsAfterRemove() {
	var reg = $("#activated-reg-center").html();
	var node = tree.findFirst(reg);
	if (node) {
		node.resetLazy();
		node.setFocus();
		node.setExpanded();
	}
}


