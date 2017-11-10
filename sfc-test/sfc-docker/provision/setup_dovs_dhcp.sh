#!/usr/bin/env bash

# Add interface to bridge with dhcp
nmcli connection del $(nmcli c show | grep eth1 | awk '{print $(NF-2)}')
nmcli connection add type bridge-slave ifname eth1 master dovs-mgmt
nmcli connection mod dovs-mgmt ipv4.method auto
nmcli connection up dovs-mgmt
