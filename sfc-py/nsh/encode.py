
__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.3"
__email__ = "rapenno@gmail.com"
__status__ = "alpha"

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

"""
Provides a Function to fully encode VXLAN-GPE + NSH Base + Context Headers

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
from nsh.common import *
import socket
import ipaddress


def build_packet(vxlan_header_values, base_header_values, ctx_header_values):
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

    return vxlan_header + base_header + context_header


def build_trace_req_packet(vxlan_header_values, base_header_values, trace_req_header_values):
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

    # Build trace context headers
    trace_header = struct.pack('!B B H I I I I',
                               trace_req_header_values.sil,
                               trace_req_header_values.flags,
                               trace_req_header_values.port,
                               trace_req_header_values.ip_1,
                               trace_req_header_values.ip_2,
                               trace_req_header_values.ip_3,
                               trace_req_header_values.ip_4)

    return vxlan_header + base_header + trace_header


def build_trace_req_header(sil, flags, remote_ip, remote_port):

        trace_req_header_values = TRACEREQHEADER()
        trace_req_header_values.sil = sil
        trace_req_header_values.flags = flags
        trace_req_header_values.port = int(remote_port)

        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect((remote_ip, trace_req_header_values.port))
        print(s.getsockname()[0])
        src_addr = ipaddress.ip_address(s.getsockname()[0])
        if src_addr.version == 4:
            print("Building IPv4 mapped IPv6 Address")
            trace_req_header_values.ip_1 = 0x00000000
            trace_req_header_values.ip_2 = 0x00000000
            trace_req_header_values.ip_3 = 0x0000FFFF
            trace_req_header_values.ip_4 = int(ipaddress.IPv4Address(src_addr))
        elif src_addr.version == 6:
            print("Building IPv6 Address")
            int_addr6 = int(ipaddress.IPv6Address(src_addr))
            trace_req_header_values.ip_1 = int_addr6 >> 96
            trace_req_header_values.ip_2 = (int_addr6 >> 64) & 0x0FFFFFFFF
            trace_req_header_values.ip_3 = (int_addr6 >> 32) & 0x0FFFFFFFF
            trace_req_header_values.ip_4 = int_addr6 & 0x0FFFFFFFF

        return trace_req_header_values


def add_sf_to_trace_pkt(rw_data, sf_type):
    sf_data = bytearray(sf_type.encode('ascii'))
    rw_data[9] += (len(sf_data) >> 2)
    trace_pkt = rw_data + sf_data
    return trace_pkt