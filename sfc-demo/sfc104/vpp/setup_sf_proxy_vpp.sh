#!/bin/bash

source /vagrant/env.sh
/vagrant/ovs_dpdk/stop_ovs_dpdk.sh
/vagrant/vpp/stop_vpp.sh
/vagrant/vpp/start_vpp.sh

nsp=$(/vagrant/common/get_rsp_info.py RSP1)
rnsp=$(/vagrant/common/get_rsp_info.py RSP1-Reverse)
host=$(hostname)
INTERFACE=$(vppctl show int | grep Ethernet | awk '{print $1;}')

vppctl set int state ${INTERFACE} up
vppctl set int ip table ${INTERFACE} 0
vppctl set int ip address ${INTERFACE} ${SF2_VPP_PROXY_IP}/24

vppctl create vxlan-gpe tunnel local ${SF2_VPP_PROXY_IP} remote ${SFF2_VPP_IP} vni 0 next-nsh encap-vrf-id 0 decap-vrf-id 0
vppctl set int l2 bridge vxlan_gpe_tunnel0 1 1

vppctl create vxlan tunnel src ${SF2_VPP_PROXY_IP} dst ${SF2_VPP_IP} vni 1 encap-vrf-id 0 decap-next node nsh-proxy
vppctl set int l2 bridge vxlan_tunnel0 1 1

### RSP
vppctl create nsh entry nsp ${nsp} nsi 254 md-type 1 c1 3232253480 c2 0 c3 3232253510 c4 4 next-ethernet
vppctl create nsh entry nsp ${nsp} nsi 253 md-type 1 c1 3232253510 c2 0 c3 3232253480 c4 4 next-ethernet

vppctl create nsh map nsp ${nsp} nsi 254 mapped-nsp ${nsp} mapped-nsi 254 nsh_action pop encap-vxlan4-intf 3
vppctl create nsh map nsp ${nsp} nsi 253 mapped-nsp ${nsp} mapped-nsi 253 nsh_action push encap-vxlan-gpe-intf 2

vppctl create vxlan tunnel src ${SF2_VPP_PROXY_IP} dst ${SF2_VPP_IP} vni 2 encap-vrf-id 0 decap-next node nsh-proxy
vppctl set int l2 bridge vxlan_tunnel1 1 1

### reverse RSP
vppctl create nsh entry nsp ${rnsp} nsi 255 md-type 1 c1 3232253480 c2 0 c3 3232253510 c4 4 next-ethernet
vppctl create nsh entry nsp ${rnsp} nsi 254 md-type 1 c1 3232253510 c2 0 c3 3232253480 c4 4 next-ethernet

vppctl create nsh map nsp ${rnsp} nsi 255 mapped-nsp ${rnsp} mapped-nsi 255 nsh_action pop encap-vxlan4-intf 4
vppctl create nsh map nsp ${rnsp} nsi 254 mapped-nsp ${rnsp} mapped-nsi 254 nsh_action push encap-vxlan-gpe-intf 2
