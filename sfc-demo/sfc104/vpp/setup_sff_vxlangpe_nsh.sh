#!/bin/bash

source /vagrant/env.sh
nsp=$(/vagrant/common/get_rsp_info.py RSP1)
rnsp=$(/vagrant/common/get_rsp_info.py RSP1-Reverse)
host=$(hostname)
if [ "${host}"  == "${SFF1_NAME}" ] ; then
    LOCAL_IP=${SFF1_VPP_IP}
    REMOTE_IP=${CLASSIFIER1_VPP_IP}
    NSP=${nsp}
    RNSP=${rnsp}
elif [ "${host}"  == "${SFF2_NAME}" ] ; then
    LOCAL_IP=${SFF2_VPP_IP}
    REMOTE_IP=${CLASSIFIER2_VPP_IP}
    NSP=${rnsp}
    RNSP=${nsp}
fi

# For Classifier in VPP Forwarder
vppctl create vxlan-gpe tunnel local ${LOCAL_IP} remote ${REMOTE_IP} vni 0 next-nsh encap-vrf-id 0 decap-vrf-id 0
vppctl set int l2 bridge vxlan_gpe_tunnel2 1 1

# For reverse RSP
vppctl create nsh entry nsp ${RNSP} nsi 253 md-type 1 c1 1 c2 9 c3 3 c4 4 next-ethernet

vppctl create nsh map nsp ${RNSP} nsi 253 mapped-nsp ${RNSP} mapped-nsi 253 encap-vxlan-gpe-intf 4
