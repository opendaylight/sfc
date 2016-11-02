#!/bin/bash

stop honeycomb
stop vpp

picaddrs=$(lspci | grep "Ethernet Controller" | awk '{print $1;}')
i=0
for addr in ${picaddrs}
do
    picaddr[$i]=$addr
    i=$((i+1))
done

source /vagrant/env.sh
$DPDK_DIR/tools/dpdk_nic_bind.py --bind=e1000 ${picaddr[2]}
ifconfig eth2 0 down
rm -f ${HC_CONFIG_DATA}
rm -f ${HC_CONTEXT_DATA}
