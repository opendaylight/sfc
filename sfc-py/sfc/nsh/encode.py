#
# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


import struct
import socket
import ipaddress
import logging

from .common import *  # noqa


__author__ = "Reinaldo Penno, Jim Guichard"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__version__ = "0.5"
__email__ = "rapenno@gmail.com, jguichar@cisco.com"
__status__ = "alpha"

"""
Provides function to encode vxlan-gpe|GRE + NSH Base + Context Headers

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |R|R|R|R|I|P|R|R|   Reserved                    |Next Protocol  |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                VXLAN Network Identifier (VNI) |   Reserved    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

    The GRE packet header has the form (ignoring K and S bits for now):

    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |C|       Reserved0       | Ver |         Protocol Type         |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |      Checksum (optional)      |       Reserved1 (Optional)    |
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

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |    dMAC1      |    dMAC2      |    dMAC3      |    dMAC4      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |    dMAC5      |    dMAC6      |    sMAC1      |    sMAC2      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |    sMAC3      |    sMAC4      |    sMAC5      |    sMAC6      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   | ethernettype0 | ethernettype1 |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

"""

logger = logging.getLogger(__file__)


def build_header(*headers):
    """
    Combine all specified headers - headers ORDER is CRUCIAL

    :param headers: NSH headers
    :type headers: `:class:nsh.common.*HEADER`

    :return bytes

    """
    composite_header = b''

    for header in headers:
        composite_header += header.build()

    return composite_header


def build_nsh_header(encapsulation_header, base_header, ctx_header):
    """
    Build NSH header

    :param encapsulation_header: VXLAN or GRE NSH header
    :type encapsulation_header: `:class:nsh.common.VXLANGPE|GREHEADER`
    :param base_header: base NSH header
    :type base_header: `:class:nsh.common.BASEHEADER`
    :param ctx_header: context NSH header
    :type ctx_header: `:class:nsh.common.CONTEXTHEADER`

    :return bytes

    """
    return build_header(encapsulation_header, base_header, ctx_header)


def build_nsh_eth_header(encapsulation_header, base_header,
                         ctx_header, ethernet_header):
    """
    Build NSH header with underlying ethernet header

    :param encapsulation_header: VXLAN or GRE NSH header
    :type encapsulation_header: `:class:nsh.common.VXLANGPE|GREHEADER`
    :param base_header: base NSH header
    :type base_header: `:class:nsh.common.BASEHEADER`
    :param ctx_header: context NSH header
    :type ctx_header: `:class:nsh.common.CONTEXTHEADER`
    :param ethernet_header: ethernet header
    :type ethernet_header: `:class:nsh.common.ETHHEADER`

    :return bytes

    """
    return build_header(encapsulation_header, base_header,
                        ctx_header, ethernet_header)


def build_vxlan_header(encapsulation_header, ethernet_header):
    """
    Build NSH header with underlying ethernet header

    :param encapsulation_header: VXLAN or GRE NSH header
    :type encapsulation_header: `:class:nsh.common.VXLANGPE|GREHEADER`
    :param base_header: base NSH header
    :type base_header: `:class:nsh.common.BASEHEADER`
    :param ctx_header: context NSH header
    :type ctx_header: `:class:nsh.common.CONTEXTHEADER`
    :param ethernet_header: ethernet header
    :type ethernet_header: `:class:nsh.common.ETHHEADER`

    :return bytes

    """
    return build_header(encapsulation_header, ethernet_header)


def build_nsh_trace_header(encapsulation_header, base_header,
                           ctx_header, trace_header):
    """
    Build NSH trace header

    :param encapsulation_header: VXLAN or GRE NSH header
    :type encapsulation_header: `:class:nsh.common.VXLANGPE|GREHEADER`
    :param base_header: base NSH header
    :type base_header: `:class:nsh.common.BASEHEADER`
    :param ctx_header: context NSH header
    :type ctx_header: `:class:nsh.common.CONTEXTHEADER`
    :param trace_header: trace context header
    :type trace_header: `:class:nsh.common.TRACEREQHEADER`

    :return bytes

    """
    return build_header(encapsulation_header, base_header,
                        ctx_header, trace_header)


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


