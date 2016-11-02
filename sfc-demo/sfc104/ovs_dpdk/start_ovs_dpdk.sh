#!/bin/bash

source /vagrant/env.sh

REMOTE_IP2=""
REMOTE_MAC2=""
REMOTE_IP3=""
REMOTE_MAC3=""
host=$(hostname)
if [ "${host}"  == "${CLASSIFIER1_NAME}" ] ; then
    LOCAL_IP=${CLASSIFIER1_IP}
    REMOTE_IP1=${SFF1_IP}
    REMOTE_MAC1=${SFF1_MAC}
elif [ "${host}"  == "${CLASSIFIER2_NAME}" ] ; then
    LOCAL_IP=${CLASSIFIER2_IP}
    REMOTE_IP1=${SFF2_IP}
    REMOTE_MAC1=${SFF2_MAC}
elif [ "${host}"  == "${SFF1_NAME}" ] ; then
    LOCAL_IP=${SFF1_IP}
    REMOTE_IP1=${CLASSIFIER1_IP}
    REMOTE_MAC1=${CLASSIFIER1_MAC}
    REMOTE_IP2=${SFF2_IP}
    REMOTE_MAC2=${SFF2_MAC}
    REMOTE_IP3=${SF1_IP}
    REMOTE_MAC3=${SF1_MAC}
elif [ "${host}"  == "${SFF2_NAME}" ] ; then
    LOCAL_IP=${SFF2_IP}
    REMOTE_IP1=${CLASSIFIER2_IP}
    REMOTE_MAC1=${CLASSIFIER2_MAC}
    REMOTE_IP2=${SFF1_IP}
    REMOTE_MAC2=${SFF1_MAC}
    REMOTE_IP3=${SF2_IP}
    REMOTE_MAC3=${SF2_MAC}
else
    exit 0
fi

INTFACENO=1
if [ "${1}" == "vpp" ] ; then
    REMOTE_IP2=""
    REMOTE_MAC2=""
    REMOTE_IP3=""
    REMOTE_MAC3=""

    INTFACENO=2
    if [ "${host}"  == "${CLASSIFIER1_NAME}" ] ; then
        LOCAL_IP=${CLASSIFIER1_VPP_IP}
        REMOTE_IP1=${SFF1_VPP_IP}
        REMOTE_MAC1=${SFF1_VPP_MAC}
    elif [ "${host}"  == "${CLASSIFIER2_NAME}" ] ; then
        LOCAL_IP=${CLASSIFIER2_VPP_IP}
        REMOTE_IP1=${SFF2_VPP_IP}
        REMOTE_MAC1=${SFF2_VPP_MAC}
    else
        exit 0
    fi
fi

mount -t hugetlbfs nodev /run/hugepages/kvm

modprobe uio
insmod $DPDK_BUILD/kmod/igb_uio.ko
sleep 1

picaddrs=$(lspci | grep "Ethernet Controller" | awk '{print $1;}')
i=0
for addr in ${picaddrs}
do
    picaddr[$i]=$addr
    i=$((i+1))
done

INTFACE="eth${INTFACENO}"
ifconfig $INTFACE 0 down
$DPDK_DIR/tools/dpdk_nic_bind.py --bind=igb_uio ${picaddr[$INTFACENO]}
sleep 1

mkdir -p $(dirname $OVS_CONF_DB)
mkdir -p $(dirname $DB_SOCK)

echo "start ovs"
rm -rf $OVS_CONF_DB
ovsdb-tool create $OVS_CONF_DB $OVS_SCHEMA
/etc/init.d/openvswitch-switch start
pkill ovs-vswitchd
ovs-vsctl --no-wait --db=unix:$DB_SOCK init
/usr/lib/openvswitch-switch-dpdk/ovs-vswitchd --dpdk -c 0x1 -n 4 --socket-mem 1024,0 -- unix:$DB_SOCK --pidfile=$VSD_PIDFILE --detach --log-file=$OVS_LOG

ovs-vsctl add-br br-sfc -- set bridge br-sfc datapath_type=netdev protocols=OpenFlow10,OpenFlow12,OpenFlow13
ovs-vsctl add-port br-sfc dpdk0 -- set Interface dpdk0 type=dpdk
ifconfig br-sfc up
ifconfig br-sfc ${LOCAL_IP}/24

ovs-appctl tnl/arp/set br-sfc ${REMOTE_IP1} ${REMOTE_MAC1}
if [ "${REMOTE_IP2}" != "" ] ; then
    ovs-appctl tnl/arp/set br-sfc ${REMOTE_IP2} ${REMOTE_MAC2}
    ovs-appctl tnl/arp/set br-sfc ${REMOTE_IP3} ${REMOTE_MAC3}
fi
ovs-appctl ovs/route/add ${REMOTE_IP1}/24 br-sfc
if [ "${1}" != "vpp" ] ; then
    ovs-vsctl set-manager "tcp:${ODL_CONTROLLER}:6640"
    ovs-vsctl set-controller br-sfc "tcp:${ODL_CONTROLLER}:6653"
fi
