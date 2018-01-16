
var main = function() {

	var query = args.query;
	if(query == null || query == "") {
		model.result = jsonUtils.toJSONString({ "data" : [] });
		return;
	}
	var docs = searchext.tenantSearch(query);
	
	var dataList = [];
	for(var i in docs) {
		var doc = docs[i];
		var tenant = doc.getTenant();
		
	var props = doc.getPropertiesTenant();
		dataList.push({
			"tenant" : tenant,
			"fileName": props["cm:name"],
			"creator": props["cm:creator"],
			"nodeRef": doc.getNodeRef()
		});
		
	}
	var data = {
		"data" : dataList 
	};
	
	model.result = jsonUtils.toJSONString(data);
};


main();



