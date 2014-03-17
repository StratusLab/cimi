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

var editor = null;

window.onload = function() {
    editor = initialize_editor();
	update_window();
}

window.onhashchange = function() {
	update_window();
}

function initialize_editor() {
    return CodeMirror.fromTextArea(document.getElementById("editor"), {
      lineNumbers: true,
      mode: "application/json",
      gutters: ["CodeMirror-lint-markers"],
      lint: true
    });
}

function update_window() {
    editor_visibility(false);
    d3.json(resource_url(), page_callback);
}

/* provides page contents once data is retrieve from CIMI server */
function page_callback(request, json) {
	if (json) {

		var title = json.resourceURI.split('/').pop();
		render_title(d3.select('main'), title);

		render_metadata(d3.select('#metadata'), metadata(json));
		render_navigation(d3.select('nav'));
		render_operations(d3.select('#operations'), json.operations)
		render_acl(d3.select('#acl'), json.acl)
		clear_message();

		var content_element = d3.select('#content');
		var contents = content(json);
		if (title == 'CloudEntryPoint') {
		    console.log('rendering CEP: ' + contents);
			render_cep(content_element, contents);
		} else if (title.match(/Collection$/)) {
			render_collection(content_element, contents)
		} else {
			render_item(content_element, contents);
		}

        /* update the JSON contents in the editor */
        var s = JSON.stringify(json, null, 2);
        if (editor) {
            editor.setValue(s);
            format_json();
        }

	} else {
		render_error(request);
	}
}

function editor_callback(request, json) {
	if (json) {

        var s = JSON.stringify(json, null, 2);
        if (editor) {
            editor.setValue(s);
            format_json();
        }

	} else {
		render_error(request);
	}
}

function format_json() {
    if (editor) {
        editor.save();
        try {
            var json = JSON.parse(editor.getValue());
            var sjson = JSON.stringify(json, null, 2);
            editor.setValue(sjson);
        } catch(e) {
            console.log('format failed because of invalid JSON: ' + e);
        }
    }
}

function clear_message() {
	d3.select('#message').selectAll('*').remove();
}

function render_message(msg) {
	clear_message();
	d3.select('#message').append('p').text(msg);
}

function render_error(request) {
	render_message(request.statusText + '(' + request.status + ')');
}

function render_operations(o, ops) {
	o.selectAll('ul').remove();

	var ul = o.append('ul');

    /* always provide a 'view json' button */
    append_button(ul, 'view json', 'start_view()', 'normal-mode');
    append_button(ul, 'done', 'finish_view()', 'view-mode');

	if (ops) {
		for (var i = 0; i < ops.length; i++) {
			format_operation(ul, ops[i]);
		}
	}

	button_visibility('normal-mode');

	return o;
}

function render_navigation(o) {

	o.selectAll('*').remove();

	var crumbs = o.append('ul');
	crumbs.append('li')
	.append('a')
	.text('CloudEntryPoint')
	.attr('href', page_uri());

	var id = '';
	var fragments = page_fragments();
	for (var i = 0; i < fragments.length; i++) {
		id = id + fragments[i];
		crumbs.append('li')
		.append('a')
		.text(fragments[i])
		.attr('href', resource_view_url(id));
		id = id + '/';
	}

	return o;
}

function render_title(o, title) {
	o.selectAll('h1').remove();
	o.insert('h1', ':first-child').text(title);
	return o;
}

function render_metadata(o, m) {

	o.selectAll('*').remove();

	var info = o.append('dl');
	if (m.created) {
	    append_term(info, 'created', m.created);
	}
	if (m.updated) {
		append_term(info, 'updated', m.updated);
	}
	if (m.name) {
	    append_term(info, 'name', m.name);
	}
	if (m.description) {
	    append_term(info, 'description', m.description);
	}

	if (m.properties) {
		for (var k in m.properties) {
		    var term = '"' + k + '" (property)';
		    var value = '"' + m.properties[k] + '"';
	        append_term(info, term, value);
		}
	}

	return o;
}

function append_term(dl, term, desc) {
    dl.append('dt').text(term);
    dl.append('dd').text(desc);
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
			
			var link = resource_view_url(id);
			var tag = id.split('/').pop();

			if (tag.match(/^[\dabcdef-]{36}$/)) {
			    tag = tag.substring(0, 8);
			}

			var row = table.append('tr');
			row.append('td').append('a').text(tag).attr('href', link);
			row.append('td').text(name);
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
        append_term(dl, entry.key, entry.value);
	}

	return o;
}

