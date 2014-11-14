__author__ = "Reinaldo Penno, Jim Guichard, Paul Quinn"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "repenno@cisco.com, jguichar@cisco.com, paulq@cisco.com"
__status__ = "alpha"

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

"""Service Function Forwarder (SFF) server & client"""

import argparse
import asyncio
import sys
import struct
import socket
import binascii
from ctypes import *

try:
    import signal
except ImportError:
    signal = None

# Service Function Forwarder (SFF) table structure. Table referenced by Service Path Identifier as key. This table
# contains all the service chains known to this SFF and lists an ordered set of service function types.

sf_config_map = {'1': ['fw1', 'dpi1', 'nat1'], '2': ['fw2', 'dpi2', 'nat2'], '3': ['fw1', 'dpi1']}

# Service Function Instance Registry.
# This serves as a Service Function Registry referenced by service function type. A Service Function registers a
# callback or an IP address/port combination.

sf_map = {"fw1": {"function": "fw1_process_packet", "ip_address": "127.0.0.1", "port": "57444"},
          "fw2": {"function": "", "ip_address": "", "port": ""},
          "dpi1": {"function": "dpi1_process_packet", "ip_address": "", "port": "10000"},
          "dpi2": {"function": "", "ip_address": "", "port": ""},
          "nat1": {"function": "nat1_process_packet", "ip_address": "", "port": "10001"},
          "nat2": {"function": "", "ip_address": "", "port": ""}}

# VXLAN, NSH Base Header, and NSH Context Header data structures.

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

# Global flags used for indication of current packet processing status.

PACKET_CHAIN = 0b00000000  # Packet needs more processing within this SFF
PACKET_CONSUMED = 0b00000001  # Packet was sent to another SFF or service function
PACKET_ERROR = 0b00000010  # Packet will be dropped
SERVICEFUNCTION_INVALID = 0xDEADBEEF  # Referenced service function is invalid

# Client side code: Choose values for VXLAN, base NSH and context headers as part of packet generation

vxlan_values = VXLANGPE(int('00000100', 2), 0, 0x894F, int('111111111111111111111111', 2), 64)
ctx_values = CONTEXTHEADER(0xffffffff, 0, 0xffffffff, 0)
base_values = BASEHEADER(0x1, int('01000000', 2), 0x6, 0x1, 0x1, 0x000001, 0x3)

# Service side code: Store received values for VXLAN, base NSH and context headers data structures

server_vxlan_values = VXLANGPE()
server_ctx_values = CONTEXTHEADER()
server_base_values = BASEHEADER()

# Local service function callbacks. These are dummy services to track progress of packets through the service chain
# at this SFF.


def fw1_process_packet(data, addr):
    print('fw1 processed packet from:', addr)
    return PACKET_CHAIN


def dpi1_process_packet(data, addr):
    print('dpi1 processed packet from:', addr)
    return PACKET_CHAIN


def nat1_process_packet(data, addr):
    print('nat1 processed packet from:', addr)
    return PACKET_CHAIN


# Client side code: Build NSH packet encapsulated in VXLAN & NSH.

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


# Decode the VXLAN header for a received packet at this SFF.

def decode_vxlan(payload):
    # VXLAN header
    vxlan_header = payload[0:8]
    server_vxlan_values.flags, server_vxlan_values.reserved, server_vxlan_values.protocol_type, \
    vni_rsvd2 = struct.unpack('!B B H I', vxlan_header)

    server_vxlan_values.vni = vni_rsvd2 >> 8;
    server_vxlan_values.reserved2 = vni_rsvd2 & 0x000000FF

    # Yes, it is weird but the comparison is against False. Display debug if started with -O option.
    if __debug__ is False:
        print("\nVXLAN Header Decode:")
        print(binascii.hexlify(vxlan_header))
        print('Flags:', server_vxlan_values.flags)
        print('Reserved:', server_vxlan_values.reserved)
        print('Protocol Type:', hex(int(server_vxlan_values.protocol_type)))
        print('VNI:', server_vxlan_values.vni)
        print('Reserved:', server_vxlan_values.reserved2)


# Decode the NSH base header for a received packet at this SFF.

