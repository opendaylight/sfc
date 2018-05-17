
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

The following is a quick summary of the changes when comparing the
latest NSH implementation to the old one:

* All NSH fields now have the prerequisite of verifying the packet is
  a NSH packet. This is achieved either by matching ether_type=0x894f
  if the outermost header is Ethernet or by matching
  packet_type=(1,0x894f) if the outermost header is NSH.

* push_nsh and pop_nsh actions are not available. Instead, encap(nsh)
  is used to add a NSH header to a packet followed by encap(ethernet)
  to add an Ethernet header on top of the nsh header. decap() can be
  used twice to remove both the Ethernet and NSH headers.

* tun_gpe_np field is not available. The corresponding header field
  is internally managed by OVS.

* encap_eth_type, encap_eth_src and encap_eth_dst fields are no longer
  available. Standard Ethernet fields will have to be used instead,
  which will apply to the outermost Ethernet header of the packet.

* nsh_mdtype and nsh_np fields are read only. nsh_mdtype can be set as
  an argument to the encap NSH action and defaults to 1. nsh_np is set
  internally by OVS.

* New fields available: nsh_flags and nsh_ttl.

* New action available: dec_nsh_ttl.

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

Currently, for a standalone SFC deployment, the SFC pipeline tables are
as follows:

    - Table 0, Standalone SFC classifier

    - Table 1, Transport Ingress

    - Table 2, Path Mapper

    - Table 3, Path Mapper ACL

    - Table 4, Next Hop

    - Table 10, Transport Egress

When SFC is integrated with the Netvirt and Genius ODL projects for an
OpenStack deployment, the table numbers are the following:

    - Table 83, Transport Ingress

    - Table 84, Path Mapper

    - Table 85, Path Mapper ACL

    - Table 86, Next Hop

    - Table 87, Transport Egress

This table structure will not change as a result of this feature. Each
of the tables is detailed in the following sections.

SFC flow output
***************

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


The following 3 samples are taken from an OPNFV SFC deployment, using
the ODL Netvirt project as a classifier. The classifier-SFF and SFF-SFF
encapsulation is VXLAN-GPE, and the SFF-SF encapsulation is ETH+NSH.

Netvirt classifier tables:

.. code-block:: rest

    cookie=0xf005ba1100000001, table=100, priority=520,nsi=253,nsp=39 actions=move:NXM_NX_NSH_C4[]->NXM_NX_REG6[],pop_nsh,resubmit(,220)
    cookie=0xf005ba1100000001, table=100, priority=511,encap_eth_type=0x894f,tun_dst=0.0.0.0 actions=resubmit(,17)
    cookie=0xf005ba1100000001, table=100, priority=510,encap_eth_type=0x894f actions=resubmit(,83)
    cookie=0xf005ba1100000001, table=100, priority=500 actions=goto_table:101

    cookie=0xf005ba1100000002, table=101, priority=500,tcp,in_port=7,tp_dst=80
    actions=push_nsh,load:0x1->NXM_NX_NSH_MDTYPE[],load:0x3->NXM_NX_NSH_NP[],load:0x27->NXM_NX_NSP[0..23],load:0xff->NXM_NX_NSI[],load:0xffffff->NXM_NX_NSH_C1[],load:0->NXM_NX_NSH_C2[],resubmit(,17)
    cookie=0xf005ba1100000002, table=101, priority=10 actions=resubmit(,17)

    cookie=0xf005ba1100000003, table=221, priority=260,nshc1=16777215 actions=load:0->NXM_NX_NSH_C1[],goto_table:222
    cookie=0xf005ba1100000003, table=221, priority=250 actions=resubmit(,220)

    cookie=0xf005ba1100000004, table=222, priority=260,nshc1=0,nshc2=0
    actions=move:NXM_NX_REG0[]->NXM_NX_NSH_C1[],move:NXM_NX_TUN_ID[0..31]->NXM_NX_NSH_C2[],move:NXM_NX_REG6[]->NXM_NX_NSH_C4[],load:0->NXM_NX_TUN_ID[0..31],goto_table:223
    cookie=0xf005ba1100000004, table=222, priority=250 actions=goto_table:223

    cookie=0xf005ba1100000005, table=223, priority=260,nsp=39 actions=resubmit(,83)


