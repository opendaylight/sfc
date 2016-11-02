#!/bin/bash

pkill python3
ip netns exec app ip link set dev lo down
ip netns exec app ip link set dev veth-app down
ip netns exec app ifconfig veth-app down
ip link set dev veth-br down
ovs-vsctl del-port br-sfc veth-br
ip link del veth-app
ip netns del app