/* finds the first key in the collection which is an array
 * needed because the key is different for each collection
 */
function get_collection_entries(m) {
	for (var k in m) {
		if (Array.isArray(m[k])) {
			return m[k];
		}
	}
	return [];
}

/* pulls only metadata from the given JSON document */
function metadata(json) {
	var result = {};
	for (var key in json) {
		if (key in metadata_keys) {
			result[key] = json[key];
		}
	}
	return result;
}

/* excludes the metadata from the data which is returned */
function content(json) {
	var result = {};
	for (var key in json) {
		if (!(key in metadata_keys)) {
			result[key] = json[key];
		}
	}
	return result;
}

/* formats a given operation as a button inside a list item */
function format_operation(o, op) {
	if (op.href && op.rel) {

		url = resolve_url(op.href);

		button_name = op.rel.split('/').pop();

		var func = '';
		
		if (op.rel=='delete') {
			var parent = parent_page_uri();
			func = 'delete_resource("' + op.href + '", "' + url + '", "' + parent + '")';
			append_button(o, button_name, func, 'normal-mode');
		} else if (op.rel=='edit') {
			append_button(o, button_name, 'start_edit()', 'normal-mode');
			append_button(o, 'save', 'finish_edit("'+ url +'")', 'edit-mode');
			append_button(o, 'cancel', 'cancel_edit()', 'edit-mode');
		} else if (op.rel=='add') {
		    append_button(o, button_name, 'start_add()', 'normal-mode');
			append_button(o, 'save', 'finish_add("' + url + '")', 'add-mode');
			append_button(o, 'cancel', 'cancel_add()', 'add-mode');
		} else {
			append_button(o, button_name, 'do_action("'+ button_name + '", "' + url +'")', 'normal-mode');
		}	
	}
	return o;
}

function append_button(ul, name, func, mode) {
    ul.append('li')
    .append('button')
    .text(name)
    .attr('type', 'button')
    .attr('onclick', func)
    .attr('class', mode + ' opbutton');
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
		return resource_view_url(entry.value.href);
	} else {
		return '';
	}
}

function valid_json(s) {
    try {
        JSON.parse(s);
        return true;
    } catch(e) {
        alert('invalid JSON: ' + e);
        return false;
    }
}

/*
 * Functions and utilities to display the ACL information for a resource.
 */


function render_acl(o, acl) {
	o.selectAll('*').remove();

	if (acl) {
		var table = o.append('table');

        append_acl_row(table, 'ALL', 'OWNER', acl.owner.principal, acl.owner.type);

        var rules = acl.rules;
		for (var i=0; i<rules.length; i++) {
			var rule = rules[i];
			append_acl_row(table, rule.right, '', rule.principal, rule.type);
		}
	} else {
		o.append('p').text('No explicit ACL.');
	}

	return o;
}

function append_acl_row(table, right, owner, identifier, type) {

    var row = table.append('tr');
    row.append('td').text(identifier);
    row.append('td').text(type);
    row.append('td').text(right);

    return table;
}

/*
 * Utility functions that deal with URLs: page, base, and resource
 * URLs.  These also determine the resource URI from the given
 * URL.
 */

function page_uri() {
	return window.location.href.split('#')[0];
}

function parent_page_uri() {
	var elements = page_fragments();
	elements.pop();
	var fragment = elements.join('/');
	return resource_view_url(fragment);
}

function page_fragments() {
	if (window.location.hash) {
		return window.location.href.split('#')[1].split('/');
	} else {
		return [];
	}
}

function base_url() {
	uri = page_uri();
	i = uri.lastIndexOf('webui');
	if (i > 0) {
		return uri.substring(0, i);
	} else {
		return uri;
	}
}

function base_url_no_slash() {
	uri = page_uri();
	i = uri.lastIndexOf('webui');
	if (i > 0) {
		return uri.substring(0, i-1);
	} else {
		return uri;
	}
}

