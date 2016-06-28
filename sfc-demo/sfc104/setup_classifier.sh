#!/bin/bash

apt-get update -y
apt-get install autoconf -y
apt-get install libtool -y
apt-get install git -y
apt-get install python3-flask requests netifaces -y
apt-get install libssl-dev openssl -y
apt-get install libnetfilter-queue-dev -y
apt-get install python3-pip -y
pip3 install sfc
apt-get install git curl -y
apt-get install mini-httpd -y

rmmod openvswitch
find /lib/modules | grep openvswitch.ko | xargs rm -rf
curl https://raw.githubusercontent.com/priteshk/ovs/nsh-v8/third-party/start-ovs-deb.sh | bash

ovs-vsctl set-manager tcp:192.168.1.5:6640

ovs-vsctl add-br br-sfc
ip netns add app
ip link add veth-app type veth peer name veth-br
ovs-vsctl add-port br-sfc veth-br
ip link set dev veth-br up
ip link set veth-app netns app
host=`hostname`
if [ $host  == 'classifier1'  ] ; then
    ip netns exec app ifconfig veth-app 192.168.2.1/24 up
    ip netns exec app ip link set dev veth-app  addr 00:00:11:11:11:11
    ip netns exec app arp -s 192.168.2.2 00:00:22:22:22:22 -i veth-app
    ip netns exec app ip link set dev veth-app up
    ip netns exec app ip link set dev lo up
    ip netns exec app ifconfig veth-app mtu 1400
else
    ip netns exec app ifconfig veth-app 192.168.2.2/24 up
    ip netns exec app ip link set dev veth-app  addr 00:00:22:22:22:22
    ip netns exec app arp -s 192.168.2.1 00:00:11:11:11:11 -i veth-app
    ip netns exec app ip link set dev veth-app up
    ip netns exec app ip link set dev lo up
    ip netns exec app ifconfig veth-app mtu 1400
    ip netns exec app python -m SimpleHTTPServer 80
fi
ovs-vsctl show
