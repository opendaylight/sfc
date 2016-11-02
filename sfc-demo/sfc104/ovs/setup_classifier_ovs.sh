#!/bin/bash

source /vagrant/env.sh
/vagrant/common/cleanup_veth.sh
/vagrant/ovs_dpdk/stop_ovs_dpdk.sh
/vagrant/ovs/start_ovs.sh
nohup /vagrant/common/setup_veth.sh & sleep 1
