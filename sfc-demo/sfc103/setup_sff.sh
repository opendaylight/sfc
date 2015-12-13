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

rmmod openvswitch
find /lib/modules | grep openvswitch.ko | xargs rm -rf

curl https://raw.githubusercontent.com/priteshk/ovs/nsh-v8/third-party/start-ovs-deb.sh | bash

ovs-vsctl set-manager tcp:192.168.1.5:6640
ovs-vsctl show