These flows are the SFC flows when SFC is integrated with the Netvirt
and Genius ODL projects.

.. code-block:: rest

    cookie=0x14, table=83, priority=250,nsp=39 actions=goto_table:86
    cookie=0x14, table=83, priority=5 actions=resubmit(,17)

    cookie=0x14, table=84, priority=5 actions=goto_table:85

    cookie=0x14, table=85, priority=5 actions=goto_table:86

    cookie=0x14, table=86, priority=550,nsi=254,nsp=39 actions=load:0xfe163eccbf0c->NXM_NX_ENCAP_ETH_SRC[],load:0xfa163eccbf0c->NXM_NX_ENCAP_ETH_DST[],goto_table:87
    cookie=0x14, table=86, priority=550,nsi=255,nsp=39 actions=load:0xfe163e8e7bca->NXM_NX_ENCAP_ETH_SRC[],load:0xfa163e8e7bca->NXM_NX_ENCAP_ETH_DST[],goto_table:87
    cookie=0x14, table=86, priority=5 actions=goto_table:87

    cookie=0xba5eba1100000207, table=87, priority=680,nsi=253,nsp=39,nshc1=2887643148,nshc2=0 actions=resubmit(,17)
    cookie=0xba5eba1100000205, table=87, priority=660,nsi=253,nsp=39,nshc1=2887643148 actions=move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],pop_nsh,resubmit(,36)
    cookie=0xba5eba1100000203, table=87, priority=680,nsi=253,nsp=39,nshc1=0 actions=pop_nsh,set_field:fa:16:3e:cc:bf:0c->eth_src,resubmit(,17)
    cookie=0xba5eba1100000206, table=87, priority=670,nsi=253,nsp=39,nshc2=0 actions=move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],output:1
    cookie=0xba5eba1100000202, table=87, priority=650,nsi=254,nsp=39 actions=load:0x1700->NXM_NX_REG6[],resubmit(,220)
    cookie=0xba5eba1100000202, table=87, priority=650,nsi=255,nsp=39 actions=load:0x1800->NXM_NX_REG6[],resubmit(,220)
    cookie=0xba5eba1100000204, table=87, priority=650,nsi=253,nsp=39 actions=move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],pop_nsh,output:1
    cookie=0x14, table=87, n_packets=0, priority=5 actions=resubmit(,17)


The following flows are the rest of the pertinent ODL Genius project
flows shown for completeness.