def build_ipv4_header(ip_tot_len, proto, src_ip, dest_ip):
    """
    Builds a complete IP header including checksum
    """

    if src_ip:
        ip_saddr = socket.inet_aton(src_ip)
    else:
        ip_saddr = socket.inet_aton(socket.gethostbyname(socket.gethostname()))

    ip_saddr = int.from_bytes(ip_saddr, byteorder='big')
    ip_daddr = socket.inet_aton(dest_ip)
    ip_daddr = int.from_bytes(ip_daddr, byteorder='big')

    ip_header = IP4HEADER(IP_HEADER_LEN, IPV4_VERSION, IPV4_TOS, ip_tot_len, IPV4_PACKET_ID, 0, IPV4_TTL, proto, 0,
                          ip_saddr, ip_daddr)

    checksum = compute_internet_checksum(ip_header.build())
    ip_header.set_ip_checksum(checksum)
    ip_header_pack = ip_header.build()

    return ip_header, ip_header_pack


def build_udp_header(src_port, dest_port, ip_header, data):
    """
    Building an UDP header requires fields from
    IP header in order to perform checksum calculation
    """

    # build UDP header with sum = 0
    udp_header = UDPHEADER(src_port, dest_port, UDP_HEADER_LEN_BYTES + len(data), 0)
    udp_header_pack = udp_header.build()

    # build Pseudo Header
    p_header = PSEUDO_UDPHEADER()
    p_header.dest_ip = ip_header.ip_daddr
    p_header.src_ip = ip_header.ip_saddr
    p_header.length = udp_header.udp_len

    p_header_pack = p_header.build()

    udp_checksum = compute_internet_checksum(p_header_pack + udp_header_pack + data)
    udp_header.udp_sum = udp_checksum
    # pack UDP header again but this time with checksum
    udp_header_pack = udp_header.build()

    return udp_header, udp_header_pack


def build_udp_packet(src_ip, dest_ip, src_port, dest_port, data):
    """
    data needs to encoded as Python bytes. In the case of strings
    this means a bytearray of an UTF-8 encoding
    """

    total_len = len(data) + IPV4_HEADER_LEN_BYTES + UDP_HEADER_LEN_BYTES
    # First we build the IP header
    ip_header, ip_header_pack = build_ipv4_header(total_len, socket.IPPROTO_UDP, src_ip, dest_ip)

    # Build UDP header
    udp_header, udp_header_pack = build_udp_header(src_port, dest_port, ip_header, data)

    udp_packet = ip_header_pack + udp_header_pack + data

    return udp_packet


def compute_internet_checksum(data):
    """
    Function for Internet checksum calculation. Works
    for both IP and UDP.

    """
    checksum = 0
    n = len(data) % 2
    # data padding
    pad = bytearray('', encoding='UTF-8')
    if n == 1:
        pad = bytearray('\x00')
    # for i in range(0, len(data + pad) - n, 2):
    for i in range(0, len(data + pad), 2):
        checksum += (data[i] << 8) + (data[i + 1])
    while checksum >> 16:
        checksum = (checksum & 0xFFFF) + (checksum >> 16)
    checksum = ~checksum & 0xffff
    return checksum


def process_context_headers(ctx1=0, ctx2=0, ctx3=0, ctx4=0):
    context_headers = []
    for ctx in [ctx1, ctx2, ctx3, ctx4]:
        try:
            ipaddr = ipaddress.IPv4Address(ctx)
            context_headers.append(int(ipaddr))
        except ValueError:
            try:
                context_headers.append((int(ctx) & 0xFFFFFFFF))
            except ValueError:
                logger.error("Context header %d can not be represented as an integer", ctx)

    return context_headers