function resolve_url(rel_url) {
	if (rel_url.match(/^\//)) {
		return base_url_no_slash() + rel_url;
	} else {
		return base_url() + rel_url;
	}
}

/* calculates the CIMI resource URL for referenced resource */
function resource_url() {
    url = base_url();
    if (window.location.hash) {
        resource = window.location.hash.substring(1);
       	url = url + resource;
    }
    return url;
}

function resource_view_url(id) {
    if (id=='') {
        return page_uri();
    } else {
        return page_uri() + "#" + id;
    }
}

/*
 * Functions to toggle the visibility of certain elements within
 * the interface.  These make buttons and the editor panel visible
 * or invisible depending on the mode of the interface.
 */

function button_visibility(class_name) {
    var all_buttons = document.getElementsByClassName('opbutton');
    for (var i=0; i<all_buttons.length; i++) {
        var e = all_buttons[i];
        e.style.display = 'none';
    }

    var active_buttons = document.getElementsByClassName(class_name);
    for (var i=0; i<active_buttons.length; i++) {
        var e = active_buttons[i];
        e.style.display = 'inline-block';
    }
}

function editor_visibility(visible) {

    var editor_visibility = 'none';
    var other_visibility = 'block';
    if (visible) {
        editor_visibility = 'block';
        other_visibility = 'none';
    }

    var sections = document.getElementsByClassName('editor-section');
    for (var i=0; i<sections.length; i++) {
        sections[i].style.display = editor_visibility;
    }

    sections = document.getElementsByClassName('normal-section');
    for (var i=0; i<sections.length; i++) {
        sections[i].style.display = other_visibility;
    }
}

/*
 * Functions implementing the actions to be performed on resources
 * and collections.  Modal actions (edit, add) have several related
 * functions to deal with the stages of the action.
 */

function start_view() {
    editor_visibility(true);
    button_visibility('view-mode');
    d3.json(resource_url(), editor_callback);
}

function finish_view() {
    editor_visibility(false);
    button_visibility('normal-mode');
}

function delete_resource(id, url, return_url) {
	if (confirm('Delete resource ' + id + '?')) {
		var xhr = d3.xhr(url, 'application/json');
		xhr.send('DELETE', '',
		    function(error, json) {
			    if (error) {
				    render_message('Delete of resource ' + id + ' failed!');
			    } else {
				    window.location = return_url;
			    }
		    });
	} else {
		render_message('Delete cancelled.');
	}
}

function start_edit() {
    editor_visibility(true);
    button_visibility('edit-mode');
    d3.json(resource_url(), editor_callback);
}

function finish_edit(url) {
    editor.save();
    var sjson = editor.getValue();

    if (valid_json(sjson)) {
        console.log('updating ' + url);
        var xhr = d3.xhr(url, 'application/json');
        xhr.send('PUT', sjson,
            function(error, json) {
                if (error) {
                    var id = window.location.hash.substring(1);
                    render_message('Update of resource ' + id + ' failed!');
                    console.log('update FAILED for ' + url);
                    editor_visibility(false);
                    button_visibility('normal-mode')
                } else {
                    console.log('update succeeded for ' + url);
                    update_window();
                }
            });
    }
}

function cancel_edit() {
    render_message('Edit resource cancelled.');
    editor_visibility(false);
    button_visibility('normal-mode')
}

function start_add() {
    editor_visibility(true);
    button_visibility('add-mode');
    editor.setValue('{"key": "value"}\n');
}

function finish_add(url) {
    editor.save();
    var sjson = editor.getValue();

    if (valid_json(sjson)) {
        console.log('finish_add to ' + url);
        var xhr = d3.xhr(url, 'application/json');
        xhr.send('POST', sjson,
            function(error, json) {
                if (error) {
                    render_message('Adding resource failed!');
                    editor_visibility(false);
                    button_visibility('normal-mode')
                } else {
                    var id = json.getResponseHeader("Location");
                    console.log('new resource ' + id);
                    window.location = resource_view_url(id);
                }
            });
    }

}

function cancel_add() {
    render_message('Adding resource cancelled.');
    editor_visibility(false);
    button_visibility('normal-mode')
}

function do_action(action, url) {
	if (confirm('Perform action ' + action + '?')) {
		var xhr = d3.xhr(url, 'application/json');
		xhr.send('POST', '',
		    function(error, json) {
			    if (error) {
				    render_message('Action ' + action + ' failed!');
			    } else {
				    update_window();
			    }
		    });
	} else {
		render_message('Action cancelled.');
	}
}

