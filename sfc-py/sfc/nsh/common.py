#
# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


from struct import pack
from ctypes import Structure, c_ubyte, c_ushort, c_uint


__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "rapenno@gmail.com"
__status__ = "alpha"

"""
NSH related constants and various header classes.

Each NSH header class is a named tuple and MUST provides only "build" method,
which is responsible for composing a (raw) bytes representation of a header.
"""

#: constants

# Inner Packet

# NSH type 1  (ENCAP+BASE+CTX)  size = 8 + 8 + 16 = 32
PAYLOAD_START_INDEX_NSH_TYPE1 = 32
# NSH type 2  (ENCAP+BASE+CTX)  size = 8 + 8 + 16 = 32
PAYLOAD_START_INDEX_NSH_TYPE2 = 32
# NSH type 2  (ENCAP+BASE+CTX+ETH)  size = 8 + 8 + 16 + 14 = 46
PAYLOAD_START_INDEX_NSH_TYPE3 = 46

# VXLAN constants

VXLAN_RFC7348_HEADER = int('00001000000000000000000000000000', 2)
VXLAN_GPE_HEADER = int('00001000000000000000000000000100', 2)
VXLAN_START_OFFSET = 0

# NSH constants

NSH_TYPE1_DATA_PACKET = int('000000000000011000000001', 2)
# the lenght of NSH itself, without encapsulation heeder in 4 byte words = 8 bytes BASE + 16 bytes
NSH_TYPE1_LEN = 0x6
NSH_MD_TYPE1 = 0x1
NSH_VERSION1 = int('00', 2)
NSH_NEXT_PROTO_IPV4 = int('00000001', 2)
NSH_NEXT_PROTO_OAM = int('00000100', 2)
NSH_NEXT_PROTO_ETH = int('00000011', 2)
NSH_FLAG_ZERO = int('00000000', 2)
NSH_BASE_HEADER_START_OFFSET = 8

#: IP constants

IP_HEADER_LEN = 5
IPV4_HEADER_LEN_BYTES = 20
IPV4_VERSION = 4
IPV4_PACKET_ID = 54321
IPV4_TTL = 255
IPV4_TOS = 0
IPV4_IHL_VER = (IPV4_VERSION << 4) + IP_HEADER_LEN

IPV6_TRAFFIC_CLASS = 20
IPV6_VERSION = 6
IPV6_FLOW_LABEL = 54321
IPV6_NEXT_HOP = 255

#: UDP constants

UDP_HEADER_LEN_BYTES = 8

#: VXLAN-gpe constants
VXLAN_NEXT_PROTO_NSH = int('00000100', 2)


#: NSH OAM Constants
NSH_TYPE1_OAM_PACKET = int('00100000000001100000000100000100', 2)
OAM_VERSION_AND_FLAG = int('00100000', 2)
OAM_FLAG_AND_RESERVED = int('10000000', 2)
OAM_TRACE_REQ_TYPE = int('00000001', 2)
OAM_TRACE_RESP_TYPE = int('00000010', 2)
NSH_OAM_PKT_START_OFFSET = 32
NSH_OAM_TRACE_DEST_IP_REPORT_OFFSET = 36
NSH_OAM_TRACE_DEST_IP_REPORT_LEN = 16
NSH_OAM_TRACE_HDR_LEN = 20
NSH_OAM_TRACE_RESP_START_OFFSET = NSH_OAM_PKT_START_OFFSET + NSH_OAM_TRACE_HDR_LEN
NSH_OAM_TRACE_RESP_SF_TYPE_LEN_START_OFFSET = NSH_OAM_TRACE_RESP_START_OFFSET
NSH_OAM_TRACE_RESP_SF_TYPE_START_OFFSET = NSH_OAM_TRACE_RESP_SF_TYPE_LEN_START_OFFSET + 1


