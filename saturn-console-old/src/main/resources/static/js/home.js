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
var tree, sideBarOverlay = $("#sidebar-overlay");

function reloadTreeData() {
	tree.reload();
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
        	tree = $("#tree").fancytree("getTree")
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
	if (regName && regName != "" && regName != "未连接") {
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
function reloadTreeAndExpandJob(regName) {
	if (tree && tree.activeNode == null) {
		var node = tree.findFirst(regName);
		if (node) {
			node.resetLazy();
		}
		expandJobs(tree, regName);
	}
}

function focusAndActiveJob(jobName) {
	var jobNode = tree.findFirst(jobName);
	if(jobNode) {
		jobNode.setFocus();
		jobNode.setActive();
	}
}

function setRegName(regName,zkAlias) {
	if (regName) {
		$("#activated-reg-center").html(regName);
	}
	if(zkAlias) {
		$("#activated-zk").html(zkAlias);
	}
}

function releaseRegName() {
    $("#activated-reg-center").html("未连接");
    $("#activated-zk").html("未连接");
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

function reloadJobsAfterRemove(regName) {
	var node = tree.findFirst(regName);
	if (node) {
		node.resetLazy();
		node.setFocus();
		node.setExpanded();
	}
}


