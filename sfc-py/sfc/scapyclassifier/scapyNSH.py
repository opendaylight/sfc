#! /usr/bin/env python
#
# Copyright (c) 2015 Qosmos.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

from scapy.all import interact, sendp, conf, sniff, log_scapy

from .nsh import NSH
from .misc import get_ip_address


__author__ = "Christophe Fontaine"
__copyright__ = "Copyright(c) 2015, Qosmos"
__version__ = "0.3"
__email__ = "christophe.fontaine@qosmos.com"
__status__ = "alpha"


def NSHForward(pkt):
    log_scapy.info("{" + pkt[NSH].mysummary()+"} "+pkt[NSH].summary())
    next = pkt[NSH]
    next.ServiceIndex = next.ServiceIndex - 1
    log_scapy.info("OUT {" + next[NSH].mysummary()+"} ")
    sendp(next, iface=egress)


def StartSF():
    conf.verb = 0
    return sniff(iface=ingress, filter='ip dst ' + get_ip_address(ingress) +
                 ' and (udp port 6633)',
                 prn=lambda x: NSHForward(x))


ingress = "eth1"
egress = "vxlan0"

if __name__ == "__main__":
    interact(mydict=globals(), mybanner="""
    Scapy with VxLAN GPE and NSH Support
    - Use sniff(iface=ingress) to display incoming packets
    - Use StartSF() to forward packets
    """)
#
# Tested topology is the following:
# Each Client / Server / SF1 / SF2 are VMs
#
# The test host machine is Debian Testing with Kernel 3.16
# OVS + NSH is taken from Pritesh'fork of OVS:
# https://github.com/priteshk/ovs.git (branch nsh-v8)
#
# OVS Configuration for each VxLAN port :
# ovs-vsctl add-port br0 vxlan-$1 -- set interface vxlan-$1 type=vxlan
#      options:local_ip=192.168.66.1 options:remote_ip=192.168.$vxlanip
#      options:key=flow options:dst_port=6633 options:nsp=flow options:nsi=flow
#
# Each SF uses a VxLAN connected to OVS :
# #ip link add vxlan0 type vxlan id 0x1337 dstport 6633
#                                remote 192.168.66.1 dev eth1
# #ip link set vxlan0 up
#
#           ______________
# Client ---|  OVS + NSH |---- Server
#           --------------
#               |     |
#              SF1   SF2
#
# Open flow rules are the following :
# - Packet classification, NSH encapsulation & send this packet to SF1
# tcp,in_port={PORT_OF_CLIENT},tp_dst=8080
#         actions=set_tunnel:0x1337,set_nsp:42,set_nsi:255,output:{PORT_OF_SF1}
# - NSP/NSI matcher, and forward to the next SF
# nsp=42,nsi=254 actions=output:{PORT_OF_SF2}
# - End of path, send the packet to the egress port.
#   Note that this port is not a VxLAN port.
# priority=1,nsp=42 actions=output:{PORT_OF_SERVER}
#
