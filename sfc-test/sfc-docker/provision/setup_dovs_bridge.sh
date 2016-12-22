#!/usr/bin/env bash

BRIDGE="dovsbr0"
BRIDGE_CFG="/etc/sysconfig/network-scripts/ifcfg-${BRIDGE}"
ETH="eth1"
ETH_CFG="/etc/sysconfig/network-scripts/ifcfg-${ETH}"
[ ! -e "$BRIDGE_CFG" ] || exit 0
echo "DEVICE=${BRIDGE}" > "$BRIDGE_CFG"
echo "TYPE=Bridge" >> "$BRIDGE_CFG"
echo "BOOTPROTO=dhcp" >> "$BRIDGE_CFG"
echo "ONBOOT=yes" >> "$BRIDGE_CFG"
echo "PERSISTENT_DHCLIENT=yes" >> "$BRIDGE_CFG"
[ -e "$ETH_CFG" ] && mv "$ETH_CFG" "${ETH_CFG}.old"
echo "DEVICE=${ETH}" > "$ETH_CFG"
echo "ONBOOT=yes" >> "$ETH_CFG"
echo "TYPE=Ethernet" >> "$ETH_CFG"
echo "BRIDGE=${BRIDGE}" >> "$ETH_CFG"
systemctl restart network

