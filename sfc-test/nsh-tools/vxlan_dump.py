#
# Copyright (c) 2015 Intel, Inc., Cisco Systems, Inc. and others.  All rights
# reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

__author__ = "Yi Yang, Reinaldo Penno"
__copyright__ = "Copyright(c) 2015, Intel, Inc. and Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "yi.y.yang@intel.com, rapenno@gmail.com"
__status__ = "beta"

import socket, sys
import argparse
from struct import *
from ctypes import Structure, c_ubyte, c_ushort, c_uint

NSH_TYPE1_LEN = 0x6
NSH_MD_TYPE1 = 0x1
NSH_VERSION1 = int('00', 2)
NSH_NEXT_PROTO_IPV4 = int('00000001', 2)
NSH_NEXT_PROTO_OAM = int('00000100', 2)
NSH_NEXT_PROTO_ETH = int('00000011', 2)
NSH_FLAG_ZERO = int('00000000', 2)

IP_HEADER_LEN = 5
IPV4_VERSION = 4
IPV4_IHL_VER = (IPV4_VERSION << 4) + IP_HEADER_LEN

class VXLAN(Structure):
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
    """
    Represent a NSH base header
    """
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

    def set_ip_checksum(self, checksum):
        self.ip_chksum = checksum

class UDPHEADER(Structure):
    """
    Represents a UDP header
    """
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

def decode_eth(payload, eth_header_values):
    eth_header = payload[0:14]

    _header_values = unpack('!B B B B B B B B B B B B B B', eth_header)
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

def decode_ip(payload, ip_header_values):
    ip_header = payload[14:34]

    _header_values = unpack('!B B H H H B B H I I', ip_header)
    ip_header_values.ip_ihl = _header_values[0] & 0x0F
    ip_header_values.ip_ver = _header_values[0] >> 4
    ip_header_values.ip_tos = _header_values[1]
    ip_header_values.ip_tot_len = _header_values[2]
    ip_header_values.ip_id = _header_values[3]
    ip_header_values.ip_frag_offset = _header_values[4]
    ip_header_values.ip_ttl = _header_values[5]
    ip_header_values.ip_proto = _header_values[6]
    ip_header_values.ip_chksum = _header_values[7]
    ip_header_values.ip_saddr = _header_values[8]
    ip_header_values.ip_daddr = _header_values[9]

def decode_udp(payload, udp_header_values):
    udp_header = payload[34:42]

    _header_values = unpack('!H H H H', udp_header)
    udp_header_values.udp_sport = _header_values[0]
    udp_header_values.udp_dport = _header_values[1]
    udp_header_values.udp_len = _header_values[2]
    udp_header_values.udp_sum = _header_values[3]


def decode_vxlan(payload, vxlan_header_values):
    """Decode the VXLAN header for a received packets"""
    vxlan_header = payload[42:50]

    _header_values = unpack('!B H B I', vxlan_header)
    vxlan_header_values.flags = _header_values[0]
    vxlan_header_values.reserved = _header_values[1]
    vxlan_header_values.next_protocol = _header_values[2]

    vni_rsvd2 = _header_values[3]
    vxlan_header_values.vni = vni_rsvd2 >> 8
    vxlan_header_values.reserved2 = vni_rsvd2 & 0x000000FF

def decode_nsh_baseheader(payload, nsh_base_header_values):
    """Decode the NSH base headers for a received packets"""
    base_header = payload[50:58]

    _header_values = unpack('!H B B I', base_header)
    start_idx = _header_values[0]
    nsh_base_header_values.md_type = _header_values[1]
    nsh_base_header_values.next_protocol = _header_values[2]
    path_idx = _header_values[3]

    nsh_base_header_values.version = start_idx >> 14
    nsh_base_header_values.flags = start_idx >> 6
    nsh_base_header_values.length = start_idx >> 0
    nsh_base_header_values.service_path = path_idx >> 8
    nsh_base_header_values.service_index = path_idx & 0x000000FF

def decode_nsh_contextheader(payload, nsh_context_header_values):
    """Decode the NSH context headers for a received packet"""
    context_header = payload[58:74]

    _header_values = unpack('!I I I I', context_header)
    nsh_context_header_values.network_platform = _header_values[0]
    nsh_context_header_values.network_shared = _header_values[1]
    nsh_context_header_values.service_platform = _header_values[2]
    nsh_context_header_values.service_shared = _header_values[3]