.. code-block:: rest

    cookie=0x8000001, table=0, priority=5,in_port=1 actions=write_metadata:0x10000000001/0xfffff0000000001,goto_table:36
    cookie=0x8000000, table=0, priority=4,in_port=2,vlan_tci=0x0000/0x1fff actions=write_metadata:0x40000000001/0xffffff0000000001,goto_table:17
    cookie=0x8000000, table=0, priority=4,in_port=6,vlan_tci=0x0000/0x1fff actions=write_metadata:0x150000000000/0xffffff0000000001,goto_table:17
    cookie=0x8000000, table=0, priority=4,in_port=7,vlan_tci=0x0000/0x1fff actions=write_metadata:0x160000000000/0xffffff0000000001,goto_table:17
    cookie=0x8000000, table=0, priority=4,in_port=8,vlan_tci=0x0000/0x1fff actions=write_metadata:0x170000000000/0xffffff0000000001,goto_table:17
    cookie=0x8000000, table=0, priority=4,in_port=9,vlan_tci=0x0000/0x1fff actions=write_metadata:0x180000000000/0xffffff0000000001,goto_table:17

    cookie=0x8000001, table=17, priority=10,metadata=0x40000000000/0xffffff0000000000 actions=load:0x186a0->NXM_NX_REG3[0..24],write_metadata:0x9000040000030d40/0xfffffffffffffffe,goto_table:19
    cookie=0x8040000, table=17, priority=10,metadata=0x9000040000000000/0xffffff0000000000 actions=load:0x4->NXM_NX_REG1[0..19],load:0x138a->NXM_NX_REG7[0..15],write_metadata:0xa00004138a000000/0xfffffffffffffffe,goto_table:43
    cookie=0x6900000, table=17, priority=10,metadata=0x150000000000/0xffffff0000000000 actions=write_metadata:0x8000150000000000/0xfffffffffffffffe,goto_table:210
    cookie=0x8040000, table=17, priority=10,metadata=0x9000150000000000/0xffffff0000000000 actions=load:0x15->NXM_NX_REG1[0..19],load:0x139c->NXM_NX_REG7[0..15],write_metadata:0xa00015139c000000/0xfffffffffffffffe,goto_table:43
    cookie=0x8000001, table=17, priority=10,metadata=0x8000150000000000/0xffffff0000000000 actions=load:0x186b3->NXM_NX_REG3[0..24],write_metadata:0x9000150000030d66/0xfffffffffffffffe,goto_table:19
    cookie=0x8040000, table=17, priority=10,metadata=0x9000160000000000/0xffffff0000000000 actions=load:0x16->NXM_NX_REG1[0..19],load:0x139c->NXM_NX_REG7[0..15],write_metadata:0xa00016139c000000/0xfffffffffffffffe,goto_table:43
    cookie=0x8000001, table=17, priority=10,metadata=0x8000160000000000/0xffffff0000000000 actions=load:0x186b3->NXM_NX_REG3[0..24],write_metadata:0x9000160000030d66/0xfffffffffffffffe,goto_table:19
    cookie=0x8040000, table=17, priority=10,metadata=0x9000170000000000/0xffffff0000000000 actions=load:0x17->NXM_NX_REG1[0..19],load:0x139c->NXM_NX_REG7[0..15],write_metadata:0xa00017139c000000/0xfffffffffffffffe,goto_table:43
    cookie=0x8040000, table=17, priority=10,metadata=0x9000180000000000/0xffffff0000000000 actions=load:0x18->NXM_NX_REG1[0..19],load:0x139c->NXM_NX_REG7[0..15],write_metadata:0xa00018139c000000/0xfffffffffffffffe,goto_table:43
    cookie=0x8000001, table=17, priority=10,metadata=0x8000180000000000/0xffffff0000000000 actions=load:0x186b3->NXM_NX_REG3[0..24],write_metadata:0x9000180000030d66/0xfffffffffffffffe,goto_table:19
    cookie=0x8030000, table=17, priority=10,metadata=0x180000000000/0xffffff0000000000 actions=write_metadata:0x8000180000000000/0xfffffffffffffffe,goto_table:83
    cookie=0x8000001, table=17, priority=10,metadata=0x8000170000000000/0xffffff0000000000 actions=load:0x186b3->NXM_NX_REG3[0..24],write_metadata:0x9000170000030d66/0xfffffffffffffffe,goto_table:19
    cookie=0xf005ba1100000001, table=17, priority=10,metadata=0x4000160000000000/0xffffff0000000000 actions=write_metadata:0x8000160000000000/0xfffffffffffffffe,goto_table:100
    cookie=0x6900000, table=17, priority=10,metadata=0x160000000000/0xffffff0000000000 actions=write_metadata:0x4000160000000000/0xfffffffffffffffe,goto_table:210
    cookie=0x8030000, table=17, priority=10,metadata=0x170000000000/0xffffff0000000000 actions=write_metadata:0x4000170000000000/0xfffffffffffffffe,goto_table:83
    cookie=0xf005ba1100000001, table=17, priority=10,metadata=0x4000170000000000/0xffffff0000000000 actions=write_metadata:0x8000170000000000/0xfffffffffffffffe,goto_table:100
    cookie=0x8000000, table=17, priority=0,metadata=0x8000000000000000/0xf000000000000000 actions=write_metadata:0x9000000000000000/0xf000000000000000,goto_table:80

    cookie=0xf005ba1100000006, table=36, priority=10,encap_eth_type=0x894f,tun_id=0 actions=resubmit(,100)
    cookie=0x900139c, table=36, priority=5,tun_id=0x2f actions=write_metadata:0x139c000000/0xfffffffff000000,goto_table:51
    cookie=0x9000000, table=36, priority=5,tun_id=0 actions=goto_table:83
    cookie=0x90186bb, table=36, priority=5,tun_id=0x186bb actions=resubmit(,25)
    cookie=0x90186bc, table=36, priority=5,tun_id=0x186bc actions=resubmit(,25)
    cookie=0x90186bd, table=36, priority=5,tun_id=0x186bd actions=resubmit(,25)
    cookie=0x90186be, table=36, priority=5,tun_id=0x186be actions=resubmit(,25)

    cookie=0x8000007, table=220, priority=10,reg6=0x90000400,metadata=0x1/0x1 actions=drop
    cookie=0x8000007, table=220, priority=9,reg6=0x90001500 actions=output:6
    cookie=0x8000007, table=220, priority=9,reg6=0x90001600 actions=output:7
    cookie=0x8000007, table=220, priority=9,reg6=0x90001700 actions=output:8
    cookie=0xf005ba1100000003, table=220, priority=8,reg6=0x1700 actions=load:0x90001700->NXM_NX_REG6[],load:0xac1df00c->NXM_NX_REG0[],write_metadata:0/0xfffffffffe,goto_table:221
    cookie=0xf005ba1100000003, table=220, priority=8,reg6=0x80001500 actions=load:0x90001500->NXM_NX_REG6[],load:0xac1df00c->NXM_NX_REG0[],write_metadata:0/0xfffffffffe,goto_table:221
    cookie=0x6900000, table=220, priority=6,reg6=0x1500 actions=load:0x80001500->NXM_NX_REG6[],write_metadata:0/0xfffffffffe,goto_table:239
    cookie=0x8000007, table=220, priority=9,reg6=0x90000400 actions=output:2
    cookie=0xf005ba1100000003, table=220, priority=8,reg6=0x400 actions=load:0x90000400->NXM_NX_REG6[],load:0xac1df00c->NXM_NX_REG0[],write_metadata:0/0xfffffffffe,goto_table:221
    cookie=0x8000007, table=220, priority=9,reg6=0x90001800 actions=output:9
    cookie=0xf005ba1100000003, table=220, priority=8,reg6=0x1800 actions=load:0x90001800->NXM_NX_REG6[],load:0xac1df00c->NXM_NX_REG0[],write_metadata:0/0xfffffffffe,goto_table:221
    cookie=0xf005ba1100000003, table=220, priority=8,reg6=0x100 actions=load:0x90000100->NXM_NX_REG6[],load:0xac1df00b->NXM_NX_REG0[],write_metadata:0/0xfffffffffe,goto_table:221
    cookie=0x8000007, table=220, priority=9,reg6=0x90000100 actions=output:1
    cookie=0xf005ba1100000003, table=220, priority=8,reg6=0x80001600 actions=load:0x90001600->NXM_NX_REG6[],load:0xac1df00c->NXM_NX_REG0[],write_metadata:0/0xfffffffffe,goto_table:221
    cookie=0x6900000, table=220, priority=6,reg6=0x1600 actions=load:0x80001600->NXM_NX_REG6[],write_metadata:0/0xfffffffffe,goto_table:239


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
the next hop.

