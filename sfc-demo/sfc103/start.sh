#!/bin/bash

set -eux

function ovs_start {
    ovsdb-server --remote=punix:/var/run/openvswitch/db.sock \
                 --remote=db:Open_vSwitch,Open_vSwitch,manager_options \
                 --private-key=db:Open_vSwitch,SSL,private_key \
                 --certificate=db:Open_vSwitch,SSL,certificate \
                 --bootstrap-ca-cert=db:Open_vSwitch,SSL,ca_cert \
                 --pidfile --detach
    ovs-vswitchd --pidfile --detach --log-file=/var/log/openvswitch/ovs-vswitchd.log
    ovs-vsctl set-manager tcp:192.168.1.5:6640
    ovs-vsctl add-br br-sfc
}

function ovs_add_app_veth-br {
    ip netns add app
    ip link add veth-app type veth peer name veth-br
    ovs-vsctl add-port br-sfc veth-br
    ip link set dev veth-br up
    ip link set veth-app netns app
}

host=`hostname`
if [ $host  == 'classifier1'  ] ; then
    ovs_start
    ovs_add_app_veth-br
    ip netns exec app ifconfig veth-app 192.168.2.1/24 up
    ip netns exec app ip link set dev veth-app  addr 00:00:11:11:11:11
    ip netns exec app arp -s 192.168.2.2 00:00:22:22:22:22 -i veth-app
    ip netns exec app ip link set dev veth-app up
    ip netns exec app ip link set dev lo up
    ip netns exec app ifconfig veth-app mtu 1400
    ovs-vsctl show
elif [ $host  == 'classifier2'  ] ; then
    ovs_start
    ovs_add_app_veth-br
    ip netns exec app ifconfig veth-app 192.168.2.2/24 up
    ip netns exec app ip link set dev veth-app  addr 00:00:22:22:22:22
    ip netns exec app arp -s 192.168.2.1 00:00:11:11:11:11 -i veth-app
    ip netns exec app ip link set dev veth-app up
    ip netns exec app ip link set dev lo up
    ip netns exec app ifconfig veth-app mtu 1400
    nohup ip netns exec app python -m SimpleHTTPServer 80 > /tmp/http_server.log 2>&1  &
    ovs-vsctl show
elif [ $host == 'sff1' ] || [ $host == 'sff2' ]; then
    ovs_start
    ovs-vsctl show
elif [ $host == 'sf1' ] || [ $host == 'sf2' ]; then
    cd /sfc/sfc-py;
    pip3 install -r requirements.txt
    nohup python3.4 sfc/sfc_agent.py --rest --odl-ip-port 192.168.1.5:8181 &
fi
/bin/bash
