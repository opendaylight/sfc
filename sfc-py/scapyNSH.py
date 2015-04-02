#! /usr/bin/env python
#
# Copyright (c) 2015 Qosmos.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

from scapy.all import *

__author__ = "Christophe Fontaine"
__copyright__ = "Copyright(c) 2015, Qosmos"
__version__ = "0.1"
__email__ = "christophe.fontaine@qosmos.com"
__status__ = "alpha"


class VxLAN(Packet):
   name = "VxLAN"
   fields_desc = [ BitField('Reserved', 0 ,2),
                   BitField('Version', 0, 2),
                   BitField('Instance', 1, 1),
                   BitField('NextProtocol', 0, 1),
                   BitField('Reserved', 0, 1),
                   BitField('OAM', 0, 1),
                   ConditionalField(BitField('Reserved', 0, 16), lambda pkt: pkt.NextProtocol == 1),
                   ConditionalField(ByteField('NextProtocolID', 0), lambda pkt: pkt.NextProtocol == 1),
                   ConditionalField(X3BytesField('Reserved', 0), lambda pkt: pkt.NextProtocol == 0),
                   X3BytesField('VNI', 0),
                   ByteField('Reserved', 0)
                 ]

# IANA VxLAN+NSH dport is defined to 6633, but also attach to 4789
bind_layers(UDP, VxLAN, dport=6633)
bind_layers(UDP, VxLAN, dport=4789)

class NSH(Packet):
   name = "NSH"
   fields_desc = [ BitField('Ver', 0, 2),
                   BitField('OAM', 0, 1), 
                   BitField('Critical Metadata', 0, 1),
                   BitField('Reserved', 0, 6),
                   BitField('Length', 0, 6),
                   ByteEnumField('MDType', 1,
                              {1:'Fixed Length', 2:'Variable Length'} ),
                   ByteEnumField('NextProto', 3, 
                             { 1:'IPv4', 2:'IPv6', 3:'Ethernet'} ),
                   BitField('ServicePath', 0, 24),
                   ByteField('ServiceIndex', 255),
                   ConditionalField(XIntField('NPC', 0), lambda pkt:pkt.MDType == 1),
                   ConditionalField(XIntField('NSC', 0), lambda pkt:pkt.MDType == 1),
                   ConditionalField(XIntField('SPC', 0), lambda pkt:pkt.MDType == 1),
                   ConditionalField(XIntField('SSC', 0), lambda pkt:pkt.MDType == 1),
                   ConditionalField(FieldLenField("ContextHeaders", None, count_of="Length") , lambda pkt:pkt.MDType == 2)
                 ]

bind_layers(NSH, IP, NextProto=1)
bind_layers(NSH, IPv6, NextProto=2)
bind_layers(NSH, Ether, NextProto=3)

# Until VxLAN NextProtocol field is defined, attach always NSH layer to VxLAN
bind_layers(VxLAN, NSH)

if __name__ == "__main__":
   interact(mybanner="Scapy with VxLAN GPE and NSH Support")


