#!/bin/bash

source /vagrant/env.sh
ip netns add app
ip link add veth-app type veth peer name veth-br
ovs-vsctl add-port br-sfc veth-br
ip link set dev veth-br up
ip link set veth-app netns app
host=`hostname`
if [ "${host}"  == "${CLASSIFIER1_NAME}" ] ; then
    ip netns exec app ifconfig veth-app 192.168.2.1/24 up
    ip netns exec app ip link set dev veth-app  addr 00:00:11:11:11:11
    ip netns exec app arp -s 192.168.2.2 00:00:22:22:22:22 -i veth-app
    ip netns exec app ip link set dev veth-app up
    ip netns exec app ip link set dev lo up
    ip netns exec app ifconfig veth-app mtu 1400
    ip netns exec app ethtool -K veth-app tx off
fi

if [ "${host}"  == "${CLASSIFIER2_NAME}" ] ; then
    ip netns exec app ifconfig veth-app 192.168.2.2/24 up
    ip netns exec app ip link set dev veth-app  addr 00:00:22:22:22:22
    ip netns exec app arp -s 192.168.2.1 00:00:11:11:11:11 -i veth-app
    ip netns exec app ip link set dev veth-app up
    ip netns exec app ip link set dev lo up
    ip netns exec app ifconfig veth-app mtu 1400
    ip netns exec app ethtool -K veth-app tx off
    ip netns exec app python3 -m http.server 80
fi
ovs-vsctl show
