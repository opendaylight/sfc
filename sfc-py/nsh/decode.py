__author__ = 'Reinaldo Penno'
__copyright__ = 'Copyright(c) 2014, Cisco Systems, Inc.'
__version__ = '0.2'
__email__ = 'rapenno@gmail.com'
__status__ = 'alpha'

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

"""
This module provides function to decode VXLAN GPE packets. Given a reference to
the beginning of the UDL payload, it decodes the appropriate header and store
values in the passed variable.

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
import logging
import binascii

from ctypes import Structure, c_ubyte, c_ushort, c_uint


#: constants
PAYLOAD_START_INDEX = 16


logger = logging.getLogger(__name__)


class VXLANGPE(Structure):
    _fields_ = [('flags', c_ubyte),
                ('reserved', c_ubyte),
                ('protocol_type', c_ushort),
                ('vni', c_uint, 24),
                ('reserved2', c_uint, 8)]


class BASEHEADER(Structure):
    _fields_ = [('version', c_ushort, 2),
                ('flags', c_ushort, 8),
                ('length', c_ushort, 6),
                ('md_type', c_ubyte),
                ('next_protocol', c_ubyte),
                ('service_path', c_uint, 24),
                ('service_index', c_uint, 8)]


class CONTEXTHEADER(Structure):
    _fields_ = [('network_platform', c_uint),
                ('network_shared', c_uint),
                ('service_platform', c_uint),
                ('service_shared', c_uint)]


def decode_vxlan(payload, vxlan_header_values):
    """Decode the VXLAN header for a received packets"""
    vxlan_header = payload[0:8]

    _header_values = struct.unpack('!B B H I', vxlan_header)
    vxlan_header_values.flags = _header_values[0]
    vxlan_header_values.reserved = _header_values[1]
    vxlan_header_values.protocol_type = _header_values[2]

    vni_rsvd2 = _header_values[3]
    vxlan_header_values.vni = vni_rsvd2 >> 8
    vxlan_header_values.reserved2 = vni_rsvd2 & 0x000000FF

    # Yes, it is weird but the comparison is against False.
    # Display debug if started with -O option.
    if not __debug__:
        logger.info('VXLAN Header Decode ...')
        logger.info(binascii.hexlify(vxlan_header))
        logger.info('Flags: %s', vxlan_header_values.flags)
        logger.info('Reserved: %s', vxlan_header_values.reserved)
        logger.info('Protocol Type: %s',
                    hex(int(vxlan_header_values.protocol_type)))
        logger.info('VNI: %s', vxlan_header_values.vni)
        logger.info('Reserved: %s', vxlan_header_values.reserved2)


def decode_baseheader(payload, base_header_values):
    """Decode the NSH base headers for a received packets"""
    base_header = payload[8:16]

    _header_values = struct.unpack('!H B B I', base_header)
    start_idx = _header_values[0]
    base_header_values.md_type = _header_values[1]
    base_header_values.next_protocol = _header_values[2]
    path_idx = _header_values[3]

    base_header_values.version = start_idx >> 14
    base_header_values.flags = start_idx >> 6
    base_header_values.length = start_idx >> 0
    base_header_values.service_path = path_idx >> 8
    base_header_values.service_index = path_idx & 0x000000FF

    if not __debug__:
        logger.info('Base NSH Header Decode ...')
        logger.info(binascii.hexlify(base_header))
        logger.info('NSH Version: %s', base_header_values.version)
        logger.info('NSH base header flags: %s', base_header_values.flags)
        logger.info('NSH base header length: %s', base_header_values.length)
        logger.info('NSH MD-type: %s', base_header_values.md_type)
        logger.info('NSH base header next protocol: %s',
                    base_header_values.next_protocol)
        logger.info('Service Path Identifier: %s',
                    base_header_values.service_path)
        logger.info('Service Index: %s', base_header_values.service_index)


def decode_contextheader(payload, context_header_values):
    """Decode the NSH context headers for a received packet"""
    context_header = payload[16:32]

    _header_values = struct.unpack('!I I I I', context_header)
    context_header_values.network_platform = _header_values[0]
    context_header_values.network_shared = _header_values[1]
    context_header_values.service_platform = _header_values[2]
    context_header_values.service_shared = _header_values[3]

    if not __debug__:
        logger.info('NSH Context Header Decode ...')
        logger.info(binascii.hexlify(context_header))
        logger.info('First context header: %s',
                    context_header_values.network_platform)
        logger.info('Second context header: %s',
                    context_header_values.network_shared)
        logger.info('Third context header: %s',
                    context_header_values.service_platform)
        logger.info('Fourth context header: %s',
                    context_header_values.service_shared)
