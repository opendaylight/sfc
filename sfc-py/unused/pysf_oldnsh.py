#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import argparse
import asyncio
import sys
import struct
import socket
import binascii
from ctypes import *  # noqa


__author__ = "Jim Guichard"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "jguichar@cisco.com"
__status__ = "alpha"

"""Network Service Header (NSH) Enabled Service Function"""

try:
    import signal
except ImportError:
    signal = None


class BASEHEADER(Structure):
    _fields_ = [("version", c_ushort, 2),
                ("flags", c_ushort, 8),
                ("length", c_ushort, 6),
                ("next_protocol", c_uint, 16),
                ("service_path", c_uint, 24),
                ("service_index", c_uint, 8)]


class CONTEXTHEADER(Structure):
    _fields_ = [("network_platform", c_uint),
                ("network_shared", c_uint),
                ("service_platform", c_uint),
                ("service_shared", c_uint)]

# Decode base NSH header and context headers

base_values = BASEHEADER()
ctx_values = CONTEXTHEADER()


class MyFwService:

    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        print('\nfw service received packet from SFF:\n', addr, binascii.hexlify(data))
        rw_data = process_incoming_packet(data)
        self.transport.sendto(rw_data, addr)
        loop.stop()

    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('closing transport', exc)
        loop = asyncio.get_event_loop()
        loop.stop()


class MyNatService:

    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        print('\nnat service received packet from SFF:\n', addr, binascii.hexlify(data))
        print('\n')
        rw_data = process_incoming_packet(data)
        self.transport.sendto(rw_data, addr)
        loop.stop()

    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('closing transport', exc)
        loop = asyncio.get_event_loop()
        loop.stop()


class MyDpiService:

    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        print('\ndpi service received packet from SFF:\n', addr, binascii.hexlify(data))
        print('\n')
        rw_data = process_incoming_packet(data)
        self.transport.sendto(rw_data, addr)
        loop.stop()

    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('closing transport', exc)
        loop = asyncio.get_event_loop()
        loop.stop()


def process_incoming_packet(data):
    print('Processing recieved packet')
    rw_data = bytearray(data)
    decode_baseheader(data)
    decode_contextheader(data)
    base_values.service_index -= 1
    set_service_index(rw_data, base_values.service_index)
    return(rw_data)


def decode_baseheader(payload):
    # Base Service header
    # base_header = payload[8:17] #starts at offset 8 of payload
    base_header = payload[7:16]

    start_idx, base_values.md_type, base_values.next_protocol, path_idx = struct.unpack('!H B B I', base_header)

    base_values.version = start_idx >> 14
    base_values.flags = start_idx >> 6
    base_values.length = start_idx >> 0
    base_values.service_path = path_idx >> 8
    base_values.service_index = path_idx & 0x000000FF

    if __debug__ is False:
        print ("\nBase NSH Header Decode:")
        print (binascii.hexlify(base_header))
        # print ('NSH Version:', base_values.version)
        # print ('NSH base header flags:', base_values.flags)
        # print ('NSH base header length:', base_values.length)
        # print ('NSH MD-type:', base_values.md_type)
        # print ('NSH base header next protocol:', base_values.next_protocol)
        print ('Service Path Identifier:', base_values.service_path)
        print ('Service Index:', base_values.service_index)

# Decode the NSH context headers for a received packet at this SFF.


def decode_contextheader(payload):
    # Context header
    context_header = payload[16:32]

    ctx_values.network_platform, ctx_values.network_shared, ctx_values.service_platform, \
        ctx_values.service_shared = struct.unpack('!I I I I', context_header)

    if __debug__ is False:
        print ("\nNSH Context Header Decode:")
        print (binascii.hexlify(context_header))
        print ('Network Platform Context:', ctx_values.network_platform)
        print ('Network Shared Context:', ctx_values.network_shared)
        print ('Service Platform Context:', ctx_values.service_platform)
        print ('Service Shared Context:', ctx_values.service_shared)


def set_service_index(rw_data, service_index):
    rw_data[15] = service_index


def start_server(loop, addr, service, myip):
    t = asyncio.Task(loop.create_datagram_endpoint(
        service, local_addr=(myip, 6633)))
    loop.run_until_complete(t)
    print('Connection made with SFF:', addr)
    print('Listening for packets on port:', myip)


def find_service(service):
    if service == 'fw':
        return(MyFwService)
    elif service == 'nat':
        return(MyNatService)
    elif service == 'dpi':
        return(MyDpiService)


def get_service_ip():
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

ARGS = argparse.ArgumentParser(description="NSH Service Function")
ARGS.add_argument('--type', action="store", dest='type',
                  default=False, help='Run service function. Options: fw, nat, dpi')
ARGS.add_argument('--host', action="store", dest='host',
                  default='127.0.0.1', help='SFF host name')
ARGS.add_argument('--port', action="store", dest='port',
                  default=4789, type=int, help='SFF port number')

if __name__ == '__main__':
    args = ARGS.parse_args()
    if ':' in args.host:
        args.host, port = args.host.split(':', 1)
        args.port = int(port)

    if not args.type:
        print('Please specify --type\n')
        ARGS.print_help()
    else:
        loop = asyncio.get_event_loop()
        if signal is not None:
            loop.add_signal_handler(signal.SIGINT, loop.stop)

        if '--type' in sys.argv:
            # local_ip = get_service_ip()
            local_ip = "10.1.1.4"
            service = find_service(args.type)
            print('Starting', args.type, 'service...')
            start_server(loop, (args.host, args.port), service, local_ip)
        else:
            print('something went wrong')

        loop.run_forever()
