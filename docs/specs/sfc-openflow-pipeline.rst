
.. contents:: Table of Contents
:depth: 3

=====================
SFC Openflow Pipeline
=====================

[gerrit filter: https://git.opendaylight.org/gerrit/#/q/topic:nsh-support]

This feature describes the changes needed to the SFC Openflow pipeline
as a result of migrating the OpenDaylight Openflow Plugin to use the
OVS 2.9 Network Service Headers (NSH) APIs.

Problem description
===================
NSH has only recently been officially merged into the mainline OVS
source code in the 2.8 and 2.9 releases. Previously, a branched
version of the OVS 2.6 code was used for the SFC project. The OVS
NSH APIs have changed from the branched version of the code to the
official OVS 2.9 version of the code, which results in the OpenDaylight
Openflow plugin NSH APIs needing to change and the way SFC uses the
Openflow plugin.

Use Cases
---------
SFC encapsulation with Network Service Headers (NSH), as described
in the NSH RFC: https://tools.ietf.org/html/rfc8300

Proposed change
===============
The details in this section focus on the changes to the SFC OpenFlow
pipeline, and will focus primarily on the NSH aspects of the pipeline.

Pipeline changes
----------------

Current SFC pipeline
++++++++++++++++++++

Currently the SFC pipeline tables are as follows:

    - Table 0, Standalone SFC classifier

    - Table 1, Transport Ingress

    - Table 2, Path Mapper

    - Table 3, Path Mapper ACL

    - Table 4, Next Hop

    - Table 10, Transport Egress

This table structure will not change as a result of this feature. Each
of the tables is detailed in the following sections.

Below is a dump of the existing NSH flows for a standalone SFC deployment
using VXLAN-GPE. Notice the duration, n_packets, and n_bytes fields have
been removed for brevity.

.. code-block:: rest

    cookie=0x14, table=0, priority=5 actions=goto_table:1
    cookie=0x14, table=1, priority=300,udp,nw_dst=10.0.0.10,tp_dst=6633 actions=output:0
    cookie=0x14, table=1, priority=300,udp,in_port=0,tp_dst=6633 actions=LOCAL
    cookie=0x14, table=1, priority=250,nsp=8388641 actions=goto_table:4
    cookie=0x14, table=1, priority=250,nsp=33 actions=goto_table:4
    cookie=0x14, table=1, priority=5 actions=drop
    cookie=0x14, table=2, priority=5 actions=goto_table:3
    cookie=0x14, table=3, priority=5 actions=goto_table:4
    cookie=0x14, table=4, priority=550,nsi=255,nsp=8388641 actions=load:0xa00000a->NXM_NX_TUN_IPV4_DST[],goto_table:10
    cookie=0x14, table=4, priority=550,nsi=255,nsp=33 actions=load:0xa00000a->NXM_NX_TUN_IPV4_DST[],goto_table:10
    cookie=0x14, table=4, priority=5 actions=goto_table:10
    cookie=0xba5eba1100000102, table=10, priority=660,nsi=254,nsp=8388641,nshc1=0 actions=load:0x4->NXM_NX_TUN_GPE_NP[],IN_PORT
    cookie=0xba5eba1100000102, table=10, priority=660,nsi=254,nsp=33,nshc1=0 actions=load:0x4->NXM_NX_TUN_GPE_NP[],IN_PORT
    cookie=0xba5eba1100000103, table=10, priority=655,nsi=254,nsp=8388641,in_port=1 actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],move:NXM_NX_NSI[]->NXM_NX_NSI[],move:NXM_NX_NSP[0..23]->NXM_NX_NSP[0..23],move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],load:0x4->NXM_NX_TUN_GPE_NP[],IN_PORT
    cookie=0xba5eba1100000101, table=10, priority=655,nsi=255,nsp=8388641,in_port=1 actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],move:NXM_NX_NSH_C1[]->NXM_NX_NSH_C1[],move:NXM_NX_NSH_C2[]->NXM_NX_NSH_C2[],move:NXM_NX_NSH_C3[]->NXM_NX_NSH_C3[],move:NXM_NX_NSH_C4[]->NXM_NX_NSH_C4[],move:NXM_NX_TUN_ID[0..31]->NXM_NX_TUN_ID[0..31],load:0x4->NXM_NX_TUN_GPE_NP[],IN_PORT
    cookie=0xba5eba1100000103, table=10, priority=655,nsi=254,nsp=33,in_port=1 actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],move:NXM_NX_NSI[]->NXM_NX_NSI[],move:NXM_NX_NSP[0..23]->NXM_NX_NSP[0..23],move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],load:0x4->NXM_NX_TUN_GPE_NP[],IN_PORT
    cookie=0xba5eba1100000101, table=10, priority=655,nsi=255,nsp=33,in_port=1 actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],move:NXM_NX_NSH_C1[]->NXM_NX_NSH_C1[],move:NXM_NX_NSH_C2[]->NXM_NX_NSH_C2[],move:NXM_NX_NSH_C3[]->NXM_NX_NSH_C3[],move:NXM_NX_NSH_C4[]->NXM_NX_NSH_C4[],move:NXM_NX_TUN_ID[0..31]->NXM_NX_TUN_ID[0..31],load:0x4->NXM_NX_TUN_GPE_NP[],IN_PORT
    cookie=0xba5eba1100000103, table=10, priority=650,nsi=254,nsp=8388641 actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],move:NXM_NX_NSI[]->NXM_NX_NSI[],move:NXM_NX_NSP[0..23]->NXM_NX_NSP[0..23],move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],load:0x4->NXM_NX_TUN_GPE_NP[],output:1
    cookie=0xba5eba1100000101, table=10, priority=650,nsi=255,nsp=8388641 actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],move:NXM_NX_NSH_C1[]->NXM_NX_NSH_C1[],move:NXM_NX_NSH_C2[]->NXM_NX_NSH_C2[],move:NXM_NX_NSH_C3[]->NXM_NX_NSH_C3[],move:NXM_NX_NSH_C4[]->NXM_NX_NSH_C4[],move:NXM_NX_TUN_ID[0..31]->NXM_NX_TUN_ID[0..31],load:0x4->NXM_NX_TUN_GPE_NP[],output:1
    cookie=0xba5eba1100000101, table=10, priority=650,nsi=255,nsp=33 actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],move:NXM_NX_NSH_C1[]->NXM_NX_NSH_C1[],move:NXM_NX_NSH_C2[]->NXM_NX_NSH_C2[],move:NXM_NX_NSH_C3[]->NXM_NX_NSH_C3[],move:NXM_NX_NSH_C4[]->NXM_NX_NSH_C4[],move:NXM_NX_TUN_ID[0..31]->NXM_NX_TUN_ID[0..31],load:0x4->NXM_NX_TUN_GPE_NP[],output:1
    cookie=0xba5eba1100000103, table=10, priority=650,nsi=254,nsp=33 actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],move:NXM_NX_NSI[]->NXM_NX_NSI[],move:NXM_NX_NSP[0..23]->NXM_NX_NSP[0..23],move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],load:0x4->NXM_NX_TUN_GPE_NP[],output:1
    cookie=0x14, table=10, priority=5 actions=drop

