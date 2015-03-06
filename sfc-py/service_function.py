#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import argparse
import asyncio
import sys

from nsh.decode import *  # noqa
from nsh.service_index import *  # noqa
from nsh.common import *  # noqa

try:
    import signal
except ImportError:
    signal = None


__author__ = "Jim Guichard"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "jguichar@cisco.com"
__status__ = "alpha"


"""Network Service Header (NSH) Enabled Service Function"""

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
        print("Sending packets to", addr)
        self.transport.sendto(rw_data, addr)
        # loop.stop()

    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('closing transport', exc)
        loop = asyncio.get_event_loop()
        loop.stop()

    def __init__(self, loop):
        self.transport = None
        self.loop = loop


class MyNatService:
    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        print('\nnat service received packet from SFF:\n', addr, binascii.hexlify(data))
        print('\n')
        rw_data = process_incoming_packet(data)
        self.transport.sendto(rw_data, addr)
        # loop.stop()

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
        # loop.stop()

    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('closing transport', exc)
        loop = asyncio.get_event_loop()
        loop.stop()


def process_incoming_packet(data):
    print('Processing received packet')
    rw_data = bytearray(data)
    decode_vxlan(data, server_vxlan_values)  # decode vxlan-gpe header
    decode_baseheader(data, server_base_values)  # decode NSH base header
    decode_contextheader(data, server_ctx_values)  # decode NSH context headers
    rw_data, si_result = process_service_index(rw_data, server_base_values)
    return rw_data


def set_service_index(rw_data, service_index):
    rw_data[15] = service_index


# This does not work in MacOS when SFF/SF are different python
# applications on the same machine
# def start_server(loop, addr, service, myip):
#     t = asyncio.Task(loop.create_datagram_endpoint(
#         service, local_addr=(myip, 57444), remote_addr=addr))
#     loop.run_until_complete(t)
#     print('Listening for packet on:', addr)


def start_server(loop, addr, udpserver, message):
    listen = loop.create_datagram_endpoint(lambda: udpserver, local_addr=addr)
    transport, protocol = loop.run_until_complete(listen)
    print(message, addr)
    return transport


def find_service(service):
    if service == 'fw':
        return MyFwService
    elif service == 'nat':
        return MyNatService
    elif service == 'dpi':
        return MyDpiService


ARGS = argparse.ArgumentParser(description="NSH Service Function")
ARGS.add_argument(
    '--type', action="store", dest='type',
    default=False, help='Run service function. Options: fw, nat, dpi')
ARGS.add_argument(
    '--host', action="store", dest='host',
    default='0.0.0.0', help='SFF host name')
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
            udpserver = service(loop)
            start_server(loop, (args.host, args.port), udpserver, "Starting new server...")
        else:
            print('something went wrong')

        loop.run_forever()
