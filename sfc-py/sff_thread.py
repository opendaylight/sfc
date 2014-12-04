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

"""Service Function Forwarder (SFF). This SFF is spawned in a thread
   by sff_rest.py. """

import asyncio
import logging
from ctypes import *
from nsh_decode import *
from nsh_encode import *
import socket
from sff_globals import *

udpserver_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

try:
    import signal
except ImportError:
    signal = None

logger = logging.getLogger(__name__)


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
SERVICE_HOP_INVALID = 0xDEADBEEF  # Referenced service function is invalid

# Client side code: Choose values for VXLAN, base NSH and context headers as part of packet generation

vxlan_values = VXLANGPE(int('00000100', 2), 0, 0x894F, int('111111111111111111111111', 2), 64)
ctx_values = CONTEXTHEADER(0xffffffff, 0, 0xffffffff, 0)
base_values = BASEHEADER(0x1, int('01000000', 2), 0x6, 0x1, 0x1, 0x000002, 0x3)

# Server side code: Store received values for VXLAN, base NSH and context headers data structures

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


def lookup_next_sf(service_path, service_index):
    next_hop = SERVICE_HOP_INVALID
    # First we determine the list of SFs in the received packet based on SPI value extracted from packet

    #TODO more robust to keyerrors
    try:
        next_hop = data_plane_path[service_path][service_index]
    except KeyError:
        logger.error("Could not determine next service hop. SP: %d, SI: %d", service_path, service_index)
    return next_hop


def set_service_index(rw_data, service_index):
    rw_data[15] = service_index


def process_incoming_packet(data, addr):

    address = ()  # empty tuple

    logger.info("Processing packet from: %s", addr)
    # Copy payload into bytearray so it can be changed
    rw_data = bytearray(data)
    # Decode the incoming packet for debug purposes and to strip out various header values
    decode_vxlan(data, server_vxlan_values)
    decode_baseheader(data, server_base_values)
    decode_contextheader(data, server_ctx_values)
    # Lookup what to do with the packet based on Service Path Identifier (SPI)
    if server_base_values.service_index == 0:
        logger.info("End of Path")
        logger.info("Packet dump: %s", binascii.hexlify(rw_data))
        # Remove all SFC headers, leave only original packet
        rw_data = find_payload_start_index()
    else:
        logger.info("Looking up next service hop...")
        next_hop = lookup_next_sf(server_base_values.service_path, server_base_values.service_index)
        if next_hop != SERVICE_HOP_INVALID:
            address = next_hop['ip'], next_hop['port']
        else:
            # bye, bye packet
            logger.error("Dropping packet")
            logger.error("Packet dump: %s", binascii.hexlify(rw_data))
            logger.info('service index end up as: %d', server_base_values.service_index)
            rw_data.__init__()
            data = ""

    logger.info("Finished processing packet from: %s", addr)
    return rw_data, address


class MyUdpServer:
    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        logger.info('Received packet from: %s', addr)
        # Process the incoming packet
        rw_data, address = process_incoming_packet(data, addr)
        if address:
            # send the packet to the next SFF based on address
            logger.info("Sending packets to: %s", address)
            self.transport.sendto(rw_data, address)
            logger.info("listening for NSH packets ...")

    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('stop', exc)

    def error_received(self, exc):
        print('Error received:', exc)

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
        print('Control Server Received packet from:', addr)
        self.loop.call_soon_threadsafe(self.loop.stop)
        #data = data.decode('utf-8')
        #print(data_plane_path)
        #sfp_topo = json.loads(data)
        #print(sfp_topo)
        #print(sfp_topo['3']['3'])


    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('stop', exc)

    def __init__(self, loop):
        self.transport = None
        self.loop = loop


# class MyUdpClient:
#     def connection_made(self, transport):
#         self.transport = transport
#         # Building client packet to send to SFF
#         packet = build_packet(vxlan_values, base_values, ctx_values)
#         print('\nsending packet to SFF:\n', binascii.hexlify(packet))
#         # Send the packet
#         self.transport.sendto(packet)
#
#     def datagram_received(self, data, addr):
#         print('\nreceived packet from SFF:\n', binascii.hexlify(data))
#         print('\n')
#         # Decode all the headers
#         decode_vxlan(data, server_vxlan_values)
#         decode_baseheader(data, server_base_values)
#         decode_contextheader(data, server_ctx_values)
#         self.loop.stop()
#
#     def connection_refused(self, exc):
#         print('Connection refused:', exc)
#
#     def connection_lost(self, exc):
#         print('closing transport', exc)
#         self.loop = asyncio.get_event_loop()
#         self.loop.stop()
#
#     def error_received(self, exc):
#         print('Error received:', exc)
#
#     def __init__(self, loop):
#         self.transport = None
#         self.loop = loop


def start_server(loop, addr, udpserver, message):
    #t = asyncio.Task(loop.create_datagram_endpoint(
    #    lambda: udpserver, local_addr=addr))
    listen = loop.create_datagram_endpoint(lambda: udpserver, local_addr=addr)
    transport, protocol = loop.run_until_complete(listen)
    print('\nStarting Service Function Forwarder (SFF)')
    print(message, addr)
    return transport


def start_client(loop, addr, myip, udpclient):
    t = asyncio.Task(loop.create_datagram_endpoint(
        lambda: udpclient, remote_addr=addr))
    loop.run_until_complete(t)


# The python agent uses this function as the thread start whenever it wants
# to create a SFF
def start_sff(sff_name, sff_ip, sff_port, sff_control_port, sff_thread):
    print("Starting SFF thread \n")
    global udpserver_socket
    logging.basicConfig(level=logging.INFO)

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    # if signal is not None:
    # loop.add_signal_handler(signal.SIGINT, loop.stop)

    udpserver = MyUdpServer(loop)
    udpserver_transport = start_server(loop, (sff_ip, sff_port), udpserver, "Listening for NSH packets on port: ")
    udpserver_socket = udpserver_transport.get_extra_info('socket')
    sff_thread[sff_name]['socket'] = udpserver_socket
    control_udp_server = ControlUdpServer(loop)
    start_server(loop, (sff_ip, sff_control_port), control_udp_server, "Listening for Control messages on port: ")

    loop.run_forever()
    udpserver_socket.close()
    loop.close()