Standalone SFC classifier Table
*******************************
This table serves as an SFC classifier when SFC is not used with OpenStack.
This table maps subscriber traffic to Rendered Service Paths (RSPs) by
implementing simple ACLs.

Transport Ingress Table
***********************
This table serves to only allow the expected transports or protocols to
enter SFC, and drops everything else. There will be an entry per expected
tunnel transport type to be received in SFC, as established in the SFC
configuration.

Currently the only way to check for packets with NSH is to check if the
NSP (Network Services Path), which is the Service Chain ID is present.
This means that there will be a transport ingress flow for each service
chain configured, as follows. Notice this directs packets directly to
the NextHop table since neither of the PathMapper tables are needed
for NSH.

.. code-block:: rest

    priority=250,nsp=33 actions=goto_table:4
    priority=250,nsp=8388641 actions=goto_table:4

Path Mapper Table
*****************
This table maps transport information to a particular Service Chain.
Currently this table is not used for NSH, but is used for instance with
VLAN or MPLS to map a VLAN tag or MPLS label to a particular service chain.
Currently the VLAN and MPLS transports have limited support.

Path Mapper ACL Table
*********************
This table is used for TCP Proxy type Service Functions (SFs). Flows are
only added to this table as a result of a PacketIn, and they will have an
inactivity expiration timeout of 60 seconds. If a SF has the TCP Proxy
flag set true, then a flow will be created in the Transport Egress table
for the SF that will cause a PacketIn to OpenDaylight for packets that
egress to the SF. Since TCP Proxy SFs can generate their own packets,
this table maps those TCP Proxy SF generated packets to the corresponding
service chain.

Next Hop Table
**************
This table determines where SFC packets should be sent next, typically
either to an SF or to another SFF. For NSH, there will be a match on
both the NSP (service chain ID) and NSI (service chain hop) to determine
the next hop. Upon matching, the VXLAN-GPE tunnel destination IPv4 address
is set, and the packets are sent to the TransportEgress table, as follows.

.. code-block:: rest

    priority=550,nsi=255,nsp=8388641 actions=load:0xa00000a->NXM_NX_TUN_IPV4_DST[],goto_table:10
    priority=550,nsi=255,nsp=33 actions=load:0xa00000a->NXM_NX_TUN_IPV4_DST[],goto_table:10