For a standalone SFC deployment, when using VXLAN-GPE towards the SFs,
the VXLAN-GPE tunnel destination IPv4 address is set, and the packets
are sent to the TransportEgress table, as follows.

.. code-block:: rest

    priority=550,nsi=255,nsp=8388641 actions=load:0xa00000a->NXM_NX_TUN_IPV4_DST[],goto_table:10
    priority=550,nsi=255,nsp=33 actions=load:0xa00000a->NXM_NX_TUN_IPV4_DST[],goto_table:10

For as OpenStack SFC deployment, when using Eth+NSH towards the SFs,
the outer Ethernet addresses are set, and the packets are sent to the
TransportEgress table, as follows.

.. code-block:: rest

    priority=550,nsi=254,nsp=39 actions=load:0xfe163eccbf0c->NXM_NX_ENCAP_ETH_SRC[],load:0xfa163eccbf0c->NXM_NX_ENCAP_ETH_DST[],goto_table:87
    priority=550,nsi=255,nsp=39 actions=load:0xfe163e8e7bca->NXM_NX_ENCAP_ETH_SRC[],load:0xfa163e8e7bca->NXM_NX_ENCAP_ETH_DST[],goto_table:87


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

Standalone SFC TransportEgress flows:

.. code-block:: rest

    cookie=0xba5eba1100000102, table=10,
    priority=660,nsi=254,nsp=33,nshc1=0
    actions=load:0x4->NXM_NX_TUN_GPE_NP[],IN_PORT

    cookie=0xba5eba1100000103, table=10,
    priority=655,nsi=254,nsp=33,in_port=1
    actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],
        move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],
        move:NXM_NX_NSI[]->NXM_NX_NSI[],
        move:NXM_NX_NSP[0..23]->NXM_NX_NSP[0..23],
        move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],
        move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],
        load:0x4->NXM_NX_TUN_GPE_NP[],
        IN_PORT

    cookie=0xba5eba1100000101, table=10,
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

    cookie=0xba5eba1100000101, table=10,
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

    cookie=0xba5eba1100000103, table=10,
    priority=650,nsi=254,nsp=33
    actions=move:NXM_NX_NSH_MDTYPE[]->NXM_NX_NSH_MDTYPE[],
        move:NXM_NX_NSH_NP[]->NXM_NX_NSH_NP[],
        move:NXM_NX_NSI[]->NXM_NX_NSI[],
        move:NXM_NX_NSP[0..23]->NXM_NX_NSP[0..23],
        move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],
        move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],
        load:0x4->NXM_NX_TUN_GPE_NP[],
        output:1


