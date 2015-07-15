#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import struct
import logging
import binascii
from .common import *  # noqa


__author__ = 'Reinaldo Penno'
__copyright__ = 'Copyright(c) 2014, Cisco Systems, Inc.'
__version__ = '0.2'
__email__ = 'rapenno@gmail.com'
__status__ = 'alpha'


"""
This module provides function to decode VXLAN GPE packets. Given a reference to
the beginning of the UDL payload, it decodes the appropriate header and store
values in the passed variable.

   VXLAN

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |R|R|R|R|I|P|R|R|   Reserved                    |Next Protocol  |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                VXLAN Network Identifier (VNI) |   Reserved    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

   VXLAN-GPE

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |R|R|Ver|I|P|R|O|       Reserved                |Next Protocol  |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                VXLAN Network Identifier (VNI) |   Reserved    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

   NSH-BASE

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


def decode_ethheader(payload, eth_header_values):
    """Decode the NSH context headers for a received packet"""
    eth_header = payload[32:46]

    _header_values = struct.unpack('!B B B B B B B B B B B B B B', eth_header)
    eth_header_values.dmac0 = _header_values[0]
    eth_header_values.dmac1 = _header_values[1]
    eth_header_values.dmac2 = _header_values[2]
    eth_header_values.dmac3 = _header_values[3]
    eth_header_values.dmac4 = _header_values[4]
    eth_header_values.dmac5 = _header_values[5]
    eth_header_values.smac0 = _header_values[6]
    eth_header_values.smac1 = _header_values[7]
    eth_header_values.smac2 = _header_values[8]
    eth_header_values.smac3 = _header_values[9]
    eth_header_values.smac4 = _header_values[10]
    eth_header_values.smac5 = _header_values[11]
    eth_header_values.ethertype0 = _header_values[12]
    eth_header_values.ethertype1 = _header_values[13]

    if not __debug__:
        logger.info('NSH ethernet Header Decode ...')
        logger.info(binascii.hexlify(eth_header))

# 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
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
    """Decode headers for a OAM Trace Req packet"""
    trace_header = payload[NSH_OAM_PKT_START_OFFSET: NSH_OAM_PKT_START_OFFSET + NSH_OAM_TRACE_HDR_LEN]

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


def decode_trace_resp(payload, trace_resp_header_values):
    """Decode headers for a OAM Trace Response"""

    sf_type = None
    sf_name = None
    trace_header = payload[NSH_OAM_PKT_START_OFFSET: NSH_OAM_PKT_START_OFFSET + NSH_OAM_TRACE_HDR_LEN]

    _header_values = struct.unpack('!B B H I I I I', trace_header)
    trace_resp_header_values.oam_type = _header_values[0]
    trace_resp_header_values.sil = _header_values[1]
    trace_resp_header_values.port = _header_values[2]
    trace_resp_header_values.ip_1 = _header_values[3]
    trace_resp_header_values.ip_2 = _header_values[4]
    trace_resp_header_values.ip_3 = _header_values[5]
    trace_resp_header_values.ip_4 = _header_values[6]

    try:
        sf_type_len = payload[NSH_OAM_TRACE_RESP_SF_TYPE_LEN_START_OFFSET]
        sf_type_end = NSH_OAM_TRACE_RESP_SF_TYPE_START_OFFSET + (sf_type_len << 2)
        sf_type = payload[NSH_OAM_TRACE_RESP_SF_TYPE_START_OFFSET:sf_type_end].decode('utf-8')
        sf_name_len = payload[sf_type_end]
        sf_name_end = sf_type_end + 1 + (sf_name_len << 2)
        sf_name = payload[sf_type_end + 1:sf_name_end].decode('utf-8')
    except IndexError:
        logger.debug("Trace with Service Index {} has no report\n".format(payload[15]))

    if not __debug__:
        logger.info('NSH Trace Req Header Decode ...')
        logger.info(binascii.hexlify(trace_header))
        logger.info('Session Index Limit: %d',
                    trace_resp_header_values.sil)

    return sf_type, sf_name


def is_trace_message(data):
    base_header_first_word_int = int.from_bytes(data[NSH_BASE_HEADER_START_OFFSET:12], byteorder='big', signed='false')
    try:
        if (base_header_first_word_int == NSH_TYPE1_OAM_PACKET) and (
                (data[NSH_OAM_PKT_START_OFFSET] == OAM_TRACE_REQ_TYPE) or (
                    data[NSH_OAM_PKT_START_OFFSET] == OAM_TRACE_RESP_TYPE)):
            return True
        else:
            return False
    except IndexError as e:
        logger.warn("OAM Protocol but no trace message. Error: {}".format(e))
        return False


def is_oam_message(data):
    if data[NSH_BASE_HEADER_START_OFFSET] == OAM_VERSION_AND_FLAG:
        return True
    else:
        return False


def is_data_message(data):
    if int.from_bytes(data[NSH_BASE_HEADER_START_OFFSET:11], byteorder='big', signed='false') == NSH_TYPE1_DATA_PACKET:
        return True
    else:
        return False


def is_vxlan_nsh_legacy_message(data):
    if int.from_bytes(data[VXLAN_START_OFFSET:4], byteorder='big', signed='false') == VXLAN_RFC7348_HEADER:
        return True
    else:
        return False
