#!/bin/bash

/vagrant/vpp/stop_vpp.sh
/vagrant/ovs_dpdk/stop_ovs_dpdk.sh
/vagrant/ovs_dpdk/start_ovs_dpdk.sh
