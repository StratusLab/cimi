% VM REST API Specification
%
% ${project.version}

Introduction
============

The virtual management management API uses RESTful Web Service
technologies.  RESTful web services allow faster service development,
lighter-weight services, easier use by clients, and faster evolution
with changing user requirements.

These web services follow the Resource Oriented Architecture (ROA)
pattern. Virtual machines are mapped into an URI hierarchy; standard
HTTP verbs are used to manipulate those resources.

This document describes three types of resources: the root resource,
authentication resources, and compute resources. Eventually this service will
be expanded to proxy other StratusLab services, providing separate REST APIs
under `/storage`, `/marketplace`, etc.

Generally, responses from the server can be provided in HTML (text/html),
[JSON (application/json)](http://www.json.org/), or [EDN
(application/edn)](https://github.com/edn-format/edn) format. Similarly, POST
requests can usually be sent in application/x-www-form-urlencoded,
multipart/form-data, JSON, and EDN. See the complete API descriptions for
details.

Root Resource
=============

The root resource of the server (`/`) does *not* require
authentication.  It is used to provide information about the current
status of the service(s).

`/` (GET)
---------

This resource provides information about the service endpoint(s), high-level
resource utilization, and the user's authentication information (if logged
in). 

### HTTP Status Codes
  * 200: server is correctly configured and running

### MIME Types
  * Response: HTML, JSON, EDN

Authentication Resources
========================

The authentication resources do *not* require authentication. These resources
allow users to authenticate with the server via a form, receiving an
authentication token (session cookie) to be used in future requests. 

The workflow for authentication is to POST user credentials to the `login`
resource that then returns an authentication token (session cookie) to the
client to be used in future requests. The token can be removed by the client
manually or by POSTING to the `logout` resource.

`/login` (GET)
--------------

Provides a form that can be posted to the same URL to obtain an authentication
token from the server. (This token in the form of a session cookie must be
returned for each subsequent request.)

### HTTP Status Codes
  * 200: server provides form for posting credentials

### MIME Types
  * Response: HTML


`/login` (POST)
---------------

Users authenticate with the server by sending credentials to the service via a
POST. The HTTP entity with the request can be in
application/x-www-form-urlencoded, multipart/form-data, JSON, or EDN format
with "username" and "password" fields.

If the credentials are valid, then the server will send an authentication
token (session cookie) to the user. The server may send either a `200` status
code or a `302` response if there was an initial referring URL.

### HTTP Status Codes
  * 200: authentication succeeded message with authentication token
  * 302: redirect to previous resource with authentication token
  * 401: invalid credentials were presented

### MIME Types
  * Request: application/x-www-form-urlencoded, multipart/form-data, JSON, EDN


`/logout` (GET)
---------------

Provides a form that can be posted to the same URL to logout from the service
(i.e. delete the authentication token).

### HTTP Status Codes
  * 200: server provides form for posting credentials

### MIME Types
  * Response: HTML


`/logout` (POST)
----------------

Users can logout from the service (remove the authentication token) by posting
to this URL. The HTTP entity with the request can be in
application/x-www-form-urlencoded, multipart/form-data, JSON, or EDN format
with a "logout" field containing a value of "true".

A successful logout will return a `302` code and redirect the client to the
root resource. It is not an error if the client does not currently possess an
authentication token.

### HTTP Status Codes
  * 302: logout succeeded; client redirected to root resource

### MIME Types
  * Request: application/x-www-form-urlencoded, multipart/form-data, JSON, EDN


Compute Resources
=================

These resources allow virtual machines to be created, manipulated, and
deleted. Accessing any of these resources requires a valid authentication
cookie from the client. All resources except the root compute resource require
authentication; those resources will return an authentication required `401`
status for un-authenticated requests.

`/compute` (GET)
----------------

The root compute resource of the server (`/compute`) does *not* require
authentication.

It will provide some general metrics of resource utilization on the platform.
If the user is authenticated, then additional information is provided giving
the user's resource utilization metrics. 

### HTTP Status Codes
  * 200: service is working and provides utilization metrics

### MIME Types
  * Response: HTML, JSON, EDN


`/compute/vm` (GET)
-------------------

Provides a form for launching a virtual machine.  This form is a
subset of the full set of options that can be specified with a
template in JSON or EDN.

### HTTP Status Codes
  * 200: successful response with list of virtual machines
  * 401: unauthorized request
  
### MIME Types
  * Response: HTML


`/compute/vm` (POST)
--------------------

This will create a new virtual machine on the server. The data to associate
with the virtual machine can be passed in with a variety of formats. The form
MIME types provide only a subset of the full machine parameters.

The response will provide the newly-created virtual machine's identifier. The
HTTP response in this case is `302` with the URI of the new virtual machine in
the "Location" header. The body will also include the raw machine identifier.

NOTE: Should OVF be used as a valid input format?

NOTE: The detailed specifications for the various input formats will be
defined as the implementation progresses.

### HTTP Status Codes
  * 302: redirect to URL of new virtual machine
  * 401: unauthorized request

### MIME Types
  * Request: application/x-www-form-urlencoded, multipart/form-data, JSON, EDN 
  * Response: HTML, JSON, EDN


`/compute/vm/:vmid` (GET)
-------------------------

Show the status of a particular virtual machine.  This will return a
status code of `200` for existing resources along with the status of
the virtual machine.

### HTTP Status Codes
  * 200: successful response with contents of named virtual machine
  * 401: unauthorized request
  * 404: non-existant resource or user cannot access this virtual machine
  * 410: server *may* provide this response for a deleted virtual machine

### MIME Types
  * Response: HTML, JSON, EDN

`/compute/vm/:vmid` (PUT)
-------------------------

This will update the status of the virtual machine. The data to update the
virtual machine can be provided in a variety of formats. Not all fields can be
updated; trying to update a read-only field will result in an error.

### HTTP Status Codes
  * 200: successful response to update virtual machine
  * 400: bad request (e.g. trying to update an invalid field)
  * 401: unauthorized request
  * 403: user may not perform this request (e.g. state change without 
    sufficient priviledges)
  * 404: non-existant resource or user cannot access this virtual machine

### MIME Types
  * Request: application/x-www-form-urlencoded, multipart/form-data, JSON, EDN 
  * Response: HTML, JSON, EDN


`/compute/vm/:vmid` (DELETE)
----------------------------

This will kill a virtual machine. This immediately kills the machine (if
running) and releases any reserved resources.

### HTTP Status Codes
  * 200: successful response to kill virtual machine
  * 302: server may redirect to `/compute/vm` on success
  * 401: unauthorized request
  * 403: user may not perform this request (e.g. deleting VM user does
    not own)
  * 409: conflict with deleting the resource

### MIME Types
  * Response: HTML, JSON, EDN
