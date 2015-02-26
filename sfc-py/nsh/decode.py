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
import ipaddress

#: constants
PAYLOAD_START_INDEX = 16
OAM_VERSION_AND_FLAG = int('00100000', 2)
OAM_TRACE_TYPE = int('00000001', 2)

logger = logging.getLogger(__name__)


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


#  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
# +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
# |Ver|1|C|R|R|R|R|R|R|   Length  |  MD-type=1    | Next Protocol |
# +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
# |          Service Path ID                      | Service Index |
# +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
# |    OAM Type   |     SIL       |          Dest Port            |
# +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
# |                       Dest IP Address                         |
# +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
# |                       Dest IP Address                         |
# +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
# |                       Dest IP Address                         |
# +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
# |                       Dest IP Address                         |
# +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

def decode_trace_req(payload, trace_req_header_values):
    """Decode headers for a packet Type MD 0x3"""
    trace_header = payload[16:36]

    _header_values = struct.unpack('!B B H I I I I', trace_header)
    trace_req_header_values.oam_type = _header_values[0]
    trace_req_header_values.sil = _header_values[1]
    trace_req_header_values.port = _header_values[2]
    trace_req_header_values.ip_1 = _header_values[3]
    trace_req_header_values.ip_2 = _header_values[4]
    trace_req_header_values.ip_3 = _header_values[5]
    trace_req_header_values.ip_4 = _header_values[6]

    if not __debug__:
        logger.info('NSH Trace Req Header Decode ...')
        logger.info(binascii.hexlify(trace_header))
        logger.info('Session Index Limit: %d',
                    trace_req_header_values.sil)


def decode_trace_resp(payload, trace_req_header_values):
    """Decode headers for a packet Type MD 0x3"""
    trace_header = payload[16:36]

    _header_values = struct.unpack('!B B H I I I I', trace_header)
    trace_req_header_values.oam_type = _header_values[0]
    trace_req_header_values.sil = _header_values[1]
    trace_req_header_values.port = _header_values[2]
    trace_req_header_values.ip_1 = _header_values[3]
    trace_req_header_values.ip_2 = _header_values[4]
    trace_req_header_values.ip_3 = _header_values[5]
    trace_req_header_values.ip_4 = _header_values[6]

    sf_type_len = payload[36]
    sf_type_end = 37 + (sf_type_len << 2)
    sf_type = payload[37:sf_type_end].decode('utf-8')
    sf_name_len = payload[sf_type_end]
    sf_name_end = sf_type_end + 1 + (sf_name_len << 2)
    sf_name = payload[sf_type_end + 1:sf_name_end].decode('utf-8')

    if not __debug__:
        logger.info('NSH Trace Req Header Decode ...')
        logger.info(binascii.hexlify(trace_header))
        logger.info('Session Index Limit: %d',
                    trace_req_header_values.sil)

    return sf_type, sf_name


def is_trace_message(data):
    if (data[8] & OAM_VERSION_AND_FLAG) and (data[16] == OAM_TRACE_TYPE):
        return True
    else:
        return False


def is_oam_message(data):
    if data[8] & OAM_VERSION_AND_FLAG:
        return True
    else:
        return False


def is_data_message(data):
    if not is_oam_message(data):
        return True
    else:
        return False