SFC integrated with OpenStack flows:

.. code-block:: rest

    cookie=0xba5eba1100000207, table=87,
    priority=680,nsi=253,nsp=39,nshc1=2887643148,nshc2=0
    actions=resubmit(,17)

    cookie=0xba5eba1100000205, table=87,
    priority=660,nsi=253,nsp=39,nshc1=2887643148
    actions=move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],
        move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],
        pop_nsh,resubmit(,36)

    cookie=0xba5eba1100000203, table=87,
    priority=680,nsi=253,nsp=39,nshc1=0
    actions=pop_nsh,set_field:fa:16:3e:cc:bf:0c->eth_src,resubmit(,17)

    cookie=0xba5eba1100000206, table=87,
    priority=670,nsi=253,nsp=39,nshc2=0
    actions=move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],
        move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],
        output:1

    cookie=0xba5eba1100000202, table=87,
    priority=650,nsi=254,nsp=39
    actions=load:0x1700->NXM_NX_REG6[],resubmit(,220)

    cookie=0xba5eba1100000202, table=87,
    priority=650,nsi=255,nsp=39
    actions=load:0x1800->NXM_NX_REG6[],resubmit(,220)

    cookie=0xba5eba1100000204, table=87,
    priority=650,nsi=253,nsp=39
    actions=move:NXM_NX_NSH_C1[]->NXM_NX_TUN_IPV4_DST[],
        move:NXM_NX_NSH_C2[]->NXM_NX_TUN_ID[0..31],
        pop_nsh,output:1


Changes to the SFC pipeline
+++++++++++++++++++++++++++
The tables that will be affected by this feature are detailed below.

Transport Ingress Table
***********************
For NSH, this table will now match on either the ether_type or the
packet_type as follows:

    - When the packet arrives from a standard port as eth+nsh, then
      match on ether_type=0x894F

    - When the packet arrives from a vxlan+eth+nsh tunnel port and the
      resulting packet after tunnel decapsulation is eth+nsh, then match
      on ether_type=0x894F

    - When the packet arrives from a vxlan+nsh tunnel port, and the
      resulting packet after tunnel decapsulation is directly nsh,
      then match on packet_type=(1,0x894F)

Path Mapper Table
*****************
Currently this table is only used for VLAN or MPLS, but since VXLAN
will be added soon, this table will be used for all transports and
encapsulations. 2 Nicira registers will be used to store the Service
Chain ID and the Hop counter. For NSH, we will need to match on
ether_type or packet_type, in addition to the NSP and NSI.

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
needed. Notice the NXM_NX_TUN_GPE field will no longer be available, and
the GPE NP fields will be handled internally by OVS. The NXM_NX_NSH_MDTYPE
field will now be read-only.

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
There will not be any CLI changes as a result of this feature.

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
The SFC OpenFlow pipeline will be updated in the User Guide as a result
of the changes for this new feature.

References
==========

[1] `Network Service Headers RFC <https://tools.ietf.org/html/rfc8300>`__

[2] https://specs.openstack.org/openstack/nova-specs/specs/kilo/template.html

.. note::

  This template was derived from [2], and has been modified to support our project.

  This work is licensed under a Creative Commons Attribution 3.0 Unported License.
  http://creativecommons.org/licenses/by/3.0/legalcode
