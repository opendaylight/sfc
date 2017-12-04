
.. contents:: Table of Contents
   :depth: 3

==============
RSP Statistics
==============

[gerrit filter: https://git.opendaylight.org/gerrit/#/q/topic:topic/rsp_stats]

This feature introduces functionality to provide statistics for
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

The RSP statistics will be collected by retrieving the SFC
OpenFlow NextHop flow entry from the first-hop SFF in the RSP.
The RSP out statistics will be collected by retrieving the SFC
OpenFlow NextHop flow entry from the last-hop SFF in the RSP.
The flows will be retrieved based on their flow name.

RSP statistics collection will be provided on-demand by issuing
a Northbound RPC RESTconf command, specifying the RSP to query.
It will also be possible to query the statistics for all RSPs using
the same RPC call. If the RSP specified in the RPC does not exist,
then an RPC error message will be returned. Otherwise the packets
and bytes in and out will be returned indicating the traffic that
has gone through the RSP since its been created.

Using this on-demand approach, if time-series RSP statistics is needed,
it can be handled externally to ODL, with a proper database since
the MD-SAL is not an appropriate place to store time-series data.

A new SFC Karaf shell command will be created allowing RSP statistics
to be retrieved via the Karaf shell.

Pipeline changes
----------------
This change will not require the SFC OpenFlow pipeline to be changed.

Yang changes
------------
Although this feature only covers RSP statistics, the following
YANG model also includes operations for Service Function (SF)
and Service Function Forwarder (SFF) statistics collection.

.. code-block:: yang

    module sfc-statistics-operations {

      namespace "urn:inocybe:params:xml:ns:yang:sfc-stats-ops";
      prefix sfc-stats-ops;

      import service-statistics {
          prefix sfc-ss;
          revision-date 2014-07-01;
      }

      organization "Inocybe, Inc.";
      contact "Brady Johnson <bjohnson@inocybe.com>";

      description
        "This module contains RPC operations to collect SFC statistics";

      revision 2017-12-15 {
        description
          "Initial Revision";
      }

      rpc get-rsp-statistics {
        description
          "Requests statistics for the specified Rendered Service Path";
        input {
          leaf name {
            type string;
            description
              "The name of the Rendered Service Path. Specifying all
               will return statistics for all Rendered Service Paths.";
          }
        }
        output {
          list statistics {
            leaf name {
              type string;
              description
                "The name of the Rendered Service Path.";
            }
            uses sfc-ss:service-statistics-group {
              description "Rendered Service Path statistics";
            }
          }
        }
      }

      rpc get-sff-statistics {
        description
          "Requests statistics for the specified Service Function Forwarder";
        input {
          leaf name {
            type string;
            description
              "The name of the Service Function Forwarder. Specifying
               all will return statistics for all Service Function
               Forwarders.";
          }
        }
        output {
          list statistics {
            leaf name {
              type string;
              description
                "The name of the Service Function Forwarder.";
            }
            uses sfc-ss:service-statistics-group {
              description "Service Function Forwarder statistics";
            }
          }
        }
      }

      rpc get-sf-statistics {
        description
          "Requests statistics for the specified Service Function";
        input {
          leaf name {
            type string;
            description
              "The name of the Service Function. Specifying all will
               return statistics for all Service Functions.";
          }
        }
        output {
          list statistics {
            leaf name {
              type string;
              description
                "The name of the Service Function.";
            }
            uses sfc-ss:service-statistics-group {
              description "Service Function statistics";
            }
          }
        }
      }

    }


Configuration impact
--------------------
There will be no configuration impacts as a result of this feature.

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
Since this will be an on-demand statistics request, there will be no
scale and performance impacts.

Targeted Release
----------------
This feature is targeted to be implemented in the Oxygen release.

Alternatives
------------
N/A

Usage
=====
Nothing special needs to be done to use this feature, as it will be
an on-demand request via the Northbound RPC RESTConf.

Features to Install
-------------------
A new Karaf feature will be created called odl-sfc-statistics. No
other existing SFC Karaf features will depend on this new feature.

REST API
--------

The following example shows the new SFC statistics RPC definitions:

.. code-block:: rest

    URL: http://localhost:8181/operations/sfc-statistics-operations:get-rsp-statistics

    {
      "input": {
        "name": "RSP-1sf1sff"
      }
    }

    {
      "output": {
        "statistics" : [
          {
            "name": "RSP-1sf1sff",
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
            ]
          }
        ]
      }
    }



CLI
---
A new Karaf CLI will be added to retrieve RSP statistics. The syntax
will similar to the following:

* sfc:rsp-statistics [<RSP name> | all]


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

* Create the SFC statistics collection RPC data model.
* Create an RSP statistics collection handler that will retrieve the
  relevant OpenFlow flows and return the results.
* Create the necessary utils to assist the RSP handler in getting the
  flows and storing the results.
* Create the new odl-sfc-statistics Karaf feature.
* Create the Karaf shell command to retrieve the statistics.

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
A new test case will be added to CSIT for this feature. The test should
inject packets and will verify that the RSP statistics counters are
incremented as expected.

Documentation Impact
====================
The User Guide will be updated to show how to use this new feature.

References
==========
N/A

.. note::

  This work is licensed under a Creative Commons Attribution 3.0 Unported License.
  http://creativecommons.org/licenses/by/3.0/legalcode
