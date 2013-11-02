var metadata_keys = {
	'id': true,
	'created': true,
	'updated': true,
	'resourceURI': true,
	'name': true,
	'description': true,
	'operations': true,
	'acl': true,
	'properties': true,
	'baseURI': true
};

/*
window.onpopstate = function(event) {
	if (event.state) {
		url = base_uri() + event.state['resource'];
		d3.json(url, callback);
	}
}
*/

window.onload = function() {
	url = base_uri();
	if (window.location.hash) {
		resource = window.location.hash.substring(1);
		url = url + resource;
	}
	d3.json(url, callback);
}

window.onhashchange = function() {
	url = base_uri();
	if (window.location.hash) {
		resource = window.location.hash.substring(1);
		url = url + resource;
	}
	d3.json(url, callback);
}

function callback(request, json) {
	if (json) {
		var resource_name = json.resourceURI.split('/').pop();
		
		render_title(d3.select('main'), resource_name);
		render_metadata(d3.select('#metadata'), metadata(json));
		render_navigation(d3.select('nav'));
		render_operations(d3.select('#operations'), json.operations)
		clear_error();

		var content_element = d3.select('#content');
		var contents = content(json);
		if (resource_name == 'CloudEntryPoint') {
			render_cep(content_element, contents);
		} else if (resource_name.match(/Collection$/)) {
			render_collection(content_element, contents)
		} else {
			render_item(content_element, contents);
		}

	} else {
		render_error(request);
	}
}

function clear_error() {
	d3.select('#message').selectAll('*').remove();
}

function render_error(request) {
	var msg = request.statusText + '(' + request.status + ')';

	clear_error();
	d3.select('#message').append('p').text(msg);
}

function render_operations(o, ops) {
	o.selectAll('ul').remove();
	if (ops) {
		var ul = o.append('ul');
		for (var i = 0; i < ops.length; i++) {
			format_operation(ul, ops[i]);
		}
	}
	return o;
}

function render_navigation(o) {

	o.selectAll('*').remove();

	var crumbs = o.append('ul');
	crumbs.append('li')
	.append('a')
	.text('CloudEntryPoint')
	.attr('href', page_uri());

	var root = page_uri() + '#';
	var fragments = page_fragments();
	for (var i = 0; i < fragments.length; i++) {
		root = root + fragments[i];
		crumbs.append('li')
		.append('a')
		.text(fragments[i]).attr('href', root);
		root = root + '/';
	}

	return o;
}

function render_title(o, resource_name) {
	o.selectAll('h1').remove();
	o.insert('h1', ':first-child').text(resource_name);
	return o;
}

function render_metadata(o, m) {

	o.selectAll('*').remove();

	var info = o.append('dl');
	if (m.created) {
		info.append('dt').text('created');
		info.append('dd').text(m.created);
	}
	if (m.updated) {
		info.append('dt').text('updated');
		info.append('dd').text(m.updated);
	}
	if (m.name) {
		info.append('dt').text('name');
		info.append('dd').text(m.name);
	}
	if (m.description) {
		info.append('dt').text('description');
		info.append('dd').text(m.description);
	}

	if (m.properties) {
		for (var k in m.properties) {
			info.append('dt').text('property[' + k + ']');
			info.append('dd').text(m.properties[k]);
		}
	}

	return o;
}

function render_cep(o, m) {
	o.selectAll('*').remove();
	o.append('ul')
	.selectAll('li')
	.data(d3.entries(m).sort(function(a, b) {return d3.ascending(a.key, b.key);}))
	.enter()
	.append('li')
	.append('a').text(function(d) {return format_entry(d);})
	.attr('href', function(d) {return format_href(d);});
	return o;
}

function render_collection(o, m) {
	o.selectAll('*').remove();
	
	if (m.count <= 0) {
		o.append('p').text('No items.');
	} else {
		o.append('p').text('Count: ' + m.count);

		var entries = get_collection_entries(m);

		var table = o.append('table');
		for (var i=0; i<entries.length; i++) {
			var id = entries[i].id;
			var name = entries[i].name || '';
			var desc = entries[i].description || '';
			
			var link = page_uri() + "#" + id;
			var tag = id.split('/').pop().substring(0, 8);

			var row = table.append('tr');
			row.append('td').append('a').text(tag).attr('href', link);
			row.append('td').text(name)
			row.append('td').text(desc);			
		}
	}
	
	return o;
}

function render_item(o, m) {
	o.selectAll('*').remove();
	
	var entries = d3.entries(m).sort(function(a, b) {return d3.ascending(a.key, b.key);})

	var dl = o.append('dl');
	for (var i=0; i<entries.length; i++) {
		var entry = entries[i];
		dl.append('dt').text(entry.key);
		dl.append('dd').text(entry.value);
	}

	return o;
}

function get_collection_entries(m) {
	for (var k in m) {
		if (Array.isArray(m[k])) {
			return m[k];
		}
	}
	return [];
}

function metadata(json) {
	var result = {};
	for (var key in json) {
		if (key in metadata_keys) {
			result[key] = json[key];
		}
	}
	return result;
}

function content(json) {
	var result = {};
	for (var key in json) {
		if (!(key in metadata_keys)) {
			result[key] = json[key];
		}
	}
	return result;
}

function format_operation(o, op) {
	if (op.href && op.rel) {
		var uri = base_uri_no_slash();
		var href = base_uri_no_slash();
		if (op.href.match(/^\//)) {
			href = href + op.href;
		} else {
			href = href + "/" + op.href;
		}
		opname = op.rel.split('/').pop();
		o.append('li')
		.append('a')
		.text(opname)
		.attr('href', uri + op.href);
	}
	return o;
}

function format_entry(entry) {
	if (entry.value.href) {
		return entry.value.href;
	} else {
		return 'KEY: ' + entry.key + ', VALUE: ' + entry.value;
	}
}

function format_href(entry) {
	if (entry.value.href) {
		return page_uri() + '#' + entry.value.href;
	} else {
		return '';
	}
}

function page_uri() {
	return window.location.href.split('#')[0];
}

function page_fragments() {
	if (window.location.hash) {
		return window.location.href.split('#')[1].split('/');
	} else {
		return [];
	}
}

function base_uri() {
	uri = page_uri();
	i = uri.lastIndexOf('webui');
	if (i > 0) {
		return uri.substring(0, i);
	} else {
		return uri;
	}
}

function base_uri_no_slash() {
	uri = page_uri();
	i = uri.lastIndexOf('webui');
	if (i > 0) {
		return uri.substring(0, i-1);
	} else {
		return uri;
	}
}

