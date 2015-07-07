#! /usr/bin/env python
#
# Copyright (c) 2015 Qosmos.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import socket
import fcntl
import struct
from scapy.all import interact, bind_layers, sendp, conf, sniff
from scapy.fields import BitField, ByteField, ByteEnumField, X3BytesField, XIntField
from scapy.fields import ConditionalField, FieldLenField
from scapy.packet import Packet
from scapy.layers.inet import Ether, IP, UDP
from scapy.layers.inet6 import IPv6


__author__ = "Christophe Fontaine"
__copyright__ = "Copyright(c) 2015, Qosmos"
__version__ = "0.2"
__email__ = "christophe.fontaine@qosmos.com"
__status__ = "alpha"


class VxLAN(Packet):
    name = "VxLAN"
    fields_desc = [BitField('Reserved', 0, 2),
                   BitField('Version', 0, 2),
                   BitField('Instance', 1, 1),
                   BitField('NextProtocol', 0, 1),
                   BitField('Reserved', 0, 1),
                   BitField('OAM', 0, 1),
                   ConditionalField(BitField('Reserved', 0, 16), lambda pkt: pkt.NextProtocol == 1),
                   ConditionalField(ByteField('NextProtocolID', 0), lambda pkt: pkt.NextProtocol == 1),
                   ConditionalField(X3BytesField('Reserved', 0), lambda pkt: pkt.NextProtocol == 0),
                   X3BytesField('VNI', 0),
                   ByteField('Reserved', 0)]

    def mysummary(self):
        s = self.sprintf("VNI:%VNI%")
        return s


# IANA VxLAN+NSH dport is defined to 6633, but also attach to 4789
bind_layers(UDP, VxLAN, dport=6633)
bind_layers(UDP, VxLAN, dport=4789)


class NSH(Packet):
    name = "NSH"
    fields_desc = [BitField('Ver', 0, 2),
                   BitField('OAM', 0, 1),
                   BitField('Critical Metadata', 0, 1),
                   BitField('Reserved', 0, 6),
                   BitField('Length', 0, 6),
                   ByteEnumField('MDType', 1, {1: 'Fixed Length', 2: 'Variable Length'}),
                   ByteEnumField('NextProto', 3, {1: 'IPv4', 2: 'IPv6', 3: 'Ethernet'}),
                   BitField('ServicePath', 0, 24),
                   ByteField('ServiceIndex', 255),
                   ConditionalField(XIntField('NPC', 0), lambda pkt: pkt.MDType == 1),
                   ConditionalField(XIntField('NSC', 0), lambda pkt: pkt.MDType == 1),
                   ConditionalField(XIntField('SPC', 0), lambda pkt: pkt.MDType == 1),
                   ConditionalField(XIntField('SSC', 0), lambda pkt: pkt.MDType == 1),
                   ConditionalField(FieldLenField("ContextHeaders", None, count_of="Length"),
                                    lambda pkt: pkt.MDType == 2)]

    def mysummary(self):
        s = self.sprintf("NSP: %ServicePath% - NSI: %ServiceIndex%")
        return s

bind_layers(NSH, IP, NextProto=1)
bind_layers(NSH, IPv6, NextProto=2)
bind_layers(NSH, Ether, NextProto=3)

# Until VxLAN NextProtocol field is defined, attach always NSH layer to VxLAN
bind_layers(VxLAN, NSH)


def get_ip_address(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', ifname[:15])
    )[20:24])


def NSHForward(pkt):
    print ("{" + pkt[NSH].mysummary()+"} "+pkt[NSH].summary())
    next = pkt[NSH]
    next.ServiceIndex = next.ServiceIndex - 1
    sendp(next, iface=egress)


def StartSF():
    conf.verb = 0
    return sniff(iface=ingress, filter='ip dst ' + get_ip_address(ingress) + ' and (udp port 6633)',
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
# OVS + NSH is taken from Pritesh'fork of OVS:  https://github.com/priteshk/ovs.git (branch nsh-v8)
#
# OVS Configuration for each VxLAN port :
# ovs-vsctl add-port br0 vxlan-$1 -- set interface vxlan-$1 type=vxlan
#          options:local_ip=192.168.66.1 options:remote_ip=192.168.$vxlanip
#          options:key=flow options:dst_port=6633 options:nsp=flow options:nsi=flow
#
# Each SF uses a VxLAN connected to OVS :
# #ip link add vxlan0 type vxlan id 0x1337 dstport 6633 remote 192.168.66.1 dev eth1
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
# tcp,in_port={PORT_OF_CLIENT},tp_dst=8080 actions=set_tunnel:0x1337,set_nsp:42,set_nsi:255,output:{PORT_OF_SF1}
# - NSP/NSI matcher, and forward to the next SF
# nsp=42,nsi=254 actions=output:{PORT_OF_SF2}
# - End of path, send the packet to the egress port. Note that this port is not a VxLAN port.
# priority=1,nsp=42 actions=output:{PORT_OF_SERVER}
#