Transport Egress Table
**********************
This table prepares packets for egress by either setting tunnel information,
such as VLAN tags, VXLAN-GPE information, or encapsulating MPLS. These flows
also determine the output port where the packets should be sent. The NSH
TransportEgress flows are more complicated than the rest, and are identified
by their cookie values. The available NSH TransportEgress cookies are listed
below.

    - 0xba5eba1100000101 - TRANSPORT_EGRESS_NSH_VXGPE_COOKIE

    - 0xba5eba1100000102 - TRANSPORT_EGRESS_NSH_VXGPE_NSC_COOKIE

    - 0xba5eba1100000103 - TRANSPORT_EGRESS_NSH_VXGPE_LASTHOP_COOKIE

    - 0xba5eba1100000201 - TRANSPORT_EGRESS_NSH_ETH_COOKIE

    - 0xba5eba1100000202 - TRANSPORT_EGRESS_NSH_ETH_LOGICAL_COOKIE

    - 0xba5eba1100000203 - TRANSPORT_EGRESS_NSH_ETH_LASTHOP_PIPELINE_COOKIE

    - 0xba5eba1100000204 - TRANSPORT_EGRESS_NSH_ETH_LASTHOP_TUNNEL_REMOTE_COOKIE

    - 0xba5eba1100000205 - TRANSPORT_EGRESS_NSH_ETH_LASTHOP_TUNNEL_LOCAL_COOKIE

    - 0xba5eba1100000206 - TRANSPORT_EGRESS_NSH_ETH_LASTHOP_NSH_REMOTE_COOKIE

    - 0xba5eba1100000207 - TRANSPORT_EGRESS_NSH_ETH_LASTHOP_NSH_LOCAL_COOKIE

As can be seen in the VXGPE NSH flows below, all of the NSH TransportEgress
flows match on at least the NSP (service chain ID) and NSI (hop in the chain).
Notice some of the flows match on the in_port and then output the packets
to IN_PORT, while other seemingly duplicate flows output the packets to
a specific port without matching on the in_port. These flows are indeed
exactly the same, except for the differences just mentioned and the flow
priorities. This is because according to the OpenFlow specification, the
only way a packet can be sent out on the same port it was received on is
by deliberately sending it out using the IN_PORT port string, or it will
be dropped, in an effort to avoid packet loops.

Notice that many of these flows have move actions. These are because in
the branched version of OVS 2.6 with NSH, these values are not explicitly
maintained when the packet is egressed.

Some additional logic is needed on the lasthop, which is when packets
have traversed the entire service chain, and need to be sent out of SFC.
Information for where to send the packet after SFC is set in the NSH C1
and C2 metadata headers by the SFC classifier. The C1 header is the
VXLAN-GPE tunnel destination IPv4 address, and C2 is the VXLAN-GPE VNI
field.

.. code-block:: rest

    cookie=0xba5eba1100000102,
    priority=660,nsi=254,nsp=33,nshc1=0
    actions=load:0x4->NXM_NX_TUN_GPE_NP[],IN_PORT

    cookie=0xba5eba1100000103,
    priority=655,nsi=254,nsp=33,in_port=1
    actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],
        move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],
        move:NXM_NX_NSI[]->NXM_NX_NSI[],
        move:NXM_NX_NSP[0..23]->NXM_NX_NSP[0..23],
        move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],
        move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],
        load:0x4->NXM_NX_TUN_GPE_NP[],
        IN_PORT

    cookie=0xba5eba1100000101,
    priority=655,nsi=255,nsp=33,in_port=1
    actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],
        move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],
        move:NXM_NX_NSH_C1[]->NXM_NX_NSH_C1[],
        move:NXM_NX_NSH_C2[]->NXM_NX_NSH_C2[],
        move:NXM_NX_NSH_C3[]->NXM_NX_NSH_C3[],
        move:NXM_NX_NSH_C4[]->NXM_NX_NSH_C4[],
        move:NXM_NX_TUN_ID[0..31]->NXM_NX_TUN_ID[0..31],
        load:0x4->NXM_NX_TUN_GPE_NP[],
        IN_PORT

    cookie=0xba5eba1100000101,
    priority=650,nsi=255,nsp=33
    actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],
        move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],
        move:NXM_NX_NSH_C1[]->NXM_NX_NSH_C1[],
        move:NXM_NX_NSH_C2[]->NXM_NX_NSH_C2[],
        move:NXM_NX_NSH_C3[]->NXM_NX_NSH_C3[],
        move:NXM_NX_NSH_C4[]->NXM_NX_NSH_C4[],
        move:NXM_NX_TUN_ID[0..31]->NXM_NX_TUN_ID[0..31],
        load:0x4->NXM_NX_TUN_GPE_NP[],
        output:1

    cookie=0xba5eba1100000103,
    priority=650,nsi=254,nsp=33
    actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],
        move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],
        move:NXM_NX_NSI[]->NXM_NX_NSI[],
        move:NXM_NX_NSP[0..23]->NXM_NX_NSP[0..23],
        move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],
        move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],
        load:0x4->NXM_NX_TUN_GPE_NP[],
        output:1


