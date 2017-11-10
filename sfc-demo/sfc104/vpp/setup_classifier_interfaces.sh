#!/bin/bash

source /vagrant/env.sh

INTERFACE=$(vppctl show int | grep Ethernet | awk '{print $1;}')

host=$(hostname)
if [ "${host}"  == "${CLASSIFIER1_NAME}" ] ; then
    LOCAL_IP=${CLASSIFIER1_VPP_IP}
    NS_IP=${CLASSIFIER1_NS_IP}
    NS_MAC=${CLASSIFIER1_NS_MAC}
elif [ "${host}"  == "${CLASSIFIER2_NAME}" ] ; then
    LOCAL_IP=${CLASSIFIER2_VPP_IP}
    NS_IP=${CLASSIFIER2_NS_IP}
    NS_MAC=${CLASSIFIER2_NS_MAC}
else
    exit 0
fi

vppctl set int ip table ${INTERFACE} 0
vppctl set int ip address ${INTERFACE} ${LOCAL_IP}/24
vppctl set int state ${INTERFACE} up

vppctl set int l2 bridge host-veth-br 2 1
vppctl set int state host-veth-br up
vppctl ip route add ${NS_IP}/32 via host-veth-br
vppctl set ip arp fib-id 0 host-veth-br ${NS_IP} ${NS_MAC}
