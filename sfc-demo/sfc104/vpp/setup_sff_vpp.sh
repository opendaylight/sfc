#!/bin/bash

source /vagrant/env.sh
/vagrant/vpp/stop_vpp.sh
/vagrant/ovs_dpdk/stop_ovs_dpdk.sh
/vagrant/vpp/start_vpp.sh

INTERFACE=$(vppctl show int | grep Ethernet | awk '{print $1;}')

host=$(hostname)
if [ "${host}"  == "${SFF1_NAME}" ] ; then
    LOCAL_IP=${SFF1_VPP_IP}
elif [ "${host}"  == "${SFF2_NAME}" ] ; then
    LOCAL_IP=${SFF2_VPP_IP}
else
    exit 0
fi

vppctl set int ip table ${INTERFACE} 0
vppctl set int ip address ${INTERFACE} ${LOCAL_IP}/24
vppctl set int state ${INTERFACE} up