def decode_baseheader(payload):
    # Base Service header
    base_header = payload[8:17]  # starts at offset 8 of payload

    start_idx, server_base_values.md_type, server_base_values.next_protocol, path_idx = struct.unpack('!H B H I',
                                                                                                      base_header)

    server_base_values.version = start_idx >> 14
    server_base_values.flags = start_idx >> 6
    server_base_values.length = start_idx >> 0
    server_base_values.service_path = path_idx >> 8;
    server_base_values.service_index = path_idx & 0x000000FF

    if __debug__ is False:
        print ("\nBase NSH Header Decode:")
        print (binascii.hexlify(base_header))
        print ('NSH Version:', server_base_values.version)
        print ('NSH base header flags:', server_base_values.flags)
        print ('NSH base header length:', server_base_values.length)
        print ('NSH MD-type:', server_base_values.md_type)
        print ('NSH base header next protocol:', server_base_values.next_protocol)
        print ('Service Path Identifier:', server_base_values.service_path)
        print ('Service Index:', server_base_values.service_index)


# Decode the NSH context headers for a received packet at this SFF.

def decode_contextheader(payload):
    # Context header
    context_header = payload[17:33]

    server_ctx_values.network_platform, server_ctx_values.network_shared, server_ctx_values.service_platform, server_ctx_values.service_shared = struct.unpack(
        '!I I I I', context_header)

    if __debug__ is False:
        print ("\nNSH Context Header Decode:")
        print (binascii.hexlify(context_header))
        print ('First context header:', server_ctx_values.network_platform)
        print ('Second context header:', server_ctx_values.network_shared)
        print ('Third context header:', server_ctx_values.service_platform)
        print ('Fourth context header:', server_ctx_values.service_shared)


def lookup_next_sf(service_path, service_index):
    next_sfi = SERVICEFUNCTION_INVALID
    # First we determine the list of SFs in the received packet based on SPI value extracted from packet
    try:
        sf_list = sf_config_map[str(service_path)]
    except KeyError as detail:
        print('Apparently no valid SPI entry', detail)
        # return SERVICEFUNCTION_INVALID to indicate no valid SPI entry
        return next_sfi

    if __debug__ is False:
        print("\nSFI list for received SFP: ", sf_list)

    # Now we determine the next SFI name
    try:
        next_sfi = sf_list[-service_index]
        if __debug__ is False:
            print("Next SF: ", next_sfi)
    except IndexError as detail:
        print('guess something went wrong - Error:', detail)
    except:
        print("Unexpected error:", sys.exc_info()[0])
    finally:
        return next_sfi


def send_next_service(next_sfi, rw_data, addr):
    # First we need to find if this SFI is internal to this Service Node
    if next_sfi in sf_map:

        if sf_map[next_sfi]['function'] != '' and sf_map[next_sfi]['ip_address'] == '':
            functionp = globals()[sf_map[next_sfi]['function']]
            address = sf_map[next_sfi]['ip_address']
            ret = functionp(rw_data, addr)
            if ret != PACKET_CONSUMED:
                print('decrementing service index by 1 as packet processed by:', next_sfi)
                server_base_values.service_index -= 1
                set_service_index(rw_data, server_base_values.service_index)
                print('current service index value:', server_base_values.service_index)
            return ret, address
        elif sf_map[next_sfi]['ip_address'] != '':
            address = sf_map[next_sfi]['ip_address'], int(sf_map[next_sfi]['port'])
            packet_status = PACKET_CONSUMED
            print('were done - service', next_sfi, 'at address', address, 'consumed the packet')
            return packet_status, address


def set_service_index(rw_data, service_index):
    rw_data[16] = service_index


