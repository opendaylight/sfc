SFC104 Demo
===========

Overview
--------

SFC103 demo is docker container based, so you have no way to run it if you will use OVS DPDK or VPP, so we have to create SFC104 demo, SFC104 demo actually includes three demos, they are for OVS, OVS DPDK and VPP, respectively.

Your host machine needs 32GB memory at least because DPDK or VPP uses hugepages which must be allocated in advance, 6 vagrant VMs will occupy 24 GB memory. Too less memory will result in vagant hang or some other weird behaviors.

Topology
-------

                           +-----------------+
                           | Host (ODL SFC)  |
                           |  192.168.60.1   |
                           +-----------------+
                       /      |          |     \
                    /         |          |         \
                /             |          |             \
+---------------+  +--------------+   +--------------+  +---------------+
|  classifier1  |  |    sff1      |   |     sff2     |  |  classifier2  |
| 192.168.60.10 |  |192.168.60.20 |   |192.168.60.50 |  | 192.168.60.60 |
+---------------+  +--------------+   +--------------+  +---------------+
                              |          |
                              |          |
                   +---------------+  +--------------+
                   |  sf1(DPI-1)   |  |   sf2(FW-1)  |
                   |192.168.60.30  |  |192.168.60.40 |
                   +---------------+  +--------------+

Setup Demo
----------
1. Install virtualbox & vagrant
2. Start ODL SFC in host machine and install necessary features

   1) For ovs or ovs_dpdk

   feature:install odl-sfc-scf-openflow odl-sfc-openflow-renderer odl-sfc-ui

   2) For vpp

   feature:install odl-sfc-vpp-renderer odl-sfc-ui

   Notice: please do stop, clean up, then restart ODL SFC when you run this demo in order that demo can run successfully.

   opendaylight-user@root>shutdown -f
   opendaylight-user@root>
   $ rm -rf data snapshots journal instances
   $ ./bin/karaf

3. Run demo

  SFC 104 demos will download Ubuntu trusty x86_64 vagrant image and install all the necessary packages into host and vagrant VMs, so please make sure to export http_proxy and http_proxy environment variables if you have proxy behind your network before run demo script, demo script will inject these proxy settings to vagrant VMS.

  1) For OVS demo

     $ ./run_demo.sh ovs

  2) For OVS DPDK demo

     $ ./run_demo.sh ovs_dpdk

  3) For VPP demo

     $ ./run_demo.sh vpp
