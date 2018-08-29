
.. contents:: Table of Contents
   :depth: 3

==============
SFF Statistics
==============

[gerrit filter: https://git.opendaylight.org/gerrit/#/q/topic:topic/sff_stats]

This feature introduces functionality to provide statistics for Service
Function Forwarders (SFFs) configured with the SFC OpenFlow renderer.

Problem description
===================
Currently there is no way to easily determine how many packets and
bytes have traversed the different SFFs configured with SFC. It is
possible to obtain this information by looking at the SFC OpenFlow
flows, but this would require a detailed knowledge of the flows.

Use Cases
---------
Provide input and output bytes and packets for each SFF, and for
each Rendered Service Path (RSP) that traverses the SFF.

Proposed change
===============
The SFF statistics will be collected by retrieving the OpenFlow
statistics counters from certain OpenFlow flow entries created
by SFC. The SFF statistics will be the following:

* Total SFF bytes in
* Total SFF bytes out
* Total SFF packets in
* Total SFF packets out
* For each RSP that traverses the SFF

  * SFF RSP per-hop bytes in
  * SFF RSP per-hop bytes out
  * SFF RSP per-hop packets in
  * SFF RSP per-hop packets out

The SFF bytes/packets out can only be less than or equal to the
bytes/packets in. The SFF bytes/packets out will be less than the
bytes/packets in if the packets are dropped by either the SF or SFF.
The sum of the SFF RSP bytes/packets in/out should equal to the
Total SFF bytes/packets in/out.

The SFF input statistics will be collected by accumulating the SFC
OpenFlow TransportIngress flow entries from the SFF. The SFF output
statistics will be collected by accumulating the SFC OpenFlow
TransportEgress flow entries from the SFF.

The SFF RSP input statistics will be collected by retrieving
the SFC OpenFlow NextHop flow entry from this SFF, and the SFF
RSP output statistics will be collected by retrieving the SFC
OpenFlow RSP TransportEgress flow entry from this SFF. The flows
will be retrieved based on their flow name. The following are
examples of the NextHop (table=4) and TransportEgress (table=10)
flows that will be used for one particular RSP hop.

* cookie=0x14, table=4, priority=550,dl_type=0x894f,nsh_spi=0x800001,nsh_si=255 actions=load:0xa00000a->NXM_NX_TUN_IPV4_DST[],goto_table:10
* cookie=0xba5eba1100000101, table=10, priority=655,in_port=1,dl_type=0x894f,nsh_spi=0x800001,nsh_si=255 actions=move:NXM_NX_TUN_ID[0..31]->NXM_NX_TUN_ID[0..31],IN_PORT
* cookie=0xba5eba1100000101, table=10, priority=650,dl_type=0x894f,nsh_spi=0x800001,nsh_si=255 actions=move:NXM_NX_TUN_ID[0..31]->NXM_NX_TUN_ID[0..31],output:1

SFF statistics collection will be provided on-demand by issuing
a Northbound RPC RESTconf command, specifying the SFF to query.
It will also be possible to query the statistics for all SFFs
using the same RPC call by leaving the SFF name empty. If the
SFF specified in the RPC does not exist, then an RPC error
message will be returned. Otherwise the packets and bytes in
and out will be returned indicating the traffic that has gone
through the SFF since its been configured.

Using this on-demand approach, if time-series SFF statistics is
needed, it can be handled externally to ODL, with a proper database
since the MD-SAL is not an appropriate place to store time-series
data.

A new SFC Karaf shell command will be created allowing SFF statistics
to be retrieved via the Karaf shell.

Pipeline changes
----------------
This change will not require the SFC OpenFlow pipeline to be changed.

Yang changes
------------
A YANG data model was previously created for RSP, SFF, and SF
statistics. The SFF data model will be slightlyy changed to allow
for SFF RSP statistics to be collected. The SFF statistics data
model is shown below. Notice the RSP and SF models have been
omitted for brevity.

.. code-block:: none

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

      revision 2018-08-29 {
        description
          "Second Revision";
      }

      rpc get-sff-statistics {
        description
          "Requests statistics for the specified Service Function Forwarder";
        input {
          leaf name {
            type string;
            description
              "The name of the Service Function Forwarder. Leaving
               the name empty will return statistics for all Service
               Function Forwarders.";
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
            list sff-rsp-statistics {
              leaf name {
                type string;
                description
                  "The name of the Rendered Service Path on this SFF.";
              }
              uses sfc-ss:service-statistics-group {
                description "SFF RSP statistics";
              }
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
This feature is targeted to be implemented in the Neon release.

Alternatives
------------
N/A

Usage
=====
Nothing special needs to be done to use this feature, as it will be
an on-demand request via the Northbound RPC RESTConf.

Features to Install
-------------------
This functionality will be included in the previously created
odl-sfc-statistics Karaf feature. No other existing SFC Karaf
features will depend on this new feature.

REST API
--------

The following example shows the new SFC statistics RPC definitions:

.. code-block:: rest

    URL: http://localhost:8181/operations/sfc-statistics-operations:get-sff-statistics

    {
      "input": {
        "name": "sff1"
      }
    }

    {
      "output": {
        "statistics" : [
          {
            "name": "sff1",
            "statistic-by-timestamp": [
              {
                "service-statistic": {
                  "bytes-in": 500,
                  "bytes-out": 500,
                  "packets-in": 25,
                  "packets-out": 25
                },
                "timestamp": 1512418230327
              }
            ],
            "sff-rsp-statistics": [
              {
                "name": "rsp1",
                "statistic-by-timestamp": [
                  {
                    "service-statistic": {
                      "bytes-in": 300,
                      "bytes-out": 300,
                      "packets-in": 15,
                      "packets-out": 15
                    },
                    "timestamp": 1512418230327
                  }
                ]
              },
              {
                "name": "rsp2",
                "statistic-by-timestamp": [
                  {
                    "service-statistic": {
                      "bytes-in": 200,
                      "bytes-out": 200,
                      "packets-in": 10,
                      "packets-out": 10
                    },
                    "timestamp": 1512418230327
                  }
                ]
              }
            ]
          }
        ]
      }
    }



CLI
---
A new Karaf CLI will be added to retrieve SFF statistics. The syntax
will be similar to the following. Leaving the SFF name empty will
return the statistics for all SFFs.

* sfc:sff-statistics [SFF-name]


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

* Modify the SFC statistics collection RPC data model for SFFs.
* Complete the SFF statistics collection handler that will
  retrieve the relevant OpenFlow flows and return the results.
* Create the necessary utils to assist the SFF handler in
  getting the flows and storing the results.
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