def main():
    parser = argparse.ArgumentParser(description='VxLAN dump',
                                     usage=("\npython3 vxlan_dump.py [-i ethn | --interface=ethn]")
                                    )
    parser.add_argument('-i', '--interface',
                        help='Dump VxLAN packet from the specified interface')
    args = parser.parse_args()
    
    try:
        s = socket.socket(socket.AF_PACKET, socket.SOCK_RAW, socket.ntohs(0x0003))
        if args.interface is not None:
            s.bind((args.interface, 0))
    except OSError as e:
        print("{}".format(e) + " '%s'" % args.interface)
        sys.exit()
    
    # receive a packet
    pktnum=0
    while True:
        packet = s.recvfrom(65565)
    
        #packet string from tuple
        packet = packet[0]
    
        #parse ethernet header
        eth_length = 14
        ip_length = 20
        udp_length = 8
        vxlan_length = 8
    
        myethheader = ETHHEADER()
        myipheader = IP4HEADER()
        myudpheader = UDPHEADER()
        myvxlanheader = VXLAN()
        mynshbaseheader = BASEHEADER()
        mynshcontextheader = CONTEXTHEADER()
    
        """ Decode ethernet header """
        decode_eth(packet, myethheader)
        if ((myethheader.ethertype0 != 0x08) or (myethheader.ethertype1 != 0x00)):
            continue
    
        """ Decode IP header """
        decode_ip(packet, myipheader)
        if (myipheader.ip_proto != 17):
            continue
    
        """ Decode UDP header """
        decode_udp(packet, myudpheader)
        if ((myudpheader.udp_dport != 4789) and (myudpheader.udp_dport != 4790)):
            continue
    
        pktnum = pktnum + 1
    
        """ Decode VxLAN/VxLAN-gpe header """
        decode_vxlan(packet, myvxlanheader)
        print("\n\nPacket #%d" % pktnum)
    
        """ Print ethernet header """
        print("Eth Dst MAC: %.2x:%.2x:%.2x:%.2x:%.2x:%.2x, Src MAC: %.2x:%.2x:%.2x:%.2x:%.2x:%.2x, Ethertype: 0x%.4x" % (myethheader.dmac0, myethheader.dmac1, myethheader.dmac2, myethheader.dmac3, myethheader.dmac4, myethheader.dmac5, myethheader.smac0, myethheader.smac1, myethheader.smac2, myethheader.smac3, myethheader.smac4, myethheader.smac5, (myethheader.ethertype0<<8) | myethheader.ethertype1))
    
        """ Print IP header """
        print("IP Version: %s IP Header Length: %s, TTL: %s, Protocol: %s, Src IP: %s, Dst IP: %s" % (myipheader.ip_ver, myipheader.ip_ihl, myipheader.ip_ttl, myipheader.ip_proto, str(socket.inet_ntoa(pack('!I', myipheader.ip_saddr))), str(socket.inet_ntoa(pack('!I', myipheader.ip_daddr)))))
    
        """ Print UDP header """
        print ("UDP Src Port: %s, Dst Port: %s, Length: %s, Checksum: %s" % (myudpheader.udp_sport, myudpheader.udp_dport, myudpheader.udp_len, myudpheader.udp_sum))
    
        """ Print VxLAN/VxLAN-gpe header """
        print("VxLAN/VxLAN-gpe VNI: %s, flags: %.2x, Next: %s" % (myvxlanheader.vni, myvxlanheader.flags, myvxlanheader.next_protocol))
    
        """ Print NSH header """
        if (myudpheader.udp_dport == 4790):
            decode_nsh_baseheader(packet, mynshbaseheader)
            decode_nsh_contextheader(packet, mynshcontextheader)
         
            """ Print NSH base header """
            print("NSH base nsp: %s, nsi: %s" % (mynshbaseheader.service_path, mynshbaseheader.service_index))
    
            """ Print NSH context header """
            print("NSH context c1: 0x%.8x, c2: 0x%.8x, c3: 0x%.8x, c4: 0x%.8x" % (mynshcontextheader.network_platform, mynshcontextheader.network_shared, mynshcontextheader.service_platform, mynshcontextheader.service_shared))

if __name__ == "__main__":
    main()
