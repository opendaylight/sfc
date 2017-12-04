
.. contents:: Table of Contents
   :depth: 3

==============
RSP Statistics
==============

[gerrit filter: https://git.opendaylight.org/gerrit/#/q/topic:topic/rsp_stats]

This feature introduces functionality to collect statistics for
Rendered Service Paths (RSPs) defined with the SFC OpenFlow renderer.

Problem description
===================
Currently there is no way to easily determine how many packets and
bytes have traversed the different RSPs created with SFC. Its possible
to obtain this information by looking at the SFC OpenFlow flows, but
this would require a detailed knowledge of the flows.

Use Cases
---------
Provide input and output bytes and packets for each RSP.

Proposed change
===============
The RSP statistics will be collected by retrieving the OpenFlow
statistics counters from certain OpenFlow flow entries created
by SFC. The RSP statistics will be the following:

* RSP bytes in
* RSP bytes out
* RSP packets in
* RSP packets out

The RSP bytes/packets out can only be less than or equal to the
bytes/packets in. The RSP bytes/packets out will be less than the
bytes/packets in if the packets are dropped by either the SF or SFF.

The RSP in statistics will be collected by retrieving the SFC
OpenFlow NextHop flow entry from the first-hop SFF in the RSP.
The RSP out statistics will be collected by retrieving the SFC
OpenFlow NextHop flow entry from the last-hop SFF in the RSP.

RSP statistics collection will be triggered by creation of the RSP
OpenFlow flows. Likewise, when an RSP is deleted, the statistics
collection will be stopped. The RSP statistics will be collected by
periodically retrieving the OpenFlow flows from the SFFs. This
collection period will be stored in the sfc-statistics-configuration
MD-SAL configuration data store, and will be configurable. The value
stored will be in seconds, and will be an unsigned 32 bit integer.
The default statistics collection period will be 120 seconds.

There will be an SFC statistics listener that listens to modifications
to the sfc-statistics-configuration data store. Upon receiving a
modification, the existing periodic RSP statistics collectors will
all be halted and restarted with the new periodic value. If the RSP
statistics collection period is 0 (zero) then the RSP statistics
collection will be turned off.

A new SFC Karaf shell command will be created that will update and
retrieve the RSP statistics collection period.

A single RSP statistics entry will be stored in the existing service
statistics node of the RSP data model. The counters stored in the RSP
represent the total packets and bytes since the OpenFlow flow entry
was created.

Pipeline changes
----------------
This change will not require the SFC OpenFlow pipeline to be changed.

Yang changes
------------
Although this feature only covers RSP statistics, the following
YANG model includes configuration for Service Function (SF) and
Service Function Forwarder (SFF) statistics collection.

.. code-block:: yang

    module sfc-statistics-configuration {
      yang-version 1;

      namespace "urn:inocybe:params:xml:ns:yang:sfc-statistics-configuration";
      prefix "sfc-stats-confic";

      organization "Inocybe, Inc.";
      contact "Brady Johnson <bjohnson@inocybe.com>";

      revision 2017-11-30 {
        description
          "This module defines the SFC statistics configuration.
           These configuration values will only be stored in the
           configuration data store.";
      }

      container sfc-statistics-configuration {
        description
          "Configuration values for the SFC statistics collection";

        leaf sfc-rsp-statistics-period {
          description
            "The collection period in seconds for RSP statistics
             gathering. Set to 0 to disable rsp statistics.";

          type uint32;
          default 120;
        }

        leaf sfc-sf-statistics-period {
          description
            "The collection period in seconds for Service Function
             statistics gathering. Set to 0 to disable sf statistics.";

          type uint32;
          default 120;
        }

        leaf sfc-sff-statistics-period {
          description
            "The collection period in seconds for Service Function
             Forwarder statistics gathering. Set to 0 to disable
             sff statistics.";

          type uint32;
          default 120;
        }
      }
    }

Configuration impact
--------------------
The sfc-statistics-configuration YANG model will be added for this
feature. This new data model will have the RSP statistics collection
period. If the period is 0 (zero) then the RSP statistics collection
will be stopped. The period will be stored in an unsigned 32 bit integer.
The default value for the statistics period will be 120 seconds.

Clustering considerations
-------------------------
The RSP statistics feature will not affect clustering, and will work
with no problems in an ODL cluster

Other Infra considerations
--------------------------
N/A

Security considerations
-----------------------
N/A

Scale and Performance Impact
----------------------------
An individual ScheduledExecutorService will be launched for each RSP.
The number of RSPs created is expected to be low, on the order of 100
to 200 at most in production environments. Only 1 thread will be used
upon creation with the Java Executors.newScheduledThreadPool(). Keeping
this in mind, the RSP statistics collection should not have any noticeable
scaling nor performance impacts.

Targeted Release
----------------
This feature is targeted to be implemented in the Oxygen release.

Alternatives
------------
N/A

Usage
=====
Nothing special needs to be done to use this feature, as it will be
started automatically upon RSP OpenFlow flow creation. The statistics
collection can be turned off by setting the collection period to 0.

Features to Install
-------------------
A new Karaf feature will be created called odl-sfc-statistics. This new
feature will be installed when the existing odl-sfc-openflow-renderer
Karaf feature is installed.

REST API
--------

The SFC statistics collection periods can be retrieved as follows:

.. code-block:: rest

    URL: http://localhost:8181/config/sfc-statistics-configuration:sfc-statistics-configuration

    {
        "sfc-statistics-configuration": {
            "sfc-rsp-statistics-period": 30,
            "sfc-sff-statistics-period": 30,
            "sfc-sf-statistics-period":  30
        }
    }

The following example shows an RSP with statistics:

.. code-block:: rest

    URL: http://localhost:8181/config/sfc-statistics-configuration:sfc-statistics-configuration

    {
        "rendered-service-paths": {
            "rendered-service-path": [
                {
                    "name": "sfc-path-1sf1sff-Reverse",
                    "parent-service-function-path": "sfc-path-1sf1sff",
                    "path-id": 8388625,
                    "rendered-service-path-hop": [
                        {
                            "hop-number": 0,
                            "service-function-forwarder": "sff1",
                            "service-function-forwarder-locator": "vxgpe",
                            "service-function-name": "sf1",
                            "service-index": 255
                        }
                    ],
                    "service-chain-name": "sfc-chain-1sf1sff",
                    "sfc-encapsulation": "service-locator:nsh",
                    "starting-index": 255,
                    "statistic-by-timestamp": [
                        {
                            "service-statistic": {
                                "bytes-in": 0,
                                "bytes-out": 0,
                                "packets-in": 0,
                                "packets-out": 0
                            },
                            "timestamp": 1512418230327
                        }
                    ],
                    "symmetric-path-id": 17,
                    "transport-type": "service-locator:vxlan-gpe"
                }
            ]
        }
    }


CLI
---
A new Karaf CLI will be added to retrieve and configure the statistics
collection period. The syntax will similar to the following 3 commands:

* sfc:statistics get rsp-period
* sfc:statistics set rsp-period <seconds>
* sfc:statistics deactivate-rsp


Implementation
==============

Assignee(s)
-----------
Primary assignee:
  <Brady Johnson>, <ebrjohn>, <bjohnson@inocybe.com>

Work Items
----------
Break up work into individual items. This should be a checklist on a
Trello card for this feature. Provide the link to the trello card or duplicate it.

* Create the SFC statistics collection configuration data model.
* Create a listener and related logic to handle collection period
  updates.
* Create a Statistics Manager to handle orchestrating the RSP
  statistics collection.
* Create an RSP statistics collection handler that will retrieve the
  relevant OpenFlow flows and store the results in the RSP.
* Create the necessary utils to assist the RSP handler in getting the
  flows and storing the results.
* Create the new odl-sfc-statistics Karaf feature.
* Create the Karaf shell command to modify statistics collection periods.

Dependencies
============
No external projects will depend on this new feature. Nor will any
additional dependencies on other ODL project be introduced.

Testing
=======
Capture details of testing that will need to be added.

Unit Tests
----------
A new Unit Test will be added for each of the new Java classes added.

Integration Tests
-----------------
N/A

CSIT
----
A new test case will be added to CSIT for this feature. The test will
verify that statistics are stored on the RSP as expected. The test will
also verify that statistics are no longer collected when the period is
set to 0.

Documentation Impact
====================
The User Guide will be updated to show how to use this new feature.

References
==========
N/A

.. note::

  This work is licensed under a Creative Commons Attribution 3.0 Unported License.
  http://creativecommons.org/licenses/by/3.0/legalcode
