__author__ = "Jim Guichard"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "jguichar@cisco.com"
__status__ = "alpha"

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

"""Service Classifier"""

from netfilterqueue import NetfilterQueue
import sys
import struct
import socket
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

classify_map = {"172.16.6.140": {"sff": "172.16.6.141", "port": "4789"}}

vxlan_values = VXLANGPE(int('00000100', 2), 0, 0x894F, int('111111111111111111111111', 2), 64)
ctx_values = CONTEXTHEADER(0xffffffff, 0, 0xffffffff, 0)
base_values = BASEHEADER(0x1, int('01000000', 2), 0x6, 0x1, 0x1, 0x000001, 0x4)

def build_packet():
    # Build VXLAN header
    vxlan_header = struct.pack('!B B H I', vxlan_values.flags, vxlan_values.reserved, vxlan_values.protocol_type,
                               (vxlan_values.vni << 8) + vxlan_values.reserved2)
    # Build base NSH header
    base_header = struct.pack('!H B H I', (base_values.version << 14) + (base_values.flags << 6) + base_values.length,
                              base_values.md_type,
                              base_values.next_protocol, (base_values.service_path << 8) + base_values.service_index)
    #Build NSH context headers
    context_header = struct.pack('!I I I I', ctx_values.network_platform, ctx_values.network_shared,
                                 ctx_values.service_platform, ctx_values.service_shared)
    return vxlan_header + base_header + context_header

# Testing: Setup linux with:
# iptables -I INPUT -d 172.16.6.140 -j NFQUEUE --queue_num 1
#
#
    
def process_and_accept(Packet):
    Packet.accept()
    data = Packet.get_payload()
    address = int(binascii.hexlify(data[16:20]), 16)
    lookup = socket.inet_ntoa(struct.pack(">I", address))

    if lookup in classify_map:
        if classify_map[lookup]['sff'] != '':
    
            print (binascii.hexlify(data))
            packet = build_packet() + data
            print (binascii.hexlify(packet))
    
            UDP_IP = classify_map[lookup]['sff'] 
            UDP_PORT = int(classify_map[lookup]['port'])
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM) 
            sock.sendto(packet, (UDP_IP, UDP_PORT))
            sock.close()

nfqueue = NetfilterQueue()
nfqueue.bind(1, process_and_accept)
try:
    nfqueue.run()
except KeyboardInterrupt:
    print
    
