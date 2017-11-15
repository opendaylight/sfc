
.. contents:: Table of Contents
   :depth: 3

=====================
Title of the feature
=====================

[gerrit filter: https://git.opendaylight.org/gerrit/#/q/topic:rsp-config]

Currently Rendered Service Paths (RSPs) are created directly in the
Operational Data Store via an RPC operation. This Spec details the
refactoring involved to create RSPs first in the Config data store,
and then to the Operational data store.

Problem description
===================
Since the Rendered Service Paths (RSPs) are currently only written to
the Operational Data Store, the RSPs will not be present in the data
store when ODL restarts.

With the refactoring proposed in this spec, SFP creation will trigger
RSP creation, which will cause a write to the RSP Configuration Data
Store with information that needs to be persisted across ODL restarts.
The RSP Configuration Data Store creation will trigger RSP Operational
Data Store creation. To avoid confusion, updates to the RSP Configuration
Data Store will be rejected by a newly created RSP MD-SAL Validator.

Upon ODL restart, the RSPs will be written to Operational Data Store
based on what is already present in the RSP Configuration Data Store.
This change will make RSP creation be inline with traditional methods
of creating entries using the Config/Operational data store methodology.

Use Cases
---------

* RSP data integrity upon ODL restart.

Proposed change
===============

Pipeline changes
----------------
The existing OpenFlow pipeline will not be affected by this change.

Yang changes
------------
All yang models related to the current RSP creation via RPC will be
deprecated in Oxygen and removed in Fluorine.

It will now be possible to create the RSP in the configuration data
store. The actual RSP data model contents will not change otherwise.

The following RSP data model is the updated data model, with
comments highlighting the changed nodes. The changed nodes will
only have the "config false" attribute modified to reflect which
nodes will only be written in the Operational data store.

.. code-block:: rendered-service-path.yang

    module rendered-service-path {
       container rendered-service-paths {
          // UPDATED This container is no longer "config false"
          description
            "A container that holds the list of all Rendered Service Paths
             in a SFC domain";
          list rendered-service-path {
            key "name";
            description
              "A list that holds operational data for all RSPs in the
               domain";
            leaf name {
              type sfc-common:rsp-name;
              description
                "The name of this rendered function path. This is the same
                 name as the associated SFP";
            }
            leaf parent-service-function-path {
              type sfc-common:sfp-name;
              description
                "Service Function Path from which this RSP was
                 instantiated";
            }
            leaf transport-type {
              // UPDATED This node is now "config false"
              config false;

              type sfc-sl:sl-transport-type-def;
              default "sfc-sl:vxlan-gpe";
              description
                "Transport type as set in the Parent Service Function
                 Path";
            }
            leaf context-metadata {
              // UPDATED This node is now "config false"
              config false;

              type sfc-md:context-metadata-ref;
              description
                "The name of the associated context metadata";
            }
            leaf variable-metadata {
              // UPDATED This node is now "config false"
              config false;

              type sfc-md:variable-metadata-ref;
              description
                "The name of the associated variable metadata";
            }
            leaf tenant-id {
              type string;
              description
                "This RSP was created for a specific tenant-id";
            }
            uses sfc-ss:service-statistics-group {
              // UPDATED This node is now "config false"
              config false;

              description "Global Rendered Service Path statistics";
            }
            list rendered-service-path-hop {
              key "hop-number";
              leaf hop-number {
                type uint8;
                description
                  "A Monotonically increasing number";
              }
              leaf service-function-name {
                type sfc-common:sf-name;
                description
                  "Service Function name";
              }
              leaf service-function-group-name {
                type string;
                description
                  "Service Function group name";
              }
              leaf service-function-forwarder {
                type sfc-common:sff-name;
                description
                  "Service Function Forwarder name";
              }
              leaf service-function-forwarder-locator {
                type sfc-common:sff-data-plane-locator-name;
                description
                  "The name of the SFF data plane locator";
              }
              leaf service-index {
                type uint8;
                description
                  "Provides location within the service path.
                   Service index MUST be decremented by service functions
                   or proxy nodes after performing required services.  MAY
                   be used in conjunction with service path for path
                   selection.  Service Index is also valuable when
                   troubleshooting/reporting service paths.  In addition to
                   location within a path, SI can be used for loop
                   detection.";
              }
              ordered-by user;
              description
                "A list of service functions that compose the
                 service path";
            }
            leaf service-chain-name {
              // UPDATED This node is now "config false"
              config false;

              type sfc-common:sfc-name;
              mandatory true;
              description
                "The Service Function Chain used as blueprint for this
                 path";
            }
            leaf starting-index {
              // UPDATED This node is now "config false"
              config false;

              type uint8;
              description
                "Starting service index";
            }
            leaf path-id {
              type uint32 {
                range "0..16777216";
              }
              mandatory true;
              description
                "Identifies a service path.
                 Participating nodes MUST use this identifier for path
                 selection.  An administrator can use the service path
                 value for reporting and troubleshooting packets along
                 a specific path.";
            }
            leaf symmetric-path-id {
              type uint32 {
                range "0..16777216";
              }
              description
                "Identifies the associated symmetric path, if any.";
            }
            leaf sfc-encapsulation {
              // UPDATED This node is now "config false"
              config false;

              type sfc-sl:sfc-encapsulation-type;
              description
                "The type of encapsulation used in this path for passing
                SFC information along the chain";
            }
          }
       }
    }


Configuration impact
--------------------
All yang models related to the current RSP creation via RPC will
be deprecated in Oxygen and removed in Fluorine. It will now be
possible to create the RSP in the configuration data store. The
"config false" flag will be removed from the RSP data model, thus
allowing it to be created in the Config data store.

Although the RSP creation via RPC will be deprecated in the Oxygen
release, it will still be supported until Fluorine. Once this change
is implemented, the preferred way of creating RSPs will be via a write
to the Config Data Store.

Clustering considerations
-------------------------
Currently RSPs support clustering, which will not be affected by this change.

Other Infra considerations
--------------------------
None

Security considerations
-----------------------
None

Scale and Performance Impact
----------------------------
With this change, there will be an additional write to the data store
for each RSP creation. Considering there shouldnt be many RSPs created
(typically less than 100) the impacts should be negligible.

Targeted Release
----------------
This feature is targeted for the Oxygen release.

Alternatives
------------
None

Usage
=====

Features to Install
-------------------
All changes will be in the following existing Karaf features:

* odl-sfc-model
* odl-sfc-provider

REST API
--------
The following JSON shows how an SFP is created, which will trigger the creation
of the RSPs in both the Configuration and Operational data store.

.. code-block:: rest

    URL: http://localhost:8181/config/service-function-path:rendered-service-paths/

    {
      "service-function-paths": {
        "service-function-path": [
          {
            "name": "sfp1",
            "service-chain-name": "sfc1",
            "transport-type": "service-locator:vxlan-gpe",
            "symmetric": true
          }
        ]
      }
    }


CLI
---
A new Karaf Shell command will be added to list the RSPs.

Implementation
==============

Assignee(s)
-----------
Primary assignee:

*  Brady Johnson, #ebrjohn, bradyallenjohnson@gmail.com

Other contributors:

*  David Suárez, #edavsua, david.suarez.fuentes@gmail.com

Work Items
----------

* Deprecate existing RSP RPC creation yang models.
* Deprecate existing RSP RPC Java classes and/or methods.
* Modify existing RSP data model "config false" values:

  * The entire RSP data model should no longer be "config false".
  * Mark those RSP data model leaf nodes as "config false" that
    will only be in operational.

* Create SFP configuration data store listener.
* Create RSP configuration data store listener.
* Create code to reject updates to the RSP configuration data store.
* Copy and retrofit existing code that writes RSPs to operational via
  RPCs to do so via the RSP configuration listener instead of via RPC.
* Create Karaf Shell CLI command to list RSPs in the config and
  operational data stores.

Dependencies
============
The following projects currently depend on SFC, and will be affected
by this change:

* GBP
* Netvirt

Testing
=======

Unit Tests
----------

* The RSP creation in the existing UT will need to be updated
  as a result of this change.
* UT will need to be added to test RSP creation.

Integration Tests
-----------------
None

CSIT
----
The RSP creation in the existing CSIT tests will need to be updated
as a result of this change.

Documentation Impact
====================
Both the User Guide and Developer Guide will need to be updated by
the current ODL SFC Documentation contact: David Suárez.

References
==========

[1] `OpenDaylight Documentation Guide <http://docs.opendaylight.org/en/latest/documentation.html>`__