def process_incoming_packet(data, addr):
    print("Processing packet from:", addr)
    # Copy payload into bytearray so it can be changed
    rw_data = bytearray(data)
    # Decode the incoming packet for debug purposes and to strip out various header values
    decode_vxlan(data)
    decode_baseheader(data)
    decode_contextheader(data)
    # Lookup what to do with the packet based on Service Path Identifier (SPI)
    print("\nLooking up received Service Path Identifier...")
    packet_status = PACKET_CHAIN
    address = ()
    while packet_status == PACKET_CHAIN and server_base_values.service_index != 0:
        next_sfi = lookup_next_sf(server_base_values.service_path, server_base_values.service_index)
        if next_sfi == SERVICEFUNCTION_INVALID:
            # bye, bye packet
            print('we reached end of chain')
            print('ended up with:', binascii.hexlify(rw_data))
            print('service index end up as:', server_base_values.service_index)
            rw_data.__init__()
            data = ""
            addr = ""
            break
        packet_status, address = send_next_service(next_sfi, rw_data, addr)

    print('\nfinished processing packet from:', addr)
    print('\nlistening for NSH packets ...')
    return rw_data, address


class MyUdpServer:
    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        print('Received packet from:', addr)
        # Process the incoming packet
        rw_data, address = process_incoming_packet(data, addr)
        if address != '':
            # send the packet to the next SFF based on address
            self.transport.sendto(rw_data, address)
        else:
            # if want to echo packet back to client use following uncommented
            self.transport.sendto(rw_data, addr)

    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('stop', exc)

    def __init__(self, loop):
        self.transport = None
        self.loop = loop


class MyUdpClient:
    def connection_made(self, transport):
        self.transport = transport
        # Building client packet to send to SFF
        packet = build_packet()
        print('\nsending packet to SFF:\n', binascii.hexlify(packet))
        # Send the packet
        self.transport.sendto(packet)

    def datagram_received(self, data, addr):
        print('\nreceived packet from SFF:\n', binascii.hexlify(data))
        print('\n')
        # Decode all the headers
        decode_vxlan(data)
        decode_baseheader(data)
        decode_contextheader(data)
        self.loop.stop()

    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('closing transport', exc)
        self.loop = asyncio.get_event_loop()
        self.loop.stop()

    def __init__(self, loop):
        self.transport = None
        self.loop = loop


def start_server(loop, addr, udpserver):
    t = asyncio.Task(loop.create_datagram_endpoint(
        lambda: udpserver, local_addr=addr))
    loop.run_until_complete(t)
    print('\nStarting Service Function Forwarder (SFF)')
    print('listening for NSH packets on port: ', addr[1])


# note using port 57444 but could be any port, just remove port syntax and
# update get_client_ip() to remove [0] from getsockname()
def start_client(loop, addr, myip, udpclient):
    t = asyncio.Task(loop.create_datagram_endpoint(
        lambda: udpclient, local_addr=(myip, 57444), remote_addr=addr))
    loop.run_until_complete(t)


def get_client_ip():
    # Let's find a local IP address to use as the source IP of client generated packets
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        client = (s.getsockname()[0])
    except socket.error:
        client = "Unknown IP"
    finally:
        s.close()
    return client


ARGS = argparse.ArgumentParser(description="UDP Echo example.")
ARGS.add_argument(
    '--server', action="store_true", dest='server',
    default=False, help='Run udp server')
ARGS.add_argument(
    '--client', action="store_true", dest='client',
    default=False, help='Run udp client')
ARGS.add_argument(
    '--host', action="store", dest='host',
    default='127.0.0.1', help='Host name')
ARGS.add_argument(
    '--port', action="store", dest='port',
    default=4789, type=int, help='Port number')


def main():
    args = ARGS.parse_args()
    if ':' in args.host:
        args.host, port = args.host.split(':', 1)
        args.port = int(port)

    if (not (args.server or args.client)) or (args.server and args.client):
        print('Please specify --server or --client\n')
        ARGS.print_help()
    else:
        loop = asyncio.get_event_loop()
        # if signal is not None:
        # loop.add_signal_handler(signal.SIGINT, loop.stop)

        if '--server' in sys.argv:
            udpserver = MyUdpServer(loop)
            start_server(loop, (args.host, args.port), udpserver)
        else:
            # Figure out a source IP address / source UDP port for the client connection
            udpclient = MyUdpClient(loop)
            local_ip = get_client_ip()
            start_client(loop, (args.host, args.port), local_ip, udpclient)

        loop.run_forever()


if __name__ == '__main__':
    main()
