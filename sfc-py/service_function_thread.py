import logging

__author__ = "Jim Guichard, Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "jguichar@cisco.com, rapenno@gmail.com"
__status__ = "alpha"

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

"""Network Service Header (NSH) Enabled Service Function. This SF is spawned in a thread
   by sfc_agent.py. """

import asyncio
from nsh_decode import *
from nsh_service_index import *

try:
    import signal
except ImportError:
    signal = None

logger = logging.getLogger(__name__)

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
        print('\nNat service received packet from SFF:\n', addr, binascii.hexlify(data))
        print('\n')
        rw_data = process_incoming_packet(data)
        self.transport.sendto(rw_data, addr)

    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('closing transport', exc)
        loop = asyncio.get_event_loop()
        loop.stop()

    def __init__(self, loop):
        self.transport = None
        self.loop = loop


class MyDpiService:
    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        print('\ndpi service received packet from SFF:\n', addr, binascii.hexlify(data))
        print('\n')
        rw_data = process_incoming_packet(data)
        self.transport.sendto(rw_data, addr)

    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('closing transport', exc)
        loop = asyncio.get_event_loop()
        loop.stop()

    def __init__(self, loop):
        self.transport = None
        self.loop = loop


class MyService:
    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        print('\nService received packet from SFF:\n', addr, binascii.hexlify(data))
        print('\n')
        rw_data = process_incoming_packet(data)
        self.transport.sendto(rw_data, addr)

    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('closing transport', exc)
        loop = asyncio.get_event_loop()
        loop.stop()

    def __init__(self, loop):
        self.transport = None
        self.loop = loop


class ControlUdpServer:
    """
    This control server class listen on a socket for commands from the main process.
    For example, if a SFF is deleted the main program can send a command to
    this data plane thread to exit.
    """
    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        logger.info('Control Server Received packet from:', addr)
        self.loop.call_soon_threadsafe(self.loop.stop)
        #data = data.decode('utf-8')
        #print(data_plane_path)
        #sfp_topo = json.loads(data)
        #print(sfp_topo)
        #print(sfp_topo['3']['3'])


    def connection_refused(self, exc):
        logger.error('Connection refused:', exc)

    def connection_lost(self, exc):
        logger.error('stop', exc)

    def __init__(self, loop):
        self.transport = None
        self.loop = loop

def process_incoming_packet(data):
    print('Processing received packet')
    rw_data = bytearray(data)
    decode_vxlan(data, server_vxlan_values) # decode vxlan-gpe header
    decode_baseheader(data, server_base_values) # decode NSH base header
    decode_contextheader(data, server_ctx_values) # decode NSH context headers
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

    if service == 'firewall':
        return MyFwService
    elif service == 'napt44':
        return MyNatService
    elif service == 'dpi':
        return MyDpiService
    elif service == 'qos':
        return MyService
    elif service == 'ids':
        return MyService
    else:
        return MyService


# The python agent uses this function as the thread start whenever it wants
# to create a SFF
def start_sf(sf_name, sf_ip, sf_port, sf_type, sf_control_port, sf_thread):
    logger.info("Starting Service Function thread \n")
    global udpserver_socket
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    logging.basicConfig(level=logging.INFO)

    service = find_service(sf_type)
    print('Starting', service, 'service...')
    udpserver = service(loop)
    udpserver_transport = start_server(loop, (sf_ip, sf_port), udpserver, "Starting new Service Function...")
    udpserver_socket = udpserver_transport.get_extra_info('socket')
    sf_thread[sf_name]['socket'] = udpserver_socket
    control_udp_server = ControlUdpServer(loop)
    start_server(loop, (sf_ip, sf_control_port), control_udp_server, "Listening for Control messages on port: ")


    loop.run_forever()
    udpserver_socket.close()
    loop.close()
