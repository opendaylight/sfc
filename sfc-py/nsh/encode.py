#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import struct
import socket
import ipaddress

from nsh.common import *  # noqa


__author__ = "Reinaldo Penno, Jim Guichard"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.3"
__email__ = "rapenno@gmail.com, jguichar@cisco.com"
__status__ = "alpha"

"""
Provides a Function to fully encode transport (VXLAN-GPE, GRE, other) + NSH Base + Context Headers

   VXLAN-GPE header format:
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |R|R|R|R|I|P|R|R|   Reserved                    |Next Protocol  |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                VXLAN Network Identifier (VNI) |   Reserved    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

   GRE header format:
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |C|       Reserved0       | Ver |         Protocol Type         |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |      Checksum (optional)      |       Reserved1 (Optional)    |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

   NSH Base header format:
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |Ver|O|C|R|R|R|R|R|R|   Length  |    MD Type    | Next Protocol |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |          Service Path ID                      | Service Index |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

   NSH Type-1 context header format:
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


def build_packet(encapsulate_type, encapsulate_header_values, base_header_values, ctx_header_values):
    """
    TODO: add docstring, params description
    """

    if encapsulate_type == 'VXLAN/NSH':
        # Build VXLAN header
        vxlan_header = struct.pack('!B B H I',
                                   encapsulate_header_values.flags,
                                   encapsulate_header_values.reserved,
                                   encapsulate_header_values.protocol_type,
                                   (encapsulate_header_values.vni << 8) +
                                   encapsulate_header_values.reserved2)

        # Build base NSH header
        base_header = struct.pack('!H B B I',
                                  (base_header_values.version << 14) +
                                  (base_header_values.flags << 6) +
                                  base_header_values.length,
                                  base_header_values.md_type,
                                  base_header_values.next_protocol,
                                  (base_header_values.service_path << 8) +
                                  base_header_values.service_index)

        # Build NSH context headers
        context_header = struct.pack('!I I I I',
                                     ctx_header_values.network_platform,
                                     ctx_header_values.network_shared,
                                     ctx_header_values.service_platform,
                                     ctx_header_values.service_shared)

        return vxlan_header + base_header + context_header

    elif encapsulate_type == 'GRE/NSH':
        # Build GRE header
        gre_header = struct.pack('!H H H H',
                                 (encapsulate_header_values.c << 15) + (encapsulate_header_values.reserved0 << 3) +
                                 encapsulate_header_values.version,
                                 encapsulate_header_values.protocol_type,
                                 encapsulate_header_values.checksum,
                                 encapsulate_header_values.reserved1)

        # Build base NSH header
        base_header = struct.pack('!H B B I',
                                  (base_header_values.version << 14) +
                                  (base_header_values.flags << 6) +
                                  base_header_values.length,
                                  base_header_values.md_type,
                                  base_header_values.next_protocol,
                                  (base_header_values.service_path << 8) +
                                  base_header_values.service_index)

        # Build NSH context headers
        context_header = struct.pack('!I I I I',
                                     ctx_header_values.network_platform,
                                     ctx_header_values.network_shared,
                                     ctx_header_values.service_platform,
                                     ctx_header_values.service_shared)

        return gre_header + base_header + context_header

    elif encapsulate_type == 'VXLAN-ETHERNET/NSH':
        # Build VXLAN-GPE + Ethernet (placeholder)
        pass


def build_trace_req_packet(vxlan_header_values, base_header_values, ctx_header_values, trace_req_header_values):
    """
    TODO: add docstring, params description
    """
    # Build VXLAN header
    vxlan_header = struct.pack('!B B H I',
                               vxlan_header_values.flags,
                               vxlan_header_values.reserved,
                               vxlan_header_values.protocol_type,
                               (vxlan_header_values.vni << 8) +
                               vxlan_header_values.reserved2)
    # Build base NSH header
    base_header = struct.pack('!H B B I',
                              (base_header_values.version << 14) +
                              (base_header_values.flags << 6) +
                              base_header_values.length,
                              base_header_values.md_type,
                              base_header_values.next_protocol,
                              (base_header_values.service_path << 8) +
                              base_header_values.service_index)

    # Build NSH context headers
    context_header = struct.pack('!I I I I',
                                 ctx_header_values.network_platform,
                                 ctx_header_values.network_shared,
                                 ctx_header_values.service_platform,
                                 ctx_header_values.service_shared)

    # Build trace context headers
    trace_header = struct.pack('!B B H I I I I',
                               trace_req_header_values.oam_type,
                               trace_req_header_values.sil,
                               trace_req_header_values.port,
                               trace_req_header_values.ip_1,
                               trace_req_header_values.ip_2,
                               trace_req_header_values.ip_3,
                               trace_req_header_values.ip_4)

    return vxlan_header + base_header + context_header + trace_header


def build_trace_req_header(oam_type, sil, remote_ip, remote_port):
    trace_req_header_values = TRACEREQHEADER()
    trace_req_header_values.oam_type = oam_type
    trace_req_header_values.sil = sil
    trace_req_header_values.port = int(remote_port)

    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect((remote_ip, trace_req_header_values.port))
    # print(s.getsockname()[0])
    src_addr = ipaddress.ip_address(s.getsockname()[0])
    if src_addr.version == 4:
        trace_req_header_values.ip_1 = 0x00000000
        trace_req_header_values.ip_2 = 0x00000000
        trace_req_header_values.ip_3 = 0x0000FFFF
        trace_req_header_values.ip_4 = int(ipaddress.IPv4Address(src_addr))
    elif src_addr.version == 6:
        int_addr6 = int(ipaddress.IPv6Address(src_addr))
        trace_req_header_values.ip_1 = int_addr6 >> 96
        trace_req_header_values.ip_2 = (int_addr6 >> 64) & 0x0FFFFFFFF
        trace_req_header_values.ip_3 = (int_addr6 >> 32) & 0x0FFFFFFFF
        trace_req_header_values.ip_4 = int_addr6 & 0x0FFFFFFFF

    return trace_req_header_values


def roundup(x):
    return x if x % 4 == 0 else x + 4 - x % 4


def add_sf_to_trace_pkt(rw_data, sf_type, sf_name):
    sf_type_len = roundup(len(sf_type))
    sf_type_pad = sf_type.ljust(sf_type_len, '\0')
    # len is 4 byte words
    sf_type_len >>= 2
    sf_type_pad = bytearray(sf_type_pad.encode('utf-8'))
    sf_name_len = roundup(len(sf_name))
    sf_name_pad = sf_name.ljust(sf_name_len, '\0')
    sf_name_len >>= 2
    sf_name_pad = bytearray(sf_name_pad.encode('utf-8'))
    trace_pkt = rw_data + struct.pack('!B', sf_type_len) + sf_type_pad + struct.pack('!B', sf_name_len) + sf_name_pad
    # rw_data[9] += (len(sf_data) >> 2)
    # trace_pkt = rw_data + sf_data
    return trace_pkt
