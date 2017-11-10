#!/bin/bash

source /vagrant/env.sh
/vagrant/common/cleanup_veth.sh
/vagrant/ovs_dpdk/stop_ovs_dpdk.sh
/vagrant/ovs_dpdk/start_ovs_dpdk.sh
nohup /vagrant/common/setup_veth.sh & sleep 1
