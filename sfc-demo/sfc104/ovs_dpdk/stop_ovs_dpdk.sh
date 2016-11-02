#!/bin/bash

source /vagrant/env.sh
exec 2>/dev/null
status=$(/etc/init.d/openvswitch-switch status | head -n 1)
if [ "${status/is running/}" != "${status}" ] ; then
    ovs-vsctl del-manager
    ovs-vsctl del-controller br-sfc
    ovs-vsctl del-controller br-int
    ovs-vsctl del-br br-sfc
    ovs-vsctl del-br br-int
    ovs-appctl tnl/arp/flush
fi

dbpid=$(cat $DB_PIDFILE 2>/dev/null)
if [ "$dbpid" != "" ] ; then
    sudo kill -9 $dbpid
fi
vsdpid=$(cat $VSD_PIDFILE 2>/dev/null)
if [ "$vsdpid" != "" ] ; then
    sudo kill -9 $vsdpid
fi

/etc/init.d/openvswitch-switch stop
rm -f $DB_PIDFILE
rm -f $VSD_PIDFILE
rm -f $DB_SOCK
rm -f /var/run/openvswitch/ovsdb-server.*.ctl
rm -f /var/run/openvswitch/ovs-vswitchd.*.ctl

rmmod vport_vxlan
rmmod openvswitch
rm -rf $OVS_LOG
rm -rf $OVS_CONF_DB
rmmod igb_uio
umount /run/hugepages/kvm

picaddrs=$(lspci | grep "Ethernet Controller" | awk '{print $1;}')
i=0
for addr in ${picaddrs}
do
    picaddr[$i]=$addr
    i=$((i+1))
done

$DPDK_DIR/tools/dpdk_nic_bind.py --bind=e1000 ${picaddr[1]}
$DPDK_DIR/tools/dpdk_nic_bind.py --bind=e1000 ${picaddr[2]}
ifconfig eth1 up
ifconfig eth2 0 down