class VXLANGPE(Structure):
    _fields_ = [('flags', c_ubyte),
                ('reserved', c_uint, 16),
                ('next_protocol', c_uint, 8),
                ('vni', c_uint, 24),
                ('reserved2', c_uint, 8)]

    header_size = 8

    def __init__(self, flags=int('00001100', 2), reserved=0, next_protocol=VXLAN_NEXT_PROTO_NSH,
                 vni=int('111111111111111111111111', 2), reserved2=0, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.flags = flags
        self.reserved = reserved
        self.next_protocol = next_protocol
        self.vni = vni
        self.reserved2 = reserved2

    def build(self):
        return pack('!B H B I',
                    self.flags,
                    self.reserved,
                    self.next_protocol,
                    (self.vni << 8) + self.reserved2)


class VXLAN(Structure):
    """
    Support for legacy NSH implementation, a.k.a, OVS nsh-v8.
    This means a packet like IP + UDP + VXLAN (not GPE) + NSH + ...
    """
    _fields_ = [('flags', c_ubyte),
                ('reserved', c_uint, 16),
                ('next_protocol', c_uint, 8),
                ('vni', c_uint, 24),
                ('reserved2', c_uint, 8)]

    def __init__(self, flags=int('00001000', 2), reserved=0, next_protocol=0,
                 vni=int('111111111111111111111111', 2), reserved2=0, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.flags = flags
        self.reserved = reserved
        self.next_protocol = next_protocol
        self.vni = vni
        self.reserved2 = reserved2

    header_size = 8

    def build(self):
        return pack('!B H B I',
                    self.flags,
                    self.reserved,
                    self.next_protocol,
                    (self.vni << 8) + self.reserved2)


class GREHEADER(Structure):
    _fields_ = [('c', c_uint, 1),
                ('reserved0', c_uint, 12),
                ('version', c_uint, 3),
                ('protocol_type', c_uint, 16),
                ('checksum', c_uint, 16),
                ('reserved1', c_uint, 16)]

    header_size = 8

    def build(self):
        return pack('!H H H H',
                    (self.c << 15) + (self.reserved0 << 3) + self.version,
                    self.protocol_type,
                    self.checksum,
                    self.reserved1)


class ETHHEADER(Structure):
    _fields_ = [('dmac0', c_ubyte),
                ('dmac1', c_ubyte),
                ('dmac2', c_ubyte),
                ('dmac3', c_ubyte),
                ('dmac4', c_ubyte),
                ('dmac5', c_ubyte),
                ('smac0', c_ubyte),
                ('smac1', c_ubyte),
                ('smac2', c_ubyte),
                ('smac3', c_ubyte),
                ('smac4', c_ubyte),
                ('smac5', c_ubyte),
                ('ethertype0', c_ubyte),
                ('ethertype1', c_ubyte)]

    header_size = 14

    def build(self):
        return pack('!B B B B B B B B B B B B B B',
                    self.dmac0,
                    self.dmac1,
                    self.dmac2,
                    self.dmac3,
                    self.dmac4,
                    self.dmac5,
                    self.smac0,
                    self.smac1,
                    self.smac2,
                    self.smac3,
                    self.smac4,
                    self.smac5,
                    self.ethertype0,
                    self.ethertype1)


class BASEHEADER(Structure):
    _fields_ = [('version', c_ushort, 2),
                ('flags', c_ushort, 8),
                ('length', c_ushort, 6),
                ('md_type', c_ubyte),
                ('next_protocol', c_ubyte),
                ('service_path', c_uint, 24),
                ('service_index', c_uint, 8)]

    def __init__(self, service_path=1, service_index=255, version=NSH_VERSION1, flags=NSH_FLAG_ZERO,
                 length=NSH_TYPE1_LEN, md_type=NSH_MD_TYPE1, proto=NSH_NEXT_PROTO_ETH, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.version = version
        self.flags = flags
        self.length = length
        self.md_type = md_type
        self.next_protocol = proto
        self.service_path = service_path
        self.service_index = service_index

    header_size = 8

    def build(self):
        return pack('!H B B I',
                    (self.version << 14) + (self.flags << 6) + self.length,
                    self.md_type,
                    self.next_protocol,
                    (self.service_path << 8) + self.service_index)


class CONTEXTHEADER(Structure):
    _fields_ = [('network_platform', c_uint),
                ('network_shared', c_uint),
                ('service_platform', c_uint),
                ('service_shared', c_uint)]

    header_size = 16

    def __init__(self, network_platform=0x00, network_shared=0x00, service_platform=0x00, service_shared=0x00, *args,
                 **kwargs):
        super().__init__(*args, **kwargs)
        self.network_platform = network_platform
        self.network_shared = network_shared
        self.service_platform = service_platform
        self.service_shared = service_shared

    def build(self):
        return pack('!I I I I',
                    self.network_platform,
                    self.network_shared,
                    self.service_platform,
                    self.service_shared)


class TRACEREQHEADER(Structure):
    _fields_ = [('oam_type', c_ubyte),
                ('sil', c_ubyte),
                ('port', c_ushort),
                ('ip_1', c_uint),
                ('ip_2', c_uint),
                ('ip_3', c_uint),
                ('ip_4', c_uint)]

    header_size = 20

    def build(self):
        return pack('!B B H I I I I',
                    self.oam_type,
                    self.sil,
                    self.port,
                    self.ip_1,
                    self.ip_2,
                    self.ip_3,
                    self.ip_4)


class IP4HEADER(Structure):
    _fields_ = [
        ('ip_ihl', c_ubyte),
        ('ip_ver', c_ubyte),
        ('ip_tos', c_ubyte),
        ('ip_tot_len', c_ushort),
        ('ip_id', c_ushort),
        ('ip_frag_offset', c_ushort),
        ('ip_ttl', c_ubyte),
        ('ip_proto', c_ubyte),
        ('ip_chksum', c_ushort),
        ('ip_saddr', c_uint),
        ('ip_daddr', c_uint)]

    header_size = 20

    def build(self):
        ip_header_pack = pack('!B B H H H B B H I I', IPV4_IHL_VER, self.ip_tos, self.ip_tot_len, self.ip_id,
                              self.ip_frag_offset, self.ip_ttl, self.ip_proto, self.ip_chksum, self.ip_saddr,
                              self.ip_daddr)
        return ip_header_pack

        # in 6
        # {
        # /* Compute Internet Checksum for "count" bytes
        # *         beginning at location "addr".
        # */
        # register long sum = 0;
        #
        # while( count > 1 )  {
        # /*  This is the inner loop */
        # sum += * (unsigned short) addr++;
        # count -= 2;
        # }
        #
        # /*  Add left-over byte, if any */
        # if( count > 0 )
        # sum += * (unsigned char *) addr;
        #
        # /*  Fold 32-bit sum to 16 bits */
        # while (sum>>16)
        #         sum = (sum & 0xffff) + (sum >> 16);
        #
        #     checksum = ~sum;
        # }

    def set_ip_checksum(self, checksum):
        self.ip_chksum = checksum


class IP6HEADER(Structure):
    _fields_ = [
        ('ip_ver', c_ubyte),
        ('ip_tc', c_ubyte),
        ('ip_flow_lbl', c_uint),
        ('ip_payload_len', c_ushort),
        ('ip_next_header', c_ubyte),
        ('ip_hop_lmt', c_ubyte),
        ('ip_saddr1', c_uint),
        ('ip_saddr2', c_uint),
        ('ip_saddr3', c_uint),
        ('ip_saddr4', c_uint),
        ('ip_daddr1', c_uint),
        ('ip_daddr2', c_uint),
        ('ip_daddr3', c_uint),
        ('ip_daddr4', c_uint)]

    header_size = 40

    def build(self):
        ipv6_ver_tc_fl = (IPV6_VERSION << 28) + (self.ip_tc << 20) + self.ip_flow_label
        ip_header_pack = pack('!I H B B I I I I I I I I', ipv6_ver_tc_fl, self.ip_payloadlen,
                              self.ip_next_header, self.ip_hop_lmt,
                              self.ip_saddr1, self.ip_saddr2, self.ip_saddr3, self.ip_saddr4,
                              self.ip_daddr1, self.ip_daddr2, self.ip_daddr3, self.ip_daddr4)
        return ip_header_pack

    def set_ip_checksum(self, checksum):
        self.ip_chksum = checksum


class UDPHEADER(Structure):
    _fields_ = [
        ('udp_sport', c_ushort),
        ('udp_dport', c_ushort),
        ('udp_len', c_ushort),
        ('udp_sum', c_ushort)]

    header_size = 8

    def build(self):
        udp_header_pack = pack('!H H H H', self.udp_sport, self.udp_dport, self.udp_len,
                               self.udp_sum)
        return udp_header_pack


class PSEUDO_UDPHEADER(Structure):
    """ Pseudoheader used in the UDP checksum."""

    def __init__(self):
        self.src_ip = 0
        self.dest_ip = 0
        self.zeroes = 0
        self.protocol = 17
        self.length = 0

    def build(self):
        """ Create a string from a pseudoheader """
        p_udp_header_pack = pack('!I I B B H', self.src_ip, self.dest_ip,
                                 self.zeroes, self.protocol, self.length)
        return p_udp_header_pack


class InnerHeader():
    """Essential Information to build inner packet"""

    def __init__(self, inner_src_ip="192.168.0.1", inner_dest_ip="192.168.0.2", inner_src_port="10000",
                 inner_dest_port="20000", inner_protocol=17, *args,
                 **kwargs):
        super().__init__(*args, **kwargs)
        self.inner_src_ip = inner_src_ip
        self.inner_dest_ip = inner_dest_ip
        self.inner_src_port = inner_src_port
        self.inner_dest_port = inner_dest_port
        self.inner_protocol = inner_protocol
        self.data = "test".encode('utf-8')