Changes to the SFC pipeline
+++++++++++++++++++++++++++
The tables that will be affected by this feature are detailed below.

Transport Ingress Table
***********************
For NSH, this table will now match on the packet_type field instead
of the NSP. Now there will only be one flow in this table for NSH.
TODO give an example of the flow.

Path Mapper Table
*****************
Currently this table is only used for VLAN or MPLS, but since VXLAN
will be added soon, this table will be used for all transports and
encapsulations. 2 Nicira registers will be used to store the Service
Chain ID and the Hop counter.
TODO give an example of the flow.

Next Hop Table
**************
Since the PathMapper table will now be used by all protocols and
transports, there will no longer be matches in this table for
specific protocol details like the NSH NSP and NSI fields. Instead
the matches in this table will be on the 2 Nicira registers set in
the PathMapper table.
TODO give an example of the flow.

Transport Egress Table
**********************
Similar to the matching changes in the NextHop table, this table will now
match on the 2 Nicira registers set in the PathMapper table. Additionally,
the previous move actions to set NSH fields on egress will no longer be
needed.

Yang changes
------------
This feature will not introduce any changes to the SFC Yang data model.

Configuration impact
--------------------
The SFC configuration API will not need to be changed for this feature.

Clustering considerations
-------------------------
There will be no clustering impacts as a result of this feature.

Other Infra considerations
--------------------------
The SFC infrastructure will no longer need to use the branched version
of OVS, called the Yi Yang patch, which was based on OVS 2.6. The
infrastructure will now need to use OVS 2.9, and a suitable version
of Linux.

Security considerations
-----------------------
There are no additional security considerations as a result of this feature.

Scale and Performance Impact
----------------------------
The changes to the SFC pipeline will be minimal, so no scaling nor
performance impacts will be introduced.

Targeted Release
----------------
This feature is targeted for Fluorine.

Alternatives
------------
The only alternative is to stay with the older branched version of OVS,
which is not ideal, since we should always strive to use official versions
of upstream projects, which this feature will do.

Usage
=====
How will end user use this feature? Primary focus here is how this feature
will be used in an actual deployment.

This section will be primary input for Test and Documentation teams.
Along with above this should also capture REST API and CLI.

Features to Install
-------------------
odl-sfc-openflow-renderer

Identify existing karaf feature to which this change applies and/or new karaf
features being introduced. These can be user facing features which are added
to integration/distribution or internal features to be used by other projects.

REST API
--------
Sample JSONS/URIs. These will be an offshoot of yang changes. Capture
these for User Guide, CSIT, etc.

CLI
---
Any CLI if being added.

Implementation
==============

Assignee(s)
-----------
Primary assignee:
  Brady Johnson, IRC: bjohnson, bjohnson@inocybe.com

Other contributors:
  Jaime Caamaño, IRC: jaicaa, jcaamano@suse.com

Work Items
----------
Break up work into individual items. This should be a checklist on a
Trello card for this feature. Provide the link to the trello card or duplicate it.

Dependencies
============
Any dependencies being added/removed? Dependencies here refers to internal
[other ODL projects] as well as external [OVS, karaf, JDK etc]. This should
also capture specific versions if any of these dependencies.
e.g. OVS version, Linux kernel version, JDK etc.

This should also capture impacts on existing projects that depend on SFC.

Following projects currently depend on SFC:
 GBP
 Netvirt

Testing
=======
Capture details of testing that will need to be added.

Unit Tests
----------

Integration Tests
-----------------

CSIT
----

Documentation Impact
====================
What is the impact on documentation for this change? If documentation
changes are needed call out one of the <contributors> who will work with
the Project Documentation Lead to get the changes done.

Don't repeat details already discussed but do reference and call them out.

References
==========
Add any useful references. Some examples:

* Links to Summit presentation, discussion etc.
* Links to mail list discussions
* Links to patches in other projects
* Links to external documentation

[1] `OpenDaylight Documentation Guide <http://docs.opendaylight.org/en/latest/documentation.html>`__

[2] https://specs.openstack.org/openstack/nova-specs/specs/kilo/template.html

.. note::

  This template was derived from [2], and has been modified to support our project.

  This work is licensed under a Creative Commons Attribution 3.0 Unported License.
  http://creativecommons.org/licenses/by/3.0/legalcode
