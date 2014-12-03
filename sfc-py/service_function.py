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

"""Network Service Header (NSH) Enabled Service Function"""

import argparse
import asyncio
import sys
import socket
from ctypes import *
from nsh_decode import *
from nsh_service_index import *

try:
    import signal
except ImportError:
    signal = None


class BASEHEADER(Structure):
    _fields_ = [("version", c_ushort, 2),
                ("flags", c_ushort, 8),
                ("length", c_ushort, 6),
                ("md_type", c_ubyte),
                ("next_protocol", c_ubyte),
                ("service_path", c_uint, 24),
                ("service_index", c_uint, 8)]

# Decode vxlan-gpe, base NSH header and NSH context headers
server_vxlan_values = VXLANGPE()
server_ctx_values = CONTEXTHEADER()
server_base_values = BASEHEADER()


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
    decode_vxlan(data, server_vxlan_values) # decode vxlan-gpe header
    decode_baseheader(data, server_base_values) # decode NSH base header
    decode_contextheader(data, server_ctx_values) # decode NSH context headers
    rw_data, si_result = process_service_index(rw_data, server_base_values)
    if si_result == 0:
        print('\nInvalid service index of 0 - packet will be dropped')
        rw_data.__init__()
        data = ""
    #server_base_values.service_index -= 1
    #set_service_index(rw_data, server_base_values.service_index)
    return rw_data


def set_service_index(rw_data, service_index):
    rw_data[15] = service_index


def start_server(loop, addr, service, myip):
    t = asyncio.Task(loop.create_datagram_endpoint(
        service, local_addr=(myip, 57444), remote_addr=addr))
    loop.run_until_complete(t)
    print('Connection made with SFF:', addr)


def find_service(service):
    if service == 'fw':
        return MyFwService
    elif service == 'nat':
        return MyNatService
    elif service == 'dpi':
        return MyDpiService


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
ARGS.add_argument(
    '--type', action="store", dest='type',
    default=False, help='Run service function. Options: fw, nat, dpi')
ARGS.add_argument(
    '--host', action="store", dest='host',
    default='127.0.0.1', help='SFF host name')
ARGS.add_argument(
    '--port', action="store", dest='port',
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
            local_ip = '0.0.0.0'
            service = find_service(args.type)
            print('Starting', args.type, 'service...')
            start_server(loop, (args.host, args.port), service, local_ip)
        else:
            print('something went wrong')

        loop.run_forever()