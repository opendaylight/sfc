#!/bin/bash

source /vagrant/env.sh
/vagrant/common/cleanup_veth.sh
/vagrant/ovs_dpdk/stop_ovs_dpdk.sh
/vagrant/vpp/start_vpp.sh

nohup /vagrant/vpp/setup_veth.sh & sleep 1
vppctl create host-interface name veth-br
