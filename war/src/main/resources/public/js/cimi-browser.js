
var metadata_keys = {'id': true, 'created': true, 'updated': true,
                     'resourceURI': true, 'name': true, 'description': true,
                     'operations': true, 'acl': true};

function page_uri() {
  if (window.location.hash) {
      return window.location.href.split('#')[0];
  } else {
      return window.location.href;
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

window.onload = function() {
  url = base_uri();
  if (window.location.hash) {
      resource = window.location.hash.substring(1);
      url = url + resource;
  }
  console.log("ONLOAD URL: " + url);
  d3.json(url, callback);
}

window.onpopstate = function(event) {
  if (event.state) {
      url = base_uri() + event.state['resource'];
      console.log("ONPOPSTATE URL: " + url);
      d3.json(url, callback);
  }
}

function callback(request, json) {
  if (json) {
      console.log("CALLBACK GOT JSON");
      render_metadata(d3.select('#metadata'), metadata(json));
      render_contents(d3.select('#content'), content(json));
      clear_error();
  } else {
      console.log("CALLBACK GOT ERROR");
      render_error(request);
  }
}

function clear_error() {
  d3.select('#message')
      .selectAll('*')
      .remove();
}

function render_error(request) {
  var msg = request.statusText + '(' + request.status + ') ::' + base_uri() + ' :: ' + page_uri();

  clear_error();
  d3.select('#message')
      .append('p')
      .text(msg);
}

function render_metadata(o, m) {

  o.selectAll('*').remove();

  o.append('h1').text(m.resourceURI);

  var times = o.append('ul');
  times.append('li').text('Created: ' + m.created);
  times.append('li').text('Updated: ' + m.updated);

  if (m.name) {
      o.append('h2').text('Name: ' + m.name);
  }
  if (m.description) {
      o.append('p').text('Description: ' + m.description);
  }

  return o;
}

function render_contents(o, m) {

  o.selectAll('*').remove();
  o.append('p').text('Resources');
  o.append('ul')
      .selectAll('li')
      .data(d3.entries(m).sort(function(a, b) {return d3.ascending(a.key, b.key);}))
      .enter()
      .append('li')
      .append('a')
      .text(function(d) {return format_entry(d);})
      .attr('href', function(d) {return format_href(d);})
      .attr('onclick', function(d) {return format_onclick(d);});
  return o;
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
      if (! (key in metadata_keys)) {
          result[key] = json[key];
      }
  }
  return result;
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

function format_onclick(entry) {
     if (entry.value.href) {
         return 'change_resource(\"' + entry.value.href + '\");'
     } else {
         return '';
     }
 }

function change_resource(resource) {
    history.pushState({'resource': page_uri()}, '', window.location.href)
    d3.json(base_uri() + resource, callback);
}
