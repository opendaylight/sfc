__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "rapenno@gmail.com"
__status__ = "alpha"

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

"""This module provides function to decode VXLAN GPE packets.
   Given a reference to the beginning of the UDL payload, it
   decodes the appropriate header and store values in the
   passed variable

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |R|R|R|R|I|P|R|R|   Reserved                    |Next Protocol  |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                VXLAN Network Identifier (VNI) |   Reserved    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+


    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |Ver|O|C|R|R|R|R|R|R|   Length  |    MD Type    | Next Protocol |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |          Service Path ID                      | Service Index |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+


    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                  Network Platform Context                     |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                  Network Shared Context                       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                  Service Platform Context                     |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                  Service Shared Context                       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

"""

import struct
import binascii
from ctypes import *


class VXLANGPE(Structure):
    _fields_ = [("flags", c_ubyte),
                ("reserved", c_ubyte),
                ("protocol_type", c_ushort),
                ("vni", c_uint, 24),
                ("reserved2", c_uint, 8)]


class BASEHEADER(Structure):
    _fields_ = [("version", c_ushort, 2),
                ("flags", c_ushort, 8),
                ("length", c_ushort, 6),
                ("md_type", c_ubyte),
                ("next_protocol", c_ubyte),
                ("service_path", c_uint, 24),
                ("service_index", c_uint, 8)]


class CONTEXTHEADER(Structure):
    _fields_ = [("network_platform", c_uint),
                ("network_shared", c_uint),
                ("service_platform", c_uint),
                ("service_shared", c_uint)]

# Decode the VXLAN header for a received packet at this SFF.

def decode_vxlan(payload, vxlan_header_values):
    # VXLAN header
    vxlan_header = payload[0:8]
    vxlan_header_values.flags, vxlan_header_values.reserved, vxlan_header_values.protocol_type, \
    vni_rsvd2 = struct.unpack('!B B H I', vxlan_header)

    vxlan_header_values.vni = vni_rsvd2 >> 8
    vxlan_header_values.reserved2 = vni_rsvd2 & 0x000000FF

    # Yes, it is weird but the comparison is against False. Display debug if started with -O option.
    if __debug__ is False:
        print("\nVXLAN Header Decode:")
        print(binascii.hexlify(vxlan_header))
        print('Flags:', vxlan_header_values.flags)
        print('Reserved:', vxlan_header_values.reserved)
        print('Protocol Type:', hex(int(vxlan_header_values.protocol_type)))
        print('VNI:', vxlan_header_values.vni)
        print('Reserved:', vxlan_header_values.reserved2)


def decode_baseheader(payload, base_header_values):
    # Base Service header
    base_header = payload[8:16]  # starts at offset 8 of payload

    start_idx, base_header_values.md_type, base_header_values.next_protocol, \
        path_idx = struct.unpack('!H B B I', base_header)

    base_header_values.version = start_idx >> 14
    base_header_values.flags = start_idx >> 6
    base_header_values.length = start_idx >> 0
    base_header_values.service_path = path_idx >> 8
    base_header_values.service_index = path_idx & 0x000000FF

    if __debug__ is False:
        print("\nBase NSH Header Decode:")
        print(binascii.hexlify(base_header))
        print('NSH Version:', base_header_values.version)
        print('NSH base header flags:', base_header_values.flags)
        print('NSH base header length:', base_header_values.length)
        print('NSH MD-type:', base_header_values.md_type)
        print('NSH base header next protocol:', base_header_values.next_protocol)
        print('Service Path Identifier:', base_header_values.service_path)
        print('Service Index:', base_header_values.service_index)


# Decode the NSH context headers for a received packet at this SFF.

def decode_contextheader(payload, context_header_values):
    # Context header
    context_header = payload[16:32]

    context_header_values.network_platform, context_header_values.network_shared, \
        context_header_values.service_platform, context_header_values.service_shared = \
        struct.unpack('!I I I I', context_header)

    if __debug__ is False:
        print("\nNSH Context Header Decode:")
        print(binascii.hexlify(context_header))
        print('First context header:', context_header_values.network_platform)
        print('Second context header:', context_header_values.network_shared)
        print('Third context header:', context_header_values.service_platform)
        print('Fourth context header:', context_header_values.service_shared)


def find_payload_start_index():
    return 16