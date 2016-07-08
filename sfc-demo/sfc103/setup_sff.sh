#!/bin/bash
set -eux

apt-get update
apt-get install git -y
curl https://raw.githubusercontent.com/yyang13/ovs_nsh_patches/master/start-ovs-deb.sh | bash
ovs-vsctl set-manager tcp:192.168.1.5:6640
ovs-vsctl add-br br-sfc
ovs-vsctl show
