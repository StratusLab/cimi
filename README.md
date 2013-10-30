CIMI
====

[![Build Status](https://secure.travis-ci.org/StratusLab/cimi.png)](http://travis-ci.org/#!/StratusLab/cimi)

This repository contains the Cloud Infrastructure Management Interface
(CIMI) for the StratusLab cloud distribution.

Standard
--------

CIMI is a standard endorsed by the DMTF, [Distributed Management Task
Force](http://dmtf.org/).  It describes a general management model and
a concrete RESTful HTTP-based protocol.  The DMTF publishes both the
[standard][cimi] and a [primer][primer].

Building and Installing
-----------------------

The code and be built via the standard maven commands.  To build
everything and to run the tests, execute the following from the
top-level directory:

    $ mvn clean install

On machines where the `rpmbuild` command is available, the RPM package
will also be generated.

Running the full tests requires having a Couchbase server running on
the build machine with administrator credentials of "admin/ADMIN4".
To execute the build without a Couchbase server, add the option
`-DNO_DB_TESTS` to the above command.

To run the service, you must have [Couchbase][couchbase] already
installed, configured, and running.  The CIMI server will use the
`default` bucket without authentication by default.

**NOTE**: There is a known problem with the Couchbase Java API when
using Java 1.7.  If you see failures when reading the database views,
switch to a version of Java 1.6 instead.  More details can be found in
the [bug report][cb-bug].


Feedback and Bug Reports
------------------------

This code is in an alpha state and quickly evolving.  Nonetheless,
feedback on the code and its functionality is appreciated; please send
this feedback to the [support mailing
list](mailto:support@stratuslab.eu).  Specific bug reports can be
provided via the GitHub issues for this project.


License
-------

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License.  You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.  See the License for the specific language governing
permissions and limitations under the License.


Acknowledgements
----------------

This software extends the cloud distribution developed in the
StratusLab project that was co-funded by the European Community’s
Seventh Framework Programme (Capacities) Grant Agreement
INFSO-RI-261552 and that ran from June 2010 to May 2012.


[cimi]: http://dmtf.org/sites/default/files/standards/documents/DSP0263_1.0.1.pdf
[primer]: http://dmtf.org/sites/default/files/standards/documents/DSP2027_1.0.0.pdf
[couchbase]: http://couchbase.com
[cb-bug]: http://www.couchbase.com/issues/browse/JCBC-151
