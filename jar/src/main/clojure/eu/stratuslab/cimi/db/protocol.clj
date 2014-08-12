;
; Copyright 2014 SixSq Sàrl
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;

(ns eu.stratuslab.cimi.db.protocol)

(defprotocol Operations
  "This protocol defines the core interface to the underlying database.
   All of the functions accept and return native clojure data structures.
   The functions must handle all necessary conversions for the database.

   On errors, the functions must throw an ex-info with an error ring
   response.  This simplifies the logic and code of the client using this
   protocol."

  (bootstrap
    [this]
    "Performs all of the necessary initialization of the database.  For
     example, adding required resources.")

  (close
    [this]
    "This function should be called when use of the binding is finished
     to free any allocated resources.")

  (add
    [this resource]
    "This function adds the given resource to the database.  The resource
     must not already exist in the database.  The resource ID will be taken
     from the resource itself.

     On success, the function must return a 201 ring response with the
     relative URL of the new resource as the Location.

     On failure, the function must throw an ex-info containing the error
     ring response.  The error must be 409 (conflict) if the resource
     exists already.  Other appropriate error codes can also be thrown.")

  (retrieve
    [this id]
    "This function retrieves the identified resource from the database.

     On success, this returns the clojure map representation of the
     resource.

     On failure, this function must throw an ex-info containing the error
     ring response. If the resource doesn't exist, use a 404 status.")

  (edit
    [this resource]
    "This function updates (edits) the given resource in the database.
     The resource must already exist in the database.  The resource ID
     is taken from the resource itself.

     On success, the function must return a 200 ring response.

     On failure, the function must throw an ex-info containing the error
     ring response.  The error must be 404 (not-found) if the resource
     does not exist.  Other appropriate error codes can also be thrown.")

  (delete
    [this resource]
    "This function removes the given resource in the database.  The resource
     is taken from the resource itself.

     On success, the function must return a 204 (no content) ring response.

     On failure, the function must throw an ex-info containing the error
     ring response.  If the resource does not exist, then a 404 response
     should be returned.  Other appropriate error codes can also be thrown.")

  (query
    [this collection-id options]
    "This function returns a list of resources, where the collection-id
     corresponds to the name of a Collection.  A map of options may be
     supported by a given protocol implementation.

     On success, the function must return a list of the given resources.
     This list may possibly be empty.

     On failure, the function must throw an ex-info containing the error
     ring response.  If the resource-id does not correpond to a Collection,
     then a 400 (bad-request) response must be returned.  Other appropriate
     error codes can also be thrown.")
  )

