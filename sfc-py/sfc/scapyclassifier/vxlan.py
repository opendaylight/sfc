#
# Copyright (c) 2015 Qosmos.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


__author__ = "Christophe Fontaine"
__copyright__ = "Copyright(c) 2015, Qosmos"
__version__ = "1.0"
__email__ = "christophe.fontaine@qosmos.com"
__status__ = ""


from scapy.all import bind_layers
from scapy.fields import BitField, ByteField, X3BytesField
from scapy.fields import ConditionalField
from scapy.packet import Packet
from scapy.layers.inet import UDP


class VxLAN(Packet):
    name = "VxLAN"
    fields_desc = [BitField('Reserved', 0, 2),
                   BitField('Version', 0, 2),
                   BitField('Instance', 1, 1),
                   BitField('NextProtocol', 0, 1),
                   BitField('Reserved', 0, 1),
                   BitField('OAM', 0, 1),
                   ConditionalField(BitField('Reserved', 0, 16),
                                    lambda pkt: pkt.NextProtocol == 1),
                   ConditionalField(ByteField('NextProtocolID', 0),
                                    lambda pkt: pkt.NextProtocol == 1),
                   ConditionalField(X3BytesField('Reserved', 0),
                                    lambda pkt: pkt.NextProtocol == 0),
                   X3BytesField('VNI', 0),
                   ByteField('Reserved', 0)]

    def mysummary(self):
        s = self.sprintf("VNI:%VNI%")
        return s

# IANA VxLAN+NSH dport is defined to 6633, but also attach to 4789
bind_layers(UDP, VxLAN, dport=6633)
bind_layers(UDP, VxLAN, dport=4789)
